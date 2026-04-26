<script lang="ts">
    import type { Snippet } from 'svelte';
    import '$lib/styles/global.css';
    import favicon from '$lib/assets/favicon.svg';

    let { children }: { children: Snippet } = $props();
</script>

<svelte:head>
    <link rel="icon" href={favicon} />
    <title>Boba Bob's</title>
</svelte:head>

{@render children()}
