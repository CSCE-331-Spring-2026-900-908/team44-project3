<script lang="ts">
    import { onMount } from 'svelte';
    import { getPickupOrders } from '$lib/api';

    let orders = [];

    async function load() {
        try {
            orders = await getPickupOrders();
            console.log("pickup orders:", orders);
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
    <h1>Ready for Pickup</h1>

    <div class="grid">
        {#each orders as order}
            <div class="card">
                <h2>#{order.orderId}</h2>
                <p>{order.timestamp}</p>
            </div>
        {/each}
    </div>
</div>