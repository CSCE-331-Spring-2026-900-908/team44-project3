<script lang="ts">
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { authenticate, getCurrentUser } from '$lib/api';
    import { isManager, restoreSession } from '$lib/stores/auth.svelte';

    type Step = 'select' | 'employee' | 'manager';
    let step = $state<Step>('select');

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
            if (step === 'manager') {
                if (!isManager()) {
                    error = 'This account does not have manager access.';
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
                <button class="role-btn employee" onclick={() => { step = 'employee'; }}>
                    <span class="role-icon">&#x1F9FE;</span>
                    <span class="role-name">POS System</span>
                    <span class="role-desc">Working POS from cashier view or employee only features</span>
                </button>
                <button class="role-btn manager" onclick={() => { step = 'manager'; }}>
                    <span class="role-icon">&#x1F527;</span>
                    <span class="role-name">Manager Dashboard</span>
                    <span class="role-desc">Menus, Reports & Administration</span>
                </button>
                <button class="role-btn display" onclick={() => goto('/display')}>
                    <span class="role-icon">📺</span>
                    <span class="role-name">Store Displays</span>
                    <span class="role-desc">Kitchen, Pickup, and Menu Boards</span>
                </button>
            </div>

            <a href={resolve('/')} class="back-link">Back to Customer View</a>
        {:else}
            <form onsubmit={handleLogin}>
                <p class="role-context">
                    Signing in as <strong>{step === 'manager' ? 'Manager' : 'Employee'}</strong>
                </p>

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

    .back-link {
        display: block;
        text-align: center;
        margin-top: 1rem;
        font-size: 0.875rem;
        color: var(--color-text-muted);
        text-decoration: none;
    }

    .back-link:hover {
        color: var(--color-primary);
    }
</style>
