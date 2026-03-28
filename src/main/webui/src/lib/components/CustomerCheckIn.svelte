<script lang="ts">
    import type { Customer } from '$lib/types';
    import { findCustomerByPhone } from '$lib/api';
    import Modal from './Modal.svelte';

    let {
        open,
        onclose,
        onconfirm
    }: {
        open: boolean;
        onclose: () => void;
        onconfirm: (customer: Customer) => void;
    } = $props();

    let phone = $state('');
    let customer = $state<Customer | null>(null);
    let error = $state('');
    let searching = $state(false);

    async function lookup() {
        error = '';
        customer = null;
        if (!phone.trim()) {
            error = 'Please enter a phone number.';
            return;
        }
        searching = true;
        try {
            customer = await findCustomerByPhone(phone.trim());
            if (!customer) error = 'Customer not found.';
        } catch {
            error = 'Lookup failed.';
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
        phone = '';
        customer = null;
        error = '';
    }
</script>

<Modal {open} title="Customer Check-In" onclose={cancel}>
    <div class="checkin-form">
        <div class="search-row">
            <input
                type="tel"
                placeholder="Phone number"
                bind:value={phone}
                onkeydown={(e) => { if (e.key === 'Enter') void lookup(); }}
            />
            <button class="btn-primary" onclick={lookup} disabled={searching}>
                {searching ? 'Searching...' : 'Look Up'}
            </button>
        </div>

        {#if error}
            <p class="error-text">{error}</p>
        {/if}

        {#if customer}
            <div class="customer-info card">
                <p><strong>Name:</strong> {customer.firstName} {customer.lastName}</p>
                <p><strong>Phone:</strong> {customer.phone}</p>
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
