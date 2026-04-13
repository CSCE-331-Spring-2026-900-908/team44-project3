<script lang="ts">
    import { onMount } from 'svelte';
    import { getPickupOrders } from '$lib/api';

    let orders = $state<any[]>([]);

    function formatTime(ts: string): string {
        return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    async function load() {
        try {
            orders = await getPickupOrders();
            console.log("pickup orders:", orders);
        } catch (e) {
            console.error("pickup load failed:", e);
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
    <h1>Ready for Pickup</h1>

    {#if orders.length === 0}
        <p class="empty">No orders ready for pickup right now.</p>
    {:else}
        <div class="grid">
            {#each orders as order}
                <div class="card">
                    <div class="order-id">#{order.orderId}</div>
                    <div class="order-time">{formatTime(order.timestamp)}</div>
                </div>
            {/each}
        </div>
    {/if}
</div>

<style>
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
    font-size: 1.3rem;
    font-weight: 700;
    margin-bottom: 0.4rem;
}
.order-time {
    font-size: 0.9rem;
    color: #888;
}
.empty {
    margin-top: 3rem;
    text-align: center;
    color: #888;
    font-size: 1.1rem;
}
</style>
