<script lang="ts">
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { authenticate, getCurrentUser } from '$lib/api';
    import { isManager, restoreSession } from '$lib/stores/auth.svelte';
    import LanguageSelector from '$lib/components/LanguageSelector.svelte';

    let email = $state('');
    let password = $state('');
    let error = $state('');
    let loading = $state(false);
    let emailInput = $state<HTMLInputElement | null>(null);

    $effect(() => {
        const saved = restoreSession();
        if (saved) {
            getCurrentUser().then((emp) => {
                if (emp) {
                    const dest = emp.role === 'manager' ? '/manager' : '/cashier';
                    void goto(resolve(dest));
                }
            });
        }
    });

    async function handleLogin() {
        error = '';
        if (!email || !password) {
            error = 'Please enter both email and password.';
            return;
        }
        if (emailInput && !emailInput.validity.valid) {
            error = 'Please enter a valid email address.';
            return;
        }
        loading = true;
        try {
            const emp = await authenticate(email, password);
            if (!emp) {
                error = 'Invalid email or password.';
                return;
            }
            const dest = isManager() ? '/manager' : '/cashier';
            await goto(resolve(dest));
        } catch {
            error = 'Login failed. Please try again.';
        } finally {
            loading = false;
        }
    }
</script>

<div class="login-page">
    <div class="login-card card">
        <div class="login-header">
            <h1>Boba Bob's</h1>
            <div class="lang-row">
                <LanguageSelector />
            </div>
        </div>

        <form onsubmit={handleLogin}>
            <div class="form-group">
                <label for="email">Email</label>
                <input
                    bind:this={emailInput}
                    id="email"
                    type="email"
                    pattern=".+@.+\..+"
                    placeholder="Enter your email"
                    bind:value={email}
                    autocomplete="username"
                />
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input
                    id="password"
                    type="password"
                    placeholder="Enter your password"
                    bind:value={password}
                    autocomplete="current-password"
                />
            </div>

            {#if error}
                <p class="error-text">{error}</p>
            {/if}

            <button type="submit" class="btn-primary btn-full btn-lg" disabled={loading}>
                {loading ? 'Signing in...' : 'Sign In'}
            </button>
        </form>
    </div>
</div>

<style>
    .login-page {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 100vh;
        background: linear-gradient(135deg, #FF5A5A 0%, #FFD45A 100%);
    }

    .login-card {
        width: 100%;
        max-width: 420px;
        padding: 2.5rem;
    }

    .login-header {
        text-align: center;
        margin-bottom: 2rem;
    }

    .login-header h1 {
        font-size: 2rem;
        font-weight: 800;
        color: var(--color-primary);
    }

    .lang-row {
        display: flex;
        justify-content: center;
        margin-top: 0.75rem;
    }

    form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }
</style>
