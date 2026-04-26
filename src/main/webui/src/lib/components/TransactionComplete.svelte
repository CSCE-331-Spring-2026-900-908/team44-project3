<script lang="ts">
    import { onDestroy } from 'svelte';
    import { formatCurrency } from '$lib/utils';
    import Modal from './Modal.svelte';
    import type { CartItem } from '$lib/types';

    let {
        open,
        orderId,
        tip,
        total,
        pointsEarned = 0,
        customerEmail = '',
        cart = [],
        highContrast = false,
        magnifierOn = false,
        onnewsale,
        onclose
    }: {
        open: boolean;
        orderId: number;
        tip: number;
        total: number;
        pointsEarned?: number;
        customerEmail?: string;
        cart?: CartItem[];
        highContrast?: boolean;
        magnifierOn?: boolean;
        onnewsale: () => void;
        onclose: () => void;
    } = $props();

    type ReceiptStatus = 'idle' | 'printed' | 'emailed';
    type EmailStep = 'none' | 'entering' | 'sending' | 'sent' | 'error';

    let receiptStatus = $state<ReceiptStatus>('idle');
    let emailStep = $state<EmailStep>('none');
    let emailInput = $state('');
    let emailError = $state('');
    let countdown = $state(10);
    let dismissTimer: ReturnType<typeof setInterval> | null = null;

    $effect(() => {
        if (open) {
            receiptStatus = 'idle';
            emailStep = 'none';
            emailInput = customerEmail ?? '';
            emailError = '';
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

    function handleEmailClick() {
        clearCountdown();
        emailStep = 'entering';
        emailInput = customerEmail ?? '';
        emailError = '';
    }

    async function submitEmail() {
        const addr = emailInput.trim();
        if (!addr || !addr.includes('@')) {
            emailError = 'Please enter a valid email address.';
            return;
        }
        emailStep = 'sending';
        emailError = '';

        const subtotal = total - tip;
        const items = cart.map(c => ({
            name: c.item.name,
            size: c.size ?? '',
            sweetness: c.sweetness ?? '',
            iceLevel: c.iceLevel ?? '',
            addOns: c.addOns.map(a => ({ name: a.name, price: a.basePrice })),
            quantity: c.quantity,
            unitPrice: c.item.basePrice
        }));

        try {
            const res = await fetch(`/api/orders/${orderId}/email-receipt`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: addr, subtotal, tip, total, pointsEarned, items })
            });
            if (res.ok) {
                emailStep = 'sent';
                receiptStatus = 'emailed';
                startCountdown();
            } else {
                emailStep = 'error';
                emailError = 'Failed to send email. Please try again.';
            }
        } catch {
            emailStep = 'error';
            emailError = 'Network error. Please try again.';
        }
    }

    function cancelEmail() {
        emailStep = 'none';
        emailError = '';
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

<Modal {open} title="Thank You!" {onclose} highContrast={highContrast}>
    <div class="complete-content" class:high-contrast={highContrast} class:magnifier-on={magnifierOn}>
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
            {#if pointsEarned > 0}
                <div class="row points">
                    <span>Points Earned</span>
                    <span>+{pointsEarned}</span>
                </div>
            {/if}
        </div>

        {#if receiptStatus === 'idle' && emailStep === 'none'}
            <p class="receipt-label">Would you like a receipt?</p>
            <div class="receipt-options">
                <button class="btn-secondary" onclick={handlePrint}>Print Receipt</button>
                <button class="btn-secondary" onclick={handleEmailClick}>Email Receipt</button>
                <button class="btn-ghost" onclick={handleNoReceipt}>No Receipt</button>
            </div>
            <p class="countdown-text">Closing in {countdown}s...</p>

        {:else if emailStep === 'entering' || emailStep === 'error'}
            <p class="receipt-label">Enter email address:</p>
            <div class="email-form">
                <input
                    class="email-input"
                    type="email"
                    placeholder="you@example.com"
                    bind:value={emailInput}
                    onkeydown={(e) => e.key === 'Enter' && submitEmail()}
                />
                {#if emailError}
                    <p class="email-error">{emailError}</p>
                {/if}
                <div class="email-actions">
                    <button class="btn-primary" onclick={submitEmail}>Send Receipt</button>
                    <button class="btn-ghost" onclick={cancelEmail}>Cancel</button>
                </div>
            </div>

        {:else if emailStep === 'sending'}
            <p class="receipt-label">Sending receipt...</p>

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

    .row.points {
        color: var(--color-primary);
        font-weight: 600;
    }

    .receipt-label {
        font-size: 0.9rem;
        color: var(--color-text-muted);
    }

    .receipt-options {
        display: flex;
        gap: 0.5rem;
    }

    .email-form {
        display: flex;
        flex-direction: column;
        align-items: stretch;
        gap: 0.5rem;
        width: 100%;
    }

    .email-input {
        width: 100%;
        padding: 0.6rem 0.75rem;
        border: 1px solid var(--color-border);
        border-radius: 6px;
        font-size: 0.95rem;
        box-sizing: border-box;
    }

    .email-input:focus {
        outline: none;
        border-color: var(--color-primary);
        box-shadow: 0 0 0 2px rgba(139, 94, 60, 0.15);
    }

    .email-error {
        font-size: 0.8rem;
        color: var(--color-error, #e53935);
        margin: 0;
    }

    .email-actions {
        display: flex;
        gap: 0.5rem;
        justify-content: center;
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

    /* ── High Contrast ── */
    .complete-content.high-contrast {
        color: #fff;
    }

    .complete-content.high-contrast .checkmark {
        background: #ffff00;
        color: #000;
    }

    .complete-content.high-contrast .thank-you {
        color: black;
    }

    .complete-content.high-contrast .order-id {
        color: #000;
    }

    .complete-content.high-contrast .summary,
    .complete-content.high-contrast .card {
        background: #000;
        color: #000;
        border: 2px solid #fff;
        box-shadow: none;
    }

    .complete-content.high-contrast .row {
        color: #fff;
    }

    .complete-content.high-contrast .row.total {
        border-top: 2px solid #fff;
    }

    .complete-content.high-contrast .row.points {
        color: #ffff00;
    }

    .complete-content.high-contrast .receipt-label,
    .complete-content.high-contrast .countdown-text {
        color: black;
        opacity: 1;
    }

    .complete-content.high-contrast .receipt-badge {
        background: #ffff00;
        color: #000;
        border: 2px solid #ffff00;
    }

    .complete-content.high-contrast .btn-primary,
    .complete-content.high-contrast .btn-secondary,
    .complete-content.high-contrast .btn-ghost,
    .complete-content.high-contrast .btn-full,
    .complete-content.high-contrast .btn-lg {
        background: #000;
        color: #fff;
        border: 2px solid #fff;
    }

    .complete-content.high-contrast .btn-primary:hover,
    .complete-content.high-contrast .btn-secondary:hover,
    .complete-content.high-contrast .btn-ghost:hover,
    .complete-content.high-contrast .btn-full:hover,
    .complete-content.high-contrast .btn-lg:hover {
        background: yellow;
        color: #000;
    }

    .complete-content.high-contrast .email-input {
        background: #000;
        color: #fff;
        border: 2px solid #fff;
    }

    .complete-content.high-contrast .email-error {
        color: #ff6b6b;
    }

    /* magnifier */

    .complete-content.magnifier-on {
        gap: 1.25rem;
    }

    .complete-content.magnifier-on .checkmark {
        width: 68px;
        height: 68px;
        font-size: 2.4rem;
    }

    .complete-content.magnifier-on .thank-you {
        font-size: 1.5rem;
    }

    .complete-content.magnifier-on .order-id {
        font-size: 1.25rem;
    }

    .complete-content.magnifier-on .summary {
        padding: 1.25rem;
    }

    .complete-content.magnifier-on .row {
        font-size: 1.1rem;
    }

    .complete-content.magnifier-on .receipt-label,
    .complete-content.magnifier-on .countdown-text,
    .complete-content.magnifier-on .receipt-badge {
        font-size: 1.05rem;
    }

    .complete-content.magnifier-on .btn-primary,
    .complete-content.magnifier-on .btn-secondary,
    .complete-content.magnifier-on .btn-ghost,
    .complete-content.magnifier-on .btn-full,
    .complete-content.magnifier-on .btn-lg {
        font-size: 1rem;
        padding: 0.85rem 1.25rem;
    }

    .complete-content.magnifier-on .email-input {
        font-size: 1rem;
        padding: 0.75rem;
    }
</style>
