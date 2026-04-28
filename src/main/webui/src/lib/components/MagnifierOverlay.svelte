<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { magnifierEnabled } from '$lib/stores/magnifier';
	import { fade } from 'svelte/transition';

	let { targetSelector = '#magnifier-root', highContrast = false } = $props();

	let lensSize = $state(340);
	let zoom = $state(1.5);

	let x = $state(window.innerWidth / 2);
	let y = $state(window.innerHeight / 3);
	let dragging = $state(false);
	let dragOffsetX = 0;
	let dragOffsetY = 0;

	let targetEl = $state<HTMLElement | null>(null);
	let mirrorHtml = $state('');
	let observer: MutationObserver | null = null;
	let lensEl: HTMLDivElement;

	let showHint = $state(false);
	let hintTimer: ReturnType<typeof setTimeout> | null = null;

	const grabRingOuter = 0;

	let scrollMap: Map<number, { top: number; left: number }> = new Map();

	function refreshMirror() {
		if (!targetEl) return;
		const clone = targetEl.cloneNode(true) as HTMLElement;
		const realInputs = targetEl.querySelectorAll('input, textarea');
		const clonedInputs = clone.querySelectorAll('input, textarea');
		realInputs.forEach((real, i) => {
			const cloned = clonedInputs[i];
			if (!cloned) return;
			const v = (real as HTMLInputElement | HTMLTextAreaElement).value;
			if (cloned.tagName === 'TEXTAREA') {
				cloned.textContent = v;
			} else {
				cloned.setAttribute('value', v);
			}
		});

		const descendants = targetEl.querySelectorAll('*');
		scrollMap = new Map();
		descendants.forEach((el, i) => {
			const e = el as HTMLElement;
			if (e.scrollTop > 0 || e.scrollLeft > 0) {
				scrollMap.set(i, { top: e.scrollTop, left: e.scrollLeft });
			}
		});

		mirrorHtml = clone.innerHTML;
	}

	let scrollRaf: number | null = null;
	function onScroll() {
		if (scrollRaf !== null) return;
		scrollRaf = requestAnimationFrame(() => {
			scrollRaf = null;
			refreshMirror();
		});
	}

	$effect(() => {
		mirrorHtml;
		if (!lensEl) return;
		queueMicrotask(() => {
			const cloneRoot = lensEl.querySelector('.lens-content');
			if (!cloneRoot) return;
			const ds = cloneRoot.querySelectorAll('*');
			scrollMap.forEach((s, i) => {
				const el = ds[i] as HTMLElement | undefined;
				if (el) {
					el.scrollTop = s.top;
					el.scrollLeft = s.left;
				}
			});
		});
	});

	function resetHintTimer() {
		showHint = false;
		if (hintTimer) clearTimeout(hintTimer);
		hintTimer = setTimeout(() => { showHint = true; }, 3000);
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
		document.addEventListener('touchcancel', onPointerUp, true);
		document.addEventListener('input', refreshMirror, true);
		document.addEventListener('change', refreshMirror, true);
		document.addEventListener('focusin', refreshMirror, true);
		document.addEventListener('focusout', refreshMirror, true);
		document.addEventListener('scroll', onScroll, true);

		resetHintTimer();

		return () => {
			observer?.disconnect();
			document.removeEventListener('mousemove', onPointerMove, true);
			document.removeEventListener('mouseup', onPointerUp, true);
			document.removeEventListener('touchmove', onTouchMove, true);
			document.removeEventListener('touchend', onPointerUp, true);
			document.removeEventListener('touchcancel', onPointerUp, true);
			document.removeEventListener('input', refreshMirror, true);
			document.removeEventListener('change', refreshMirror, true);
			document.removeEventListener('focusin', refreshMirror, true);
			document.removeEventListener('focusout', refreshMirror, true);
			document.removeEventListener('scroll', onScroll, true);
			if (scrollRaf !== null) cancelAnimationFrame(scrollRaf);
			if (hintTimer) clearTimeout(hintTimer);
		};
	});

	let didMove = false;
	let downX = 0;
	let downY = 0;
	const MOVE_THRESHOLD = 5;

	function clickThrough(clientX: number, clientY: number) {
		if (!lensEl) return;
		const pageX = x + (clientX - x) / zoom;
		const pageY = y + (clientY - y) / zoom;
		lensEl.style.pointerEvents = 'none';
		const target = document.elementFromPoint(pageX, pageY);
		lensEl.style.pointerEvents = '';
		if (!target) return;

		const opts: MouseEventInit = {
			clientX: pageX,
			clientY: pageY,
			bubbles: true,
			cancelable: true,
			view: window,
			button: 0
		};
		target.dispatchEvent(new MouseEvent('mousedown', opts));
		target.dispatchEvent(new MouseEvent('mouseup', opts));
		target.dispatchEvent(new MouseEvent('click', opts));
		if (target instanceof HTMLElement) {
			setTimeout(() => target.focus(), 0);
		}
	}

	function onMouseDown(event: MouseEvent) {
		dragging = true;
		didMove = false;
		downX = event.clientX;
		downY = event.clientY;
		dragOffsetX = x - event.clientX;
		dragOffsetY = y - event.clientY;
		event.preventDefault();
		event.stopPropagation();
	}

	function onTouchStart(event: TouchEvent) {
		const t = event.touches[0];
		if (!t) return;
		dragging = true;
		didMove = false;
		downX = t.clientX;
		downY = t.clientY;
		dragOffsetX = x - t.clientX;
		dragOffsetY = y - t.clientY;
		event.preventDefault();
		event.stopPropagation();
	}

	function moveLens(clientX: number, clientY: number) {
		const margin = 40;
		x = Math.min(Math.max(clientX + dragOffsetX, margin), window.innerWidth - margin);
		y = Math.min(Math.max(clientY + dragOffsetY, margin), window.innerHeight - margin);
	}

	function onPointerMove(event: MouseEvent) {
		if (!dragging) return;
		const dx = event.clientX - downX;
		const dy = event.clientY - downY;
		if (!didMove && Math.sqrt(dx * dx + dy * dy) > MOVE_THRESHOLD) didMove = true;
		if (didMove) {
			event.preventDefault();
			moveLens(event.clientX, event.clientY);
		}
	}

	function onTouchMove(event: TouchEvent) {
		if (!dragging) return;
		const t = event.touches[0];
		if (!t) return;
		const dx = t.clientX - downX;
		const dy = t.clientY - downY;
		if (!didMove && Math.sqrt(dx * dx + dy * dy) > MOVE_THRESHOLD) didMove = true;
		if (didMove) {
			event.preventDefault();
			moveLens(t.clientX, t.clientY);
		}
	}

	function onPointerUp(event: MouseEvent | TouchEvent) {
		if (!dragging) return;
		dragging = false;

		let cx: number;
		let cy: number;
		if ('changedTouches' in event) {
			const t = event.changedTouches[0];
			if (!t) {
				resetHintTimer();
				return;
			}
			cx = t.clientX;
			cy = t.clientY;
		} else {
			cx = event.clientX;
			cy = event.clientY;
		}

		if (!didMove) {
			clickThrough(cx, cy);
		} else {
			resetHintTimer();
		}
	}

	let cursorStyle = $derived(dragging && didMove ? 'grabbing' : 'pointer');
</script>

{#if $magnifierEnabled && targetEl}
	{@const rect = targetEl.getBoundingClientRect()}

	<div
		class="lens"
		role="application"
		aria-label="Screen magnifier"
		class:dragging
		class:high-contrast={highContrast}
		bind:this={lensEl}
		style:width={`${lensSize + grabRingOuter * 2}px`}
		style:height={`${lensSize + grabRingOuter * 2}px`}
		style:transform={`translate3d(${x - lensSize / 2 - grabRingOuter}px, ${y - lensSize / 2 - grabRingOuter}px, 0)`}
		style:cursor={cursorStyle}
		style:--outer-pad={`${grabRingOuter}px`}
		onmousedown={onMouseDown}
		ontouchstart={onTouchStart}
	>
		<svg class="lens-svg" width={lensSize} height={lensSize} viewBox="0 0 {lensSize} {lensSize}">
			<defs>
				<clipPath id="lens-svg-clip">
					<circle cx={lensSize / 2} cy={lensSize / 2} r={lensSize / 2} />
				</clipPath>
			</defs>
			<foreignObject
				x="0"
				y="0"
				width={lensSize}
				height={lensSize}
				clip-path="url(#lens-svg-clip)"
			>
				<div
					xmlns="http://www.w3.org/1999/xhtml"
					class="lens-content"
					style="width:{rect.width}px;height:{rect.height}px;transform:translate({lensSize / 2 - (x - rect.left) * zoom}px,{lensSize / 2 - (y - rect.top) * zoom}px) scale({zoom});transform-origin:top left;position:absolute;top:0;left:0;pointer-events:none;"
				>
					{@html mirrorHtml}
				</div>
			</foreignObject>
		</svg>

		<div class="lens-ring" aria-hidden="true"></div>
		<div class="lens-glare" aria-hidden="true"></div>

		{#if showHint && !dragging}
			<div class="pulse-ring" aria-hidden="true" in:fade={{ duration: 800 }} out:fade={{ duration: 300 }}></div>
		{/if}
	</div>

	{#if showHint && !dragging}
		<div
			class="hint-wrapper"
			style:left="{x}px"
			style:top="{y - lensSize / 2 - 58}px"
			in:fade={{ duration: 800 }}
			out:fade={{ duration: 300 }}
		>
			<div class="hint-bubble">Drag Edge to Move</div>
			<div class="hint-arrow"></div>
		</div>
	{/if}
{/if}

<style>
	.lens {
		position: fixed;
		top: 0;
		left: 0;
		z-index: 999999;
		border-radius: 50%;
		overflow: hidden;
		pointer-events: auto;
		touch-action: none;
		will-change: transform;
		animation: lens-fade-in 0.2s ease-out;
		box-shadow:
			0 4px 20px rgba(0, 0, 0, 0.3),
			0 0 50px 6px rgba(0, 0, 0, 0.12);
	}

	.lens.high-contrast {
		box-shadow:
			0 0 0 3px white,
			0 4px 20px rgba(255, 255, 255, 0.5),
			0 0 50px 6px rgba(255, 255, 255, 0.4);
	}

	.lens.high-contrast :global(.lens-ring) {
		border-color: white !important;
	}

	@keyframes lens-fade-in {
		from { opacity: 0; }
		to { opacity: 1; }
	}

	.lens-svg {
		position: absolute;
		inset: var(--outer-pad);
		pointer-events: none;
		border-radius: 50%;
		box-shadow:
			0 0 0 2px rgba(0, 0, 0, 0.15),
			0 4px 16px rgba(0, 0, 0, 0.25),
			0 0 40px 4px rgba(0, 0, 0, 0.12);
		transition: box-shadow 0.15s;
	}

	.lens.dragging .lens-svg {
		box-shadow:
			0 0 0 2px rgba(0, 0, 0, 0.2),
			0 6px 24px rgba(0, 0, 0, 0.3),
			0 0 50px 8px rgba(0, 0, 0, 0.15);
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

	.lens-content :global(img),
	.lens-content :global(video),
	.lens-content :global(canvas) {
		will-change: auto !important;
		transform: none !important;
		-webkit-transform: none !important;
		backface-visibility: visible !important;
		-webkit-backface-visibility: visible !important;
	}

	.lens-content :global(.idle-overlay),
	.lens-content :global(.zoom-controls) {
		display: none !important;
	}

	.lens-content :global(.chatbot-root),
	.lens-content :global(.backdrop) {
		position: absolute !important;
	}

	.lens-content :global(.backdrop) {
		background: transparent !important;
	}

	.lens-ring {
		position: absolute;
		inset: var(--outer-pad);
		border-radius: 50%;
		pointer-events: none;
		z-index: 5;
		border: 2px solid rgba(0, 0, 0, 0.12);
		box-shadow:
			0 4px 20px rgba(0, 0, 0, 0.3),
			0 0 40px 4px rgba(0, 0, 0, 0.1),
			inset 0 0 8px 2px rgba(0, 0, 0, 0.04);
	}

	.lens-glare {
		position: absolute;
		inset: var(--outer-pad);
		border-radius: 50%;
		pointer-events: none;
		z-index: 6;
		background: radial-gradient(
			ellipse 40% 30% at 30% 25%,
			rgba(255, 255, 255, 0.2) 0%,
			transparent 100%
		);
	}

	.pulse-ring {
		position: absolute;
		inset: var(--outer-pad);
		border-radius: 50%;
		pointer-events: none;
		z-index: 8;
		border: 2px solid transparent;
		background:
			conic-gradient(
				from var(--pulse-angle, 0deg),
				transparent 0deg,
				rgba(230, 140, 50, 0.6) 30deg,
				rgba(230, 140, 50, 0.9) 60deg,
				rgba(230, 140, 50, 0.6) 90deg,
				transparent 130deg,
				transparent 360deg
			)
			border-box;
		-webkit-mask:
			linear-gradient(#000 0 0) content-box,
			linear-gradient(#000 0 0);
		mask:
			linear-gradient(#000 0 0) content-box,
			linear-gradient(#000 0 0);
		-webkit-mask-composite: xor;
		mask-composite: exclude;
		padding: 3px;
		animation: pulse-spin 2.5s linear infinite;
	}

	@keyframes pulse-spin {
		from { --pulse-angle: 0deg; }
		to { --pulse-angle: 360deg; }
	}

	@property --pulse-angle {
		syntax: '<angle>';
		initial-value: 0deg;
		inherits: false;
	}

	.hint-wrapper {
		position: fixed;
		transform: translateX(-50%);
		z-index: 1000000;
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
		padding: 0.5rem 1.1rem;
		border-radius: 14px;
		background: linear-gradient(135deg, rgba(60, 60, 60, 0.92), rgba(30, 30, 30, 0.92));
		backdrop-filter: blur(8px);
		-webkit-backdrop-filter: blur(8px);
		color: white;
		font-size: 0.82rem;
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
