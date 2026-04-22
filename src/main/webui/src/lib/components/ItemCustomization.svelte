<script lang="ts">
    import type { MenuItem, CartItem, SweetnessLevel, IceLevel } from '$lib/types';
    import { getAddOns } from '$lib/api';
    import { toTitleCase } from '$lib/utils';
    import Modal from './Modal.svelte';

    let {
        open,
        item,
        highContrast = false,
        magnifierOn = false,
        onclose,
        onadd
    }: {
        open: boolean;
        item: MenuItem | null;
        highContrast?: boolean;
        magnifierOn?: boolean;
        onclose: () => void;
        onadd: (cartItem: CartItem) => void;
    } = $props();

    const sizes = ['Small', 'Medium', 'Large'];
    const sweetnessLevels: SweetnessLevel[] = ['0%', '25%', '50%', '75%', '100%'];
    const iceLevels: IceLevel[] = ['No Ice', 'Less Ice', 'Regular Ice', 'Extra Ice'];

    let selectedSize = $state('Medium');
    let selectedSweetness = $state<SweetnessLevel>('100%');
    let selectedIce = $state<IceLevel>('Regular Ice');
    let selectedAddOns = $state<MenuItem[]>([]);
    let availableAddOns = $state<MenuItem[]>([]);

    const iceLevelColors: Record<string, string> = {
        'No Ice': '#e3f2fd',
        'Less Ice': '#bbdefb',
        'Regular Ice': '#64b5f6',
        'Extra Ice': '#1565c0'
    };

    $effect(() => {
        if (open) {
            selectedSize = item?.size ?? 'Medium';
            selectedSweetness = '100%';
            selectedIce = item?.isHot ? 'Hot' : 'Regular Ice';
            selectedAddOns = [];
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

    function toggleAddOn(addon: MenuItem) {
        const idx = selectedAddOns.findIndex((a) => a.menuItemId === addon.menuItemId);
        if (idx >= 0) {
            selectedAddOns = selectedAddOns.filter((a) => a.menuItemId !== addon.menuItemId);
        } else {
            selectedAddOns = [...selectedAddOns, addon];
        }
    }

    function totalPrice(): number {
        if (!item) return 0;
        const addOnTotal = selectedAddOns.reduce((sum, a) => sum + a.basePrice, 0);
        return item.basePrice + addOnTotal;
    }

    function addToCart() {
        if (!item) return;
        onadd({
            item,
            size: selectedSize,
            sweetness: selectedSweetness,
            iceLevel: selectedIce,
            addOns: selectedAddOns,
            totalPrice: totalPrice()
        });
        onclose();
    }
</script>

<Modal {open} title={item ? toTitleCase(item.name) : 'Customize'} {onclose} wide highContrast={highContrast} magnifierOn={magnifierOn}>
    <div class="customize-form" class:high-contrast={highContrast} class:magnifier-on={magnifierOn}>
        <section>
            <h4>Size</h4>
            <div class="option-row">
                {#each sizes as size (size)}
                    <button
                        class="option-btn"
                        class:selected={selectedSize === size}
                        onclick={() => (selectedSize = size)}
                    >
                        {size}
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
            <h4>{item?.isHot ? 'Temperature' : 'Ice Level'}</h4>
            {#if item?.isHot}
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
                <div class="option-row wrap">
                    {#each availableAddOns as addon (addon.menuItemId)}
                        <button
                            class="option-btn"
                            class:selected={selectedAddOns.some(
                                (a) => a.menuItemId === addon.menuItemId
                            )}
                            onclick={() => { toggleAddOn(addon); }}
                        >
                            {toTitleCase(addon.name)} (+${addon.basePrice.toFixed(2)})
                        </button>
                    {/each}
                </div>
            </section>
        {/if}

        <div class="footer">
            <span class="total">Total: ${totalPrice().toFixed(2)}</span>
            <button class="btn-primary btn-lg" onclick={addToCart}>Add to Cart</button>
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
