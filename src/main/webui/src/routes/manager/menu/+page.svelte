<script lang="ts">
    import type { MenuItem } from '$lib/types';
    import { getAllMenuItems, deleteMenuItem } from '$lib/api';
    import { formatCurrency, toTitleCase } from '$lib/utils';
    import MenuItemForm from '$lib/components/MenuItemForm.svelte';

    let items = $state<MenuItem[]>([]);
    let loading = $state(true);
    let showForm = $state(false);
    let editingItem = $state<MenuItem | null>(null);
    let sizeVariantOf = $state<MenuItem | null>(null);

    $effect(() => {
        void loadItems();
    });

    async function loadItems() {
        loading = true;
        try {
            items = await getAllMenuItems();
        } catch {
            items = [];
        } finally {
            loading = false;
        }
    }

    function openAdd() {
        editingItem = null;
        sizeVariantOf = null;
        showForm = true;
    }

    function openEdit(item: MenuItem) {
        editingItem = item;
        sizeVariantOf = null;
        showForm = true;
    }

    function openAddSize(item: MenuItem) {
        editingItem = null;
        sizeVariantOf = item;
        showForm = true;
    }

    async function handleDelete(item: MenuItem) {
        if (!confirm(`Delete "${item.name}"?`)) return;
        try {
            await deleteMenuItem(item.menuItemId);
            await loadItems();
        } catch {
            alert('Failed to delete menu item.');
        }
    }
</script>

<div class="page-header">
    <h1 class="page-title">Menu Items</h1>
    <button class="btn-primary" onclick={openAdd}>+ Add Item</button>
</div>

{#if loading}
    <p>Loading...</p>
{:else}
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Category</th>
                <th>Size</th>
                <th>Price</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            {#each items as item (item.menuItemId)}
                <tr>
                    <td>{item.menuItemId}</td>
                    <td>{item.name}</td>
                    <td>{toTitleCase(item.category)}</td>
                    <td>{item.size}</td>
                    <td>{formatCurrency(item.basePrice)}</td>
                    <td>
                        <span
                            class="badge"
                            class:badge-success={item.isAvailable}
                            class:badge-danger={!item.isAvailable}
                        >
                            {item.isAvailable ? 'Available' : 'Unavailable'}
                        </span>
                    </td>
                    <td class="action-cell">
                        <button class="btn-ghost" onclick={() => { openEdit(item); }}>Edit</button>
                        <button class="btn-ghost" onclick={() => { openAddSize(item); }}>+ Size</button>
                        <button class="btn-ghost danger-text" onclick={() => { void handleDelete(item); }}>
                            Delete
                        </button>
                    </td>
                </tr>
            {/each}
        </tbody>
    </table>
{/if}

<MenuItemForm
    open={showForm}
    item={editingItem}
    {sizeVariantOf}
    onclose={() => { showForm = false; sizeVariantOf = null; }}
    onsaved={loadItems}
/>

<style>
    .action-cell {
        display: flex;
        gap: 0.25rem;
    }

    .danger-text {
        color: var(--color-danger);
    }
</style>
