<script lang="ts">
    import { onMount } from 'svelte';
    import { getDisplayMenu, menuItemImageByName } from '$lib/api';

    type Item = {
        name: string;
        category: string;
        basePrice: number;
        hasImage: boolean;
    };

    type Section = { category: string; items: Item[] };
    type SingleSlide = { type: 'single'; category: string; items: Item[] };
    type CombinedSlide = { type: 'combined'; sections: Section[] };
    type Slide = SingleSlide | CombinedSlide;

    const MIN_ITEMS_FOR_OWN_SLIDE = 5;
    const MAX_ITEMS_PER_COMBINED_SLIDE = 10;
    const SLIDE_DURATION_MS = 7000;
    const REFRESH_INTERVAL_MS = 60000;

    const OVERSIZED_IMAGES = new Set(['mango w/ ice cream']);
    const isOversized = (name: string) =>
        OVERSIZED_IMAGES.has(name.toLowerCase().trim());

    type Temperature = 'hot' | 'cold' | null;
    function temperatureFor(category: string): Temperature {
        const c = category.toLowerCase().trim();
        if (c === 'topping') return null;
        if (c === 'fresh brew') return 'hot';
        return 'cold';
    }

    let slides = $state<Slide[]>([]);
    let currentIndex = $state(0);

    function buildSlides(grouped: Record<string, Item[]>): Slide[] {
        const big: [string, Item[]][] = [];
        const small: [string, Item[]][] = [];

        for (const [cat, items] of Object.entries(grouped)) {
            if (items.length === 0) continue;
            if (items.length >= MIN_ITEMS_FOR_OWN_SLIDE) {
                big.push([cat, items]);
            } else {
                small.push([cat, items]);
            }
        }

        small.sort((a, b) => b[1].length - a[1].length);

        const result: Slide[] = [];
        for (const [cat, items] of big) {
            result.push({ type: 'single', category: cat, items });
        }

        let pool: Section[] = [];
        let poolCount = 0;
        const flushPool = () => {
            if (pool.length === 0) return;
            if (pool.length === 1) {
                result.push({ type: 'single', category: pool[0].category, items: pool[0].items });
            } else {
                result.push({ type: 'combined', sections: pool });
            }
            pool = [];
            poolCount = 0;
        };

        for (const [cat, items] of small) {
            if (poolCount + items.length > MAX_ITEMS_PER_COMBINED_SLIDE && pool.length > 0) {
                flushPool();
            }
            pool.push({ category: cat, items });
            poolCount += items.length;
        }
        flushPool();

        return result;
    }

    function combinedTitle(sections: Section[]): string {
        if (sections.length === 1) return sections[0].category;
        if (sections.length === 2) return `${sections[0].category} & ${sections[1].category}`;
        return sections.map((s) => s.category).join(', ');
    }

    function combinedItems(sections: Section[]): Item[] {
        return sections.flatMap((s) => s.items);
    }

    async function load() {
        try {
            const items: Item[] = await getDisplayMenu();
            const grouped: Record<string, Item[]> = {};
            for (const item of items) {
                if (!grouped[item.category]) grouped[item.category] = [];
                if (!grouped[item.category].some((i) => i.name === item.name)) {
                    grouped[item.category].push(item);
                }
            }
            slides = buildSlides(grouped);
            if (currentIndex >= slides.length) currentIndex = 0;
        } catch (e) {
            console.error('menu load failed:', e);
            slides = [];
        }
    }

    onMount(() => {
        load();
        const refresh = setInterval(load, REFRESH_INTERVAL_MS);
        const rotate = setInterval(() => {
            if (slides.length > 1) {
                currentIndex = (currentIndex + 1) % slides.length;
            }
        }, SLIDE_DURATION_MS);
        return () => {
            clearInterval(refresh);
            clearInterval(rotate);
        };
    });
</script>

{#snippet itemCard(item: Item, showCategory: boolean)}
    {@const temp = temperatureFor(item.category)}
    <article class="item">
        <div class="img-wrap">
            {#if item.hasImage}
                <img
                    src={menuItemImageByName(item.name)}
                    alt=""
                    class:oversized={isOversized(item.name)}
                />
            {:else}
                <span class="placeholder" aria-hidden="true">🧋</span>
            {/if}
            {#if temp}
                <span class="temp-badge temp-{temp}">
                    <span class="temp-icon" aria-hidden="true">
                        {temp === 'hot' ? '🔥' : '🧊'}
                    </span>
                    <span>{temp === 'hot' ? 'Hot' : 'Cold'}</span>
                </span>
            {/if}
        </div>
        <div class="info">
            {#if showCategory}
                <div class="cat-badge">{item.category}</div>
            {/if}
            <div class="name">{item.name}</div>
            <div class="price">
                <span class="from">From</span>${Number(item.basePrice).toFixed(2)}
            </div>
        </div>
    </article>
{/snippet}

<div class="board">
    <header class="board-header">
        <div class="brand">
            <span class="brand-mark" aria-hidden="true">🧋</span>
            <span class="brand-name">Boba Bob's</span>
        </div>
        {#if slides.length > 1}
            <div class="dots" aria-hidden="true">
                {#each slides as _, i}
                    <span class="dot" class:active={i === currentIndex}></span>
                {/each}
            </div>
        {/if}
    </header>

    <main class="stage">
        {#if slides.length === 0}
            <p class="empty">Menu unavailable.</p>
        {:else}
            {#each slides as slide, i}
                <section class="slide" class:visible={i === currentIndex}>
                    {#if slide.type === 'single'}
                        <h2 class="category-title">{slide.category}</h2>
                        <div class="grid" data-count={slide.items.length}>
                            {#each slide.items as item}
                                {@render itemCard(item, false)}
                            {/each}
                        </div>
                    {:else}
                        <h2 class="category-title">{combinedTitle(slide.sections)}</h2>
                        <div class="grid" data-count={combinedItems(slide.sections).length}>
                            {#each combinedItems(slide.sections) as item}
                                {@render itemCard(item, true)}
                            {/each}
                        </div>
                    {/if}
                </section>
            {/each}
        {/if}
    </main>
</div>

<style>
    .board {
        height: 100vh;
        width: 100vw;
        background: var(--color-bg);
        color: var(--color-text);
        display: flex;
        flex-direction: column;
        overflow: hidden;
    }

    .board-header {
        flex: 0 0 auto;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: clamp(0.9rem, 1.6vh, 1.5rem) clamp(1.5rem, 3vw, 3rem);
        background: linear-gradient(135deg, #d4712a 0%, #e8944c 100%);
        color: white;
        box-shadow: 0 2px 12px rgba(45, 32, 23, 0.12);
    }

    .brand {
        display: flex;
        align-items: center;
        gap: 0.75rem;
    }

    .brand-mark {
        font-size: clamp(1.8rem, 3.5vh, 2.6rem);
        line-height: 1;
    }

    .brand-name {
        font-size: clamp(1.6rem, 3.2vh, 2.4rem);
        font-weight: 800;
        letter-spacing: -0.5px;
    }

    .dots {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
        max-width: 50%;
        justify-content: flex-end;
    }

    .dot {
        width: 0.6rem;
        height: 0.6rem;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.45);
        transition:
            background 300ms ease,
            transform 300ms ease;
    }

    .dot.active {
        background: white;
        transform: scale(1.35);
    }

    .stage {
        position: relative;
        flex: 1 1 auto;
        overflow: hidden;
    }

    .empty {
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--color-text-muted);
        font-size: 1.5rem;
    }

    .slide {
        position: absolute;
        inset: 0;
        padding: clamp(1.25rem, 3vh, 2.5rem) clamp(1.75rem, 4vw, 3.5rem);
        display: flex;
        flex-direction: column;
        opacity: 0;
        transition: opacity 500ms ease-in-out;
        pointer-events: none;
    }

    .slide.visible {
        opacity: 1;
    }

    .category-title {
        font-size: clamp(1.8rem, 4.5vh, 3.2rem);
        font-weight: 800;
        color: #d4712a;
        margin-bottom: clamp(0.9rem, 2.2vh, 1.75rem);
        letter-spacing: -0.5px;
        text-transform: capitalize;
        flex: 0 0 auto;
    }

    .grid {
        flex: 1 1 auto;
        display: grid;
        gap: clamp(0.75rem, 1.6vh, 1.4rem);
        min-height: 0;
    }

    .grid[data-count='1'] {
        grid-template-columns: 1fr;
        grid-template-rows: 1fr;
    }
    .grid[data-count='2'] {
        grid-template-columns: 1fr 1fr;
        grid-template-rows: 1fr;
    }
    .grid[data-count='3'] {
        grid-template-columns: 1fr 1fr 1fr;
        grid-template-rows: 1fr;
    }
    .grid[data-count='4'] {
        grid-template-columns: repeat(4, 1fr);
        grid-template-rows: 1fr;
    }
    .grid[data-count='5'] {
        grid-template-columns: repeat(5, 1fr);
        grid-template-rows: 1fr;
    }
    .grid[data-count='6'] {
        grid-template-columns: repeat(3, 1fr);
        grid-template-rows: 1fr 1fr;
    }
    .grid[data-count='7'] {
        grid-template-columns: repeat(8, 1fr);
        grid-template-rows: 1fr 1fr;
    }
    .grid[data-count='7'] > .item {
        grid-column: span 2;
    }
    .grid[data-count='7'] > .item:nth-child(5) {
        grid-column: 2 / span 2;
    }
    .grid[data-count='7'] > .item:nth-child(6) {
        grid-column: 4 / span 2;
    }
    .grid[data-count='7'] > .item:nth-child(7) {
        grid-column: 6 / span 2;
    }
    .grid[data-count='8'] {
        grid-template-columns: repeat(4, 1fr);
        grid-template-rows: 1fr 1fr;
    }
    .grid[data-count='9'] {
        grid-template-columns: repeat(10, 1fr);
        grid-template-rows: 1fr 1fr;
    }
    .grid[data-count='9'] > .item {
        grid-column: span 2;
    }
    .grid[data-count='9'] > .item:nth-child(6) {
        grid-column: 2 / span 2;
    }
    .grid[data-count='9'] > .item:nth-child(7) {
        grid-column: 4 / span 2;
    }
    .grid[data-count='9'] > .item:nth-child(8) {
        grid-column: 6 / span 2;
    }
    .grid[data-count='9'] > .item:nth-child(9) {
        grid-column: 8 / span 2;
    }
    .grid[data-count='10'] {
        grid-template-columns: repeat(5, 1fr);
        grid-template-rows: 1fr 1fr;
    }
    .grid[data-count='11'],
    .grid[data-count='12'] {
        grid-template-columns: repeat(4, 1fr);
        grid-template-rows: repeat(3, 1fr);
    }

    .item {
        background: var(--color-surface);
        border-radius: 16px;
        box-shadow: 0 2px 10px rgba(45, 32, 23, 0.08);
        border: 1px solid var(--color-border);
        overflow: hidden;
        display: flex;
        flex-direction: column;
        align-items: stretch;
        padding: clamp(0.5rem, 1.2vh, 1rem);
        gap: clamp(0.3rem, 0.8vh, 0.7rem);
        min-width: 0;
        container-type: size;
    }

    .img-wrap {
        position: relative;
        flex: 1 1 auto;
        width: 100%;
        min-height: 35%;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
    }

    .img-wrap img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
        display: block;
    }

    .img-wrap img.oversized {
        max-width: 50%;
        max-height: 50%;
    }

    .temp-badge {
        position: absolute;
        top: clamp(0.3rem, 1.5cqh, 0.7rem);
        right: clamp(0.3rem, 1.5cqh, 0.7rem);
        display: inline-flex;
        align-items: center;
        gap: 0.3em;
        padding: clamp(0.2rem, 0.9cqh, 0.45rem) clamp(0.5rem, 1.4cqh, 0.85rem);
        border-radius: 9999px;
        font-size: clamp(0.6rem, 3.2cqh, 1.05rem);
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        line-height: 1;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
        backdrop-filter: blur(4px);
        -webkit-backdrop-filter: blur(4px);
    }

    .temp-icon {
        font-size: 1.15em;
        line-height: 1;
    }

    .temp-hot {
        background: rgba(254, 226, 226, 0.95);
        color: #b91c1c;
        border: 1px solid #fca5a5;
    }

    .temp-cold {
        background: rgba(219, 234, 254, 0.95);
        color: #1d4ed8;
        border: 1px solid #93c5fd;
    }

    .placeholder {
        font-size: clamp(2.2rem, 22cqh, 5rem);
        line-height: 1;
    }

    .info {
        flex: 0 0 auto;
        width: 100%;
        text-align: center;
        display: flex;
        flex-direction: column;
        gap: clamp(0.1rem, 0.4cqh, 0.4rem);
    }

    .cat-badge {
        font-size: clamp(0.55rem, 3cqh, 0.95rem);
        font-weight: 600;
        color: var(--color-text-muted);
        text-transform: uppercase;
        letter-spacing: 0.08em;
    }

    .name {
        font-size: clamp(0.95rem, 7cqh, 2.2rem);
        font-weight: 700;
        color: var(--color-text);
        text-transform: capitalize;
        line-height: 1.15;
        word-break: break-word;
        hyphens: auto;
    }

    .price {
        font-size: clamp(0.9rem, 5.5cqh, 1.7rem);
        font-weight: 800;
        color: #d4712a;
        line-height: 1.1;
    }

    .from {
        font-size: 0.65em;
        font-weight: 500;
        color: var(--color-text-muted);
        margin-right: 0.3em;
        text-transform: capitalize;
        letter-spacing: 0.02em;
    }
</style>
