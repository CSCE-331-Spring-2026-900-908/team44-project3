<script lang="ts">
    import type { MenuItem, CartItem, SweetnessLevel, IceLevel } from '$lib/types';
    import { getAddOns } from '$lib/api';
    import Modal from './Modal.svelte';

    let {
        open,
        item,
        onclose,
        onadd
    }: {
        open: boolean;
        item: MenuItem | null;
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

    $effect(() => {
        if (open) {
            selectedSize = item?.size ?? 'Medium';
            selectedSweetness = '100%';
            selectedIce = 'Regular Ice';
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

<Modal {open} title={item?.name ?? 'Customize'} {onclose} wide>
    <div class="customize-form">
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
            <h4>Ice Level</h4>
            <div class="option-row">
                {#each iceLevels as level (level)}
                    <button
                        class="option-btn"
                        class:selected={selectedIce === level}
                        onclick={() => (selectedIce = level)}
                    >
                        {level}
                    </button>
                {/each}
            </div>
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
                            {addon.name} (+${addon.basePrice.toFixed(2)})
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
</style>
