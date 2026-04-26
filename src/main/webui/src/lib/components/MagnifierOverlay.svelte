<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { magnifierEnabled } from '$lib/stores/magnifier';
	import { fade } from 'svelte/transition';

	let { targetSelector = '#magnifier-root' } = $props();

	let lensSize = $state(340);
	let zoom = $state(1.5);

	let x = $state(window.innerWidth / 2);
	let y = $state(window.innerHeight / 3);
	let dragging = $state(false);
	let hoverEdge = $state(false);
	let dragOffsetX = 0;
	let dragOffsetY = 0;

	let targetEl = $state<HTMLElement | null>(null);
	let mirrorHtml = $state('');
	let observer: MutationObserver | null = null;
	let lensEl: HTMLDivElement;

	let showHint = $state(false);
	let hintTimer: ReturnType<typeof setTimeout> | null = null;

	const grabRingWidth = 55;

	function refreshMirror() {
		if (!targetEl) return;
		mirrorHtml = targetEl.outerHTML;
	}

	function resetHintTimer() {
		showHint = false;
		if (hintTimer) clearTimeout(hintTimer);
		hintTimer = setTimeout(() => { showHint = true; }, 5000);
	}

	onMount(async () => {
		await tick();

		targetEl = document.querySelector(targetSelector) as HTMLElement | null;

		if (targetEl) {
			refreshMirror();
			observer = new MutationObserver(() => refreshMirror());
			observer.observe(targetEl, {
				childList: true,
				subtree: true,
				attributes: true,
				characterData: true
			});
		}

		document.addEventListener('mousemove', onPointerMove, true);
		document.addEventListener('mouseup', onPointerUp, true);
		document.addEventListener('touchmove', onTouchMove, { passive: false, capture: true });
		document.addEventListener('touchend', onPointerUp, true);

		resetHintTimer();

		return () => {
			observer?.disconnect();
			document.removeEventListener('mousemove', onPointerMove, true);
			document.removeEventListener('mouseup', onPointerUp, true);
			document.removeEventListener('touchmove', onTouchMove, true);
			document.removeEventListener('touchend', onPointerUp, true);
			if (hintTimer) clearTimeout(hintTimer);
		};
	});

	function isEdge(clientX: number, clientY: number): boolean {
		const dx = clientX - x;
		const dy = clientY - y;
		const dist = Math.sqrt(dx * dx + dy * dy);
		return dist >= lensSize / 2 - grabRingWidth;
	}

	function clickThrough(clientX: number, clientY: number) {
		if (!lensEl) return;
		lensEl.style.visibility = 'hidden';
		const target = document.elementFromPoint(clientX, clientY);
		lensEl.style.visibility = '';
		if (target instanceof HTMLElement) {
			target.focus();
			target.click();
		}
	}

	function onLensHover(event: MouseEvent) {
		if (!dragging) {
			hoverEdge = isEdge(event.clientX, event.clientY);
		}
	}

	function onMouseDown(event: MouseEvent) {
		if (isEdge(event.clientX, event.clientY)) {
			dragging = true;
			dragOffsetX = x - event.clientX;
			dragOffsetY = y - event.clientY;
			resetHintTimer();
			event.preventDefault();
			event.stopPropagation();
		} else {
			clickThrough(event.clientX, event.clientY);
			event.preventDefault();
			event.stopPropagation();
		}
	}

	function onTouchStart(event: TouchEvent) {
		const t = event.touches[0];
		if (isEdge(t.clientX, t.clientY)) {
			dragging = true;
			dragOffsetX = x - t.clientX;
			dragOffsetY = y - t.clientY;
			resetHintTimer();
			event.preventDefault();
			event.stopPropagation();
		} else {
			clickThrough(t.clientX, t.clientY);
			event.preventDefault();
			event.stopPropagation();
		}
	}

	function moveLens(clientX: number, clientY: number) {
		const margin = 40;
		x = Math.min(Math.max(clientX + dragOffsetX, margin), window.innerWidth - margin);
		y = Math.min(Math.max(clientY + dragOffsetY, margin), window.innerHeight - margin);
	}

	function onPointerMove(event: MouseEvent) {
		if (!dragging) return;
		event.preventDefault();
		moveLens(event.clientX, event.clientY);
	}

	function onTouchMove(event: TouchEvent) {
		if (!dragging) return;
		event.preventDefault();
		moveLens(event.touches[0].clientX, event.touches[0].clientY);
	}

	function onPointerUp() {
		if (dragging) {
			dragging = false;
			resetHintTimer();
		}
	}

	let cursorStyle = $derived(dragging ? 'grabbing' : hoverEdge ? 'grab' : 'default');
</script>

{#if $magnifierEnabled && targetEl}
	{@const rect = targetEl.getBoundingClientRect()}

	<div
		class="lens"
		class:dragging
		bind:this={lensEl}
		style:width={`${lensSize}px`}
		style:height={`${lensSize}px`}
		style:transform={`translate(${x - lensSize / 2}px, ${y - lensSize / 2}px)`}
		style:cursor={cursorStyle}
		onmousedown={onMouseDown}
		onmousemove={onLensHover}
		ontouchstart={onTouchStart}
	>
		<div class="lens-viewport">
			<div
				class="lens-content"
				style:width={`${rect.width}px`}
				style:height={`${rect.height}px`}
				style:transform={`
					translate(
						${lensSize / 2 - (x - rect.left) * zoom}px,
						${lensSize / 2 - (y - rect.top) * zoom}px
					)
					scale(${zoom})
				`}
			>
				{@html mirrorHtml}
			</div>
		</div>

		<div class="lens-ring" aria-hidden="true"></div>
		<div class="lens-glare" aria-hidden="true"></div>

		{#if showHint && !dragging}
			<div class="hint-wrapper" transition:fade={{ duration: 300 }}>
				<div class="hint-bubble">drag edge to move</div>
				<div class="hint-arrow"></div>
			</div>
		{/if}
	</div>
{/if}

<style>
	.lens {
		position: fixed;
		top: 0;
		left: 0;
		z-index: 999999;
		border-radius: 50%;
		overflow: visible;
		pointer-events: auto;
		will-change: transform;
		box-shadow:
			0 0 24px 8px rgba(0, 0, 0, 0.12),
			0 0 60px 12px rgba(0, 0, 0, 0.06);
		animation: lens-fade-in 0.2s ease-out;
	}

	.lens.dragging {
		box-shadow:
			0 0 30px 10px rgba(0, 0, 0, 0.18),
			0 0 80px 16px rgba(0, 0, 0, 0.1);
	}

	@keyframes lens-fade-in {
		from { opacity: 0; }
		to { opacity: 1; }
	}

	.lens-viewport {
		position: absolute;
		inset: 0;
		overflow: hidden;
		border-radius: 50%;
		pointer-events: none;
	}

	.lens-content {
		position: absolute;
		top: 0;
		left: 0;
		transform-origin: top left;
		pointer-events: none;
	}

	.lens-content :global(*) {
		pointer-events: none !important;
	}

	.lens-ring {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		pointer-events: none;
		z-index: 5;
		box-shadow:
			inset 0 0 60px 30px rgba(0, 0, 0, 0.06),
			inset 0 0 35px 18px rgba(0, 0, 0, 0.05),
			inset 0 0 16px 8px rgba(0, 0, 0, 0.04),
			inset 0 0 6px 2px rgba(0, 0, 0, 0.03);
	}

	.lens-glare {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		pointer-events: none;
		z-index: 6;
		background: radial-gradient(
			ellipse 40% 30% at 30% 25%,
			rgba(255, 255, 255, 0.2) 0%,
			transparent 100%
		);
	}

	.hint-wrapper {
		position: absolute;
		top: -58px;
		left: 50%;
		transform: translateX(-50%);
		z-index: 20;
		pointer-events: none;
		display: flex;
		flex-direction: column;
		align-items: center;
		animation: hint-float 2s ease-in-out infinite;
	}

	@keyframes hint-float {
		0%, 100% { transform: translateX(-50%) translateY(0); }
		50% { transform: translateX(-50%) translateY(-4px); }
	}

	.hint-bubble {
		padding: 0.4rem 0.9rem;
		border-radius: 12px;
		background: linear-gradient(135deg, rgba(60, 60, 60, 0.92), rgba(30, 30, 30, 0.92));
		backdrop-filter: blur(8px);
		-webkit-backdrop-filter: blur(8px);
		color: white;
		font-size: 0.78rem;
		font-weight: 500;
		letter-spacing: 0.02em;
		white-space: nowrap;
		box-shadow:
			0 4px 16px rgba(0, 0, 0, 0.25),
			0 0 0 1px rgba(255, 255, 255, 0.08) inset;
	}

	.hint-arrow {
		width: 0;
		height: 0;
		border-left: 8px solid transparent;
		border-right: 8px solid transparent;
		border-top: 8px solid rgba(30, 30, 30, 0.92);
	}
</style>
