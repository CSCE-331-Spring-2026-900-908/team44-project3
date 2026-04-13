<script lang="ts">
    import { onMount } from 'svelte';
    import { getDisplayMenu } from '$lib/api';

    let itemsByCategory = $state<Record<string, any[]>>({});

    async function load() {
        try {
            const items = await getDisplayMenu();
            console.log("menu:", items);
            const grouped: Record<string, any[]> = {};
            for (const item of items) {
                if (!grouped[item.category]) grouped[item.category] = [];
                grouped[item.category].push(item);
            }
            itemsByCategory = grouped;
        } catch (e) {
            console.error("menu load failed:", e);
            itemsByCategory = {};
        }
    }

    onMount(() => {
        load();
        const interval = setInterval(load, 30000);
        return () => clearInterval(interval);
    });
</script>

<div class="screen">
    <h1>Menu</h1>

    {#if Object.keys(itemsByCategory).length === 0}
        <p class="empty">Menu unavailable.</p>
    {:else}
        {#each Object.entries(itemsByCategory) as [category, items]}
            <section>
                <h2 class="category-title">{category}</h2>
                <div class="grid">
                    {#each items as item}
                        <div class="card">
                            <div class="item-name">{item.name}{item.size ? ` (${item.size})` : ''}</div>
                            <div class="item-price">${item.basePrice}</div>
                        </div>
                    {/each}
                </div>
            </section>
        {/each}
    {/if}
</div>

<style>
.screen {
    padding: 2rem;
    background: var(--color-bg, #f8f4f0);
    min-height: 100vh;
}
section {
    margin-bottom: 2rem;
}
.category-title {
    font-size: 1.25rem;
    font-weight: 700;
    margin-bottom: 0.75rem;
    border-bottom: 2px solid var(--color-primary, #c47a3a);
    padding-bottom: 0.25rem;
}
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 1rem;
}
.card {
    background: var(--color-surface, #fff);
    border-radius: var(--radius, 8px);
    box-shadow: var(--shadow, 0 2px 8px rgba(0,0,0,0.08));
    padding: 1rem;
    text-align: center;
}
.item-name {
    font-size: 0.95rem;
    font-weight: 600;
    margin-bottom: 0.4rem;
}
.item-price {
    font-size: 1rem;
    color: var(--color-primary, #c47a3a);
    font-weight: 500;
}
.empty {
    margin-top: 3rem;
    text-align: center;
    color: #888;
    font-size: 1.1rem;
}
</style>
