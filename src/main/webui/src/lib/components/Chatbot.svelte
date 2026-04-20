<script lang="ts">
    type Msg = { from: 'bot' | 'user'; text: string; time: string };

    let open = $state(false);
    let input = $state('');

    //Default introduction message to welcome user
    let messages = $state<Msg[]>([
        { from: 'bot', text: "Hi! I'm Boba Bob — how can I help?", time: new Date().toISOString() }
    ]);

    let endEl = $state<HTMLElement | null>(null);
//Basic toggle function to open and close chat box
    function toggle() {
        open = !open;
    }

//Send Function. Sends over the user prompt over to backend. Handles errors if backend is not connected
    async function send() {
        const text = input.trim();
        if (!text) return;

        messages.push({ from: 'user', text, time: new Date().toISOString() });
        input = '';

        try {
            const response = await fetch('/api/chatbot', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ prompt: text })
            });

            if (!response.ok) {
                const errorBody = await response.text();
                throw new Error(`Chatbot API failed: ${response.status} ${response.statusText} — ${errorBody}`);
            }

            const data = await response.json();
            messages.push({ 
                from: 'bot', 
                text: data.text ?? 'Boba Bob did not return a response.', 
                time: new Date().toISOString() 
            });
        } catch (err) {
            const errorMessage = err instanceof Error ? err.message : 'Unknown error';
            messages.push({ 
                from: 'bot', 
                text: `Sorry, I'm having trouble connecting to Boba Bob: ${errorMessage}`,
                time: new Date().toISOString() 
            });
            console.error('Chatbot send error:', err);
        }
    }


    $effect(() => {
        if (open || messages.length) {
            endEl?.scrollIntoView({ behavior: 'smooth' });
        }
    });
</script>

//Styling for Chatbot

<div class="chatbot-root">
    {#if open}
        <div class="chat-window" role="dialog" aria-label="Chat with us">
            <header class="chat-header">
                <div class="chat-title">Boba Bob</div>
                <button class="chat-close" aria-label="Close" onclick={toggle}>&times;</button>
            </header>

            <section class="chat-messages">
                {#each messages as m (m.time)}
                    <div class="msg" class:bot={m.from === 'bot'} class:user={m.from === 'user'}>
                        <div class="bubble">{m.text}</div>
                    </div>
                {/each}
                <div bind:this={endEl} />
            </section>

            <footer class="chat-input">
                <input
                    placeholder="Type a message..."
                    bind:value={input}
                    onkeydown={(e) => { if (e.key === 'Enter') { e.preventDefault(); send(); } }}
                />
                <button class="send-btn" onclick={send} aria-label="Send">Send</button>
            </footer>
        </div>
    {/if}

    <button class="chat-toggle" aria-label="Open chat" onclick={toggle}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M21 15a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10z" fill="currentColor" />
        </svg>
    </button>
</div>

<style>
    .chatbot-root {
        position: fixed;
        right: var(--chatbot-right-offset, 20px);
        bottom: var(--chatbot-bottom-offset, 20px);
        z-index: 9999;
        font-family: system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial;
    }

    .chat-toggle {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        border: none;
        background: var(--color-primary, #7c3aed);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 6px 18px rgba(0,0,0,0.18);
        cursor: pointer;
    }

    .chat-toggle:hover { transform: translateY(-2px); }

    .chat-window {
        width: 340px;
        height: 460px;
        display: flex;
        flex-direction: column;
        background: var(--color-surface, #fff);
        border: 1px solid var(--color-border, #e6e6e6);
        border-radius: 12px;
        box-shadow: 0 12px 40px rgba(0,0,0,0.2);
        overflow: hidden;
        margin-bottom: 12px;
    }

    .chat-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.75rem 1rem;
        border-bottom: 1px solid var(--color-border, #e6e6e6);
        background: linear-gradient(180deg, rgba(0,0,0,0.02), transparent);
    }

    .chat-title { font-weight: 700; color: var(--color-primary); }
    .chat-close { background: transparent; border: none; font-size: 1.25rem; cursor: pointer; }

    .chat-messages {
        flex: 1;
        padding: 0.75rem;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        background: linear-gradient(180deg, transparent, rgba(0,0,0,0.02));
    }

    .msg { display: flex; }
    .msg.bot { justify-content: flex-start; }
    .msg.user { justify-content: flex-end; }

    .bubble {
        max-width: 76%;
        padding: 0.5rem 0.75rem;
        border-radius: 12px;
        font-size: 0.9rem;
        line-height: 1.25;
    }

    .msg.bot .bubble {
        background: var(--color-surface, #f3f4f6);
        color: var(--color-text, #111827);
        border: 1px solid var(--color-border, #e6e6e6);
        border-bottom-left-radius: 6px;
    }

    .msg.user .bubble {
        background: var(--color-primary, #7c3aed);
        color: white;
        border-bottom-right-radius: 6px;
    }

    .chat-input {
        display: flex;
        gap: 0.5rem;
        padding: 0.6rem;
        border-top: 1px solid var(--color-border, #e6e6e6);
        background: var(--color-surface, #fff);
    }

    .chat-input input {
        flex: 1;
        padding: 0.5rem 0.75rem;
        border-radius: 8px;
        border: 1px solid var(--color-border, #e6e6e6);
        outline: none;
        font-size: 0.95rem;
    }

    .send-btn {
        background: var(--color-primary, #7c3aed);
        color: white;
        border: none;
        padding: 0.5rem 0.8rem;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
    }

    @media (max-width: 560px) {
        .chat-window { width: 92vw; right: 0; left: 0; margin: 0 auto; }
    }
</style>
