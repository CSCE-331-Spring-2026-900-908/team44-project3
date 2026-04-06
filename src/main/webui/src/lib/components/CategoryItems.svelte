<script lang="ts">
    import type { MenuItem } from '$lib/types';
    import { getItemsByCategory } from '$lib/api';
    import { toTitleCase } from '$lib/utils';

    let {
        category,
        onselect
    }: {
        category: string;
        onselect: (item: MenuItem) => void;
    } = $props();

    let items = $state<MenuItem[]>([]);
    let loading = $state(false);

    $effect(() => {
        if (category) void loadItems();
    });

    async function loadItems() {
        loading = true;
        try {
            items = await getItemsByCategory(category);
        } catch {
            items = [];
        } finally {
            loading = false;
        }
    }
</script>

<div class="category-items">
    <h3>{category}</h3>

    {#if loading}
        <p class="muted">Loading...</p>
    {:else if items.length === 0}
        <p class="muted">No items in this category.</p>
    {:else}
        <div class="item-grid">
            {#each items as item (item.menuItemId)}
                <button
                    class="item-card"
                    class:unavailable={!item.isAvailable}
                    onclick={() => { onselect(item); }}
                    disabled={!item.isAvailable}
                >
                    <span class="item-name">{toTitleCase(item.name)}</span>
                    <span class="item-price">${item.basePrice.toFixed(2)}</span>
                    {#if !item.isAvailable}
                        <span class="badge badge-danger">Sold Out</span>
                    {/if}
                </button>
            {/each}
        </div>
    {/if}
</div>

<style>
    .category-items h3 {
        font-size: 1rem;
        font-weight: 600;
        margin-bottom: 0.75rem;
        text-transform: capitalize;
    }

    .muted {
        color: var(--color-text-muted);
        font-size: 0.875rem;
    }

    .item-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
        gap: 0.5rem;
    }

    .item-card {
        background: var(--color-surface);
        border: 4px solid var(--color-border);
        border-radius: var(--radius);
        padding: 0.75rem;
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.25rem;
        transition: border-color var(--transition), box-shadow var(--transition);
    }

    .item-card:hover:not(:disabled) {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 2px rgba(255, 90, 90, 0.15);
    }

    .item-card.unavailable {
        opacity: 0.5;
    }

    .item-name {
        font-weight: 500;
        font-size: 1.175rem;
    }

    .item-price {
        color: var(--color-text-muted);
        font-size: 1.2rem;
    }
</style>
