<script lang="ts">
    import { resolve } from '$app/paths';
    import { getDisplayName } from '$lib/stores/auth.svelte';

    const sections = [
        {
            title: 'Employees',
            description: 'Manage staff accounts and roles',
            path: '/manager/employees' as const,
            color: '#FF5A5A'
        },
        {
            title: 'Menu',
            description: 'Edit menu items, categories, and pricing',
            path: '/manager/menu' as const,
            color: '#FF8B5A'
        },
        {
            title: 'Inventory',
            description: 'Track stock levels and reorders',
            path: '/manager/inventory' as const,
            color: '#FFA95A'
        },
        {
            title: 'Reports',
            description: 'Sales, X/Z reports, and trends',
            path: '/manager/reports' as const,
            color: '#FFD45A'
        }
    ] as const;
</script>

<div class="dashboard">
    <div class="page-header">
        <h1 class="page-title">Welcome, {getDisplayName()}</h1>
    </div>

    <div class="section-grid">
        {#each sections as section (section.path)}
            <a href={resolve(section.path)} class="section-card card">
                <div class="section-accent" style="background: {section.color}"></div>
                <h3>{section.title}</h3>
                <p>{section.description}</p>
            </a>
        {/each}
    </div>
</div>

<style>
    .section-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
        gap: 1.25rem;
    }

    .section-card {
        text-decoration: none;
        color: inherit;
        position: relative;
        overflow: hidden;
        transition:
            box-shadow var(--transition),
            transform var(--transition);
    }

    .section-card:hover {
        box-shadow: var(--shadow-md);
        transform: translateY(-2px);
    }

    .section-accent {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 4px;
    }

    .section-card h3 {
        font-size: 1.125rem;
        font-weight: 600;
        margin-bottom: 0.25rem;
    }

    .section-card p {
        font-size: 0.875rem;
        color: var(--color-text-muted);
    }
</style>
