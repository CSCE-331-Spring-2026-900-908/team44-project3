<script lang="ts">
    import { onMount } from 'svelte';
    import { getMenu } from '$lib/api';

    let items = [];

    async function load() {
        try{
            items = await getMenu();
            console.log("menu:", items);
        } catch {
            items = [];
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

    <div class="grid">
        {#each items as item}
            <div class="card">
                <h2>{item.name}</h2>
                <p>${item.basePrice}</p>
            </div>
        {/each}
    </div>
</div>