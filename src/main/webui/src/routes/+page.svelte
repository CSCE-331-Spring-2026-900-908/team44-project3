<script lang="ts">
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { authenticate } from '$lib/api';
    import { setEmployee, isManager, setCustomerMode } from '$lib/stores/auth.svelte';

    type Step = 'select' | 'employee' | 'manager';
    let step = $state<Step>('select');

    let email = $state('');
    let password = $state('');
    let error = $state('');
    let loading = $state(false);

    async function enterCustomer() {
        setCustomerMode(true);
        await goto(resolve('/customer'));
    }

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
            if (step === 'manager') {
                if (!isManager()) {
                    error = 'This account does not have manager access.';
                    setEmployee(null);
                    return;
                }
                await goto(resolve('/manager'));
            } else {
                await goto(resolve('/cashier'));
            }
        } catch {
            error = 'Login failed. Please try again.';
        } finally {
            loading = false;
        }
    }

    function back() {
        step = 'select';
        email = '';
        password = '';
        error = '';
    }
</script>

<div class="login-page">
    <div class="login-card card">
        <div class="login-header">
            <h1>Team 44 Boba POS</h1>
        </div>

        {#if step === 'select'}
            <div class="role-select">
                <p class="role-label">Select your role to continue</p>
                <button class="role-btn customer" onclick={enterCustomer}>
                    <span class="role-name">Customer Kiosk</span>
                    <span class="role-desc">Browse menu & order in website or in house</span>
                </button>
                <button class="role-btn employee" onclick={() => { step = 'employee'; }}>
                    <span class="role-name">POS System</span>
                    <span class="role-desc">Working POS from cashier view or employee only features</span>
                </button>
                <button class="role-btn manager" onclick={() => { step = 'manager'; }}>
                    <span class="role-name">Manager Dashboard</span>
                    <span class="role-desc">Menus, Reports & Administration</span>
                </button>
            </div>
        {:else}
            <form onsubmit={handleLogin}>
                <p class="role-context">
                    Signing in as <strong>{step === 'manager' ? 'Manager' : 'Employee'}</strong>
                </p>

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

                <button type="button" class="btn-ghost btn-full" onclick={back}>
                    Back
                </button>
            </form>
        {/if}
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

    .role-select {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
    }

    .role-label {
        text-align: center;
        color: var(--color-text-muted);
        font-size: 0.875rem;
        margin-bottom: 0.25rem;
    }

    .role-btn {
        display: grid;
        grid-template-columns: 2.5rem 1fr;
        grid-template-rows: auto auto;
        align-items: center;
        gap: 0 0.75rem;
        padding: 1rem 1.25rem;
        border-radius: var(--radius);
        border: 2px solid var(--color-border);
        background: var(--color-surface);
        text-align: left;
        cursor: pointer;
        transition: border-color 0.15s, box-shadow 0.15s;
    }

    .role-btn:hover {
        box-shadow: var(--shadow);
    }

    .role-btn.customer:hover { border-color: #4caf50; }
    .role-btn.employee:hover { border-color: #2196f3; }
    .role-btn.manager:hover { border-color: var(--color-primary); }

    .role-icon {
        grid-row: span 2;
        font-size: 1.75rem;
        text-align: center;
    }

    .role-name {
        font-weight: 700;
        font-size: 1rem;
    }

    .role-desc {
        font-size: 0.75rem;
        color: var(--color-text-muted);
    }

    .role-context {
        font-size: 0.875rem;
        color: var(--color-text-muted);
        margin-bottom: 0.5rem;
    }

    form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }
</style>
