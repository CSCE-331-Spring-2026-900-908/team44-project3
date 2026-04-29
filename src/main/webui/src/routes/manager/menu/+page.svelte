<script lang="ts">
    import type { MenuItem, MenuItemGroup } from '$lib/types';
    import { getAllMenuItemsForManager, deleteMenuItem } from '$lib/api';
    import { formatCurrency, toTitleCase, compareSizes } from '$lib/utils';
    import MenuItemForm from '$lib/components/MenuItemForm.svelte';

    let items = $state<MenuItem[]>([]);
    let loading = $state(true);
    let showForm = $state(false);
    let editingGroup = $state<MenuItemGroup | null>(null);
    let expandedGroups = $state<Set<string>>(new Set());

    function sortVariants(variants: MenuItem[]): MenuItem[] {
        return [...variants].sort((a, b) => compareSizes(a.size, b.size));
    }

    let groups = $derived((() => {
        const map = new Map<string, MenuItem[]>();
        for (const item of items) {
            const existing = map.get(item.name);
            if (existing) existing.push(item);
            else map.set(item.name, [item]);
        }
        const result: MenuItemGroup[] = [];
        for (const [name, variants] of map) {
            const sorted = sortVariants(variants);
            const first = sorted[0]!;
            result.push({
                name,
                category: first.category,
                isHot: first.isHot,
                isAvailable: sorted.some(v => v.isAvailable),
                hasImage: sorted.some(v => v.hasImage),
                variants: sorted,
                minPrice: Math.min(...sorted.map(v => v.basePrice)),
                maxPrice: Math.max(...sorted.map(v => v.basePrice))
            });
        }
        return result;
    })());

    $effect(() => {
        void loadItems();
    });

    async function loadItems() {
        const scrollY = window.scrollY;
        const isRefresh = items.length > 0;
        if (!isRefresh) loading = true;
        try {
            items = await getAllMenuItemsForManager();
        } catch {
            items = [];
        } finally {
            loading = false;
            requestAnimationFrame(() => window.scrollTo(0, scrollY));
        }
    }

    function openAdd() {
        editingGroup = null;
        showForm = true;
    }

    function openEdit(group: MenuItemGroup) {
        editingGroup = group;
        showForm = true;
    }

    function toggleExpand(name: string) {
        const next = new Set(expandedGroups);
        if (next.has(name)) next.delete(name);
        else next.add(name);
        expandedGroups = next;
    }

    let updatedName = $state<string | null>(null);
    let updatedTimer: ReturnType<typeof setTimeout> | null = null;

    function flashUpdated(name: string) {
        if (updatedTimer) clearTimeout(updatedTimer);
        updatedName = name;
        updatedTimer = setTimeout(() => { updatedName = null; }, 3000);
    }

    async function handleDeleteGroup(group: MenuItemGroup) {
        if (!confirm(`Delete "${group.name}" and all its sizes?`)) return;
        try {
            for (const v of group.variants) {
                await deleteMenuItem(v.menuItemId);
            }
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
                <th></th>
                <th>Name</th>
                <th>Category</th>
                <th>Sizes</th>
                <th>Price</th>
                <th>Temp</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            {#each groups as group (group.name)}
                <tr class="group-row" onclick={() => { if (group.variants.length > 1) toggleExpand(group.name); }}>
                    <td class="expand-cell">
                        {#if group.variants.length > 1}
                            <span class="chevron" class:expanded={expandedGroups.has(group.name)}>&#9654;</span>
                        {/if}
                    </td>
                    <td>{toTitleCase(group.name)}</td>
                    <td>{toTitleCase(group.category)}</td>
                    <td>
                        {#if group.variants.length === 1}
                            {toTitleCase(group.variants[0]?.size ?? '')}
                        {:else}
                            {group.variants.length} sizes
                        {/if}
                    </td>
                    <td>
                        {#if group.minPrice === group.maxPrice}
                            {formatCurrency(group.minPrice)}
                        {:else}
                            {formatCurrency(group.minPrice)} – {formatCurrency(group.maxPrice)}
                        {/if}
                    </td>
                    <td>{group.isHot ? 'Hot' : 'Cold'}</td>
                    <td>
                        <span
                            class="badge"
                            class:badge-success={group.isAvailable}
                            class:badge-danger={!group.isAvailable}
                        >
                            {group.isAvailable ? 'Available' : 'Unavailable'}
                        </span>
                    </td>
                    <td class="action-cell" onclick={(e) => { e.stopPropagation(); }}>
                        <button class="btn-ghost" onclick={() => { openEdit(group); }}>Edit</button>
                        <button class="btn-ghost danger-text" onclick={() => { void handleDeleteGroup(group); }}>Delete</button>
                    </td>
                </tr>
                {#if expandedGroups.has(group.name)}
                    {#each group.variants as variant (variant.menuItemId)}
                        <tr class="variant-row">
                            <td></td>
                            <td class="variant-indent">{toTitleCase(variant.size)}</td>
                            <td></td>
                            <td></td>
                            <td>{formatCurrency(variant.basePrice)}</td>
                            <td></td>
                            <td>
                                <span
                                    class="badge"
                                    class:badge-success={variant.isAvailable}
                                    class:badge-danger={!variant.isAvailable}
                                >
                                    {variant.isAvailable ? 'Available' : 'Unavailable'}
                                </span>
                            </td>
                            <td></td>
                        </tr>
                    {/each}
                {/if}
            {/each}
        </tbody>
    </table>
{/if}

<MenuItemForm
    open={showForm}
    group={editingGroup}
    onclose={() => { showForm = false; editingGroup = null; }}
    onsaved={(savedName) => { void loadItems().then(() => flashUpdated(savedName)); }}
/>

{#if updatedName}
    <div class="toast">Saved</div>
{/if}

<style>
    .action-cell {
        display: flex;
        gap: 0.25rem;
    }

    .danger-text {
        color: var(--color-danger);
    }

    .toast {
        position: fixed;
        bottom: 1.5rem;
        right: 1.5rem;
        background: #2e7d32;
        color: white;
        padding: 0.5rem 1.25rem;
        border-radius: 8px;
        font-size: 0.85rem;
        font-weight: 600;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        animation: toast-in-out 3s forwards;
        z-index: 1000;
    }

    @keyframes toast-in-out {
        0% { opacity: 0; transform: translateY(8px); }
        10% { opacity: 1; transform: translateY(0); }
        75% { opacity: 1; }
        100% { opacity: 0; }
    }

    .group-row {
        cursor: default;
    }

    .group-row:has(.chevron) {
        cursor: pointer;
    }

    .expand-cell {
        width: 24px;
        text-align: center;
    }

    .chevron {
        display: inline-block;
        font-size: 0.6rem;
        transition: transform 0.15s;
        color: var(--color-text-muted);
    }

    .chevron.expanded {
        transform: rotate(90deg);
    }

    .variant-row {
        background: var(--color-bg, #f8f5f0);
    }

    .variant-indent {
        padding-left: 1.5rem;
        font-size: 0.85rem;
        color: var(--color-text-muted);
        text-transform: capitalize;
    }
</style>
