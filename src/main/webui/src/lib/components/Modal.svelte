<script lang="ts">
    import type { Snippet } from 'svelte';
    import { fade } from 'svelte/transition';

    let {
        open,
        title,
        onclose,
        wide = false,
        children
    }: {
        open: boolean;
        title: string;
        onclose: () => void;
        wide?: boolean;
        children: Snippet;
    } = $props();

    function handleBackdrop(e: MouseEvent) {
        if (e.target === e.currentTarget) onclose();
    }

    function handleKeydown(e: KeyboardEvent) {
        if (e.key === 'Escape') onclose();
    }
</script>

{#if open}
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div class="backdrop" onclick={handleBackdrop} onkeydown={handleKeydown} transition:fade={{ duration: 200 }}>
        <div class="modal" class:wide role="dialog" aria-label={title}>
            <div class="modal-header">
                <h2>{title}</h2>
                <button class="btn-ghost close-btn" onclick={onclose}>&times;</button>
            </div>
            <div class="modal-body">
                {@render children()}
            </div>
        </div>
    </div>
{/if}

<style>
    .backdrop {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
    }

    .modal {
        background: var(--color-surface);
        border-radius: var(--radius);
        box-shadow: var(--shadow-md);
        width: 90%;
        max-width: 480px;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
    }

    .modal.wide {
        max-width: 720px;
    }

    .modal-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 1rem 1.5rem;
        border-bottom: 1px solid var(--color-border);
    }

    .modal-header h2 {
        font-size: 1.125rem;
        font-weight: 600;
    }

    .close-btn {
        font-size: 1.5rem;
        line-height: 1;
        padding: 0.25rem 0.5rem;
    }

    .modal-body {
        padding: 1.5rem;
        overflow-y: auto;
    }
</style>
