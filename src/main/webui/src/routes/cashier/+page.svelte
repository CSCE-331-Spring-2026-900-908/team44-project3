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

    function groupAddOns(addOns: MenuItem[]): [string, number, number][] {
        const map = new Map<number, { name: string; qty: number; price: number }>();
        for (const a of addOns) {
            const existing = map.get(a.menuItemId);
            if (existing) existing.qty++;
            else map.set(a.menuItemId, { name: toTitleCase(a.name), qty: 1, price: a.basePrice });
        }
        return Array.from(map.values()).map(e => [e.name, e.qty, e.price]);
    }

    let categories = $state<string[]>([]);
    let selectedCategory = $state('');
    let cart = $state<CartItem[]>([]);
    let customer = $state<Customer | null>(null);

    let customizeItem = $state<MenuItem | null>(null);
    let customizeVariants = $state<MenuItem[]>([]);
    let showCustomize = $state(false);
    let editingIndex = $state<number | null>(null);
    let editingCartItem = $state<CartItem | null>(null);
    let showCheckIn = $state(false);
    let showPayment = $state(false);
    let showComplete = $state(false);

    let completedOrderId = $state(0);
    let completedTip = $state(0);
    let completedTotal = $state(0);
    let completedPointsEarned = $state(0);

    const POINTS_PER_REDEEM = 10;

    let redeemedCounts = $state(new Map<number, number>());

    let totalRedeemed = $derived(
        Array.from(redeemedCounts.values()).reduce((sum, n) => sum + n, 0)
    );

    let maxRedeemable = $derived(
        customer ? Math.floor(customer.rewardPoints / POINTS_PER_REDEEM) : 0
    );

    let discount = $derived(
        cart.reduce((sum, c, i) => sum + (redeemedCounts.get(i) ?? 0) * c.item.basePrice, 0)
    );

    let subtotal = $derived(cart.reduce((sum, c) => sum + c.totalPrice * c.quantity, 0));
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

    function openCustomization(variants: MenuItem[]) {
        customizeVariants = variants;
        customizeItem = variants[0] ?? null;
        showCustomize = true;
    }

    function sameAddOns(a: MenuItem[], b: MenuItem[]): boolean {
        if (a.length !== b.length) return false;
        const ids = (list: MenuItem[]) => list.map(x => x.menuItemId).sort((x, y) => x - y);
        const sa = ids(a), sb = ids(b);
        return sa.every((id, idx) => id === sb[idx]);
    }

    function addToCart(cartItem: CartItem) {
        const existingIndex = cart.findIndex(c =>
            c.item.menuItemId === cartItem.item.menuItemId &&
            c.size === cartItem.size &&
            c.sweetness === cartItem.sweetness &&
            c.iceLevel === cartItem.iceLevel &&
            sameAddOns(c.addOns, cartItem.addOns)
        );
        if (existingIndex >= 0) {
            cart = cart.map((c, i) =>
                i === existingIndex ? { ...c, quantity: c.quantity + 1 } : c
            );
        } else {
            cart = [...cart, { ...cartItem, quantity: 1 }];
        }
    }

    function removeFromCart(index: number) {
        cart = cart.filter((_, i) => i !== index);
        const updated = new Map<number, number>();
        for (const [idx, count] of redeemedCounts) {
            if (idx < index) updated.set(idx, count);
            else if (idx > index) updated.set(idx - 1, count);
        }
        redeemedCounts = updated;
    }

    function incrementQuantity(index: number) {
        cart = cart.map((c, i) =>
            i === index ? { ...c, quantity: c.quantity + 1 } : c
        );
    }

    function decrementQuantity(index: number) {
        const item = cart[index];
        if (item && item.quantity <= 1) {
            removeFromCart(index);
        } else {
            cart = cart.map((c, i) =>
                i === index ? { ...c, quantity: c.quantity - 1 } : c
            );
        }
    }

    function openEditItem(index: number) {
        editingIndex = index;
        editingCartItem = cart[index] ?? null;
        const cartMenuItem = cart[index]?.item;
        customizeItem = cartMenuItem ?? null;
        customizeVariants = cartMenuItem ? [cartMenuItem] : [];
        showCustomize = true;
    }

    function saveEditedItem(updatedCartItem: CartItem) {
        if (editingIndex !== null) {
            cart = cart.map((c, i) =>
                i === editingIndex ? updatedCartItem : c
            );
            editingIndex = null;
            editingCartItem = null;
        }
    }

    function redeemOne(index: number) {
        const current = redeemedCounts.get(index) ?? 0;
        if (totalRedeemed >= maxRedeemable) return;
        if (current >= cart[index].quantity) return;
        const next = new Map(redeemedCounts);
        next.set(index, current + 1);
        redeemedCounts = next;
    }

    function unredeemOne(index: number) {
        const current = redeemedCounts.get(index) ?? 0;
        if (current <= 0) return;
        const next = new Map(redeemedCounts);
        if (current === 1) next.delete(index);
        else next.set(index, current - 1);
        redeemedCounts = next;
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
        redeemedCounts = new Map();
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
            <h1>Boba Bob's</h1>
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

            {#if customer && maxRedeemable > 0 && maxRedeemable > totalRedeemed}
                <div class="free-drink-hint">
                    {maxRedeemable - totalRedeemed} free drink{maxRedeemable - totalRedeemed > 1 ? 's' : ''} available
                </div>
            {/if}

            <div class="cart-items">
                {#if cart.length === 0}
                    <p class="empty-cart">No items yet</p>
                {:else}
                    {#each cart as cartItem, i (i)}
                        {@const redeemed = redeemedCounts.get(i) ?? 0}
                        <div class="cart-row" class:redeemed={redeemed > 0}>
                            <div class="cart-item-info">
                                <span class="cart-item-name">{toTitleCase(cartItem.item.name)}</span>
                                <span class="cart-item-details">Size: {toTitleCase(cartItem.size)}</span>
                                <span class="cart-item-details">Sweetness: {cartItem.sweetness}</span>
                                <span class="cart-item-details">{cartItem.iceLevel}</span>
                                {#if cartItem.addOns.length > 0}
                                    <div class="cart-addons">
                                        <span class="cart-addons-label">Add-ons:</span>
                                        {#each groupAddOns(cartItem.addOns) as [name, qty, price]}
                                            <span class="cart-addon-item">
                                                {qty > 1 ? `${qty}x ` : ''}{name}
                                                <span class="cart-addon-price">{formatCurrency(price * qty)}</span>
                                            </span>
                                        {/each}
                                    </div>
                                {/if}
                                <div class="cart-qty-row">
                                    <button class="qty-btn" onclick={() => decrementQuantity(i)}><svg viewBox="0 0 12 12" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="2" y1="6" x2="10" y2="6" /></svg></button>
                                    <span class="qty-value">{cartItem.quantity}</span>
                                    <button class="qty-btn" onclick={() => incrementQuantity(i)}><svg viewBox="0 0 12 12" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="6" y1="2" x2="6" y2="10" /><line x1="2" y1="6" x2="10" y2="6" /></svg></button>
                                    <button class="cart-edit-btn" onclick={() => openEditItem(i)}>Edit</button>
                                </div>
                                <div class="cart-item-bottom">
                                    {#if redeemed > 0}
                                        <span class="redeem-label">{cartItem.quantity > 1 ? `${redeemed}x ` : ''}FREE (reward)</span>
                                        <span class="cart-item-price-row">
                                            <span class="price-struck">{formatCurrency(redeemed * cartItem.item.basePrice)}</span>
                                            <span class="price-free">{formatCurrency(cartItem.totalPrice * cartItem.quantity - redeemed * cartItem.item.basePrice)}</span>
                                        </span>
                                    {:else}
                                        <span class="cart-item-price">{formatCurrency(cartItem.totalPrice * cartItem.quantity)}</span>
                                    {/if}
                                    {#if maxRedeemable > 0 && customer}
                                        {#if redeemed > 0}
                                            <button
                                                class="btn-ghost redeem-toggle active"
                                                onclick={() => unredeemOne(i)}
                                                title="Undo redeem"
                                            >
                                                Undo
                                            </button>
                                        {/if}
                                        {#if redeemed < cartItem.quantity && totalRedeemed < maxRedeemable}
                                            <button
                                                class="btn-ghost redeem-toggle"
                                                onclick={() => redeemOne(i)}
                                                title="Redeem free drink"
                                            >
                                                Redeem
                                            </button>
                                        {/if}
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
                {#if totalRedeemed > 0}
                    <div class="cart-discount-banner">
                        {totalRedeemed} drink{totalRedeemed > 1 ? 's' : ''} redeemed
                        ({totalRedeemed * POINTS_PER_REDEEM} pts)
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
    variants={customizeVariants}
    {editingCartItem}
    onclose={() => {
        showCustomize = false;
        editingIndex = null;
        editingCartItem = null;
    }}
    onadd={addToCart}
    onsave={saveEditedItem}
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
    {redeemedCounts}
    onclose={() => (showPayment = false)}
    oncomplete={handlePaymentComplete}
/>

<TransactionComplete
    open={showComplete}
    orderId={completedOrderId}
    tip={completedTip}
    total={completedTotal}
    pointsEarned={completedPointsEarned}
    customerEmail={customer?.email ?? ''}
    cart={cart}
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

    .cart-qty-row {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        margin-top: 0.25rem;
    }

    .qty-btn {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        border: 1px solid var(--color-primary);
        background: white;
        color: var(--color-primary);
        font-size: 0.9rem;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0;
    }

    .qty-btn:hover {
        background: var(--color-primary);
        color: white;
    }

    .qty-value {
        font-weight: 700;
        font-size: 0.85rem;
        min-width: 1.2rem;
        text-align: center;
        font-variant-numeric: tabular-nums;
    }

    .cart-edit-btn {
        margin-left: auto;
        padding: 0.15rem 0.5rem;
        border-radius: var(--radius);
        border: 1px solid var(--color-primary);
        background: transparent;
        color: var(--color-primary);
        font-size: 0.7rem;
        font-weight: 600;
    }

    .cart-edit-btn:hover {
        background: var(--color-primary);
        color: white;
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

    .cart-addons {
        display: flex;
        flex-direction: column;
        gap: 0.1rem;
        margin-top: 0.15rem;
        padding-left: 0.25rem;
    }

    .cart-addons-label {
        font-size: 0.65rem;
        font-weight: 600;
        color: var(--color-text-muted);
        text-transform: uppercase;
        letter-spacing: 0.03em;
    }

    .cart-addon-item {
        font-size: 0.75rem;
        color: var(--color-text-muted);
        padding-left: 0.5rem;
        display: flex;
        justify-content: space-between;
    }

    .cart-addon-price {
        color: var(--color-text-muted);
        opacity: 0.7;
        margin-left: 0.5rem;
        flex-shrink: 0;
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
