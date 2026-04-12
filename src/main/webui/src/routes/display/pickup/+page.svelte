<script lang="ts">
    import { onMount } from 'svelte';
    import { getPickupOrders } from '$lib/api';

    let orders = [];

    async function load() {
        orders = await getPickupOrders();
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
                <h2>#{order.id}</h2>
                <p>{order.customerName}</p>
            </div>
        {/each}
    </div>
</div>