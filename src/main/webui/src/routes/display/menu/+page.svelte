<script lang="ts">
    import { onMount } from 'svelte';
    import { getDisplayMenu, menuItemImageUrl } from '$lib/api';

    let itemsByCategory = $state<Record<string, any[]>>({});

    async function load() {
        try {
            const items = await getDisplayMenu();
            const grouped: Record<string, any[]> = {};
            for (const item of items) {
                if (!grouped[item.category]) grouped[item.category] = [];
                if (!grouped[item.category].some((i: any) => i.name === item.name)) {
                    grouped[item.category].push(item);
                }
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

    onMount(() => {
        load();
        const interval = setInterval(load, 30000);

        // autoscroll
        let scrolling = true;
        const scroll = setInterval(() => {
            if (!scrolling) return;
            window.scrollBy({ top: 1, behavior: 'instant' });
            // reset to top when near bottom
            if (window.innerHeight + window.scrollY >= document.body.scrollHeight - 5) {
                window.scrollTo({ top: 0, behavior: 'instant' });
            }
        }, 30); // lower = faster scroll

        return () => {
            clearInterval(interval);
            clearInterval(scroll);
        };
    });
</script>

<div class="screen">
    <h1 class="board-title">Our Menu</h1>

    {#if Object.keys(itemsByCategory).length === 0}
        <p class="empty">Menu unavailable.</p>
    {:else}
        {#each Object.entries(itemsByCategory) as [category, items]}
            <section>
                <h2 class="category-title">{category}</h2>
                <div class="grid">
                    {#each items as item}
                        <div class="card">
                            {#if item.hasImage}
                                <div class="img-wrap">
                                    <img src={menuItemImageUrl(item.menuItemId)} alt={item.name} />
                                </div>
                            {:else}
                                <div class="img-wrap placeholder">🧋</div>
                            {/if}
                            <div class="card-body">
                                <div class="item-name">{item.name}</div>
                                <div class="item-price">${Number(item.basePrice).toFixed(2)}</div>
                            </div>
                        </div>
                    {/each}
                </div>
            </section>
        {/each}
    {/if}
</div>

<style>
.screen {
    padding: 2rem 2.5rem;
    background: var(--color-bg, #f8f4f0);
    min-height: 100vh;
}
.board-title {
    font-size: 2rem;
    font-weight: 800;
    color: var(--color-primary, #c47a3a);
    margin-bottom: 1.5rem;
    letter-spacing: -0.5px;
}
section {
    margin-bottom: 2.5rem;
}
.category-title {
    font-size: 1.2rem;
    font-weight: 700;
    margin-bottom: 0.75rem;
    border-bottom: 2px solid var(--color-primary, #c47a3a);
    padding-bottom: 0.3rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #444;
}
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
    gap: 1rem;
}
.card {
    background: var(--color-surface, #fff);
    border-radius: 12px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.07);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.card:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 18px rgba(0,0,0,0.12);
}
.img-wrap {
    width: 100%;
    aspect-ratio: 1 / 1;
    overflow: hidden;
    background: #f0ebe4;
}
.img-wrap img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}
.img-wrap.placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 3rem;
}
.card-body {
    padding: 0.75rem;
    text-align: center;
}
.item-name {
    font-size: 0.9rem;
    font-weight: 600;
    margin-bottom: 0.3rem;
    color: #222;
    line-height: 1.3;
    text-transform: capitalize;
}
.item-price {
    font-size: 1rem;
    color: var(--color-primary, #c47a3a);
    font-weight: 700;
}
.empty {
    margin-top: 3rem;
    text-align: center;
    color: #888;
    font-size: 1.1rem;
}
</style>