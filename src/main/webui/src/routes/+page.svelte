<script lang="ts">
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { customerCheckin } from '$lib/api';
    import { setCustomer, restoreCustomer } from '$lib/stores/auth.svelte';

    let email = $state('');
    let error = $state('');
    let loading = $state(false);
    let emailInput = $state<HTMLInputElement | null>(null);

async function enterCustomer() {
    error = '';
    loading = true;

    try {
        const emp = await authenticate('erica.allen@company.com', '98usTpQyDWcK');

        if (!emp) {
            error = 'Customer mode login failed.';
            return;
        }

        setEmployee(emp);
        setCustomerMode(true);
        await goto(resolve('/customer'));
    } catch (e) {
        console.error('Customer mode login failed', e);
        error = 'Customer mode login failed.';
    } finally {
        loading = false;
    }
}
    $effect(() => {
        const saved = restoreCustomer();
        if (saved) {
            void goto(resolve('/order'));
        }
    });

    async function handleCheckin() {
        error = '';
        if (!email) {
            error = 'Please enter your email.';
            return;
        }
        if (emailInput && !emailInput.validity.valid) {
            error = 'Please enter a valid email address.';
            return;
        }
        loading = true;
        try {
            const customer = await customerCheckin(email);
            setCustomer(customer);
            await goto(resolve('/order'));
        } catch {
            error = 'Check-in failed. Please try again.';
        } finally {
            loading = false;
        }
    }

    async function continueAsGuest() {
        await goto(resolve('/order'));
    }
</script>

<div class="landing">
    <div class="landing-content">
        <div class="brand">
            <img src="/boba.png" alt="boba" class="brand-img" />
            <h1>Team 44 Boba</h1>
            <p class="tagline">Welcome! Check in to earn rewards, or continue as guest.</p>
        </div>

        <div class="checkin-card card">
            <form onsubmit={handleCheckin}>
                <div class="form-group">
                    <label for="checkin-email">Email</label>
                    <input
                        bind:this={emailInput}
                        id="checkin-email"
                        type="email"
                        pattern=".+@.+\..+"
                        placeholder="Enter your email to check in"
                        bind:value={email}
                        autocomplete="email"
                    />
                </div>

                {#if error}
                    <p class="error-text">{error}</p>
                {/if}

                <button type="submit" class="btn-primary btn-full btn-lg" disabled={loading}>
                    {loading ? 'Checking in...' : 'Start Ordering'}
                </button>
            </form>

            <button class="btn-ghost btn-full" onclick={continueAsGuest}>
                Continue as Guest
            </button>
        </div>

        <a href={resolve('/login')} class="staff-link">Employee Login</a>
    </div>
</div>

<style>
    .landing {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
        overflow: hidden;
        color: white;
    }

    .landing::before {
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
        0% { transform: scale(1) translate(0%, 0%); }
        50% { transform: scale(1.2) translate(10%, -10%); }
        100% { transform: scale(1) translate(-10%, 10%); }
    }

    .landing-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 2rem;
        width: 100%;
        max-width: 420px;
        padding: 2rem;
    }

    .brand {
        text-align: center;
    }

    .brand-img {
        width: 120px;
        height: auto;
        margin-bottom: 1rem;
    }

    .brand h1 {
        font-size: 2.5rem;
        font-weight: 800;
    }

    .tagline {
        font-size: 1rem;
        opacity: 0.9;
        margin-top: 0.5rem;
    }

    .checkin-card {
        width: 100%;
        padding: 2rem;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
    }

    form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }

    .staff-link {
        font-size: 0.8rem;
        color: rgba(255, 255, 255, 0.6);
        text-decoration: none;
    }

    .staff-link:hover {
        color: white;
    }
</style>
