<script lang="ts">
    import { onMount } from 'svelte';
    import { getKitchenOrders } from '$lib/api';

    let orders = [];

    async function load() {
        try {
            orders = await getKitchenOrders();
        } catch {
            orders = [];
        }
    }

    onMount(() => {
        load();
        const interval = setInterval(load, 4000);
        return () => clearInterval(interval);
    });
</script>

<div class="screen">
    <h1>Kitchen Orders</h1>

    <div class="grid">
        {#each orders as order}
            <div class="card">
                <h2>#{order.id}</h2>

                {#each order.items as item}
                    <div>{item.name}</div>
                {/each}
            </div>
        {/each}
    </div>
</div>

<style>
.screen {
    padding: 2rem;
    font-size: 1.5rem;
}

.grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 1.5rem;
}

.card {
    padding: 1.5rem;
    background: white;
    border-radius: 10px;
}
</style>