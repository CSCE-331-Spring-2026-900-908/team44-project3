<script lang="ts">
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { authenticate } from '$lib/api';
    import { setEmployee, isManager } from '$lib/stores/auth.svelte';

    let email = $state('');
    let password = $state('');
    let error = $state('');
    let loading = $state(false);

    async function handleLogin() {
        error = '';
        if (!email || !password) {
            error = 'Please enter both email and password.';
            return;
        }
        loading = true;
        try {
            const emp = await authenticate(email, password);
            if (!emp) {
                error = 'Invalid email or password.';
                return;
            }
            setEmployee(emp);
            await goto(resolve(isManager() ? '/manager' : '/cashier'));
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
            <h1>Team 44 Boba POS</h1>
            <p>Point of Sale</p>
        </div>

        <form onsubmit={handleLogin}>
            <div class="form-group">
                <label for="email">Email</label>
                <input
                    id="email"
                    type="email"
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
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .login-card {
        width: 100%;
        max-width: 400px;
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

    .login-header p {
        color: var(--color-text-muted);
        font-size: 0.875rem;
    }

    form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }
</style>
