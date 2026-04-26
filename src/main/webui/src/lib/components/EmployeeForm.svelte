<script lang="ts">
    import type { Employee } from '$lib/types';
    import { addEmployee, updateEmployee } from '$lib/api';
    import { todayISO } from '$lib/utils';
    import Modal from './Modal.svelte';

    let {
        open,
        employee,
        onclose,
        onsaved
    }: {
        open: boolean;
        employee: Employee | null;
        onclose: () => void;
        onsaved: () => void;
    } = $props();

    let firstName = $state('');
    let lastName = $state('');
    let role = $state('cashier');
    let startDate = $state(todayISO());
    let email = $state('');
    let password = $state('');
    let isActive = $state(true);
    let error = $state('');
    let saving = $state(false);

    let isEdit = $derived(employee !== null);

    $effect(() => {
        if (open && employee) {
            firstName = employee.firstName;
            lastName = employee.lastName;
            role = employee.role;
            startDate = employee.startDate;
            email = employee.email;
            password = '';
            isActive = employee.isActive;
        } else if (open) {
            firstName = '';
            lastName = '';
            role = 'cashier';
            startDate = todayISO();
            email = '';
            password = '';
            isActive = true;
        }
        error = '';
    });

    async function save() {
        if (!firstName || !lastName || !email) {
            error = 'First name, last name, and email are required.';
            return;
        }
        if (!isEdit && !password) {
            error = 'Password is required for new employees.';
            return;
        }

        saving = true;
        error = '';
        try {
            if (isEdit && employee) {
                await updateEmployee({
                    ...employee,
                    firstName,
                    lastName,
                    role,
                    startDate,
                    email,
                    passwordHash: password,
                    isActive
                });
            } else {
                await addEmployee({
                    firstName,
                    lastName,
                    role,
                    startDate,
                    email,
                    passwordHash: password,
                    isActive
                });
            }
            onsaved();
            onclose();
        } catch {
            error = 'Failed to save employee.';
        } finally {
            saving = false;
        }
    }
</script>

<Modal {open} title={isEdit ? 'Edit Employee' : 'Add Employee'} {onclose}>
    <form class="emp-form" onsubmit={save}>
        <div class="form-grid">
            <label for="emp-first">First Name</label>
            <input id="emp-first" bind:value={firstName} />

            <label for="emp-last">Last Name</label>
            <input id="emp-last" bind:value={lastName} />

            <label for="emp-role">Role</label>
            <select id="emp-role" bind:value={role}>
                <option value="cashier">Cashier</option>
                <option value="manager">Manager</option>
            </select>

            <label for="emp-start">Start Date</label>
            <input id="emp-start" type="date" bind:value={startDate} />

            <label for="emp-email">Email</label>
            <input id="emp-email" type="email" bind:value={email} />

            <label for="emp-pass">Password</label>
            <input
                id="emp-pass"
                type="password"
                bind:value={password}
                placeholder={isEdit ? '(leave blank to keep)' : ''}
            />

            <label for="emp-active">Active</label>
            <input id="emp-active" type="checkbox" bind:checked={isActive} />
        </div>

        {#if error}
            <p class="error-text">{error}</p>
        {/if}

        <div class="actions">
            <button type="button" class="btn-secondary" onclick={onclose}>Cancel</button>
            <button type="submit" class="btn-primary" disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
            </button>
        </div>
    </form>
</Modal>

<style>
    .emp-form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }

    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
    }
</style>
