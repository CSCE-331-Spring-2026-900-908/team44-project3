<script lang="ts">
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { restoreCustomer } from '$lib/stores/auth.svelte';
    import LanguageSelector from '$lib/components/LanguageSelector.svelte';

    $effect(() => {
        if (restoreCustomer()) void goto(resolve('/customer'));
    });
</script>

<div class="portal">
    <div class="portal-lang">
        <LanguageSelector />
    </div>
    <header class="portal-header">
        <img src="/boba.png" alt="" class="logo" />
        <h1>Team 44 Boba</h1>
        <p class="subtitle">Select your station</p>
    </header>

    <nav class="tile-grid" aria-label="Station selector">
        <a class="tile tile--kiosk" href={resolve('/customer')}>
            <span class="tile-eyebrow">Guest</span>
            <span class="tile-title">Customer Kiosk</span>
            <span class="tile-sub">Self-serve ordering</span>
            <svg
                class="tile-arrow"
                aria-hidden="true"
                viewBox="0 0 40 12"
                width="40"
                height="12"
                fill="none"
                stroke="currentColor"
                stroke-width="1.25"
                stroke-linecap="round"
                stroke-linejoin="round"
            >
                <line x1="0" y1="6" x2="34" y2="6" />
                <polyline points="28,1 34,6 28,11" />
            </svg>
        </a>

        <a class="tile tile--staff" href={resolve('/login')}>
            <span class="tile-eyebrow">Employees</span>
            <span class="tile-title">Staff Login</span>
            <span class="tile-sub">Cashier &amp; manager sign-in</span>
            <svg
                class="tile-arrow"
                aria-hidden="true"
                viewBox="0 0 40 12"
                width="40"
                height="12"
                fill="none"
                stroke="currentColor"
                stroke-width="1.25"
                stroke-linecap="round"
                stroke-linejoin="round"
            >
                <line x1="0" y1="6" x2="34" y2="6" />
                <polyline points="28,1 34,6 28,11" />
            </svg>
        </a>

        <a class="tile tile--display" href={resolve('/display')}>
            <span class="tile-eyebrow">In-Store</span>
            <span class="tile-title">Store Displays</span>
            <span class="tile-sub">Menu board, kitchen &amp; pickup</span>
            <svg
                class="tile-arrow"
                aria-hidden="true"
                viewBox="0 0 40 12"
                width="40"
                height="12"
                fill="none"
                stroke="currentColor"
                stroke-width="1.25"
                stroke-linecap="round"
                stroke-linejoin="round"
            >
                <line x1="0" y1="6" x2="34" y2="6" />
                <polyline points="28,1 34,6 28,11" />
            </svg>
        </a>
    </nav>

    <footer class="portal-footer">
        <span>Bubble-tea POS · team44</span>
    </footer>
</div>

<style>
    .portal-lang {
        position: absolute;
        top: 1rem;
        right: 1.5rem;
        z-index: 50;
    }

    .portal {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 3rem 1.5rem;
        position: relative;
        overflow: hidden;
        background:
            radial-gradient(circle at 20% 15%, rgba(255, 90, 90, 0.18), transparent 55%),
            radial-gradient(circle at 85% 85%, rgba(255, 212, 90, 0.22), transparent 55%),
            var(--color-bg);
    }

    .portal-header {
        text-align: center;
        margin-bottom: 3rem;
    }

    .logo {
        width: 80px;
        height: auto;
        margin-bottom: 0.75rem;
        filter: drop-shadow(0 4px 12px rgba(45, 32, 23, 0.12));
    }

    .portal-header h1 {
        font-size: 2.25rem;
        font-weight: 800;
        color: var(--color-text);
        letter-spacing: -0.02em;
    }

    .subtitle {
        font-size: 1rem;
        color: var(--color-text-muted);
        margin-top: 0.25rem;
        text-transform: uppercase;
        letter-spacing: 0.12em;
        font-size: 0.75rem;
    }

    .tile-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 1.5rem;
        width: 100%;
        max-width: 960px;
    }

    .tile {
        --tile-from: var(--color-primary);
        --tile-to: var(--color-secondary);
        position: relative;
        display: grid;
        grid-template-rows: auto auto auto;
        gap: 0.5rem;
        padding: 2rem 1.75rem;
        min-height: 220px;
        text-align: left;
        text-decoration: none;
        border-radius: 20px;
        background:
            linear-gradient(135deg, var(--tile-from), var(--tile-to));
        color: white;
        border: none;
        cursor: pointer;
        overflow: hidden;
        box-shadow:
            0 10px 20px -10px rgba(45, 32, 23, 0.25),
            0 4px 8px -4px rgba(45, 32, 23, 0.1);
        transition:
            transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1),
            box-shadow 220ms ease;
    }

    .tile--kiosk {
        --tile-from: #FF5A5A;
        --tile-to: #FF8B5A;
    }

    .tile--staff {
        --tile-from: #FFA95A;
        --tile-to: #FFD45A;
        color: #4a3310;
    }

    .tile--display {
        --tile-from: #2d2017;
        --tile-to: #5a4330;
    }

    .tile::before {
        content: '';
        position: absolute;
        top: -40%;
        right: -30%;
        width: 180px;
        height: 180px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.12);
        transition: transform 400ms cubic-bezier(0.2, 0.8, 0.2, 1);
    }

    .tile:hover {
        transform: translateY(-4px);
        box-shadow:
            0 20px 30px -12px rgba(45, 32, 23, 0.3),
            0 8px 16px -8px rgba(45, 32, 23, 0.15);
    }

    .tile:hover::before {
        transform: scale(1.25) translate(-10%, 10%);
    }

    .tile:hover .tile-arrow {
        transform: translateX(6px);
    }

    .tile:focus-visible {
        outline: 3px solid var(--color-text);
        outline-offset: 3px;
    }

    .tile:active {
        transform: translateY(-1px);
    }

    .tile-eyebrow {
        font-size: 0.7rem;
        font-weight: 600;
        letter-spacing: 0.18em;
        text-transform: uppercase;
        opacity: 0.75;
    }

    .tile-title {
        font-size: 1.5rem;
        font-weight: 700;
        letter-spacing: -0.01em;
        margin-top: 0.25rem;
    }

    .tile-sub {
        font-size: 0.9rem;
        opacity: 0.85;
    }

    .tile-arrow {
        position: absolute;
        bottom: 1.5rem;
        right: 1.75rem;
        opacity: 0.75;
        transition: transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1), opacity 220ms ease;
    }

    .tile:hover .tile-arrow {
        transform: translateX(6px);
        opacity: 1;
    }

    .portal-footer {
        margin-top: 3rem;
        font-size: 0.75rem;
        color: var(--color-text-muted);
        letter-spacing: 0.08em;
        text-transform: uppercase;
    }

    @media (prefers-reduced-motion: reduce) {
        .tile,
        .tile::before,
        .tile-arrow {
            transition: none;
        }
    }
</style>
