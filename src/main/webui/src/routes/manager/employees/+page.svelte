<script lang="ts">
    import type { Employee } from '$lib/types';
    import { getAllEmployees, deleteEmployee } from '$lib/api';
    import { toTitleCase } from '$lib/utils';
    import EmployeeForm from '$lib/components/EmployeeForm.svelte';

    let employees = $state<Employee[]>([]);
    let loading = $state(true);
    let showForm = $state(false);
    let editingEmployee = $state<Employee | null>(null);

    $effect(() => {
        void loadEmployees();
    });

    async function loadEmployees() {
        loading = true;
        try {
            employees = await getAllEmployees();
        } catch {
            employees = [];
        } finally {
            loading = false;
        }
    }

    function openAdd() {
        editingEmployee = null;
        showForm = true;
    }

    function openEdit(emp: Employee) {
        editingEmployee = emp;
        showForm = true;
    }

    async function handleDelete(emp: Employee) {
        if (!confirm(`Delete ${emp.firstName} ${emp.lastName}?`)) return;
        try {
            await deleteEmployee(emp.employeeId);
            await loadEmployees();
        } catch {
            alert('Failed to delete employee.');
        }
    }
</script>

<div class="page-header">
    <h1 class="page-title">Employees</h1>
    <button class="btn-primary" onclick={openAdd}>+ Add Employee</button>
</div>

{#if loading}
    <p>Loading...</p>
{:else}
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Role</th>
                <th>Email</th>
                <th>Start Date</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            {#each employees as emp (emp.employeeId)}
                <tr>
                    <td>{emp.employeeId}</td>
                    <td>{toTitleCase(emp.firstName)} {toTitleCase(emp.lastName)}</td>
                    <td>{toTitleCase(emp.role)}</td>
                    <td>{emp.email}</td>
                    <td>{emp.startDate}</td>
                    <td>
                        <span class="badge" class:badge-success={emp.isActive} class:badge-danger={!emp.isActive}>
                            {emp.isActive ? 'Active' : 'Inactive'}
                        </span>
                    </td>
                    <td class="action-cell">
                        <button class="btn-ghost" onclick={() => { openEdit(emp); }}>Edit</button>
                        <button class="btn-ghost danger-text" onclick={() => { void handleDelete(emp); }}>
                            Delete
                        </button>
                    </td>
                </tr>
            {/each}
        </tbody>
    </table>
{/if}

<EmployeeForm
    open={showForm}
    employee={editingEmployee}
    onclose={() => (showForm = false)}
    onsaved={loadEmployees}
/>

<style>
    .action-cell {
        display: flex;
        gap: 0.25rem;
    }

    .danger-text {
        color: var(--color-danger);
    }
</style>
