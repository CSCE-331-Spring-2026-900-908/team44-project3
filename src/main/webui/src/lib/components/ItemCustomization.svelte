<script lang="ts">
    import type { MenuItem, CartItem, SweetnessLevel, IceLevel } from '$lib/types';
    import { getAddOns } from '$lib/api';
    import { toTitleCase } from '$lib/utils';
    import Modal from './Modal.svelte';

    let {
        open,
        item,
        variants = [],
        editingCartItem = null,
        highContrast = false,
        magnifierOn = false,
        onclose,
        onadd,
        onsave
    }: {
        open: boolean;
        item: MenuItem | null;
        variants?: MenuItem[];
        editingCartItem?: CartItem | null;
        highContrast?: boolean;
        magnifierOn?: boolean;
        onclose: () => void;
        onadd: (cartItem: CartItem) => void;
        onsave?: (cartItem: CartItem) => void;
    } = $props();

    let availableSizes = $derived(
        variants.length > 0
            ? [...new Set(variants.map(v => v.size))]
            : item ? [item.size] : ['Medium']
    );

    let selectedVariant = $derived(
        variants.find(v => v.size === selectedSize) ?? item
    );

    const sweetnessLevels: SweetnessLevel[] = ['0%', '25%', '50%', '75%', '100%'];
    const iceLevels: IceLevel[] = ['No Ice', 'Less Ice', 'Regular Ice', 'Extra Ice'];

    let selectedSize = $state('Medium');
    let selectedSweetness = $state<SweetnessLevel>('100%');
    let selectedIce = $state<IceLevel>('Regular Ice');
    let addOnQtys = $state(new Map<number, number>());
    let availableAddOns = $state<MenuItem[]>([]);

    const iceLevelColors: Record<string, string> = {
        'No Ice': '#e3f2fd',
        'Less Ice': '#bbdefb',
        'Regular Ice': '#64b5f6',
        'Extra Ice': '#1565c0'
    };

    $effect(() => {
        if (open) {
            if (editingCartItem) {
                selectedSize = editingCartItem.size;
                selectedSweetness = editingCartItem.sweetness as SweetnessLevel;
                selectedIce = editingCartItem.iceLevel as IceLevel;
                const counts = new Map<number, number>();
                for (const a of editingCartItem.addOns) {
                    counts.set(a.menuItemId, (counts.get(a.menuItemId) ?? 0) + 1);
                }
                addOnQtys = counts;
            } else {
                selectedSize = item?.size ?? 'Medium';
                selectedSweetness = '100%';
                selectedIce = item?.isHot ? 'Hot' : 'Regular Ice';
                addOnQtys = new Map();
            }
            void loadAddOns();
        }
    });

    async function loadAddOns() {
        try {
            availableAddOns = await getAddOns();
        } catch {
            availableAddOns = [];
        }
    }

    function addOnQty(id: number): number {
        return addOnQtys.get(id) ?? 0;
    }

    const MAX_ADDON_QTY = 5;

    function incrementAddOn(addon: MenuItem) {
        const current = addOnQtys.get(addon.menuItemId) ?? 0;
        if (current >= MAX_ADDON_QTY) return;
        const next = new Map(addOnQtys);
        next.set(addon.menuItemId, current + 1);
        addOnQtys = next;
    }

    function decrementAddOn(addon: MenuItem) {
        const current = addOnQtys.get(addon.menuItemId) ?? 0;
        if (current <= 0) return;
        const next = new Map(addOnQtys);
        if (current === 1) next.delete(addon.menuItemId);
        else next.set(addon.menuItemId, current - 1);
        addOnQtys = next;
    }

    function buildAddOnsList(): MenuItem[] {
        const result: MenuItem[] = [];
        for (const addon of availableAddOns) {
            const qty = addOnQtys.get(addon.menuItemId) ?? 0;
            for (let i = 0; i < qty; i++) result.push(addon);
        }
        return result;
    }

    function totalPrice(): number {
        if (!selectedVariant) return 0;
        let addOnTotal = 0;
        for (const [id, qty] of addOnQtys) {
            const addon = availableAddOns.find(a => a.menuItemId === id);
            if (addon) addOnTotal += addon.basePrice * qty;
        }
        return selectedVariant.basePrice + addOnTotal;
    }

    function addToCart() {
        if (!selectedVariant) return;
        const cartItem: CartItem = {
            item: selectedVariant,
            size: selectedSize,
            sweetness: selectedSweetness,
            iceLevel: selectedIce,
            addOns: buildAddOnsList(),
            totalPrice: totalPrice(),
            quantity: editingCartItem?.quantity ?? 1
        };
        if (editingCartItem && onsave) {
            onsave(cartItem);
        } else {
            onadd(cartItem);
        }
        onclose();
    }
</script>

<Modal {open} title={selectedVariant ? toTitleCase(selectedVariant.name) : 'Customize'} {onclose} wide highContrast={highContrast} magnifierOn={magnifierOn}>
    <div class="customize-form" class:high-contrast={highContrast} class:magnifier-on={magnifierOn}>
        <section>
            <h4>Size</h4>
            <div class="option-row">
                {#each availableSizes as size (size)}
                    <button
                        class="option-btn"
                        class:selected={selectedSize === size}
                        onclick={() => (selectedSize = size)}
                    >
                        {toTitleCase(size)}
                    </button>
                {/each}
            </div>
        </section>

        <section>
            <h4>Sweetness</h4>
            <div class="option-row">
                {#each sweetnessLevels as level (level)}
                    <button
                        class="option-btn"
                        class:selected={selectedSweetness === level}
                        onclick={() => (selectedSweetness = level)}
                    >
                        {level}
                    </button>
                {/each}
            </div>
        </section>

        <section>
            <h4>{selectedVariant?.isHot ? 'Temperature' : 'Ice Level'}</h4>
            {#if selectedVariant?.isHot}
                <div class="option-row">
                    <button class="option-btn ice-btn hot selected">
                        Hot
                    </button>
                </div>
            {:else}
                <div class="option-row">
                    {#each iceLevels as level (level)}
                        <button
                            class="option-btn ice-btn"
                            class:selected={selectedIce === level}
                            style="--ice-color: {iceLevelColors[level] ?? '#e3f2fd'}; --ice-text: {level === 'Extra Ice' ? 'white' : '#1565c0'}"
                            onclick={() => (selectedIce = level)}
                        >
                            {level}
                        </button>
                    {/each}
                </div>
            {/if}
        </section>

        {#if availableAddOns.length > 0}
            <section>
                <h4>Add-Ons</h4>
                <div class="addon-list">
                    {#each availableAddOns as addon (addon.menuItemId)}
                        {@const qty = addOnQty(addon.menuItemId)}
                        <div class="addon-row" class:active={qty > 0}>
                            <span class="addon-label">{toTitleCase(addon.name)}</span>
                            <span class="addon-price">+${addon.basePrice.toFixed(2)}</span>
                            <div class="addon-controls">
                                {#if qty > 0}
                                    <button class="addon-btn" onclick={() => decrementAddOn(addon)}><svg viewBox="0 0 12 12" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="2" y1="6" x2="10" y2="6" /></svg></button>
                                    <span class="addon-qty">{qty}</span>
                                {/if}
                                <button class="addon-btn add" onclick={() => incrementAddOn(addon)} disabled={qty >= MAX_ADDON_QTY}><svg viewBox="0 0 12 12" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="6" y1="2" x2="6" y2="10" /><line x1="2" y1="6" x2="10" y2="6" /></svg></button>
                            </div>
                        </div>
                    {/each}
                </div>
            </section>
        {/if}

        <div class="footer">
            <span class="total">Total: ${totalPrice().toFixed(2)}</span>
            <button class="btn-primary btn-lg" onclick={addToCart}>{editingCartItem ? 'Save Changes' : 'Add to Cart'}</button>
        </div>
    </div>
</Modal>

<style>
    .customize-form {
        display: flex;
        flex-direction: column;
        gap: 1.25rem;
    }

    section h4 {
        font-size: 0.8rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--color-text-muted);
        margin-bottom: 0.5rem;
    }

    .option-row {
        display: flex;
        gap: 0.5rem;
    }

    .option-row.wrap {
        flex-wrap: wrap;
    }

    .addon-list {
        display: flex;
        flex-direction: column;
        gap: 0.4rem;
    }

    .addon-row {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.4rem 0.75rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius);
        background: var(--color-surface);
        transition: border-color var(--transition), background var(--transition);
    }

    .addon-row.active {
        border-color: var(--color-primary);
        background: #fff5f5;
    }

    .addon-label {
        font-size: 0.875rem;
        font-weight: 500;
    }

    .addon-price {
        font-size: 0.8rem;
        color: var(--color-text-muted);
        margin-right: auto;
    }

    .addon-controls {
        display: flex;
        align-items: center;
        gap: 0.35rem;
    }

    .addon-btn {
        width: 26px;
        height: 26px;
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

    .addon-btn:hover:not(:disabled) {
        background: var(--color-primary);
        color: white;
    }

    .addon-btn:disabled {
        border-color: #ddd;
        color: #ccc;
        cursor: not-allowed;
        opacity: 0.4;
    }

    .addon-qty {
        font-weight: 700;
        font-size: 0.85rem;
        min-width: 1rem;
        text-align: center;
    }

    .option-btn {
        padding: 0.5rem 1rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius);
        background: var(--color-surface);
        font-size: 0.875rem;
        transition:
            border-color var(--transition),
            background var(--transition);
    }

    .option-btn:hover {
        border-color: var(--color-primary);
    }

    .option-btn.selected {
        background: var(--color-primary);
        color: white;
        border-color: var(--color-primary);
    }

    .option-btn.ice-btn.selected {
        background: var(--ice-color);
        color: var(--ice-text);
        border-color: var(--ice-color);
    }

    .option-btn.ice-btn.hot {
        background: #fbe9e7;
        color: #d84315;
        border-color: #e64a19;
        cursor: default;
    }

    .footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-top: 1rem;
        border-top: 1px solid var(--color-border);
    }

    .total {
        font-size: 1.25rem;
        font-weight: 700;
    }

    .customize-form.high-contrast {
        color: #fff;
    }

    .customize-form.high-contrast section h4 {
        color: #000;
    }

    .customize-form.high-contrast .option-btn {
        background: #000;
        color: #fff;
        border: 2px solid #fff;
        box-shadow: none;
    }

    .customize-form.high-contrast .option-btn:hover {
        background: yellow;
        color: #000;
        border-color: #fff;
    }

    .customize-form.high-contrast .option-btn.selected {
        background: #ffff00;
        color: #000;
        border-color: #ffff00;
    }

    .customize-form.high-contrast .addon-row {
        background: #000;
        color: #fff;
        border: 2px solid #fff;
    }

    .customize-form.high-contrast .addon-row.active {
        background: #000;
        border-color: #ffff00;
    }

    .customize-form.high-contrast .addon-price {
        color: #ccc;
    }

    .customize-form.high-contrast .addon-btn {
        background: #000;
        color: #fff;
        border-color: #fff;
    }

    .customize-form.high-contrast .addon-btn:hover {
        background: #fff;
        color: #000;
    }

    .customize-form.high-contrast .footer {
        border-top: 2px solid #fff;
    }

    .customize-form.high-contrast .total {
        color: black;
    }

    .customize-form.high-contrast .btn-primary,
    .customize-form.high-contrast .btn-secondary,
    .customize-form.high-contrast .btn-ghost,
    .customize-form.high-contrast .btn-lg {
        background: #000;
        color: #fff;
        border: 2px solid #fff;
    }

    .customize-form.high-contrast .btn-primary:hover,
    .customize-form.high-contrast .btn-secondary:hover,
    .customize-form.high-contrast .btn-ghost:hover,
    .customize-form.high-contrast .btn-lg:hover {
        background: #fff;
        color: #000;
    }


    /* Magnifier */
    .customize-form.magnifier-on {
        gap: 1.5rem;
    }

    .customize-form.magnifier-on section h4 {
        font-size: 1rem;
    }

    .customize-form.magnifier-on .option-btn {
        padding: 0.75rem 1.2rem;
        font-size: 1rem;
    }

    .customize-form.magnifier-on .total {
        font-size: 1.5rem;
    }

    .customize-form.magnifier-on .btn-primary,
    .customize-form.magnifier-on .btn-secondary,
    .customize-form.magnifier-on .btn-ghost,
    .customize-form.magnifier-on .btn-lg {
        font-size: 1rem;
        padding: 0.85rem 1.25rem;
    }
</style>
