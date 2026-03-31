<script lang="ts">
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';

    function proceed() {
        goto(resolve('/customer/order'));
    }
</script>

<div
    class="splash"
    on:click={proceed}
    on:keydown={proceed}
    tabindex="0"
>
    click anywhere to start
</div>

<style>
    .splash {
        height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 2rem;
        cursor: pointer;
    }
</style>