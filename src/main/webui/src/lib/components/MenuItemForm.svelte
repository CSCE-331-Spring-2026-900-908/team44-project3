<script lang="ts">
    import type { MenuItem } from '$lib/types';
    import {
        addMenuItem,
        updateMenuItem,
        getAllInventoryNames,
        getOrAddInventoryItem,
        addMenuItemContent,
        uploadMenuItemImage,
        deleteMenuItemImage,
        menuItemImageUrl
    } from '$lib/api';
    import Modal from './Modal.svelte';

    interface IngredientRow {
        name: string;
        quantity: number;
    }

    let {
        open,
        item,
        sizeVariantOf = null,
        onclose,
        onsaved
    }: {
        open: boolean;
        item: MenuItem | null;
        sizeVariantOf?: MenuItem | null;
        onclose: () => void;
        onsaved: () => void;
    } = $props();

    let name = $state('');
    let category = $state('');
    let size = $state('regular');
    let basePrice = $state(0);
    let isAvailable = $state(true);
    let isHot = $state(false);
    let ingredients = $state<IngredientRow[]>([]);
    let inventoryNames = $state<string[]>([]);
    let error = $state('');
    let saving = $state(false);
    let imageFile = $state<File | null>(null);
    let imagePreview = $state<string | null>(null);
    let removeImage = $state(false);

    let isEdit = $derived(item !== null && !sizeVariantOf);
    let isSizeVariant = $derived(sizeVariantOf !== null);

    $effect(() => {
        if (open) {
            void loadInventoryNames();
            if (sizeVariantOf) {
                name = sizeVariantOf.name;
                category = sizeVariantOf.category;
                isHot = sizeVariantOf.isHot;
                isAvailable = true;
                size = 'regular';
                basePrice = sizeVariantOf.basePrice;
            } else if (item) {
                name = item.name;
                category = item.category;
                size = item.size;
                basePrice = item.basePrice;
                isAvailable = item.isAvailable;
                isHot = item.isHot;
            } else {
                name = '';
                category = '';
                size = 'regular';
                basePrice = 0;
                isAvailable = true;
                isHot = false;
            }
            ingredients = [];
            error = '';
            imageFile = null;
            imagePreview = item?.hasImage ? menuItemImageUrl(item.menuItemId) : null;
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
            imagePreview = item?.hasImage ? menuItemImageUrl(item.menuItemId) : null;
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

    function addIngredient() {
        ingredients = [...ingredients, { name: '', quantity: 1 }];
    }

    function removeIngredient(index: number) {
        ingredients = ingredients.filter((_, i) => i !== index);
    }

    async function save() {
        if (!name || !category) {
            error = 'Name and category are required.';
            return;
        }

        saving = true;
        error = '';
        try {
            let itemId: number;
            if (isEdit && item) {
                itemId = item.menuItemId;
                await updateMenuItem({
                    ...item,
                    name,
                    category,
                    size,
                    basePrice,
                    isAvailable,
                    isHot
                });
            } else {
                itemId = await addMenuItem({
                    name,
                    category,
                    size,
                    basePrice,
                    isAvailable,
                    isHot
                });
                for (const ing of ingredients) {
                    if (!ing.name) continue;
                    const invId = await getOrAddInventoryItem(
                        ing.name, 'ingredient', 0, 'units', 10, 0
                    );
                    await addMenuItemContent({
                        menuItemId: itemId,
                        inventoryId: invId,
                        recipeQuantity: ing.quantity
                    });
                }
            }
            if (imageFile) {
                await uploadMenuItemImage(itemId, imageFile);
            } else if (removeImage && isEdit) {
                await deleteMenuItemImage(itemId);
            }
            onsaved();
            onclose();
        } catch {
            error = 'Failed to save menu item.';
        } finally {
            saving = false;
        }
    }
</script>

<Modal {open} title={isSizeVariant ? 'Add Size Variant' : isEdit ? 'Edit Menu Item' : 'Add Menu Item'} {onclose} wide>
    <form class="menu-form" onsubmit={(e) => { e.preventDefault(); void save(); }}>
        <div class="form-grid">
            <label for="mi-name">Name</label>
            <input id="mi-name" bind:value={name} disabled={isSizeVariant} />

            <label for="mi-category">Category</label>
            <input id="mi-category" bind:value={category} disabled={isSizeVariant} />

            <label for="mi-size">Size</label>
            <select id="mi-size" bind:value={size}>
                <option value="regular">Regular</option>
                <option value="Small">Small</option>
                <option value="Medium">Medium</option>
                <option value="Large">Large</option>
            </select>

            <label for="mi-price">Base Price</label>
            <input id="mi-price" type="number" step="0.01" min="0" bind:value={basePrice} />

            <label for="mi-avail">Available</label>
            <input id="mi-avail" type="checkbox" bind:checked={isAvailable} />

            <label>Serve Temperature</label>
            <div class="temp-row">
                <button
                    type="button"
                    class="temp-btn temp-cold"
                    class:selected={!isHot}
                    onclick={() => (isHot = false)}
                >
                    Cold
                </button>
                <button
                    type="button"
                    class="temp-btn temp-hot"
                    class:selected={isHot}
                    onclick={() => (isHot = true)}
                >
                    Hot
                </button>
            </div>
        </div>

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
                <input
                    id="mi-image"
                    type="file"
                    accept="image/*"
                    class="file-input"
                    onchange={handleImageSelect}
                />
            </div>
        </section>

        {#if !isEdit || isSizeVariant}
            <section class="ingredients-section">
                <div class="section-header">
                    <h4>Ingredients</h4>
                    <button type="button" class="btn-ghost" onclick={addIngredient}>
                        + Add Ingredient
                    </button>
                </div>
                {#each ingredients as ing, i (i)}
                    <div class="ingredient-row">
                        <input
                            list="inv-names"
                            placeholder="Ingredient name"
                            bind:value={ing.name}
                        />
                        <input
                            type="number"
                            min="1"
                            placeholder="Qty"
                            bind:value={ing.quantity}
                            class="qty-input"
                        />
                        <button
                            type="button"
                            class="btn-ghost remove-ing"
                            onclick={() => { removeIngredient(i); }}
                        >
                            &times;
                        </button>
                    </div>
                {/each}
                <datalist id="inv-names">
                    {#each inventoryNames as invName (invName)}
                        <option value={invName}></option>
                    {/each}
                </datalist>
            </section>
        {/if}

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

    .ingredients-section {
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

    .ingredient-row {
        display: flex;
        gap: 0.5rem;
        margin-bottom: 0.5rem;
    }

    .ingredient-row input:first-child {
        flex: 1;
    }

    .qty-input {
        width: 80px;
    }

    .remove-ing {
        color: var(--color-danger);
        font-size: 1.25rem;
        padding: 0 0.25rem;
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

    .image-section {
        border-top: 1px solid var(--color-border);
        padding-top: 1rem;
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
