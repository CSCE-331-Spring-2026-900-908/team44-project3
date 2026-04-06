<script lang="ts">
    import type { MenuItem, CartItem, Customer } from '$lib/types';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { getCategories, getItemsByCategory } from '$lib/api';
    import { getCustomer, clearCustomer } from '$lib/stores/auth.svelte';
    import { formatCurrency, TAX_RATE, toTitleCase } from '$lib/utils';
    import ItemCustomization from '$lib/components/ItemCustomization.svelte';
    import CustomerCheckIn from '$lib/components/CustomerCheckIn.svelte';
    import PaymentModal from '$lib/components/PaymentModal.svelte';
    import TransactionComplete from '$lib/components/TransactionComplete.svelte';

    const categoryEmojis: Record<string, string> = {
        milk_tea: '\u{1F95B}',
        fruit_tea: '\u{1F353}',
        slush: '\u{1F9CA}',
        coffee: '\u2615',
        classic: '\u{1F375}',
        seasonal: '\u{1F338}',
        topping: '\u{1F369}'
    };

    let categories = $state<string[]>([]);
    let selectedCategory = $state('');
    let items = $state<MenuItem[]>([]);
    let itemsLoading = $state(false);
    let cart = $state<CartItem[]>([]);
    let customer = $state<Customer | null>(getCustomer());

    let customizeItem = $state<MenuItem | null>(null);
    let showCustomize = $state(false);
    let showCheckIn = $state(false);
    let showPayment = $state(false);
    let showComplete = $state(false);

    let completedOrderId = $state(0);
    let completedTip = $state(0);
    let completedTotal = $state(0);

    let subtotal = $derived(cart.reduce((sum, c) => sum + c.totalPrice, 0));
    let tax = $derived(Math.round(subtotal * TAX_RATE * 100) / 100);

    $effect(() => {
        void loadCategories();
    });

    $effect(() => {
        if (selectedCategory) void loadItems(selectedCategory);
    });

    async function loadCategories() {
        try {
            categories = await getCategories();
            if (categories.length > 0) selectedCategory = categories[0] ?? '';
        } catch {
            categories = [];
        }
    }

    async function loadItems(category: string) {
        itemsLoading = true;
        try {
            items = await getItemsByCategory(category);
        } catch {
            items = [];
        } finally {
            itemsLoading = false;
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
    }

    function handleCustomerConfirm(c: Customer) {
        customer = c;
        showCheckIn = false;
    }

    function handlePaymentComplete(orderId: number, tip: number, total: number) {
        completedOrderId = orderId;
        completedTip = tip;
        completedTotal = total;
        showPayment = false;
        showComplete = true;
    }

    function newSale() {
        cart = [];
        customer = null;
        showComplete = false;
    }

    function startOver() {
        clearCustomer();
        void goto(resolve('/'));
    }

    function formatCategory(cat: string): string {
        return cat.replace(/_/g, ' ');
    }
</script>

<div class="order-page">
    <!-- Header -->
    <header class="order-header">
        <div class="header-left">
            <h1>Team 44 Boba</h1>
        </div>
        <div class="header-right">
            {#if customer}
                <span class="welcome-text">Hi, {customer.email}</span>
            {:else}
                <button class="header-btn" onclick={() => (showCheckIn = true)}>
                    Sign In
                </button>
            {/if}
            <button class="header-btn subtle" onclick={startOver}>Start Over</button>
        </div>
    </header>

    <div class="order-body">
        <!-- Main content -->
        <div class="menu-section">
            <!-- Hero banner -->
            <div class="hero-banner">
                <div class="hero-text">
                    <h2>{formatCategory(selectedCategory)}</h2>
                    <p>{items.length} {items.length === 1 ? 'item' : 'items'} available</p>
                </div>
                <div class="hero-emoji">{categoryEmojis[selectedCategory] ?? '\u{1F964}'}</div>
            </div>

            <!-- Category pills -->
            <div class="category-strip">
                {#each categories as cat (cat)}
                    <button
                        class="cat-pill"
                        class:active={selectedCategory === cat}
                        onclick={() => (selectedCategory = cat)}
                    >
                        <span class="cat-emoji">{categoryEmojis[cat] ?? '\u{1F964}'}</span>
                        <span class="cat-label">{formatCategory(cat)}</span>
                    </button>
                {/each}
            </div>

            <!-- Items grid -->
            <h3 class="section-title">{formatCategory(selectedCategory)}</h3>

            {#if itemsLoading}
                <p class="muted-text">Loading...</p>
            {:else if items.length === 0}
                <p class="muted-text">No items in this category.</p>
            {:else}
                <div class="items-grid">
                    {#each items as item (item.menuItemId)}
                        <button
                            class="item-card"
                            class:unavailable={!item.isAvailable}
                            onclick={() => openCustomization(item)}
                            disabled={!item.isAvailable}
                        >
                            <div class="item-icon">{categoryEmojis[item.category] ?? '\u{1F964}'}</div>
                            <div class="item-info">
                                <span class="item-name">{toTitleCase(item.name)}</span>
                            </div>
                            <div class="item-bottom">
                                <span class="item-price">{formatCurrency(item.basePrice)}</span>
                                {#if item.isAvailable}
                                    <span class="add-icon">+</span>
                                {:else}
                                    <span class="sold-out">Sold Out</span>
                                {/if}
                            </div>
                        </button>
                    {/each}
                </div>
            {/if}
        </div>

        <!-- Cart sidebar -->
        <aside class="cart-panel">
            <h3 class="cart-title">Your Cart</h3>

            <div class="cart-items">
                {#if cart.length === 0}
                    <p class="cart-empty">Your cart is empty. Browse the menu and add items!</p>
                {:else}
                    {#each cart as cartItem, i (i)}
                        <div class="cart-card">
                            <div class="cart-card-icon">{categoryEmojis[cartItem.item.category] ?? '\u{1F964}'}</div>
                            <div class="cart-card-info">
                                <span class="cart-card-name">{toTitleCase(cartItem.item.name)}</span>
                                <span class="cart-card-details">
                                    {cartItem.size} &middot; {cartItem.sweetness} &middot; {cartItem.iceLevel}
                                </span>
                                {#if cartItem.addOns.length > 0}
                                    <span class="cart-card-details">
                                        + {cartItem.addOns.map((a) => toTitleCase(a.name)).join(', ')}
                                    </span>
                                {/if}
                                <span class="cart-card-price">{formatCurrency(cartItem.totalPrice)}</span>
                            </div>
                            <button class="cart-remove" onclick={() => removeFromCart(i)}>&times;</button>
                        </div>
                    {/each}
                {/if}
            </div>

            <div class="cart-summary">
                <div class="summary-line">
                    <span>Subtotal</span>
                    <span>{formatCurrency(subtotal)}</span>
                </div>
                <div class="summary-line tax-line">
                    <span>Tax (8.25%)</span>
                    <span>{formatCurrency(tax)}</span>
                </div>
                <div class="summary-line total-line">
                    <span>Total</span>
                    <span>{formatCurrency(subtotal + tax)}</span>
                </div>
                <button
                    class="pay-btn"
                    disabled={cart.length === 0}
                    onclick={() => (showPayment = true)}
                >
                    Pay {formatCurrency(subtotal + tax)}
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
    employeeId={null}
    onclose={() => (showPayment = false)}
    oncomplete={handlePaymentComplete}
/>

<TransactionComplete
    open={showComplete}
    orderId={completedOrderId}
    tip={completedTip}
    total={completedTotal}
    onnewsale={newSale}
    onclose={() => (showComplete = false)}
/>

<style>
    /* ── Page ── */
    .order-page {
        display: flex;
        flex-direction: column;
        height: 100vh;
        background: #faf5ee;
    }

    /* ── Header ── */
    .order-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.75rem 2rem;
        background: white;
        border-bottom: 1px solid #ece4d8;
    }

    .order-header h1 {
        font-size: 1.4rem;
        font-weight: 800;
        color: #d4712a;
    }

    .header-right {
        display: flex;
        align-items: center;
        gap: 0.75rem;
    }

    .welcome-text {
        font-size: 0.85rem;
        color: #8b7355;
        font-weight: 500;
    }

    .header-btn {
        padding: 0.4rem 1rem;
        border-radius: 20px;
        border: 2px solid #d4712a;
        background: transparent;
        color: #d4712a;
        font-weight: 600;
        font-size: 0.85rem;
        cursor: pointer;
        transition: all 0.15s;
    }

    .header-btn:hover {
        background: #d4712a;
        color: white;
    }

    .header-btn.subtle {
        border-color: #ccc;
        color: #999;
    }

    .header-btn.subtle:hover {
        border-color: #999;
        background: #f0f0f0;
        color: #666;
    }

    /* ── Body ── */
    .order-body {
        display: flex;
        flex: 1;
        overflow: hidden;
    }

    /* ── Menu Section ── */
    .menu-section {
        flex: 1;
        overflow-y: auto;
        padding: 1.5rem 2rem;
    }

    /* ── Hero Banner ── */
    .hero-banner {
        display: flex;
        align-items: center;
        justify-content: space-between;
        background: linear-gradient(135deg, #d4712a 0%, #e8944c 100%);
        border-radius: 16px;
        padding: 1.5rem 2rem;
        color: white;
        margin-bottom: 1.5rem;
    }

    .hero-text h2 {
        font-size: 1.6rem;
        font-weight: 800;
        margin-bottom: 0.25rem;
        text-transform: capitalize;
    }

    .hero-text p {
        font-size: 0.9rem;
        opacity: 0.85;
    }

    .hero-emoji {
        font-size: 3.5rem;
    }

    /* ── Category Pills ── */
    .category-strip {
        display: flex;
        gap: 1rem;
        margin-bottom: 1.5rem;
        overflow-x: auto;
        padding-bottom: 0.5rem;
    }

    .cat-pill {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.4rem;
        min-width: 80px;
        padding: 0.75rem 0.5rem;
        border-radius: 16px;
        border: 2px solid transparent;
        background: white;
        cursor: pointer;
        transition: all 0.15s;
        box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    }

    .cat-pill:hover {
        border-color: #d4712a;
    }

    .cat-pill.active {
        border-color: #d4712a;
        background: #fff7f0;
        box-shadow: 0 2px 8px rgba(212, 113, 42, 0.15);
    }

    .cat-emoji {
        font-size: 1.5rem;
    }

    .cat-label {
        font-size: 0.7rem;
        font-weight: 600;
        text-transform: capitalize;
        color: #555;
    }

    .cat-pill.active .cat-label {
        color: #d4712a;
    }

    /* ── Section Title ── */
    .section-title {
        font-size: 1.4rem;
        font-weight: 700;
        text-transform: capitalize;
        margin-bottom: 1rem;
        color: #333;
    }

    .muted-text {
        color: #999;
        font-size: 0.9rem;
    }

    /* ── Items Grid ── */
    .items-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 1rem;
    }

    .item-card {
        background: white;
        border-radius: 16px;
        padding: 1.25rem;
        border: none;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        cursor: pointer;
        transition: transform 0.15s, box-shadow 0.15s;
        box-shadow: 0 1px 4px rgba(0,0,0,0.06);
        text-align: center;
    }

    .item-card:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(0,0,0,0.1);
    }

    .item-card.unavailable {
        opacity: 0.45;
        cursor: not-allowed;
    }

    .item-icon {
        font-size: 2.5rem;
        margin-bottom: 0.25rem;
    }

    .item-info {
        display: flex;
        flex-direction: column;
        gap: 0.15rem;
    }

    .item-name {
        font-weight: 600;
        font-size: 0.95rem;
        color: #333;
    }

    .item-bottom {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        margin-top: 0.25rem;
    }

    .item-price {
        font-weight: 700;
        font-size: 1rem;
        color: #d4712a;
    }

    .add-icon {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background: #d4712a;
        color: white;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: 1.2rem;
        font-weight: 700;
        line-height: 1;
        flex-shrink: 0;
    }

    .sold-out {
        font-size: 0.7rem;
        font-weight: 600;
        color: #e74c3c;
        text-transform: uppercase;
    }

    /* ── Cart Panel ── */
    .cart-panel {
        width: 340px;
        background: white;
        border-left: 1px solid #ece4d8;
        display: flex;
        flex-direction: column;
        padding: 1.5rem;
    }

    .cart-title {
        font-size: 1.6rem;
        font-weight: 800;
        text-align: center;
        margin-bottom: 1rem;
        color: #333;
    }

    .cart-items {
        flex: 1;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
    }

    .cart-empty {
        text-align: center;
        color: #b8a898;
        font-size: 0.85rem;
        padding: 2rem 0.5rem;
    }

    .cart-card {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        background: #faf5ee;
        border-radius: 12px;
        padding: 0.75rem;
        border: 1px solid #ece4d8;
    }

    .cart-card-icon {
        font-size: 3rem;
        flex-shrink: 0;
        width: 50px;
        height: 50px;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .cart-card-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 0.1rem;
    }

    .cart-card-name {
        font-weight: 600;
        font-size: 0.85rem;
        color: #333;
    }

    .cart-card-details {
        font-size: 0.7rem;
        color: #999;
    }

    .cart-card-price {
        font-weight: 700;
        font-size: 0.85rem;
        color: #d4712a;
        margin-top: 0.15rem;
    }

    .cart-remove {
        flex-shrink: 0;
        width: 24px;
        height: 24px;
        border-radius: 50%;
        border: none;
        background: #f0e6da;
        color: #c4956a;
        font-size: 1rem;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.15s, color 0.15s;
    }

    .cart-remove:hover {
        background: #e74c3c;
        color: white;
    }

    /* ── Cart Summary ── */
    .cart-summary {
        border-top: 1px solid #ece4d8;
        padding-top: 1rem;
        margin-top: 1rem;
    }

    .summary-line {
        display: flex;
        justify-content: space-between;
        font-size: 0.9rem;
        margin-bottom: 0.4rem;
        color: #666;
    }

    .tax-line {
        font-size: 0.85rem;
        color: #999;
    }

    .total-line {
        font-weight: 700;
        font-size: 1.05rem;
        color: #333;
        border-top: 1px solid #ece4d8;
        padding-top: 0.5rem;
        margin-top: 0.25rem;
        margin-bottom: 0.75rem;
    }

    .pay-btn {
        width: 100%;
        padding: 1rem;
        border: none;
        border-radius: 14px;
        background: linear-gradient(135deg, #d4712a, #e8944c);
        color: white;
        font-size: 1.15rem;
        font-weight: 700;
        cursor: pointer;
        transition: opacity 0.15s, transform 0.1s;
    }

    .pay-btn:hover:not(:disabled) {
        opacity: 0.9;
        transform: scale(1.01);
    }

    .pay-btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
    }
</style>
