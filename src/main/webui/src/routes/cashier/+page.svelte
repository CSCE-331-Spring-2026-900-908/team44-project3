<script lang="ts">
    import type { MenuItem, CartItem, Customer } from '$lib/types';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { getCategories } from '$lib/api';
    import { getEmployee, getDisplayName, isManager } from '$lib/stores/auth.svelte';
    import { logout as apiLogout } from '$lib/api';
    import { formatCurrency, TAX_RATE, toTitleCase } from '$lib/utils';
    import CategoryItems from '$lib/components/CategoryItems.svelte';
    import ItemCustomization from '$lib/components/ItemCustomization.svelte';
    import CustomerCheckIn from '$lib/components/CustomerCheckIn.svelte';
    import PaymentModal from '$lib/components/PaymentModal.svelte';
    import TransactionComplete from '$lib/components/TransactionComplete.svelte';
    import LanguageSelector from '$lib/components/LanguageSelector.svelte';


    let categories = $state<string[]>([]);
    let selectedCategory = $state('');
    let cart = $state<CartItem[]>([]);
    let customer = $state<Customer | null>(null);

    let customizeItem = $state<MenuItem | null>(null);
    let showCustomize = $state(false);
    let showCheckIn = $state(false);
    let showPayment = $state(false);
    let showComplete = $state(false);

    let completedOrderId = $state(0);
    let completedTip = $state(0);
    let completedTotal = $state(0);
    let completedPointsEarned = $state(0);

    const POINTS_PER_REDEEM = 10;

    let redeemedIndices = $state<Set<number>>(new Set());

    let maxRedeemable = $derived(
        customer ? Math.floor(customer.rewardPoints / POINTS_PER_REDEEM) : 0
    );

    let discount = $derived(
        cart.reduce((sum, c, i) => sum + (redeemedIndices.has(i) ? c.item.basePrice : 0), 0)
    );

    let subtotal = $derived(cart.reduce((sum, c) => sum + c.totalPrice, 0));
    let tax = $derived(Math.round((subtotal - discount) * TAX_RATE * 100) / 100);

    $effect(() => {
        if (!getEmployee()) {
            void goto(resolve('/login'));
        }
        void loadCategories();
    });

    async function loadCategories() {
        try {
            categories = await getCategories();
            if (categories.length > 0) selectedCategory = categories[0] ?? '';
        } catch {
            categories = [];
        }
    }

    function openCustomization(item: MenuItem) {
        customizeItem = item;
        showCustomize = true;
    }

    function addToCart(cartItem: CartItem) {
        cart = [...cart, cartItem];
    }

    function removeFromCart(index: number) {
        cart = cart.filter((_, i) => i !== index);
        // Rebuild redeemed indices after removal
        const updated = new Set<number>();
        for (const idx of redeemedIndices) {
            if (idx < index) updated.add(idx);
            else if (idx > index) updated.add(idx - 1);
        }
        redeemedIndices = updated;
    }

    function toggleRedeem(index: number) {
        const next = new Set(redeemedIndices);
        if (next.has(index)) {
            next.delete(index);
        } else if (next.size < maxRedeemable) {
            next.add(index);
        }
        redeemedIndices = next;
    }

    function handleCustomerConfirm(c: Customer) {
        customer = c;
        showCheckIn = false;
    }

    function handlePaymentComplete(orderId: number, tip: number, total: number, pointsEarned: number) {
        completedOrderId = orderId;
        completedTip = tip;
        completedTotal = total;
        completedPointsEarned = pointsEarned;
        showPayment = false;
        showComplete = true;
    }

    function newSale() {
        cart = [];
        customer = null;
        redeemedIndices = new Set();
        showComplete = false;
    }

    function logout() {
        void apiLogout().then(() => goto(resolve('/login')));
    }
</script>

{#if getEmployee()}
<div class="ordering-layout">
    <header class="ordering-header">
        <div class="header-left">
            <h1>Team 44 Boba POS</h1>
            {#if isManager()}
                <span class="header-divider"></span>
                <a href={resolve('/manager')} class="dashboard-link">&larr; Dashboard</a>
            {/if}
        </div>
        <div class="header-right">
            <LanguageSelector />
            <span class="employee-name">{getDisplayName()}</span>
            <button class="btn-ghost" onclick={logout}>Logout</button>
        </div>
    </header>

    

    <div class="ordering-body">
        <aside class="category-sidebar">
            <h3>Categories</h3>
            <nav class="category-nav">
                {#each categories as cat (cat)}
                    <button
                        class="cat-btn"
                        class:active={selectedCategory === cat}
                        onclick={() => (selectedCategory = cat)}
                    >
                        {cat}
                    </button>
                {/each}
            </nav>
        </aside>

        <main class="menu-area">
            {#if selectedCategory}
                <CategoryItems category={selectedCategory} onselect={openCustomization} />
            {/if}
        </main>

        <aside class="cart-sidebar">
            <div class="cart-header">
                <h3>Current Order</h3>
                {#if customer}
                    <span class="badge badge-success">{customer.firstName}</span>
                    <span class="points-badge" class:points-redeemable={customer.rewardPoints >= 10}>
                        {customer.rewardPoints} pts
                    </span>
                {:else}
                    <button class="btn-ghost" onclick={() => (showCheckIn = true)}>
                        + Customer
                    </button>
                {/if}
            </div>

            {#if customer && maxRedeemable > 0 && maxRedeemable > redeemedIndices.size}
                <div class="free-drink-hint">
                    {maxRedeemable - redeemedIndices.size} free drink{maxRedeemable - redeemedIndices.size > 1 ? 's' : ''} available
                </div>
            {/if}

            <div class="cart-items">
                {#if cart.length === 0}
                    <p class="empty-cart">No items yet</p>
                {:else}
                    {#each cart as cartItem, i (i)}
                        <div class="cart-row" class:redeemed={redeemedIndices.has(i)}>
                            <div class="cart-item-info">
                                <span class="cart-item-name">{toTitleCase(cartItem.item.name)}</span>
                                <span class="cart-item-details">
                                    {cartItem.size} &middot; {cartItem.sweetness} &middot;
                                    {cartItem.iceLevel}
                                </span>
                                {#if cartItem.addOns.length > 0}
                                    <span class="cart-item-details">
                                        + {cartItem.addOns.map((a) => toTitleCase(a.name)).join(', ')}
                                    </span>
                                {/if}
                                <div class="cart-item-bottom">
                                    {#if redeemedIndices.has(i)}
                                        <span class="redeem-label">FREE (reward)</span>
                                        <span class="cart-item-price-row">
                                            <span class="price-struck">{formatCurrency(cartItem.item.basePrice)}</span>
                                            <span class="price-free">{formatCurrency(cartItem.totalPrice - cartItem.item.basePrice)}</span>
                                        </span>
                                    {:else}
                                        <span class="cart-item-price">{formatCurrency(cartItem.totalPrice)}</span>
                                    {/if}
                                    {#if maxRedeemable > 0 && customer}
                                        <button
                                            class="btn-ghost redeem-toggle"
                                            class:active={redeemedIndices.has(i)}
                                            onclick={() => toggleRedeem(i)}
                                            disabled={!redeemedIndices.has(i) && redeemedIndices.size >= maxRedeemable}
                                            title={redeemedIndices.has(i) ? 'Undo redeem' : 'Redeem free drink'}
                                        >
                                            {redeemedIndices.has(i) ? 'Undo' : 'Redeem'}
                                        </button>
                                    {/if}
                                </div>
                            </div>
                            <div class="cart-item-actions">
                                <button class="btn-ghost remove-btn" onclick={() => { removeFromCart(i); }}>
                                    &times;
                                </button>
                            </div>
                        </div>
                    {/each}
                {/if}
            </div>

            <div class="cart-footer">
                {#if redeemedIndices.size > 0}
                    <div class="cart-discount-banner">
                        {redeemedIndices.size} drink{redeemedIndices.size > 1 ? 's' : ''} redeemed
                        ({redeemedIndices.size * POINTS_PER_REDEEM} pts)
                    </div>
                {/if}
                <div class="cart-total">
                    <span>Subtotal</span>
                    <span>{formatCurrency(subtotal)}</span>
                </div>
                {#if discount > 0}
                    <div class="cart-discount">
                        <span>Rewards Discount</span>
                        <span>-{formatCurrency(discount)}</span>
                    </div>
                {/if}
                <div class="cart-tax">
                    <span>Tax (8.25%)</span>
                    <span>{formatCurrency(tax)}</span>
                </div>
                <div class="cart-total cart-grand-total">
                    <span>Total</span>
                    <span>{formatCurrency(subtotal - discount + tax)}</span>
                </div>
                <button
                    class="btn-primary btn-full btn-lg"
                    disabled={cart.length === 0}
                    onclick={() => (showPayment = true)}
                >
                    Charge {formatCurrency(subtotal - discount + tax)}
                </button>
            </div>
        </aside>
    </div>
</div>

<ItemCustomization
    open={showCustomize}
    item={customizeItem}
    onclose={() => (showCustomize = false)}
    onadd={addToCart}
/>

<CustomerCheckIn
    open={showCheckIn}
    mode="email"
    onclose={() => (showCheckIn = false)}
    onconfirm={handleCustomerConfirm}
/>

<PaymentModal
    open={showPayment}
    {cart}
    {customer}
    {redeemedIndices}
    onclose={() => (showPayment = false)}
    oncomplete={handlePaymentComplete}
/>

<TransactionComplete
    open={showComplete}
    orderId={completedOrderId}
    tip={completedTip}
    total={completedTotal}
    pointsEarned={completedPointsEarned}
    onnewsale={newSale}
    onclose={() => (showComplete = false)}
/>
{/if}

<style>
    .ordering-layout {
        display: flex;
        flex-direction: column;
        height: 100vh;
    }

    .ordering-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.75rem 1.5rem;
        background: var(--color-surface);
        border-bottom: 1px solid var(--color-border);
        box-shadow: var(--shadow);
    }

    .ordering-header h1 {
        font-size: 1.25rem;
        font-weight: 700;
        color: var(--color-primary);
    }

    .header-right {
        display: flex;
        align-items: center;
        gap: 0.75rem;
    }

    .header-left {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .header-divider {
        width: 1px;
        height: 1.2rem;
        background: var(--color-border);
    }

    .dashboard-link {
        font-size: 0.8rem;
        text-decoration: none;
        color: var(--color-primary);
        font-weight: 600;
        padding: 0.35rem 0.75rem;
        border-radius: 20px;
        border: 1.5px solid var(--color-primary);
        transition: background 0.15s, color 0.15s;
    }

    .dashboard-link:hover {
        background: var(--color-primary);
        color: white;
    }

    .employee-name {
        font-size: 0.875rem;
        color: var(--color-text-muted);
    }

    .ordering-body {
        display: flex;
        flex: 1;
        overflow: hidden;
    }

    .category-sidebar {
        width: 220px;
        background: var(--color-surface);
        border-right: 10px solid var(--color-border);
        padding: 1rem;
        overflow-y: auto;
    }

    .category-sidebar h3 {
        font-size: 0.95rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--color-text-muted);
        margin-bottom: 0.75rem;
    }

    .category-nav {
        display: flex;
        flex-direction: column;
        align-items: stretch;
        gap: 0.55rem;
    }

    .cat-btn {
        justify-content: flex-start;
        text-align: left;
        padding: clamp(0.5rem, 1vw, 0.85rem) clamp(0.75rem, 1.2vw, 1.1rem);
        border-radius: var(--radius);
        background: transparent;
        font-size: clamp(1.075rem, 1.4vw, 1.3rem);
        font-weight: 500;
        text-transform: capitalize;
        transition: background var(--transition);
    }

    .cat-btn:hover {
        background: var(--color-border);
    }

    .cat-btn.active {
        background: var(--color-primary);
        color: white;
    }

    .menu-area {
        flex: 1;
        padding: 1.25rem;
        overflow-y: auto;
    }

    .cart-sidebar {
        width: 320px;
        background: var(--color-surface);
        border-left: 1px solid var(--color-border);
        display: flex;
        flex-direction: column;
    }

    .cart-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 1rem 1.25rem;
        border-bottom: 1px solid var(--color-border);
    }

    .cart-header h3 {
        font-size: 1rem;
        font-weight: 600;
    }

    .cart-items {
        flex: 1;
        overflow-y: auto;
        padding: 0.75rem 1.25rem;
    }

    .empty-cart {
        text-align: center;
        color: var(--color-text-muted);
        padding: 2rem 0;
        font-size: 0.875rem;
    }

    .cart-row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding: 0.5rem 0;
        border-bottom: 1px solid var(--color-border);
    }

    .cart-item-name {
        font-weight: 500;
        font-size: 0.875rem;
    }

    .cart-item-details {
        display: block;
        font-size: 0.75rem;
        color: var(--color-text-muted);
    }

    .cart-item-actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.875rem;
        font-weight: 500;
    }

    .remove-btn {
        font-size: 1.25rem;
        padding: 0 0.25rem;
        color: var(--color-danger);
    }

    .cart-footer {
        padding: 1rem 1.25rem;
        border-top: 1px solid var(--color-border);
    }

    .cart-total {
        display: flex;
        justify-content: space-between;
        font-weight: 600;
        margin-bottom: 0.35rem;
    }

    .cart-tax {
        display: flex;
        justify-content: space-between;
        font-size: 0.8rem;
        color: var(--color-text-muted);
        margin-bottom: 0.35rem;
    }

    .cart-grand-total {
        border-top: 1px solid var(--color-border);
        padding-top: 0.5rem;
        margin-top: 0.25rem;
        margin-bottom: 0.75rem;
        font-size: 1.05rem;
    }

    .free-drink-hint {
        text-align: center;
        font-size: 0.78rem;
        font-weight: 600;
        color: #e65100;
        background: #fff3e0;
        padding: 0.35rem 0.75rem;
        margin: 0 1.25rem;
        border-radius: var(--radius);
    }

    .cart-row.redeemed {
        background: #f0faf0;
        border-radius: var(--radius);
        padding: 0.5rem 0.4rem;
        border-bottom-color: transparent;
    }

    .cart-item-bottom {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-top: 0.25rem;
    }

    .cart-item-price {
        font-weight: 500;
        font-size: 0.875rem;
    }

    .cart-item-price-row {
        display: inline-flex;
        align-items: center;
        gap: 0.3rem;
    }

    .redeem-label {
        display: inline-block;
        font-size: 0.7rem;
        font-weight: 700;
        color: #2e7d32;
        background: #e8f5e9;
        padding: 0.1rem 0.4rem;
        border-radius: 4px;
    }

    .price-struck {
        text-decoration: line-through;
        color: var(--color-text-muted);
        font-size: 0.8rem;
    }

    .price-free {
        font-weight: 600;
        color: #2e7d32;
    }

    .redeem-toggle {
        font-size: 0.7rem;
        padding: 0.15rem 0.4rem;
        border-radius: 4px;
        color: #e65100;
        border: 1px solid #e65100;
        background: transparent;
        font-weight: 600;
    }

    .redeem-toggle.active {
        background: #e65100;
        color: white;
    }

    .redeem-toggle:disabled {
        opacity: 0.3;
        cursor: not-allowed;
    }

    .cart-discount-banner {
        text-align: center;
        font-size: 0.8rem;
        font-weight: 600;
        color: #2e7d32;
        background: #e8f5e9;
        padding: 0.4rem;
        border-radius: var(--radius);
        margin-bottom: 0.5rem;
    }

    .cart-discount {
        display: flex;
        justify-content: space-between;
        font-size: 0.8rem;
        color: #2e7d32;
        font-weight: 600;
        margin-bottom: 0.35rem;
    }

    .points-badge {
        font-size: 0.75rem;
        font-weight: 600;
        padding: 0.15rem 0.5rem;
        border-radius: 12px;
        background: var(--color-border);
        color: var(--color-text-muted);
    }

    .points-badge.points-redeemable {
        background: #fff3e0;
        color: #e65100;
        animation: pulse-glow 2s ease-in-out infinite;
    }

    @keyframes pulse-glow {
        0%, 100% { box-shadow: 0 0 0 0 rgba(230, 81, 0, 0.2); }
        50% { box-shadow: 0 0 8px 2px rgba(230, 81, 0, 0.3); }
    }
</style>
