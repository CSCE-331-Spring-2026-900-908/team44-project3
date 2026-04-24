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
    <h1 class="board-title">🧋 Ready for Pickup</h1>

    {#if orders.length === 0}
        <p class="empty">No orders ready right now — check back soon!</p>
    {:else}
        <div class="grid">
            {#each orders as order}
                <div class="card">
                    <div class="card-header">
                        <span class="order-id">#{order.orderId}</span>
                        <span class="order-time">{formatTime(order.timestamp)}</span>
                    </div>

                    {#if order.customerName}
                        <div class="customer-name">{order.customerName}</div>
                    {/if}

                    <ul class="item-list">
                        {#each (order.items ?? []) as item}
                            <li class="item-row">
                                <div class="item-top">
                                    <span class="item-name">{item.name}</span>
                                    {#if item.size}
                                        <span class="item-size">{item.size}</span>
                                    {/if}
                                </div>
                                {#if item.customizations}
                                    <div class="item-custom">{item.customizations}</div>
                                {/if}
                            </li>
                        {/each}
                    </ul>

                    <div class="ready-badge">✓ Ready</div>
                </div>
            {/each}
        </div>
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
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 1.5rem;
    margin-top: 1rem;
}
.card {
    background: var(--color-surface, #fff);
    border-radius: 14px;
    box-shadow: 0 3px 14px rgba(0,0,0,0.09);
    padding: 1.25rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    border-left: 5px solid var(--color-primary, #c47a3a);
}
.card-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}
.order-id {
    font-size: 1.5rem;
    font-weight: 800;
    color: #222;
}
.order-time {
    font-size: 0.82rem;
    color: #999;
}
.customer-name {
    font-size: 1rem;
    font-weight: 600;
    color: var(--color-primary, #c47a3a);
    margin-top: -0.25rem;
}
.item-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}
.item-row {
    padding: 0.5rem 0.6rem;
    background: #faf7f4;
    border-radius: 8px;
}
.item-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
}
.item-name {
    font-size: 0.95rem;
    font-weight: 600;
    color: #222;
}
.item-size {
    font-size: 0.78rem;
    color: #777;
    background: #ede8e2;
    padding: 0.15rem 0.45rem;
    border-radius: 4px;
    white-space: nowrap;
}
.item-custom {
    font-size: 0.78rem;
    color: #888;
    margin-top: 0.25rem;
    font-style: italic;
}
.ready-badge {
    align-self: flex-start;
    margin-top: 0.25rem;
    background: #d4f0dc;
    color: #2a7a42;
    font-size: 0.8rem;
    font-weight: 700;
    padding: 0.25rem 0.65rem;
    border-radius: 20px;
    letter-spacing: 0.03em;
}
.empty {
    margin-top: 4rem;
    text-align: center;
    color: #aaa;
    font-size: 1.1rem;
}
</style>