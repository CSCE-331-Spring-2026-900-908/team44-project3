<script lang="ts">
    import type { MenuItemGroup, MenuItemContent } from '$lib/types';
    import {
        addMenuItem,
        updateMenuItem,
        deleteMenuItem,
        getAllInventoryNames,
        getOrAddInventoryItem,
        addMenuItemContent,
        getMenuItemContents,
        replaceMenuItemContents,
        updateMenuItemGroup,
        uploadGroupImage,
        deleteGroupImage,
        menuItemImageByName,
        bumpImageVersion
    } from '$lib/api';
    import Modal from './Modal.svelte';

    interface IngredientRow {
        name: string;
        quantity: number;
    }

    interface SizeEntry {
        menuItemId: number | null;
        size: string;
        price: number;
        ingredients: IngredientRow[];
        expanded: boolean;
        ingredientsLoaded: boolean;
        ingredientsDirty: boolean;
    }

    let {
        open,
        group,
        onclose,
        onsaved
    }: {
        open: boolean;
        group: MenuItemGroup | null;
        onclose: () => void;
        onsaved: (savedName: string) => void;
    } = $props();

    let name = $state('');
    let category = $state('');
    let isHot = $state(false);
    let sizes = $state<SizeEntry[]>([]);
    let deactivatedSizes = $state<SizeEntry[]>([]);
    let inventoryNames = $state<string[]>([]);
    let error = $state('');
    let saving = $state(false);
    let imageFile = $state<File | null>(null);
    let imagePreview = $state<string | null>(null);
    let removeImage = $state(false);

    let isEdit = $derived(group !== null);

    function newSizeEntry(menuItemId: number | null, size: string, price: number): SizeEntry {
        return {
            menuItemId, size, price,
            ingredients: [], expanded: false,
            ingredientsLoaded: menuItemId === null,
            ingredientsDirty: false
        };
    }

    $effect(() => {
        if (open) {
            void loadInventoryNames();
            if (group) {
                name = group.name;
                category = group.category;
                isHot = group.isHot;
                sizes = group.variants.filter(v => v.isAvailable).map(v =>
                    newSizeEntry(v.menuItemId, v.size, v.basePrice)
                );
                deactivatedSizes = group.variants.filter(v => !v.isAvailable).map(v =>
                    newSizeEntry(v.menuItemId, v.size, v.basePrice)
                );
                imagePreview = group.hasImage ? menuItemImageByName(group.name) : null;
            } else {
                name = '';
                category = '';
                isHot = false;
                sizes = [newSizeEntry(null, 'regular', 0)];
                deactivatedSizes = [];
                imagePreview = null;
            }
            error = '';
            imageFile = null;
            removeImage = false;
        }
    });

    function handleImageSelect(e: Event) {
        const input = e.target as HTMLInputElement;
        const file = input.files?.[0] ?? null;
        imageFile = file;
        removeImage = false;
        if (file) {
            const reader = new FileReader();
            reader.onload = () => { imagePreview = reader.result as string; };
            reader.readAsDataURL(file);
        } else {
            imagePreview = group?.hasImage ? menuItemImageByName(group.name) : null;
        }
    }

    function clearImage() {
        imageFile = null;
        imagePreview = null;
        removeImage = true;
    }

    async function loadInventoryNames() {
        try {
            inventoryNames = await getAllInventoryNames();
        } catch {
            inventoryNames = [];
        }
    }

    async function addSize() {
        const persistedSource = sizes.find(s => s.menuItemId !== null);
        if (persistedSource && !persistedSource.ingredientsLoaded) {
            try {
                const contents = await getMenuItemContents(persistedSource.menuItemId!);
                persistedSource.ingredients = contents.map(c => ({ name: c.itemName, quantity: Number(c.quantity) }));
            } catch {
                persistedSource.ingredients = [];
            }
            persistedSource.ingredientsLoaded = true;
        }
        const source = persistedSource ?? sizes.find(s => s.ingredients.length > 0);
        const inherited: IngredientRow[] = source
            ? source.ingredients.map(i => ({ name: i.name, quantity: i.quantity }))
            : [];
        const entry = newSizeEntry(null, 'regular', 0);
        entry.ingredients = inherited;
        entry.ingredientsLoaded = true;
        entry.ingredientsDirty = inherited.length > 0;
        sizes = [...sizes, entry];
    }

    function removeSize(index: number) {
        const entry = sizes[index];
        if (entry && entry.menuItemId !== null) {
            deactivatedSizes = [...deactivatedSizes, entry];
        }
        sizes = sizes.filter((_, i) => i !== index);
    }

    function reactivateSize(index: number) {
        const entry = deactivatedSizes[index];
        if (entry) {
            sizes = [...sizes, entry];
            deactivatedSizes = deactivatedSizes.filter((_, i) => i !== index);
        }
    }

    async function toggleExpand(index: number) {
        const entry = sizes[index];
        if (!entry) return;
        if (!entry.expanded && !entry.ingredientsLoaded && entry.menuItemId !== null) {
            try {
                const contents = await getMenuItemContents(entry.menuItemId);
                entry.ingredients = contents.map(c => ({ name: c.itemName, quantity: Number(c.quantity) }));
            } catch {
                entry.ingredients = [];
            }
            entry.ingredientsLoaded = true;
        }
        entry.expanded = !entry.expanded;
        sizes = [...sizes];
    }

    function addSizeIngredient(sizeIndex: number) {
        const entry = sizes[sizeIndex];
        if (!entry) return;
        entry.ingredients = [...entry.ingredients, { name: '', quantity: 1 }];
        entry.ingredientsDirty = true;
        sizes = [...sizes];
    }

    function removeSizeIngredient(sizeIndex: number, ingIndex: number) {
        const entry = sizes[sizeIndex];
        if (!entry) return;
        entry.ingredients = entry.ingredients.filter((_, i) => i !== ingIndex);
        entry.ingredientsDirty = true;
        sizes = [...sizes];
    }

    function markDirty(sizeIndex: number) {
        const entry = sizes[sizeIndex];
        if (!entry) return;
        entry.ingredientsDirty = true;
        sizes = [...sizes];
    }

    async function saveIngredients(menuItemId: number, entry: SizeEntry) {
        const contentList: MenuItemContent[] = [];
        for (const ing of entry.ingredients) {
            if (!ing.name) continue;
            const invId = await getOrAddInventoryItem(ing.name, 'ingredient', 0, 'units', 10, 0);
            contentList.push({ menuItemId, inventoryId: invId, quantity: ing.quantity });
        }
        if (entry.menuItemId !== null) {
            await replaceMenuItemContents(menuItemId, contentList);
        } else {
            for (const c of contentList) {
                await addMenuItemContent(c);
            }
        }
    }

    async function save() {
        if (!name || !category) {
            error = 'Name and category are required.';
            return;
        }
        if (sizes.length === 0 && deactivatedSizes.length === 0) {
            error = 'At least one size is required.';
            return;
        }

        saving = true;
        error = '';
        try {
            if (group) {
                await updateMenuItemGroup(group.name, { name, category, isHot });

                for (const entry of sizes) {
                    if (entry.menuItemId !== null) {
                        await updateMenuItem({
                            menuItemId: entry.menuItemId,
                            name, category,
                            size: entry.size,
                            basePrice: entry.price,
                            isAvailable: true, isHot,
                            hasImage: false
                        });
                        if (entry.ingredientsDirty) {
                            await saveIngredients(entry.menuItemId, entry);
                        }
                    } else {
                        const newId = await addMenuItem({ name, category, size: entry.size, basePrice: entry.price, isAvailable: true, isHot });
                        if (entry.ingredients.length > 0) {
                            await saveIngredients(newId, entry);
                        }
                    }
                }

                for (const entry of deactivatedSizes) {
                    if (entry.menuItemId !== null) {
                        await updateMenuItem({
                            menuItemId: entry.menuItemId,
                            name, category,
                            size: entry.size,
                            basePrice: entry.price,
                            isAvailable: false, isHot,
                            hasImage: false
                        });
                    }
                }

                const allKeptIds = new Set([
                    ...sizes.filter(s => s.menuItemId !== null).map(s => s.menuItemId),
                    ...deactivatedSizes.filter(s => s.menuItemId !== null).map(s => s.menuItemId)
                ]);
                for (const variant of group.variants) {
                    if (!allKeptIds.has(variant.menuItemId)) {
                        await deleteMenuItem(variant.menuItemId);
                    }
                }

                if (imageFile) {
                    await uploadGroupImage(name, imageFile);
                    bumpImageVersion();
                } else if (removeImage) {
                    await deleteGroupImage(name);
                    bumpImageVersion();
                }
            } else {
                for (const entry of sizes) {
                    const id = await addMenuItem({
                        name, category,
                        size: entry.size,
                        basePrice: entry.price,
                        isAvailable: true, isHot
                    });
                    if (entry.ingredients.length > 0) {
                        await saveIngredients(id, entry);
                    }
                }
                if (imageFile) {
                    await uploadGroupImage(name, imageFile);
                    bumpImageVersion();
                }
            }
            onsaved(name.toLowerCase());
            onclose();
        } catch {
            error = 'Failed to save menu item.';
        } finally {
            saving = false;
        }
    }
</script>

<Modal {open} title={isEdit ? 'Edit Menu Item' : 'Add Menu Item'} {onclose} wide>
    <form class="menu-form" onsubmit={(e) => { e.preventDefault(); void save(); }}>
        <div class="form-grid">
            <label for="mi-name">Name</label>
            <input id="mi-name" bind:value={name} />

            <label for="mi-category">Category</label>
            <input id="mi-category" bind:value={category} />

            <label>Serve Temperature</label>
            <div class="temp-row">
                <button type="button" class="temp-btn temp-cold" class:selected={!isHot} onclick={() => (isHot = false)}>Cold</button>
                <button type="button" class="temp-btn temp-hot" class:selected={isHot} onclick={() => (isHot = true)}>Hot</button>
            </div>
        </div>

        <section class="sizes-section">
            <div class="section-header">
                <h4>Sizes &amp; Pricing</h4>
                <button type="button" class="btn-ghost" onclick={() => { void addSize(); }}>+ Add Size</button>
            </div>
            {#each sizes as entry, i (i)}
                <div class="size-block">
                    <div class="size-row">
                        <button type="button" class="expand-btn" onclick={() => { void toggleExpand(i); }}>
                            <svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" class:rotated={entry.expanded}>
                                <polyline points="6 4 10 8 6 12" />
                            </svg>
                        </button>
                        <select bind:value={entry.size}>
                            <option value="regular">Regular</option>
                            <option value="Small">Small</option>
                            <option value="Medium">Medium</option>
                            <option value="Large">Large</option>
                        </select>
                        <div class="price-input-wrap">
                            <span class="price-prefix">$</span>
                            <input type="number" step="0.01" min="0" bind:value={entry.price} class="price-input" />
                        </div>
                        <button
                            type="button"
                            class="remove-size-btn"
                            disabled={sizes.length <= 1 && !entry.menuItemId}
                            onclick={() => { removeSize(i); }}
                        >
                            <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                                <line x1="4" y1="4" x2="12" y2="12" />
                                <line x1="12" y1="4" x2="4" y2="12" />
                            </svg>
                        </button>
                    </div>
                    {#if entry.expanded}
                        <div class="size-ingredients">
                            <div class="ing-header">
                                <span class="ing-label">Ingredients</span>
                                <button type="button" class="btn-ghost" onclick={() => { addSizeIngredient(i); }}>+ Add</button>
                            </div>
                            {#if entry.ingredients.length === 0}
                                <p class="empty-ing">No ingredients linked.</p>
                            {:else}
                                {#each entry.ingredients as ing, j (j)}
                                    <div class="ingredient-row">
                                        <input list="inv-names" placeholder="Ingredient name" bind:value={ing.name} onchange={() => { markDirty(i); }} />
                                        <input type="number" min="0.01" step="0.01" placeholder="Qty" bind:value={ing.quantity} onchange={() => { markDirty(i); }} class="qty-input" />
                                        <button type="button" class="btn-ghost remove-ing" onclick={() => { removeSizeIngredient(i, j); }}>&times;</button>
                                    </div>
                                {/each}
                            {/if}
                        </div>
                    {/if}
                </div>
            {/each}
        </section>

        <datalist id="inv-names">
            {#each inventoryNames as invName (invName)}
                <option value={invName}></option>
            {/each}
        </datalist>

        {#if deactivatedSizes.length > 0}
            <section class="deactivated-section">
                <div class="section-header">
                    <h4>Deactivated Sizes</h4>
                </div>
                {#each deactivatedSizes as entry, i (i)}
                    <div class="size-row deactivated-row">
                        <span class="deactivated-size">{entry.size}</span>
                        <span class="deactivated-price">${entry.price.toFixed(2)}</span>
                        <button type="button" class="reactivate-btn" onclick={() => { reactivateSize(i); }}>Reactivate</button>
                    </div>
                {/each}
            </section>
        {/if}

        <section class="image-section">
            <div class="section-header">
                <h4>Image</h4>
            </div>
            <div class="image-upload-area">
                {#if imagePreview}
                    <div class="image-preview">
                        <img src={imagePreview} alt="Preview" />
                        <button type="button" class="remove-img" onclick={clearImage}>
                            <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                                <line x1="4" y1="4" x2="12" y2="12" />
                                <line x1="12" y1="4" x2="4" y2="12" />
                            </svg>
                        </button>
                    </div>
                {:else}
                    <label class="upload-placeholder" for="mi-image">
                        <span class="upload-emoji">🧋</span>
                        <span class="upload-text">Click to upload image</span>
                    </label>
                {/if}
                <input id="mi-image" type="file" accept="image/*" class="file-input" onchange={handleImageSelect} />
            </div>
        </section>

        {#if error}
            <p class="error-text">{error}</p>
        {/if}

        <div class="actions">
            <button type="button" class="btn-secondary" onclick={onclose}>Cancel</button>
            <button type="submit" class="btn-primary" disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
            </button>
        </div>
    </form>
</Modal>

<style>
    .menu-form {
        display: flex;
        flex-direction: column;
        gap: 1.25rem;
    }

    .sizes-section,
    .image-section,
    .deactivated-section {
        border-top: 1px solid var(--color-border);
        padding-top: 1rem;
    }

    .section-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 0.75rem;
    }

    .section-header h4 {
        font-size: 0.875rem;
        font-weight: 600;
    }

    .size-block {
        margin-bottom: 0.5rem;
    }

    .size-row {
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    .size-row select {
        flex: 1;
    }

    .expand-btn {
        width: 24px;
        height: 24px;
        border-radius: 4px;
        border: none;
        background: transparent;
        color: var(--color-text-muted);
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        flex-shrink: 0;
        padding: 0;
    }

    .expand-btn:hover {
        background: var(--color-border);
    }

    :global(.expand-btn svg.rotated) {
        transform: rotate(90deg);
    }

    .size-ingredients {
        margin: 0.35rem 0 0.25rem 12px;
        padding: 0.5rem 0.75rem;
        background: var(--color-bg, #f8f5f0);
        border-radius: 0 var(--radius) var(--radius) var(--radius);
        border-left: 2px solid var(--color-border);
    }

    .ing-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 0.35rem;
    }

    .ing-label {
        font-size: 0.7rem;
        font-weight: 600;
        color: var(--color-text-muted);
        text-transform: uppercase;
        letter-spacing: 0.04em;
    }

    .empty-ing {
        font-size: 0.75rem;
        color: var(--color-text-muted);
        font-style: italic;
        margin: 0;
    }

    .ingredient-row {
        display: flex;
        align-items: center;
        gap: 0.35rem;
        margin-bottom: 0.3rem;
    }

    .ingredient-row input:first-child {
        flex: 1;
        font-size: 0.85rem;
    }

    .qty-input {
        width: 80px;
        font-size: 0.85rem;
    }

    .remove-ing {
        color: var(--color-danger);
        font-size: 1.1rem;
        padding: 0 0.15rem;
        opacity: 0.5;
    }

    .remove-ing:hover {
        opacity: 1;
    }

    .price-input-wrap {
        display: flex;
        align-items: center;
        gap: 0.15rem;
    }

    .price-prefix {
        font-weight: 600;
        color: var(--color-text-muted);
    }

    .price-input {
        width: 80px;
    }

    .remove-size-btn {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: 1px solid var(--color-border);
        background: transparent;
        color: var(--color-danger, #e74c3c);
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        flex-shrink: 0;
        padding: 0;
    }

    .remove-size-btn:disabled {
        opacity: 0.2;
        cursor: not-allowed;
    }

    .remove-size-btn:not(:disabled):hover {
        background: var(--color-danger, #e74c3c);
        border-color: var(--color-danger, #e74c3c);
        color: white;
    }

    .deactivated-row {
        opacity: 0.6;
    }

    .deactivated-size {
        flex: 1;
        font-size: 0.875rem;
        text-transform: capitalize;
    }

    .deactivated-price {
        font-size: 0.875rem;
        color: var(--color-text-muted);
        width: 80px;
    }

    .reactivate-btn {
        padding: 0.25rem 0.6rem;
        border-radius: var(--radius);
        border: 1px solid var(--color-primary, #d4712a);
        background: transparent;
        color: var(--color-primary, #d4712a);
        font-size: 0.75rem;
        font-weight: 600;
        cursor: pointer;
    }

    .reactivate-btn:hover {
        background: var(--color-primary, #d4712a);
        color: white;
    }

    .temp-row {
        display: flex;
        gap: 0.5rem;
    }

    .temp-btn {
        flex: 1;
        padding: 0.5rem 1rem;
        border-radius: var(--radius);
        border: 2px solid var(--color-border);
        background: var(--color-surface);
        font-size: 0.875rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.15s;
    }

    .temp-btn.temp-cold.selected {
        background: #e3f2fd;
        border-color: #1976d2;
        color: #1565c0;
    }

    .temp-btn.temp-hot.selected {
        background: #fbe9e7;
        border-color: #e64a19;
        color: #d84315;
    }

    .temp-btn:not(.selected):hover {
        border-color: #999;
    }

    .image-upload-area {
        position: relative;
    }

    .file-input {
        display: none;
    }

    .upload-placeholder {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        width: 100%;
        height: 120px;
        border: 2px dashed var(--color-border);
        border-radius: var(--radius);
        cursor: pointer;
        transition: border-color 0.15s;
    }

    .upload-placeholder:hover {
        border-color: var(--color-primary, #d4712a);
    }

    .upload-emoji {
        font-size: 2rem;
    }

    .upload-text {
        font-size: 0.8rem;
        color: var(--color-text-muted);
    }

    .image-preview {
        position: relative;
        width: 120px;
        height: 150px;
        border-radius: var(--radius);
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .image-preview img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
    }

    .remove-img {
        position: absolute;
        top: 4px;
        right: 4px;
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: rgba(0, 0, 0, 0.6);
        color: white;
        font-size: 1rem;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0;
        cursor: pointer;
        border: none;
    }

    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
    }
</style>
