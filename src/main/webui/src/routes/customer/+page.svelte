<script lang="ts">
    import {goto} from '$app/navigation';
    import {resolve} from '$app/paths';
    import {employee} from '$lib/stores/auth.svelte';

    function login() {
        goto(resolve('/login'));
    }

    function goToPOS() {
        if (!$employee) {
            goto(resolve('/login'));
            return;
        }
    }
</script>

<div class="portal-page">
    <div class="portal-card card">
        <div class="portal-header">
            <h1>Team 44 Boba POS</h1>
            <p>Customer Portal</p>
        </div>

        <div class="portal-buttons">
            <button class="btn-primary btn-lg btn-full" onclick={login}>
                Employee Login
            </button>

            <button class="btn-secondary btn-lg btn-full">
                View Menu
            </button>

            <button class="btn-secondary btn-lg btn-full">
                Order Status
            </button>
        </div>
    </div>
</div>

<style>
    .portal-page {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 100vh;
        background: linear-gradient(135deg, #FF5A5A 0%, #FFD45A 100%);
    }

    .portal-card {
        width: 100%;
        max-width: 420px;
        padding: 2.5rem;
    }

    .portal-header {
        text-align: center;
        margin-bottom: 2rem;
    }

    .portal-header h1 {
        font-size: 2rem;
        font-weight: 800;
        color: var(--color-primary);
    }

    .portal-header p {
        color: var(--color-text-muted);
        font-size: 0.875rem;
    }

    .portal-buttons {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }
</style>