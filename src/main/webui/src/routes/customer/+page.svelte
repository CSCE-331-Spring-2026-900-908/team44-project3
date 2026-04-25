<script lang="ts">
    import { tick } from 'svelte';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { customerCheckin } from '$lib/api';
    import { setCustomer, restoreCustomer } from '$lib/stores/auth.svelte';
    import LanguageSelector from '$lib/components/LanguageSelector.svelte';

    let email = $state('');
    let error = $state('');
    let loading = $state(false);
    let starting = $state(false);
    let showCheckin = $state(false);
    let emailInput = $state<HTMLInputElement | null>(null);

    $effect(() => {
        const saved = restoreCustomer();
        if (saved) {
            void goto(resolve('/order'));
        }
    });

    async function startOrder() {
        starting = true;
        await goto(resolve('/order'));
    }

    async function toggleCheckin() {
        showCheckin = !showCheckin;
        error = '';
        if (showCheckin) {
            await tick();
            emailInput?.focus();
        } else {
            email = '';
        }
    }

    async function handleCheckin(event: Event) {
        event.preventDefault();
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
            const customer = await customerCheckin(email.trim().toLowerCase());
            setCustomer(customer);
            await goto(resolve('/order'));
        } catch {
            error = 'Check-in failed. Please try again.';
        } finally {
            loading = false;
        }
    }
</script>

<div class="landing">
    <div class="lang-bar">
        <LanguageSelector />
    </div>

    <div class="landing-content">
        <div class="hero">
            <img src="/boba.png" alt="" class="hero-img" />
            <h1>Team 44 Boba</h1>
            <p class="tagline">Fresh bubble tea, made to order.</p>
        </div>

        <div class="cta-stack">
            <button
                class="start-btn"
                onclick={startOrder}
                disabled={starting}
                aria-label="Start a new order as a guest"
            >
                {starting ? 'Starting…' : 'Start order'}
                <svg
                    class="start-btn-arrow"
                    aria-hidden="true"
                    viewBox="0 0 48 16"
                    width="48"
                    height="16"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.75"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                >
                    <line x1="2" y1="8" x2="42" y2="8" />
                    <polyline points="36,2 42,8 36,14" />
                </svg>
            </button>

            <div class="rewards-panel" class:open={showCheckin}>
                <button
                    type="button"
                    class="rewards-toggle"
                    onclick={toggleCheckin}
                    aria-expanded={showCheckin}
                    aria-controls="rewards-form"
                >
                    <span class="rewards-toggle-text">
                        <span class="rewards-toggle-title">Member check-in</span>
                        <span class="rewards-toggle-sub"> Earn points toward free drinks </span>
                    </span>
                    <svg
                        class="rewards-chevron"
                        aria-hidden="true"
                        viewBox="0 0 16 16"
                        width="16"
                        height="16"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.75"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    >
                        <polyline points="4,6 8,10 12,6" />
                    </svg>
                </button>

                {#if showCheckin}
                    <form id="rewards-form" onsubmit={handleCheckin}>
                        <label for="checkin-email" class="sr-only">Email</label>
                        <input
                            bind:this={emailInput}
                            id="checkin-email"
                            type="email"
                            pattern=".+@.+\..+"
                            placeholder="your@email.com"
                            bind:value={email}
                            autocomplete="email"
                            inputmode="email"
                        />

                        {#if error}
                            <p class="error-text">{error}</p>
                        {/if}

                        <button type="submit" class="checkin-submit" disabled={loading}>
                            {loading ? 'Checking in…' : 'Check In & Order'}
                        </button>
                    </form>
                {/if}
            </div>
        </div>
    </div>
</div>

<style>
    .landing {
        min-height: 100vh;
        display: grid;
        grid-template-rows: clamp(3rem, 22vh, 14rem) auto 1fr;
        justify-items: center;
        position: relative;
        overflow: hidden;
        color: white;
        padding: 0 1.5rem 3rem;
    }

    .landing::before {
        content: '';
        position: absolute;
        inset: -50%;
        z-index: -1;
        background:
            radial-gradient(circle at 30% 30%, #ff5a5a, transparent 60%),
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

    @media (prefers-reduced-motion: reduce) {
        .landing::before {
            animation: none;
        }
    }

    .lang-bar {
        position: absolute;
        top: 1.5rem;
        right: 1.5rem;
        z-index: 2;
    }

    .landing-content {
        position: relative;
        z-index: 1;
        grid-row: 2;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 2.5rem;
        width: 100%;
        max-width: 560px;
    }

    .hero {
        text-align: center;
    }

    .hero-img {
        width: 200px;
        height: auto;
        margin-bottom: 1.25rem;
        filter: drop-shadow(0 12px 24px rgba(45, 32, 23, 0.25));
    }

    .hero h1 {
        font-size: 3rem;
        font-weight: 800;
        letter-spacing: -0.02em;
        line-height: 1.05;
    }

    .tagline {
        font-size: 1.125rem;
        opacity: 0.92;
        margin-top: 0.5rem;
    }

    .cta-stack {
        width: 100%;
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }

    .start-btn {
        position: relative;
        width: 100%;
        min-height: 88px;
        padding: 0 2rem;
        border: none;
        border-radius: 18px;
        background: linear-gradient(135deg, #ffffff 0%, #fff6ea 100%);
        color: #2d2017;
        font-size: 1.75rem;
        font-weight: 700;
        letter-spacing: -0.01em;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1.5rem;
        box-shadow:
            0 16px 32px -12px rgba(45, 32, 23, 0.35),
            0 4px 8px -4px rgba(45, 32, 23, 0.15);
        transition:
            transform 180ms cubic-bezier(0.2, 0.8, 0.2, 1),
            box-shadow 180ms ease;
    }

    .start-btn:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow:
            0 22px 40px -14px rgba(45, 32, 23, 0.4),
            0 8px 16px -6px rgba(45, 32, 23, 0.2);
    }

    .start-btn:active:not(:disabled) {
        transform: translateY(0);
    }

    .start-btn:disabled {
        opacity: 0.7;
        cursor: wait;
    }

    .start-btn-arrow {
        color: var(--color-primary);
        transition: transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
    }

    .start-btn:hover:not(:disabled) .start-btn-arrow {
        transform: translateX(6px);
    }

    .rewards-panel {
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: 16px;
        overflow: hidden;
        color: var(--color-text);
        box-shadow: 0 8px 20px -10px rgba(45, 32, 23, 0.2);
        transition: box-shadow 180ms ease;
    }

    .rewards-panel.open {
        box-shadow: 0 16px 32px -12px rgba(45, 32, 23, 0.28);
    }

    .rewards-toggle {
        width: 100%;
        padding: 1.125rem 1.5rem;
        background: transparent;
        border: none;
        color: inherit;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        text-align: left;
    }

    .rewards-toggle-text {
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
    }

    .rewards-toggle-title {
        font-size: 1.0625rem;
        font-weight: 600;
    }

    .rewards-toggle-sub {
        font-size: 0.875rem;
        color: var(--color-text-muted);
    }

    .rewards-chevron {
        flex-shrink: 0;
        color: var(--color-text-muted);
        transition: transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
    }

    .rewards-panel.open .rewards-chevron {
        transform: rotate(180deg);
        color: var(--color-text);
    }

    .rewards-toggle:hover {
        background: #fdf6f0;
    }

    form {
        padding: 0 1.5rem 1.5rem;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
    }

    form input {
        font-size: 1.125rem;
        padding: 0.875rem 1rem;
        min-height: 56px;
        border: 1px solid var(--color-border);
        border-radius: 12px;
        background: var(--color-surface);
    }

    .checkin-submit {
        min-height: 56px;
        padding: 0 1.5rem;
        border: none;
        border-radius: 12px;
        background: var(--color-primary);
        color: white;
        font-size: 1.0625rem;
        font-weight: 600;
        cursor: pointer;
        transition:
            background 150ms ease,
            transform 150ms ease;
    }

    .checkin-submit:hover:not(:disabled) {
        background: var(--color-primary-hover);
    }

    .checkin-submit:active:not(:disabled) {
        transform: scale(0.99);
    }

    .checkin-submit:disabled {
        opacity: 0.7;
        cursor: wait;
    }

    .error-text {
        color: var(--color-danger);
        font-size: 0.9375rem;
        text-align: center;
        margin: 0;
    }

    .sr-only {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
    }

    @media (max-width: 480px) {
        .hero-img {
            width: 150px;
        }
        .hero h1 {
            font-size: 2.25rem;
        }
        .start-btn {
            font-size: 1.375rem;
            min-height: 72px;
        }
    }
</style>
