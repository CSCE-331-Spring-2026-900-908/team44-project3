<script lang="ts">
    import type { Inventory } from '$lib/types';
    import {
        getAllInventory,
        updateStock,
        addInventoryItem,
        deleteInventoryItem,
        createRestockOrder
    } from '$lib/api';
    import { getEmployeeId } from '$lib/stores/auth.svelte';
    import { formatCurrency } from '$lib/utils';

    type Tab = 'stock' | 'take' | 'refill';

    let activeTab = $state<Tab>('stock');
    let inventory = $state<Inventory[]>([]);
    let loading = $state(true);

    let selectedId = $state(0);
    let newQuantity = $state(0);
    let updateMsg = $state('');

    let newItemName = $state('');
    let newItemCategory = $state('');
    let newItemQty = $state(0);
    let newItemUnit = $state('');
    let newItemThreshold = $state(10);
    let newItemCost = $state(0);
    let showAddForm = $state(false);

    let restockQty = $state(0);
    let restockMsg = $state('');

    let lowStockItems = $derived(inventory.filter((i) => i.currentQuantity <= i.reorderThreshold));

    $effect(() => {
        void loadInventory();
    });

    async function loadInventory() {
        loading = true;
        try {
            inventory = await getAllInventory();
        } catch {
            inventory = [];
        } finally {
            loading = false;
        }
    }

    async function handleUpdateStock() {
        if (!selectedId) return;
        updateMsg = '';
        try {
            await updateStock(selectedId, newQuantity);
            await loadInventory();
            updateMsg = 'Stock updated successfully.';
        } catch {
            updateMsg = 'Failed to update stock.';
        }
    }

    async function handleAddItem() {
        if (!newItemName || !newItemUnit) return;
        try {
            await addInventoryItem({
                itemName: newItemName,
                category: newItemCategory,
                currentQuantity: newItemQty,
                unit: newItemUnit,
                reorderThreshold: newItemThreshold,
                supplierCost: newItemCost,
                lastRestocked: null
            });
            showAddForm = false;
            newItemName = '';
            newItemCategory = '';
            newItemQty = 0;
            newItemUnit = '';
            newItemThreshold = 10;
            newItemCost = 0;
            await loadInventory();
        } catch {
            alert('Failed to add item.');
        }
    }

    async function handleDelete(item: Inventory) {
        if (!confirm(`Delete "${item.itemName}"?`)) return;
        try {
            await deleteInventoryItem(item.inventoryId);
            await loadInventory();
        } catch {
            alert('Failed to delete item.');
        }
    }

    async function handleRestock(item: Inventory) {
        if (restockQty <= 0) return;
        restockMsg = '';
        try {
            await createRestockOrder(getEmployeeId(), item.inventoryId, restockQty);
            restockMsg = `Restock order created for ${item.itemName}.`;
            restockQty = 0;
            await loadInventory();
        } catch {
            restockMsg = 'Failed to create restock order.';
        }
    }
</script>

<div class="page-header">
    <h1 class="page-title">Inventory</h1>
</div>

<div class="tabs">
    <button class="tab" class:active={activeTab === 'stock'} onclick={() => (activeTab = 'stock')}>
        Current Stock
    </button>
    <button class="tab" class:active={activeTab === 'take'} onclick={() => (activeTab = 'take')}>
        Take Inventory
    </button>
    <button class="tab" class:active={activeTab === 'refill'} onclick={() => (activeTab = 'refill')}>
        Refill Orders
        {#if lowStockItems.length > 0}
            <span class="badge badge-warning">{lowStockItems.length}</span>
        {/if}
    </button>
</div>

{#if loading}
    <p>Loading...</p>
{:else if activeTab === 'stock'}
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Item</th>
                <th>Category</th>
                <th>Quantity</th>
                <th>Unit</th>
                <th>Reorder At</th>
                <th>Cost</th>
                <th>Last Restocked</th>
            </tr>
        </thead>
        <tbody>
            {#each inventory as item (item.inventoryId)}
                <tr class:low-stock={item.currentQuantity <= item.reorderThreshold}>
                    <td>{item.inventoryId}</td>
                    <td>{item.itemName}</td>
                    <td>{item.category}</td>
                    <td>{item.currentQuantity}</td>
                    <td>{item.unit}</td>
                    <td>{item.reorderThreshold}</td>
                    <td>{formatCurrency(item.supplierCost)}</td>
                    <td>{item.lastRestocked ?? 'Never'}</td>
                </tr>
            {/each}
        </tbody>
    </table>

{:else if activeTab === 'take'}
    <div class="toolbar">
        <button class="btn-primary" onclick={() => (showAddForm = !showAddForm)}>
            {showAddForm ? 'Cancel' : '+ Add Item'}
        </button>
    </div>

    {#if showAddForm}
        <div class="card add-form">
            <div class="form-grid">
                <label for="ni-name">Name</label>
                <input id="ni-name" bind:value={newItemName} />
                <label for="ni-cat">Category</label>
                <input id="ni-cat" bind:value={newItemCategory} />
                <label for="ni-qty">Quantity</label>
                <input id="ni-qty" type="number" bind:value={newItemQty} />
                <label for="ni-unit">Unit</label>
                <input id="ni-unit" bind:value={newItemUnit} />
                <label for="ni-thresh">Reorder Threshold</label>
                <input id="ni-thresh" type="number" bind:value={newItemThreshold} />
                <label for="ni-cost">Supplier Cost</label>
                <input id="ni-cost" type="number" step="0.01" bind:value={newItemCost} />
            </div>
            <button class="btn-primary" onclick={handleAddItem} style="margin-top:0.75rem">
                Save New Item
            </button>
        </div>
    {/if}

    <table>
        <thead>
            <tr>
                <th>Item</th>
                <th>Quantity</th>
                <th>Unit</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            {#each inventory as item (item.inventoryId)}
                <tr>
                    <td>{item.itemName}</td>
                    <td>{item.currentQuantity}</td>
                    <td>{item.unit}</td>
                    <td class="action-cell">
                        <button class="btn-ghost danger-text" onclick={() => handleDelete(item)}>
                            Delete
                        </button>
                    </td>
                </tr>
            {/each}
        </tbody>
    </table>

    <div class="card update-section">
        <h4>Update Stock</h4>
        <div class="update-row">
            <select bind:value={selectedId}>
                <option value={0}>Select an item...</option>
                {#each inventory as item (item.inventoryId)}
                    <option value={item.inventoryId}>{item.itemName}</option>
                {/each}
            </select>
            <input type="number" min="0" bind:value={newQuantity} placeholder="New qty" />
            <button class="btn-primary" onclick={handleUpdateStock}>Update</button>
        </div>
        {#if updateMsg}
            <p class="success-text">{updateMsg}</p>
        {/if}
    </div>

{:else}
    <table>
        <thead>
            <tr>
                <th>Item</th>
                <th>Current Qty</th>
                <th>Reorder Threshold</th>
                <th>Unit</th>
                <th>Restock</th>
            </tr>
        </thead>
        <tbody>
            {#each lowStockItems as item (item.inventoryId)}
                <tr>
                    <td>{item.itemName}</td>
                    <td class="low-qty">{item.currentQuantity}</td>
                    <td>{item.reorderThreshold}</td>
                    <td>{item.unit}</td>
                    <td class="restock-cell">
                        <input
                            type="number"
                            min="1"
                            bind:value={restockQty}
                            placeholder="Qty"
                            class="restock-input"
                        />
                        <button class="btn-primary" onclick={() => handleRestock(item)}>
                            Order
                        </button>
                    </td>
                </tr>
            {/each}
        </tbody>
    </table>

    {#if lowStockItems.length === 0}
        <p class="no-data">All items are above reorder threshold.</p>
    {/if}

    {#if restockMsg}
        <p class="success-text">{restockMsg}</p>
    {/if}
{/if}

<style>
    .tabs {
        display: flex;
        gap: 0;
        margin-bottom: 1.5rem;
        border-bottom: 2px solid var(--color-border);
    }

    .tab {
        padding: 0.6rem 1.25rem;
        background: transparent;
        border-radius: 0;
        font-weight: 500;
        color: var(--color-text-muted);
        border-bottom: 2px solid transparent;
        margin-bottom: -2px;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    .tab:hover {
        color: var(--color-text);
    }

    .tab.active {
        color: var(--color-primary);
        border-bottom-color: var(--color-primary);
    }

    .low-stock {
        background: #fef3c7;
    }

    .low-qty {
        color: var(--color-danger);
        font-weight: 600;
    }

    .add-form {
        margin-bottom: 1rem;
    }

    .update-section {
        margin-top: 1.5rem;
    }

    .update-section h4 {
        margin-bottom: 0.75rem;
        font-size: 0.875rem;
        font-weight: 600;
    }

    .update-row {
        display: flex;
        gap: 0.5rem;
        align-items: center;
    }

    .update-row select {
        width: auto;
        min-width: 200px;
    }

    .update-row input {
        width: 100px;
    }

    .action-cell {
        display: flex;
        gap: 0.25rem;
    }

    .danger-text {
        color: var(--color-danger);
    }

    .restock-cell {
        display: flex;
        gap: 0.5rem;
        align-items: center;
    }

    .restock-input {
        width: 80px;
    }

    .no-data {
        text-align: center;
        color: var(--color-text-muted);
        padding: 2rem;
    }
</style>
