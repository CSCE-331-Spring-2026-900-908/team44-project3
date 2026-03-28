<script lang="ts">
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
</script>

<Modal {open} title="Transaction Complete" {onclose}>
    <div class="complete-content">
        <div class="checkmark">&#10003;</div>

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

        <div class="receipt-options">
            <button class="btn-secondary">Print Receipt</button>
            <button class="btn-secondary">Email Receipt</button>
            <button class="btn-ghost">No Receipt</button>
        </div>

        <button class="btn-primary btn-full btn-lg" onclick={onnewsale}>New Sale</button>
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

    .receipt-options {
        display: flex;
        gap: 0.5rem;
    }
</style>
