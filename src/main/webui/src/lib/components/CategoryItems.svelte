<script lang="ts">
    import type { MenuItem } from '$lib/types';
    import { getItemsByCategory, menuItemImageUrl } from '$lib/api';
    import { toTitleCase, formatCurrency } from '$lib/utils';

    let {
        category,
        onselect
    }: {
        category: string;
        onselect: (variants: MenuItem[]) => void;
    } = $props();

    let items = $state<MenuItem[]>([]);
    let loading = $state(false);

    let itemGroups = $derived((() => {
        const groups = new Map<string, MenuItem[]>();
        for (const item of items) {
            const existing = groups.get(item.name);
            if (existing) existing.push(item);
            else groups.set(item.name, [item]);
        }
        return Array.from(groups.values());
    })());

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
            {#each itemGroups as variants (variants[0].name)}
                {@const anyAvailable = variants.some(v => v.isAvailable)}
                {@const minPrice = Math.min(...variants.map(v => v.basePrice))}
                <button
                    class="item-card"
                    class:unavailable={!anyAvailable}
                    onclick={() => { onselect(variants); }}
                    disabled={!anyAvailable}
                >
                    <div class="item-icon">
                        {#if variants[0].hasImage}
                            <img src={menuItemImageUrl(variants[0].menuItemId)} alt={variants[0].name} class="item-img" />
                        {:else}
                            🧋
                        {/if}
                    </div>
                    <span class="item-name">{toTitleCase(variants[0].name)}</span>
                    <span class="item-price">
                        {#if variants.length > 1}from&nbsp;{/if}{formatCurrency(minPrice)}
                    </span>
                    {#if !anyAvailable}
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
        grid-template-columns: repeat(auto-fill, minmax(clamp(140px, 15vw, 220px), 1fr));
        gap: clamp(0.5rem, 1vw, 1rem);
    }

    .item-card {
        background: var(--color-surface);
        border: 4px solid var(--color-border);
        border-radius: var(--radius);
        padding: clamp(0.75rem, 1.5vw, 1.25rem);
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.35rem;
        transition: border-color var(--transition), box-shadow var(--transition);
    }

    .item-card:hover:not(:disabled) {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 2px rgba(255, 90, 90, 0.15);
    }

    .item-card.unavailable {
        opacity: 0.5;
    }

    .item-icon {
        font-size: clamp(2rem, 3.5vw, 3rem);
        width: clamp(4.5rem, 9vw, 7rem);
        height: clamp(5.5rem, 11vw, 8rem);
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .item-img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
    }

    .item-name {
        font-weight: 500;
        font-size: clamp(1.175rem, 1.5vw, 1.4rem);
    }

    .item-price {
        color: var(--color-text-muted);
        font-size: clamp(1.2rem, 1.5vw, 1.4rem);
    }
</style>
