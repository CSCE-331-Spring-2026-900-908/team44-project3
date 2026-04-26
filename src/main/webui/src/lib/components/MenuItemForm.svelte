<script lang="ts">
    import type { MenuItem } from '$lib/types';
    import {
        addMenuItem,
        updateMenuItem,
        getAllInventoryNames,
        getOrAddInventoryItem,
        addMenuItemContent
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
        }
    });

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
            if (isEdit && item) {
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
                const newId = await addMenuItem({
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
                        menuItemId: newId,
                        inventoryId: invId,
                        recipeQuantity: ing.quantity
                    });
                }
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

    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
    }
</style>
