<script lang="ts">
    let { openUp = false }: { openUp?: boolean } = $props();

    let open = $state(false);
    let currentLang = $state('en');
    let selectorEl = $state<HTMLDivElement | null>(null);

    const languages = [
        { code: 'en', name: 'English' },
        { code: 'es', name: 'Español' },
        { code: 'zh-CN', name: '中文 (简体)' },
        { code: 'fr', name: 'Français' },
        { code: 'vi', name: 'Tiếng Việt' },
        { code: 'ko', name: '한국어' },
        { code: 'pt', name: 'Português' },
        { code: 'ar', name: 'العربية' },
        { code: 'ja', name: '日本語' },
        { code: 'hi', name: 'हिन्दी' },
        { code: 'de', name: 'Deutsch' },
        { code: 'it', name: 'Italiano' },
        { code: 'ru', name: 'Русский' },
        { code: 'th', name: 'ภาษาไทย' },
        { code: 'tl', name: 'Filipino' }
    ];

    $effect(() => {
        const match = document.cookie.match(/(?:^|;\s*)googtrans=\/en\/([^;]+)/);
        if (match) {
            currentLang = match[1];
        }
    });

    $effect(() => {
        if (!open) return;
        function handleClick(e: MouseEvent) {
            if (selectorEl && !selectorEl.contains(e.target as Node)) {
                open = false;
            }
        }
        document.addEventListener('click', handleClick);
        return () => document.removeEventListener('click', handleClick);
    });

    function selectLanguage(code: string) {
        if (code === 'en') {
            document.cookie = 'googtrans=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
            document.cookie = 'googtrans=; path=/; domain=' + location.hostname + '; expires=Thu, 01 Jan 1970 00:00:00 GMT';
        } else {
            document.cookie = `googtrans=/en/${code}; path=/`;
        }
        open = false;
        window.location.reload();
    }

    function getCurrentName() {
        return languages.find(l => l.code === currentLang)?.name ?? 'English';
    }
</script>

<div class="lang-selector" bind:this={selectorEl} translate="no">
    <button class="lang-btn" onclick={() => (open = !open)} aria-label="Select language">
        <span class="globe">🌐</span>
        <span class="lang-name">{getCurrentName()}</span>
        <span class="chevron">{open ? '▲' : '▼'}</span>
    </button>

    {#if open}
        <div class="lang-dropdown" class:open-up={openUp}>
            {#each languages as lang}
                <button
                    class="lang-option"
                    class:active={lang.code === currentLang}
                    onclick={() => selectLanguage(lang.code)}
                >
                    {lang.name}
                </button>
            {/each}
        </div>
    {/if}
</div>

<style>
    .lang-selector {
        position: relative;
        display: inline-block;
        z-index: 50;
    }

    .lang-btn {
        display: flex;
        align-items: center;
        gap: 0.35rem;
        padding: 0.35rem 0.75rem;
        border: 1px solid var(--color-border, #e2e8f0);
        border-radius: 999px;
        background: var(--color-surface, #fff);
        color: var(--color-text, #1a202c);
        font-size: 0.8rem;
        cursor: pointer;
        transition: border-color 0.15s, box-shadow 0.15s;
        white-space: nowrap;
    }

    .lang-btn:hover {
        border-color: var(--color-primary, #FF5A5A);
        box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary, #FF5A5A) 15%, transparent);
    }

    .globe { font-size: 1rem; }
    .chevron { font-size: 0.6rem; opacity: 0.6; }

    .lang-dropdown {
        position: absolute;
        top: calc(100% + 6px);
        right: 0;
        background: var(--color-surface, #fff);
        border: 1px solid var(--color-border, #e2e8f0);
        border-radius: 0.5rem;
        box-shadow: 0 4px 16px rgba(0,0,0,0.12);
        z-index: 51;
        min-width: 160px;
        max-height: 320px;
        overflow-y: auto;
        padding: 0.25rem;
    }

    .lang-dropdown.open-up {
        top: auto;
        bottom: calc(100% + 6px);
    }

    .lang-option {
        display: block;
        width: 100%;
        padding: 0.45rem 0.75rem;
        text-align: left;
        background: none;
        border: none;
        border-radius: 0.35rem;
        font-size: 0.85rem;
        color: var(--color-text, #1a202c);
        cursor: pointer;
        transition: background 0.1s;
    }

    .lang-option:hover {
        background: var(--color-bg, #f7f8fa);
    }

    .lang-option.active {
        font-weight: 700;
        color: var(--color-primary, #FF5A5A);
    }
</style>
