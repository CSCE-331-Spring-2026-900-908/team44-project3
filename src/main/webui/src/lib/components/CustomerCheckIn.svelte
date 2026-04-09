<script lang="ts">
    import type { Customer } from '$lib/types';
    import { findCustomerByPhone, customerCheckin } from '$lib/api';
    import Modal from './Modal.svelte';

    let {
        open,
        mode = 'phone',
        onclose,
        onconfirm
    }: {
        open: boolean;
        mode?: 'phone' | 'email';
        onclose: () => void;
        onconfirm: (customer: Customer) => void;
    } = $props();

    let input = $state('');
    let customer = $state<Customer | null>(null);
    let error = $state('');
    let searching = $state(false);
    let inputEl = $state<HTMLInputElement | null>(null);

    async function lookup() {
        error = '';
        customer = null;

        if (!input.trim()) {
            error = mode === 'phone' ? 'Please enter a phone number.' : 'Please enter your email.';
            return;
        }

        if (mode === 'email' && inputEl && !inputEl.validity.valid) {
            error = 'Please enter a valid email address.';
            return;
        }

        searching = true;
        try {
            if (mode === 'phone') {
                customer = await findCustomerByPhone(input.trim());
                if (!customer) error = 'Customer not found.';
            } else {
                customer = await customerCheckin(input.trim().toLowerCase());
            }
        } catch {
            error = mode === 'phone' ? 'Lookup failed.' : 'Check-in failed. Please try again.';
        } finally {
            searching = false;
        }
    }

    function confirm() {
        if (customer) {
            onconfirm(customer);
            reset();
        }
    }

    function cancel() {
        reset();
        onclose();
    }

    function reset() {
        input = '';
        customer = null;
        error = '';
    }
</script>

<Modal {open} title={mode === 'phone' ? 'Customer Check-In' : 'Sign In'} onclose={cancel}>
    <div class="checkin-form">
        <div class="search-row">
            {#if mode === 'phone'}
                <input
                    type="tel"
                    placeholder="Phone number"
                    bind:value={input}
                    onkeydown={(e) => { if (e.key === 'Enter') void lookup(); }}
                />
            {:else}
                <input
                    bind:this={inputEl}
                    type="email"
                    pattern=".+@.+\..+"
                    placeholder="Enter your email"
                    bind:value={input}
                    onkeydown={(e) => { if (e.key === 'Enter') void lookup(); }}
                />
            {/if}
            <button class="btn-primary" onclick={lookup} disabled={searching}>
                {searching ? 'Searching...' : mode === 'phone' ? 'Look Up' : 'Sign In'}
            </button>
        </div>

        {#if error}
            <p class="error-text">{error}</p>
        {/if}

        {#if customer}
            <div class="customer-info card">
                {#if customer.firstName || customer.lastName}
                    <p><strong>Name:</strong> {customer.firstName} {customer.lastName}</p>
                {/if}
                {#if customer.phone}
                    <p><strong>Phone:</strong> {customer.phone}</p>
                {/if}
                {#if customer.email}
                    <p><strong>Email:</strong> {customer.email}</p>
                {/if}
                <p><strong>Reward Points:</strong> {customer.rewardPoints}</p>
            </div>
        {/if}

        <div class="actions">
            <button class="btn-secondary" onclick={cancel}>Cancel</button>
            <button class="btn-primary" onclick={confirm} disabled={!customer}>Confirm</button>
        </div>
    </div>
</Modal>

<style>
    .checkin-form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }

    .search-row {
        display: flex;
        gap: 0.5rem;
    }

    .search-row input {
        flex: 1;
    }

    .customer-info {
        padding: 1rem;
    }

    .customer-info p {
        margin-bottom: 0.25rem;
    }

    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
    }
</style>
