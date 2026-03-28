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
        onclose,
        onsaved
    }: {
        open: boolean;
        item: MenuItem | null;
        onclose: () => void;
        onsaved: () => void;
    } = $props();

    let name = $state('');
    let category = $state('');
    let size = $state('Medium');
    let basePrice = $state(0);
    let isAvailable = $state(true);
    let ingredients = $state<IngredientRow[]>([]);
    let inventoryNames = $state<string[]>([]);
    let error = $state('');
    let saving = $state(false);

    let isEdit = $derived(item !== null);

    $effect(() => {
        if (open) {
            void loadInventoryNames();
            if (item) {
                name = item.name;
                category = item.category;
                size = item.size;
                basePrice = item.basePrice;
                isAvailable = item.isAvailable;
            } else {
                name = '';
                category = '';
                size = 'Medium';
                basePrice = 0;
                isAvailable = true;
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
                    isAvailable
                });
            } else {
                const newId = await addMenuItem({
                    name,
                    category,
                    size,
                    basePrice,
                    isAvailable
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

<Modal {open} title={isEdit ? 'Edit Menu Item' : 'Add Menu Item'} {onclose} wide>
    <form class="menu-form" onsubmit={save}>
        <div class="form-grid">
            <label for="mi-name">Name</label>
            <input id="mi-name" bind:value={name} />

            <label for="mi-category">Category</label>
            <input id="mi-category" bind:value={category} />

            <label for="mi-size">Size</label>
            <select id="mi-size" bind:value={size}>
                <option>Small</option>
                <option>Medium</option>
                <option>Large</option>
            </select>

            <label for="mi-price">Base Price</label>
            <input id="mi-price" type="number" step="0.01" min="0" bind:value={basePrice} />

            <label for="mi-avail">Available</label>
            <input id="mi-avail" type="checkbox" bind:checked={isAvailable} />
        </div>

        {#if !isEdit}
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

    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
    }
</style>
