<script lang="ts">
    import type { CartItem, Customer } from '$lib/types';
    import { submitOrder } from '$lib/api';
    import { getEmployeeId } from '$lib/stores/auth.svelte';
    import { formatCurrency } from '$lib/utils';
    import Modal from './Modal.svelte';

    let {
        open,
        cart,
        customer,
        onclose,
        oncomplete
    }: {
        open: boolean;
        cart: CartItem[];
        customer: Customer | null;
        onclose: () => void;
        oncomplete: (orderId: number, tip: number, total: number) => void;
    } = $props();

    type Step = 'method' | 'tip' | 'confirm';

    const paymentMethods = ['Cash', 'Card', 'Gift Card', 'Card on File'];
    const tipOptions = [0, 0.1, 0.15, 0.2];

    let step = $state<Step>('method');
    let selectedMethod = $state('');
    let tipPercent = $state(0);
    let customTip = $state('');
    let submitting = $state(false);
    let error = $state('');

    let subtotal = $derived(cart.reduce((sum, c) => sum + c.totalPrice, 0));
    let tipAmount = $derived(
        customTip ? parseFloat(customTip) || 0 : subtotal * tipPercent
    );
    let total = $derived(subtotal + tipAmount);

    $effect(() => {
        if (open) {
            step = 'method';
            selectedMethod = '';
            tipPercent = 0;
            customTip = '';
            error = '';
        }
    });

    function selectMethod(method: string) {
        selectedMethod = method;
        step = 'tip';
    }

    function selectTip(pct: number) {
        tipPercent = pct;
        customTip = '';
        step = 'confirm';
    }

    function skipTip() {
        tipPercent = 0;
        customTip = '';
        step = 'confirm';
    }

    async function confirmPayment() {
        submitting = true;
        error = '';
        try {
            const order = await submitOrder(
                getEmployeeId(),
                customer?.customerId ?? null,
                selectedMethod,
                tipAmount,
                cart
            );
            if (order) {
                oncomplete(order.orderId, tipAmount, total);
            } else {
                error = 'Failed to submit order.';
            }
        } catch {
            error = 'Payment failed. Please try again.';
        } finally {
            submitting = false;
        }
    }
</script>

<Modal {open} title="Payment" {onclose}>
    <div class="payment-flow">
        {#if step === 'method'}
            <p class="step-label">Select Payment Method</p>
            <div class="method-grid">
                {#each paymentMethods as method (method)}
                    <button class="method-btn" onclick={() => { selectMethod(method); }}>
                        {method}
                    </button>
                {/each}
            </div>

        {:else if step === 'tip'}
            <p class="step-label">Add a Tip?</p>
            <p class="subtotal-label">Subtotal: {formatCurrency(subtotal)}</p>
            <div class="tip-grid">
                {#each tipOptions as pct (pct)}
                    <button class="method-btn" onclick={() => { selectTip(pct); }}>
                        {pct === 0 ? 'No Tip' : `${String(pct * 100)}%`}
                        {#if pct > 0}
                            <small>({formatCurrency(subtotal * pct)})</small>
                        {/if}
                    </button>
                {/each}
            </div>
            <div class="custom-tip">
                <input
                    type="number"
                    placeholder="Custom tip amount"
                    bind:value={customTip}
                    min="0"
                    step="0.01"
                />
                <button
                    class="btn-primary"
                    onclick={() => (step = 'confirm')}
                    disabled={!customTip}
                >
                    Apply
                </button>
            </div>
            <button class="btn-ghost" onclick={skipTip}>Skip</button>

        {:else}
            <div class="confirm-summary">
                <div class="summary-row">
                    <span>Subtotal</span>
                    <span>{formatCurrency(subtotal)}</span>
                </div>
                <div class="summary-row">
                    <span>Tip</span>
                    <span>{formatCurrency(tipAmount)}</span>
                </div>
                <div class="summary-row total-row">
                    <span>Total</span>
                    <span>{formatCurrency(total)}</span>
                </div>
                <div class="summary-row">
                    <span>Payment</span>
                    <span>{selectedMethod}</span>
                </div>
            </div>

            {#if error}
                <p class="error-text">{error}</p>
            {/if}

            <div class="actions">
                <button class="btn-secondary" onclick={() => (step = 'method')}>Back</button>
                <button class="btn-primary btn-lg" onclick={confirmPayment} disabled={submitting}>
                    {submitting ? 'Processing...' : 'Confirm Payment'}
                </button>
            </div>
        {/if}
    </div>
</Modal>

<style>
    .payment-flow {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }

    .step-label {
        font-size: 1rem;
        font-weight: 600;
        text-align: center;
    }

    .subtotal-label {
        text-align: center;
        color: var(--color-text-muted);
        font-size: 0.875rem;
    }

    .method-grid,
    .tip-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.5rem;
    }

    .method-btn {
        padding: 1rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius);
        background: var(--color-surface);
        font-size: 0.95rem;
        font-weight: 500;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.25rem;
        transition:
            border-color var(--transition),
            background var(--transition);
    }

    .method-btn:hover {
        border-color: var(--color-primary);
        background: #fff0e6;
    }

    .method-btn small {
        color: var(--color-text-muted);
        font-size: 0.75rem;
    }

    .custom-tip {
        display: flex;
        gap: 0.5rem;
    }

    .custom-tip input {
        flex: 1;
    }

    .confirm-summary {
        background: #f8fafc;
        border-radius: var(--radius);
        padding: 1rem;
    }

    .summary-row {
        display: flex;
        justify-content: space-between;
        padding: 0.5rem 0;
        font-size: 0.875rem;
    }

    .total-row {
        border-top: 1px solid var(--color-border);
        font-weight: 700;
        font-size: 1.1rem;
        margin-top: 0.25rem;
        padding-top: 0.75rem;
    }

    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
    }
</style>
