<script lang="ts">
    import type { MenuItem, CartItem, Customer } from '$lib/types';
    import { onDestroy, onMount } from 'svelte';

    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { getAllMenuItems, getCategories, getItemsByCategory, getTopMenuItems, menuItemImageByName } from '$lib/api';
    import { getCustomer, clearCustomer } from '$lib/stores/auth.svelte';
    import { formatCurrency, TAX_RATE, toTitleCase } from '$lib/utils';
    import ItemCustomization from '$lib/components/ItemCustomization.svelte';
    import CustomerCheckIn from '$lib/components/CustomerCheckIn.svelte';
    import PaymentModal from '$lib/components/PaymentModal.svelte';
    import TransactionComplete from '$lib/components/TransactionComplete.svelte';
    import Chatbot from '$lib/components/Chatbot.svelte';
    import LanguageSelector from '$lib/components/LanguageSelector.svelte';
    import WeatherMini from '$lib/components/WeatherMini.svelte';
    import { magnifierEnabled } from '$lib/stores/magnifier';
	import MagnifierOverlay from '$lib/components/MagnifierOverlay.svelte';

    const HOT_DRINKS = 'hot_drinks';
    const TOP_5 = 'top_5';

    const categoryEmojis: Record<string, string> = {
        'milky series': '\ud83e\udd5b',
        'fresh brew': '\ud83e\uded6',
        'fruit tea': '\ud83c\udf53',
        'fruity beverage': '\ud83c\udf79',
        'new matcha series': '\ud83c\udf75',
        'ice-blended': '\ud83e\uddca',
        'topping': '\ud83c\udf61',
        'seasonal poob': '\ud83c\udf38',
        // 'non-caffeinated' has no obvious match \u2014 falls back to \ud83e\uddcb
        [HOT_DRINKS]: '\u2668\ufe0f',
        [TOP_5]: '\ud83c\udfc6'
    };

    function emojiFor(cat: string | null | undefined): string {
        if (!cat) return '\u{1F9CB}';
        const lower = cat.toLowerCase();
        return categoryEmojis[cat]
            ?? categoryEmojis[lower]
            ?? categoryEmojis[lower.replace(/[_-]+/g, ' ')]
            ?? '\u{1F9CB}';
    }

    function groupAddOns(addOns: MenuItem[]): [string, number, number][] {
        const map = new Map<number, { name: string; qty: number; price: number }>();
        for (const a of addOns) {
            const existing = map.get(a.menuItemId);
            if (existing) existing.qty++;
            else map.set(a.menuItemId, { name: toTitleCase(a.name), qty: 1, price: a.basePrice });
        }
        return Array.from(map.values()).map(e => [e.name, e.qty, e.price]);
    }

    const IDLE_TIMEOUT = 30;
    const IDLE_WARNING = 20;

    let categories = $state<string[]>([]);
    let selectedCategory = $state('');
    let items = $state<MenuItem[]>([]);
    let itemsLoading = $state(false);
    let cart = $state<CartItem[]>([]);
    let customer = $state(getCustomer());

    let customizeItem = $state<MenuItem | null>(null);
    let customizeVariants = $state<MenuItem[]>([]);
    let showCustomize = $state(false);
    let editingIndex = $state<number | null>(null);
    let editingCartItem = $state<CartItem | null>(null);
    let showCheckIn = $state(false);
    let showPayment = $state(false);
    let showComplete = $state(false);
    let showConfirmReset = $state(false);
    let showConfirmExit = $state(false);

    let highContrast = $state(false);

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

    let itemGroups = $derived((() => {
        const groups = new Map<string, MenuItem[]>();
        for (const item of items) {
            const existing = groups.get(item.name);
            if (existing) existing.push(item);
            else groups.set(item.name, [item]);
        }
        return Array.from(groups.values());
    })());

    let subtotal = $derived(cart.reduce((sum, c) => sum + c.totalPrice * c.quantity, 0));
    let tax = $derived(Math.round((subtotal - discount) * TAX_RATE * 100) / 100);

    let idleSeconds = $state(0);
    let showIdleWarning = $state(false);
    let idleCountdown = $state(IDLE_WARNING);
    let idleTimer: ReturnType<typeof setInterval> | null = null;

    function toggleMagnifier() {
        $magnifierEnabled = !$magnifierEnabled;
    }

    function resetIdle() {
        idleSeconds = 0;
        if (showIdleWarning) {
            showIdleWarning = false;
            idleCountdown = IDLE_WARNING;
        }
    }

    function startIdleTimer() {
        clearIdleTimer();
        idleTimer = setInterval(() => {
            idleSeconds += 1;
            if (showIdleWarning) {
                idleCountdown -= 1;
                if (idleCountdown <= 0) {
                    exitToHome();
                }
            } else if (idleSeconds >= IDLE_TIMEOUT) {
                showIdleWarning = true;
                idleCountdown = IDLE_WARNING;
            }
        }, 1000);
    }

    function clearIdleTimer() {
        if (idleTimer) {
            clearInterval(idleTimer);
            idleTimer = null;
        }
    }

    function exitToHome() {
        clearIdleTimer();
        clearCustomer();
        void goto(resolve('/customer'));
    }

    function handleGlobalKeydown(event: KeyboardEvent) {
        const target = event.target as HTMLElement | null;
        const tag = target?.tagName;

        const isTypingField =
            tag === 'INPUT' ||
            tag === 'TEXTAREA' ||
            tag === 'SELECT' ||
            target?.isContentEditable;

        if (isTypingField) return;

        if (event.key === 'c' || event.key === 'C') {
            highContrast = !highContrast;
        }

    }

    function handleGlobalPointer() {
        resetIdle();
    }

    $effect(() => {
        startIdleTimer();
    });

    onDestroy(() => {
        clearIdleTimer();
    });

    onMount(() => {
        $magnifierEnabled = false;
        window.addEventListener('keydown', handleGlobalKeydown);
        window.addEventListener('pointerdown', handleGlobalPointer);
        window.addEventListener('mousemove', resetIdle);
        window.addEventListener('touchstart', handleGlobalPointer);

        return () => {
            window.removeEventListener('keydown', handleGlobalKeydown);
            window.removeEventListener('pointerdown', handleGlobalPointer);
            window.removeEventListener('mousemove', resetIdle);
            window.removeEventListener('touchstart', handleGlobalPointer);
        };
    });

    $effect(() => {
        void loadCategories();
    });

    $effect(() => {
        if (selectedCategory) void loadItems(selectedCategory);
    });

    async function loadCategories() {
        try {
            const fetched = await getCategories();
            const seasonalKey = fetched.find(c => c.toLowerCase().includes('seasonal'));
            const rest = fetched.filter(c => c !== seasonalKey);
            categories = [
                TOP_5,
                ...(seasonalKey ? [seasonalKey] : []),
                ...rest,
                HOT_DRINKS
            ];
            if (categories.length > 0) selectedCategory = categories[0] ?? '';
        } catch {
            categories = [TOP_5, HOT_DRINKS];
        }
    }

    async function loadItems(category: string) {
        itemsLoading = true;
        try {
            if (category === HOT_DRINKS) {
                const all = await getAllMenuItems();
                items = all.filter(i => i.isHot);
            } else if (category === TOP_5) {
                items = await getTopMenuItems(7, 5);
            } else {
                items = await getItemsByCategory(category);
            }
        } catch {
            items = [];
        } finally {
            itemsLoading = false;
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
        customizeVariants = items.filter(i => i.name === cartMenuItem?.name);
        if (customizeVariants.length === 0 && cartMenuItem) customizeVariants = [cartMenuItem];
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
        clearIdleTimer();
        clearCustomer();
        showComplete = false;
        void goto(resolve('/customer'));
    }

    function startOver() {
        cart = [];
        customer = getCustomer();
        redeemedCounts = new Map();
        customizeItem = null;
        showCustomize = false;
        showCheckIn = false;
        showPayment = false;
        showComplete = false;
        resetIdle();
    }

    function formatCategory(cat: string): string {
        if (cat === TOP_5) return "Bob's Best";
        return cat.replace(/_/g, ' ');
    }

    let zoom = 1;

	function zoomIn() {
		zoom = Math.min(zoom + 0.1, 2);
        document.body.style.zoom = zoom;
	}

	function zoomOut() {
		zoom = Math.max(zoom - 0.1, 1);
        document.body.style.zoom = zoom;
	}

	function resetZoom() {
		zoom = 1;
        document.body.style.zoom = zoom;
	}
</script>
<div class="zoom-controls">
	<button onclick={zoomOut}>−</button>
	<button onclick={zoomIn}>+</button>
</div>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div id="magnifier-root" class="zoom-wrapper" style={`transform: scale(${zoom}); transform-origin: top left;`}>
<div class="order-page" class:high-contrast={highContrast} onclick={resetIdle} onkeydown={resetIdle} onscroll={resetIdle}>
    <!-- Header -->
    <header class="order-header">
        <div class="header-left">
            <h1>Boba Bob's</h1>
        </div>
        <div class="header-right">
            <WeatherMini />
            <button class="header-btn accessibility" onclick={toggleMagnifier}> Screen Magnifier </button>
            <button class="header-btn accessibility" onclick={() => (highContrast = !highContrast)}> High Contrast </button>
            <LanguageSelector />
            {#if customer}
                <span class="welcome-text"
                    >Hi, {customer.firstName && customer.lastName
                        ? toTitleCase(`${customer.firstName} ${customer.lastName}`)
                        : customer.email}</span
                >
                <span class="points-badge" class:points-redeemable={customer.rewardPoints >= 10}>
                    {customer.rewardPoints} pts
                </span>
            {:else}
                <button class="header-btn" onclick={() => (showCheckIn = true)}> Sign In </button>
            {/if}
            <button class="header-btn exit" onclick={() => (showConfirmExit = true)}>Exit</button>
        </div>
    </header>

    <div class="order-body">
        <!-- Main content -->
        <div class="menu-section">
            <!-- Hero banner -->
            <div class="hero-banner">
                <div class="hero-text">
                    <h2>{formatCategory(selectedCategory)}</h2>
                    <p>{itemGroups.length} {itemGroups.length === 1 ? 'item' : 'items'} available</p>
                </div>
                <div class="hero-emoji">{emojiFor(selectedCategory)}</div>
            </div>

            <!-- Category pills -->
            <div class="category-strip">
                {#each categories as cat (cat)}
                    <button
                        class="cat-pill"
                        class:active={selectedCategory === cat}
                        onclick={() => (selectedCategory = cat)}
                    >
                        <span class="cat-emoji">{emojiFor(cat)}</span>
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
                    {#each itemGroups as variants (variants[0].name)}
                        {@const anyAvailable = variants.some(v => v.isAvailable)}
                        {@const minPrice = Math.min(...variants.map(v => v.basePrice))}
                        <button
                            class="item-card"
                            class:unavailable={!anyAvailable}
                            onclick={() => {
                                openCustomization(variants);
                            }}
                            disabled={!anyAvailable}
                        >
                            <div class="item-icon">
                                {#if variants[0].hasImage}
                                    <img src={menuItemImageByName(variants[0].name)} alt={variants[0].name} class="item-img" style="max-width:100%;max-height:100%;object-fit:contain;" />
                                {:else}
                                    {emojiFor(variants[0].category)}
                                {/if}
                            </div>
                            <div class="item-info">
                                <span class="item-name">{toTitleCase(variants[0].name)}</span>
                            </div>
                            <div class="item-bottom">
                                <span class="item-price">
                                    {#if variants.length > 1}from&nbsp;{/if}{formatCurrency(minPrice)}
                                </span>
                                {#if anyAvailable}
                                    <span class="add-icon" aria-hidden="true">
                                        <svg
                                            viewBox="0 0 12 12"
                                            width="12"
                                            height="12"
                                            fill="none"
                                            stroke="currentColor"
                                            stroke-width="2"
                                            stroke-linecap="round"
                                        >
                                            <line x1="6" y1="2" x2="6" y2="10" />
                                            <line x1="2" y1="6" x2="10" y2="6" />
                                        </svg>
                                    </span>
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

            {#if customer && maxRedeemable > 0 && maxRedeemable > totalRedeemed && cart.length > 0}
                <div class="free-drink-banner">
                    You have {maxRedeemable - totalRedeemed} free drink{maxRedeemable - totalRedeemed > 1 ? 's' : ''}! Tap the <strong>Redeem</strong> button next to any item below.
                </div>
            {/if}

            <div class="cart-items">
                {#if cart.length === 0}
                    <p class="cart-empty">Your cart is empty. Browse the menu and add items!</p>
                {:else}
                    {#each cart as cartItem, i (i)}
                        {@const redeemed = redeemedCounts.get(i) ?? 0}
                        <div class="cart-card" class:redeemed={redeemed > 0}>
                            <div class="cart-card-icon">
                                {#if cartItem.item.hasImage}
                                    <img src={menuItemImageByName(cartItem.item.name)} alt={cartItem.item.name} class="cart-img" style="max-width:100%;max-height:100%;object-fit:contain;" />
                                {:else}
                                    {emojiFor(cartItem.item.category)}
                                {/if}
                            </div>
                            <div class="cart-card-info">
                                <span class="cart-card-name">{toTitleCase(cartItem.item.name)}</span>
                                <span class="cart-card-details">Size: {toTitleCase(cartItem.size)}</span>
                                <span class="cart-card-details">Sweetness: {cartItem.sweetness}</span>
                                <span class="cart-card-details">{cartItem.iceLevel}</span>
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
                                <div class="cart-card-bottom">
                                    {#if redeemed > 0}
                                        <span class="redeem-label">{cartItem.quantity > 1 ? `${redeemed}x ` : ''}FREE (reward)</span>
                                        <span class="cart-card-price">
                                            <span class="price-struck">{formatCurrency(redeemed * cartItem.item.basePrice)}</span>
                                            {formatCurrency(cartItem.totalPrice * cartItem.quantity - redeemed * cartItem.item.basePrice)}
                                        </span>
                                    {:else}
                                        <span class="cart-card-price">
                                            {formatCurrency(cartItem.totalPrice * cartItem.quantity)}
                                        </span>
                                    {/if}
                                    {#if maxRedeemable > 0 && customer}
                                        {#if redeemed > 0}
                                            <button
                                                class="redeem-toggle active"
                                                onclick={() => unredeemOne(i)}
                                            >
                                                Undo
                                            </button>
                                        {/if}
                                        {#if redeemed < cartItem.quantity && totalRedeemed < maxRedeemable}
                                            <button
                                                class="redeem-toggle"
                                                onclick={() => redeemOne(i)}
                                            >
                                                Redeem
                                            </button>
                                        {/if}
                                    {/if}
                                </div>
                            </div>
                            <button
                                class="cart-remove"
                                onclick={() => {
                                    removeFromCart(i);
                                }}>&times;</button
                            >
                        </div>
                    {/each}
                {/if}
            </div>

            <div class="cart-summary">
                {#if totalRedeemed > 0}
                    <div class="cart-discount-banner">
                        {totalRedeemed} drink{totalRedeemed > 1 ? 's' : ''} redeemed
                        ({totalRedeemed * POINTS_PER_REDEEM} pts)
                    </div>
                {/if}
                <div class="summary-line">
                    <span>Subtotal</span>
                    <span>{formatCurrency(subtotal)}</span>
                </div>
                {#if discount > 0}
                    <div class="summary-line discount-line">
                        <span>Rewards Discount</span>
                        <span>-{formatCurrency(discount)}</span>
                    </div>
                {/if}
                <div class="summary-line tax-line">
                    <span>Tax (8.25%)</span>
                    <span>{formatCurrency(tax)}</span>
                </div>
                <div class="summary-line total-line">
                    <span>Total</span>
                    <span>{formatCurrency(subtotal - discount + tax)}</span>
                </div>
                <button
                    class="pay-btn"
                    disabled={cart.length === 0}
                    onclick={() => (showPayment = true)}
                >
                    Pay {formatCurrency(subtotal - discount + tax)}
                </button>
                {#if cart.length > 0}
                    <button class="reset-btn" onclick={() => (showConfirmReset = true)}
                        >Reset Cart</button
                    >
                {/if}
            </div>
        </aside>
    </div>
</div>


{#if showConfirmReset}
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
        class="idle-overlay"
        onclick={() => (showConfirmReset = false)}
        onkeydown={() => (showConfirmReset = false)}
    >
        <div
            class="idle-card card"
            onclick={(e) => {
                e.stopPropagation();
            }}
            onkeydown={(e) => {
                e.stopPropagation();
            }}
            role="none"
        >
            <p class="idle-title">Reset Cart?</p>
            <p class="idle-text">This will remove all items from your cart.</p>
            <div class="confirm-actions">
                <button class="btn-ghost" onclick={() => (showConfirmReset = false)}>Cancel</button>
                <button
                    class="btn-primary"
                    onclick={() => {
                        showConfirmReset = false;
                        startOver();
                    }}>Reset</button
                >
            </div>
        </div>
    </div>
{/if}

{#if showConfirmExit}
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
        class="idle-overlay"
        onclick={() => (showConfirmExit = false)}
        onkeydown={() => (showConfirmExit = false)}
    >
        <div
            class="idle-card card"
            onclick={(e) => {
                e.stopPropagation();
            }}
            onkeydown={(e) => {
                e.stopPropagation();
            }}
            role="none"
        >
            <p class="idle-title">Leave ordering?</p>
            <p class="idle-text">Your cart and session will be cleared.</p>
            <div class="confirm-actions">
                <button class="btn-ghost" onclick={() => (showConfirmExit = false)}>Cancel</button>
                <button class="confirm-exit-btn" onclick={exitToHome}>Exit</button>
            </div>
        </div>
    </div>
{/if}

{#if showIdleWarning}
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div class="idle-overlay" onclick={resetIdle} onkeydown={resetIdle}>
        <div class="idle-card card">
            <p class="idle-title">Still there?</p>
            <p class="idle-text">Returning to home in <span class="idle-count">{idleCountdown}</span>s...</p>
            <button class="btn-primary btn-lg" onclick={resetIdle}>I'm still here</button>
        </div>
    </div>
{/if}

<ItemCustomization
    open={showCustomize}
    item={customizeItem}
    variants={customizeVariants}
    {editingCartItem}
    highContrast={highContrast}
    magnifierOn={false}
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
    highContrast={highContrast}
    magnifierOn={false}
    onclose={() => (showCheckIn = false)}
    onconfirm={handleCustomerConfirm}
/>

<PaymentModal
    open={showPayment}
    {cart}
    {customer}
    {redeemedCounts}
    employeeId={null}
    highContrast={highContrast}
    magnifierOn={false}
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
    highContrast={highContrast}
    magnifierOn={false}
    onnewsale={newSale}
    onclose={newSale}
/>

<!-- Chatbot UI (floating) -->
<div style="--chatbot-right-offset: calc(340px + 40px);">
    <Chatbot />
</div>
</div>
<MagnifierOverlay targetSelector="#magnifier-root" highContrast={highContrast} />

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
        border: 2px solid #43a047;
        background: transparent;
        color: #43a047;
        font-weight: 600;
        font-size: 0.85rem;
        cursor: pointer;
        transition: all 0.15s;
    }

    .header-btn:hover {
        background: #43a047;
        color: white;
    }

    .header-btn.exit {
        border-color: transparent;
        color: #e74c3c;
        font-size: 0.8rem;
    }

    .header-btn.exit:hover {
        border-color: #e74c3c;
        background: #fdf0ef;
        color: #c0392b;
    }

    .header-btn.accessibility{
        border-color: #2d2017;
        color: #2d2017;
        font-size: 0.8rem;
        position: relative;
        z-index: 10001;
        background: white;
    }

    .header-btn.accessibility:hover{
        border-color: #2d2017;
        background: #2d2017;
        color: white;
        font-size: 0.8rem;
    }

    /* zoom controls */

    .zoom-controls {
		position: fixed;
		bottom: 16px;
		left: 16px;
		display: flex;
		gap: 8px;
		z-index: 9999;

		/* Optional styling */
		background: rgba(0, 0, 0, 0.6);
		padding: 8px;
		border-radius: 8px;
	}

	.zoom-controls>button {
		font-size: 1.2rem;
		padding: 0.4rem 0.8rem;
		cursor: pointer;
	}

	.zoom-wrapper {
		transform-origin: top left;
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
        gap: clamp(0.5rem, 1vw, 1rem);
        margin-bottom: 1.5rem;
        overflow-x: auto;
        padding-bottom: 0.5rem;
    }

    .cat-pill {
        display: flex;
        flex: 0 0 auto;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        min-width: 140px;
        padding: clamp(0.75rem, 1.5vw, 1.25rem) clamp(0.75rem, 1.25vw, 1.25rem);
        border-radius: 16px;
        border: 2px solid transparent;
        background: white;
        cursor: pointer;
        transition: all 0.15s;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
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
        font-size: clamp(1.5rem, 2.5vw, 2.25rem);
    }

    .cat-label {
        font-size: clamp(0.7rem, 1vw, 0.95rem);
        font-weight: 600;
        text-transform: capitalize;
        color: #555;
        line-height: 1.15;
        white-space: nowrap;
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
        grid-template-columns: repeat(auto-fill, minmax(clamp(150px, 16vw, 230px), 1fr));
        gap: clamp(0.6rem, 1.2vw, 1rem);
    }

    .item-card {
        background: white;
        border-radius: 10px;
        padding: clamp(0.35rem, 0.7vw, 0.5rem) clamp(0.5rem, 1vw, 0.85rem) clamp(0.55rem, 1.1vw, 0.75rem);
        border: none;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.35rem;
        cursor: pointer;
        transition:
            transform 0.15s,
            box-shadow 0.15s;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
        text-align: center;
    }

    .item-card:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
    }

    .item-card.unavailable {
        opacity: 0.45;
        cursor: not-allowed;
    }

    .item-icon {
        font-size: clamp(2.5rem, 4vw, 3.5rem);
        margin-bottom: 0;
        width: 100%;
        aspect-ratio: 1.05 / 1;
        max-height: clamp(8rem, 16vw, 13rem);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0.15rem 0;
    }

    .item-img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
    }

    .item-info {
        display: flex;
        flex-direction: column;
        gap: 0.15rem;
    }

    .item-name {
        font-weight: 600;
        font-size: clamp(0.95rem, 1.3vw, 1.2rem);
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
        font-size: clamp(1rem, 1.3vw, 1.25rem);
        color: #d4712a;
    }

    .add-icon {
        width: clamp(28px, 3vw, 36px);
        height: clamp(28px, 3vw, 36px);
        border-radius: 50%;
        background: #d4712a;
        color: white;
        display: inline-flex;
        align-items: center;
        justify-content: center;
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
        height: 60px;
        display: flex;
        align-items: flex-start;
        justify-content: center;
        align-self: flex-start;
        overflow: hidden;
        border-radius: 8px;
    }

    .cart-img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
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
        color: #888;
        text-transform: uppercase;
        letter-spacing: 0.03em;
    }

    .cart-addon-item {
        font-size: 0.7rem;
        color: #666;
        padding-left: 0.5rem;
        display: flex;
        justify-content: space-between;
    }

    .cart-addon-price {
        color: #999;
        margin-left: 0.5rem;
        flex-shrink: 0;
    }

    .cart-card-price {
        font-weight: 700;
        font-size: 0.85rem;
        color: #d4712a;
        margin-top: 0.15rem;
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
        border: 1px solid #d4712a;
        background: white;
        color: #d4712a;
        font-size: 0.9rem;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0;
        cursor: pointer;
    }

    .qty-btn:hover {
        background: #d4712a;
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
        border-radius: var(--radius, 8px);
        border: 1px solid #d4712a;
        background: transparent;
        color: #d4712a;
        font-size: 0.7rem;
        font-weight: 600;
        cursor: pointer;
    }

    .cart-edit-btn:hover {
        background: #d4712a;
        color: white;
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
        transition:
            background 0.15s,
            color 0.15s;
    }

    .cart-remove:hover {
        background: #e74c3c;
        color: white;
    }

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
        transition:
            opacity 0.15s,
            transform 0.1s;
    }

    .pay-btn:hover:not(:disabled) {
        opacity: 0.9;
        transform: scale(1.01);
    }

    .pay-btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
    }

    .reset-btn {
        width: 100%;
        padding: 0.5rem;
        margin-top: 0.5rem;
        border: none;
        border-radius: 10px;
        background: transparent;
        color: #999;
        font-size: 0.8rem;
        font-weight: 500;
        cursor: pointer;
        transition:
            color 0.15s,
            background 0.15s;
    }

    .reset-btn:hover {
        color: #666;
        background: #f0ebe4;
    }

    .idle-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.6);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
    }

    .idle-card {
        text-align: center;
        padding: 2.5rem 3rem;
        border-radius: 20px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
    }

    .idle-title {
        font-size: 1.5rem;
        font-weight: 700;
        color: #333;
    }

    .idle-text {
        font-size: 1rem;
        color: #999;
    }

    .idle-count {
        display: inline-block;
        min-width: 1.25em;
        text-align: center;
        font-variant-numeric: tabular-nums;
    }

    .confirm-actions {
        display: flex;
        gap: 0.75rem;
    }

    .confirm-exit-btn {
        padding: 0.5rem 1.5rem;
        border: none;
        border-radius: var(--radius);
        background: #e74c3c;
        color: white;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.15s;
    }

    .confirm-exit-btn:hover {
        background: #c0392b;
    }

    .free-drink-banner {
        text-align: center;
        font-size: 0.82rem;
        color: #e65100;
        background: #fff3e0;
        padding: 0.6rem 0.75rem;
        border-radius: 12px;
        margin-bottom: 0.75rem;
        line-height: 1.4;
    }

    .cart-card.redeemed {
        background: #f0faf0;
        border-color: #c8e6c9;
    }

    .redeem-label {
        display: inline-block;
        font-size: 0.65rem;
        font-weight: 700;
        color: #2e7d32;
        background: #e8f5e9;
        padding: 0.1rem 0.4rem;
        border-radius: 4px;
        margin-top: 0.1rem;
    }

    .price-struck {
        text-decoration: line-through;
        color: #999;
        margin-right: 0.3rem;
    }

    .cart-card-bottom {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-top: 0.2rem;
        flex-wrap: wrap;
    }

    .redeem-toggle {
        font-size: 0.65rem;
        padding: 0.2rem 0.5rem;
        border-radius: 8px;
        border: 1.5px solid #e65100;
        background: transparent;
        color: #e65100;
        font-weight: 700;
        cursor: pointer;
        transition: all 0.15s;
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
        border-radius: 10px;
        margin-bottom: 0.75rem;
    }

    .discount-line {
        color: #2e7d32;
        font-weight: 600;
    }

    .points-badge {
        font-size: 0.8rem;
        font-weight: 600;
        padding: 0.2rem 0.6rem;
        border-radius: 12px;
        background: #ece4d8;
        color: #8b7355;
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

        /* ── High Contrast ── */
    .order-page.high-contrast {
        background: #000;
        color: #fff;
    }

    .order-page.high-contrast .order-header,
    .order-page.high-contrast .cart-panel,
    .order-page.high-contrast .cat-pill,
    .order-page.high-contrast .item-card,
    .order-page.high-contrast .cart-card,
    .order-page.high-contrast .idle-card,
    .order-page.high-contrast .hero-banner {
        background: #000;
        color: #fff;
        border: 2px solid #fff;
        box-shadow: none;
    }

    .order-page.high-contrast .order-header {
        border-bottom: 2px solid #fff;
    }

    .order-page.high-contrast .cart-panel {
        border-left: 2px solid #fff;
    }

    .order-page.high-contrast .cart-summary,
    .order-page.high-contrast .total-line,
    .order-page.high-contrast .modal-header {
        border-color: #fff;
    }

    .order-page.high-contrast .order-header h1,
    .order-page.high-contrast .welcome-text,
    .order-page.high-contrast .points-badge,
    .order-page.high-contrast .cat-label,
    .order-page.high-contrast .section-title,
    .order-page.high-contrast .item-name,
    .order-page.high-contrast .cart-title,
    .order-page.high-contrast .cart-card-name,
    .order-page.high-contrast .summary-line,
    .order-page.high-contrast .total-line,
    .order-page.high-contrast .idle-title,
    .order-page.high-contrast .idle-text,
    .order-page.high-contrast .muted-text,
    .order-page.high-contrast .cart-empty,
    .order-page.high-contrast .cart-card-details,
    .order-page.high-contrast .price-struck,
    .order-page.high-contrast .hero-text p,
    .order-page.high-contrast .hero-text h2 {
        color: #fff;
    }

    .order-page.high-contrast .header-btn,
    .order-page.high-contrast .pay-btn,
    .order-page.high-contrast .reset-btn,
    .order-page.high-contrast .redeem-toggle,
    .order-page.high-contrast .cart-remove,
    .order-page.high-contrast .confirm-exit-btn,
    .order-page.high-contrast .btn-primary,
    .order-page.high-contrast .btn-ghost {
        background: #000;
        color: #fff;
        border: 2px solid #fff;
    }

    .order-page.high-contrast .header-btn:hover,
    .order-page.high-contrast .pay-btn:hover:not(:disabled),
    .order-page.high-contrast .reset-btn:hover,
    .order-page.high-contrast .redeem-toggle:hover:not(:disabled),
    .order-page.high-contrast .cart-remove:hover,
    .order-page.high-contrast .confirm-exit-btn:hover,
    .order-page.high-contrast .btn-primary:hover,
    .order-page.high-contrast .btn-ghost:hover {
        background: #fff;
        color: #000;
        transform: none;
        opacity: 1;
    }

    .order-page.high-contrast .cat-pill.active{
        background-color: yellow;
    }

    .order-page.high-contrast .redeem-toggle.active,
    .order-page.high-contrast .points-badge,
    .order-page.high-contrast .points-badge.points-redeemable,
    .order-page.high-contrast .free-drink-banner,
    .order-page.high-contrast .cart-discount-banner,
    .order-page.high-contrast .redeem-label,
    .order-page.high-contrast .cart-card.redeemed {
        background: #fff;
        color: #000;
        border-color: #fff;
        animation: none;
        box-shadow: none;
    }

    .order-page.high-contrast .item-price,
    .order-page.high-contrast .cart-card-price,
    .order-page.high-contrast .sold-out,
    .order-page.high-contrast .discount-line,
    .order-page.high-contrast .tax-line {
        color: #fff;
    }

    .order-page.high-contrast .add-icon {
        background: #fff;
        color: #000;
    }

    .order-page.high-contrast .idle-overlay {
        background: rgba(0, 0, 0, 0.9);
    }


.order-page.high-contrast .cat-pill {
    background: #000;
    color: #fff;
    border: 2px solid #fff;
}

.order-page.high-contrast .cat-pill .cat-label {
    color: #fff;
}

.order-page.high-contrast .cat-pill.active {
    background: #ffff00;
    color: #000;
    border-color: #ffff00;
}

.zoom-controls.high-contrast>button{
    background: #ffff00;
    color: #000;
}

.order-page.high-contrast .cat-pill.active .cat-label {
    color: #000;
}

</style>
