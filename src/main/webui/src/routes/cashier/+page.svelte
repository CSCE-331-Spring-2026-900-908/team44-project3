<script lang="ts">
    import type { MenuItem, CartItem, Customer } from '$lib/types';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { getCategories } from '$lib/api';
    import { getEmployee, setEmployee, getDisplayName } from '$lib/stores/auth.svelte';
    import { formatCurrency } from '$lib/utils';
    import CategoryItems from '$lib/components/CategoryItems.svelte';
    import ItemCustomization from '$lib/components/ItemCustomization.svelte';
    import CustomerCheckIn from '$lib/components/CustomerCheckIn.svelte';
    import PaymentModal from '$lib/components/PaymentModal.svelte';
    import TransactionComplete from '$lib/components/TransactionComplete.svelte';

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

    let subtotal = $derived(cart.reduce((sum, c) => sum + c.totalPrice, 0));

    $effect(() => {
        if (!getEmployee()) {
            void goto(resolve('/'));
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

    function logout() {
        setEmployee(null);
        void goto(resolve('/'));
    }
</script>

<div class="ordering-layout">
    <header class="ordering-header">
        <h1>Team 44 Boba POS</h1>
        <div class="header-right">
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
                {:else}
                    <button class="btn-ghost" onclick={() => (showCheckIn = true)}>
                        + Customer
                    </button>
                {/if}
            </div>

            <div class="cart-items">
                {#if cart.length === 0}
                    <p class="empty-cart">No items yet</p>
                {:else}
                    {#each cart as cartItem, i (i)}
                        <div class="cart-row">
                            <div class="cart-item-info">
                                <span class="cart-item-name">{cartItem.item.name}</span>
                                <span class="cart-item-details">
                                    {cartItem.size} &middot; {cartItem.sweetness} &middot;
                                    {cartItem.iceLevel}
                                </span>
                                {#if cartItem.addOns.length > 0}
                                    <span class="cart-item-details">
                                        + {cartItem.addOns.map((a) => a.name).join(', ')}
                                    </span>
                                {/if}
                            </div>
                            <div class="cart-item-actions">
                                <span>{formatCurrency(cartItem.totalPrice)}</span>
                                <button class="btn-ghost remove-btn" onclick={() => { removeFromCart(i); }}>
                                    &times;
                                </button>
                            </div>
                        </div>
                    {/each}
                {/if}
            </div>

            <div class="cart-footer">
                <div class="cart-total">
                    <span>Subtotal</span>
                    <span>{formatCurrency(subtotal)}</span>
                </div>
                <button
                    class="btn-primary btn-full btn-lg"
                    disabled={cart.length === 0}
                    onclick={() => (showPayment = true)}
                >
                    Charge {formatCurrency(subtotal)}
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
    onclose={() => (showCheckIn = false)}
    onconfirm={handleCustomerConfirm}
/>

<PaymentModal
    open={showPayment}
    {cart}
    {customer}
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
        width: 180px;
        background: var(--color-surface);
        border-right: 1px solid var(--color-border);
        padding: 1rem;
        overflow-y: auto;
    }

    .category-sidebar h3 {
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--color-text-muted);
        margin-bottom: 0.75rem;
    }

    .category-nav {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
    }

    .cat-btn {
        text-align: left;
        padding: 0.5rem 0.75rem;
        border-radius: var(--radius);
        background: transparent;
        font-size: 0.875rem;
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
        margin-bottom: 0.75rem;
    }
</style>
