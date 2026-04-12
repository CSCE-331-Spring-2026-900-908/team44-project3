<script lang="ts">
    import { onMount } from 'svelte';
    import { getKitchenOrders } from '$lib/api';

    let orders = [];

    async function load() {
        try {
            orders = await getKitchenOrders();
            console.log("kitchen orders:", orders);
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
                <h2>#{order.orderId}</h2>
                <div>{order.item}</div>
            </div>
        {/each}
    </div>
</div>