<script lang="ts">
    import type { Snippet } from 'svelte';
    import { goto } from '$app/navigation';
    import { resolve } from '$app/paths';
    import { page } from '$app/state';
    import { getEmployee, setEmployee, getDisplayName } from '$lib/stores/auth.svelte';

    let { children }: { children: Snippet } = $props();

    $effect(() => {
        if (!getEmployee()) void goto(resolve('/'));
    });

    const navItems = [
        { label: 'Dashboard', path: '/manager' as const },
        { label: 'Employees', path: '/manager/employees' as const },
        { label: 'Menu', path: '/manager/menu' as const },
        { label: 'Inventory', path: '/manager/inventory' as const },
        { label: 'Reports', path: '/manager/reports' as const }
    ];

    function isActive(path: string): boolean {
        if (path === '/manager') return page.url.pathname === '/manager';
        return page.url.pathname.startsWith(path);
    }

    function logout() {
        if (confirm('Are you sure you want to log out?')) {
            setEmployee(null);
            void goto(resolve('/'));
        }
    }
</script>

<div class="manager-layout">
    <aside class="manager-sidebar">
        <div class="sidebar-brand">
            <h2>Team 44 Boba POS</h2>
            <span class="role-label">Manager</span>
        </div>

        <nav class="sidebar-nav">
            {#each navItems as item (item.path)}
                <a
                    href={resolve(item.path)}
                    class="nav-link"
                    class:active={isActive(item.path)}
                >
                    {item.label}
                </a>
            {/each}
        </nav>

        <div class="sidebar-footer">
            <span class="user-name">{getDisplayName()}</span>
            <button class="btn-ghost" onclick={logout}>Logout</button>
        </div>
    </aside>

    <main class="manager-main">
        {@render children()}
    </main>
</div>

<style>
    .manager-layout {
        display: flex;
        height: 100vh;
    }

    .manager-sidebar {
        width: 220px;
        background: #1e293b;
        color: white;
        display: flex;
        flex-direction: column;
        padding: 1.25rem 0;
    }

    .sidebar-brand {
        padding: 0 1.25rem 1.25rem;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .sidebar-brand h2 {
        font-size: 1.25rem;
        font-weight: 700;
    }

    .role-label {
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: rgba(255, 255, 255, 0.5);
    }

    .sidebar-nav {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
        padding: 1rem 0.75rem;
    }

    .nav-link {
        display: block;
        padding: 0.6rem 1rem;
        border-radius: var(--radius);
        font-size: 0.875rem;
        font-weight: 500;
        color: rgba(255, 255, 255, 0.7);
        text-decoration: none;
        transition: background var(--transition), color var(--transition);
    }

    .nav-link:hover {
        background: rgba(255, 255, 255, 0.08);
        color: white;
    }

    .nav-link.active {
        background: var(--color-primary);
        color: white;
    }

    .sidebar-footer {
        padding: 1rem 1.25rem 0;
        border-top: 1px solid rgba(255, 255, 255, 0.1);
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .user-name {
        font-size: 0.8rem;
        color: rgba(255, 255, 255, 0.6);
    }

    .sidebar-footer button {
        color: rgba(255, 255, 255, 0.6);
        justify-content: flex-start;
        padding-left: 0;
    }

    .sidebar-footer button:hover {
        color: white;
        background: transparent;
    }

    .manager-main {
        flex: 1;
        overflow-y: auto;
        padding: 2rem;
    }
</style>
