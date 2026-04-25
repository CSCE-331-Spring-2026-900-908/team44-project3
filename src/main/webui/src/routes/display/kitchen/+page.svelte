<script lang="ts">
    import { onMount } from 'svelte';
    import { getKitchenOrders } from '$lib/api';

    let orders = $state<any[]>([]);

    async function load() {
        try {
            orders = await getKitchenOrders();
            console.log("kitchen orders:", orders);
        } catch (e) {
            console.error("kitchen load failed:", e);
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
    <h1 class=kitchen-title>📋 Kitchen Orders</h1>

    {#if orders.length === 0}
        <p class="empty">No orders in the last 2 minutes.</p>
    {:else}
        <div class="grid">
            {#each orders as order}
                <div class="card">
                    <div class="order-id">#{order.orderId}</div>
                    <div class="item-name">{order.item}</div>
                </div>
            {/each}
        </div>
    {/if}
</div>

<style>
.kitchen-title {
    font-size: 2rem;
    font-weight: 800;
    color: var(--color-primary, #c47a3a);
    margin-bottom: 1.5rem;
    letter-spacing: -0.5px;
}
.screen {
    padding: 2rem;
    background: var(--color-bg, #f8f4f0);
    min-height: 100vh;
}
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 1.5rem;
    margin-top: 1.5rem;
}
.card {
    background: var(--color-surface, #fff);
    border-radius: var(--radius, 8px);
    box-shadow: var(--shadow, 0 2px 8px rgba(0,0,0,0.08));
    padding: 1.5rem;
    text-align: center;
}
.order-id {
    font-size: 0.9rem;
    color: #888;
    margin-bottom: 0.5rem;
}
.item-name {
    font-size: 1.1rem;
    font-weight: 600;
}
.empty {
    margin-top: 3rem;
    text-align: center;
    color: #888;
    font-size: 1.1rem;
}
</style>
