<script lang="ts">
import { goto } from '$app/navigation';
</script>

<div class="display-portal">
    <h1>Store Displays</h1>

    <div class="display-grid">
        <button onclick={() => goto('/kitchen')}>
            🍳 Kitchen Display
        </button>

        <button onclick={() => goto('/pickup')}>
            🧋 Pickup Display
        </button>

        <button onclick={() => goto('/menu')}>
            📋 Menu Board
        </button>
    </div>
</div>

<style>
.display-portal {
    padding: 2rem;
    text-align: center;
}

.display-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 2rem;
    margin-top: 2rem;
}

button {
    padding: 2rem;
    font-size: 1.25rem;
}
</style>