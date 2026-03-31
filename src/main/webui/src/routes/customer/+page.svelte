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

    <div class="top-center"
    >Welcome to our Boba Shop!</div>
    <div class="center"
    
    >Click anywhere to begin your order</div>
</div>



<style>

    .top-center {
        position: absolute;
        top: 20px;
        left: 50%;
        transform: translateX(-50%);
    }

    .center {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
    }   

    .splash {
        height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 2rem;
        cursor: pointer;
        color: white;
        overflow: hidden;
        position: relative;
    }

    /* Background layer */
    .splash::before {
        content: '';
        position: absolute;
        inset: -50%;
        z-index: -1;

        background: radial-gradient(circle at 30% 30%, #ff5a5a, transparent 60%),
                    radial-gradient(circle at 70% 60%, #ffd45a, transparent 60%),
                    radial-gradient(circle at 50% 80%, #ff8a5a, transparent 60%);

        filter: blur(80px);
        animation: blobMove 12s ease-in-out infinite alternate;
    }

    @keyframes blobMove {
        0% {
            transform: scale(1) translate(0%, 0%);
        }
        50% {
            transform: scale(1.2) translate(10%, -10%);
        }
        100% {
            transform: scale(1) translate(-10%, 10%);
        }
    }
</style>