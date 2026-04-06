<script lang="ts">
    import { onDestroy } from 'svelte';
    import { formatCurrency } from '$lib/utils';
    import Modal from './Modal.svelte';

    let {
        open,
        orderId,
        tip,
        total,
        onnewsale,
        onclose
    }: {
        open: boolean;
        orderId: number;
        tip: number;
        total: number;
        onnewsale: () => void;
        onclose: () => void;
    } = $props();

    type ReceiptStatus = 'idle' | 'printed' | 'emailed';

    let receiptStatus = $state<ReceiptStatus>('idle');
    let countdown = $state(10);
    let dismissTimer: ReturnType<typeof setInterval> | null = null;

    $effect(() => {
        if (open) {
            receiptStatus = 'idle';
            startCountdown();
        } else {
            clearCountdown();
        }
    });

    onDestroy(() => {
        clearCountdown();
    });

    function startCountdown() {
        clearCountdown();
        countdown = 10;
        dismissTimer = setInterval(() => {
            countdown -= 1;
            if (countdown <= 0) {
                clearCountdown();
                onnewsale();
            }
        }, 1000);
    }

    function clearCountdown() {
        if (dismissTimer) {
            clearInterval(dismissTimer);
            dismissTimer = null;
        }
    }

    function handlePrint() {
        receiptStatus = 'printed';
        startCountdown();
    }

    function handleEmail() {
        receiptStatus = 'emailed';
        startCountdown();
    }

    function handleNoReceipt() {
        clearCountdown();
        onnewsale();
    }

    function handleDone() {
        clearCountdown();
        onnewsale();
    }
</script>

<Modal {open} title="Thank You!" {onclose}>
    <div class="complete-content">
        <div class="checkmark">&#10003;</div>

        <p class="thank-you">Thank you for your purchase!</p>
        <p class="order-id">Order #{orderId}</p>

        <div class="summary card">
            <div class="row">
                <span>Subtotal</span>
                <span>{formatCurrency(total - tip)}</span>
            </div>
            <div class="row">
                <span>Tip</span>
                <span>{formatCurrency(tip)}</span>
            </div>
            <div class="row total">
                <span>Total</span>
                <span>{formatCurrency(total)}</span>
            </div>
        </div>

        {#if receiptStatus === 'idle'}
            <p class="receipt-label">Would you like a receipt?</p>
            <div class="receipt-options">
                <button class="btn-secondary" onclick={handlePrint}>Print Receipt</button>
                <button class="btn-secondary" onclick={handleEmail}>Email Receipt</button>
                <button class="btn-ghost" onclick={handleNoReceipt}>No Receipt</button>
            </div>
            <p class="countdown-text">Closing in {countdown}s...</p>
        {:else}
            <div class="receipt-confirmed">
                <span class="receipt-badge">
                    &#10003; Receipt {receiptStatus === 'printed' ? 'Printed' : 'Emailed'}
                </span>
            </div>
            <button class="btn-primary btn-full btn-lg" onclick={handleDone}>Done</button>
            <p class="countdown-text">Closing in {countdown}s...</p>
        {/if}
    </div>
</Modal>

<style>
    .complete-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
        text-align: center;
    }

    .checkmark {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: var(--color-success);
        color: white;
        font-size: 2rem;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .thank-you {
        font-size: 1.25rem;
        font-weight: 700;
        color: var(--color-success);
    }

    .order-id {
        font-size: 1.125rem;
        font-weight: 600;
    }

    .summary {
        width: 100%;
        padding: 1rem;
    }

    .row {
        display: flex;
        justify-content: space-between;
        padding: 0.35rem 0;
        font-size: 0.875rem;
    }

    .row.total {
        border-top: 1px solid var(--color-border);
        font-weight: 700;
        margin-top: 0.25rem;
        padding-top: 0.5rem;
    }

    .receipt-label {
        font-size: 0.9rem;
        color: var(--color-text-muted);
    }

    .receipt-options {
        display: flex;
        gap: 0.5rem;
    }

    .countdown-text {
        font-size: 0.8rem;
        color: var(--color-text-muted);
        opacity: 0.7;
    }

    .receipt-confirmed {
        padding: 0.5rem 0;
    }

    .receipt-badge {
        display: inline-block;
        padding: 0.4rem 1rem;
        border-radius: 20px;
        background: #e8f5e9;
        color: var(--color-success);
        font-weight: 600;
        font-size: 0.9rem;
    }
</style>
