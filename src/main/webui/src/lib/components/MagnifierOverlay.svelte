<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { magnifierEnabled } from '$lib/stores/magnifier';

	let { targetSelector = '#magnifier-root' } = $props();

	let lensSize = 260;
	let zoom = 2;

	let x = 350;
	let y = 250;
	let dragging = false;

	let targetEl: HTMLElement | null = null;
	let mirrorHtml = '';
	let observer: MutationObserver | null = null;

	function refreshMirror() {
		if (!targetEl) return;
		mirrorHtml = targetEl.outerHTML;
	}

	onMount(async () => {
		await tick();

		targetEl = document.querySelector(targetSelector);

		if (targetEl) {
			refreshMirror();

			observer = new MutationObserver(() => {
				refreshMirror();
			});

			observer.observe(targetEl, {
				childList: true,
				subtree: true,
				attributes: true,
				characterData: true
			});
		}

		window.addEventListener('pointermove', pointerMove);
		window.addEventListener('pointerup', pointerUp);
		window.addEventListener('pointercancel', pointerUp);

		return () => {
			observer?.disconnect();
			window.removeEventListener('pointermove', pointerMove);
			window.removeEventListener('pointerup', pointerUp);
			window.removeEventListener('pointercancel', pointerUp);
		};
	});

	function clamp(value: number, min: number, max: number) {
		return Math.min(Math.max(value, min), max);
	}

	function moveLens(clientX: number, clientY: number) {
		const half = lensSize / 2;
		x = clamp(clientX, half, window.innerWidth - half);
		y = clamp(clientY, half, window.innerHeight - half);
	}

	function pointerDown(event: PointerEvent) {
		event.preventDefault();
		event.stopPropagation();
		dragging = true;
		moveLens(event.clientX, event.clientY);
	}

	function pointerMove(event: PointerEvent) {
		if (!dragging) return;
		moveLens(event.clientX, event.clientY);
	}

	function pointerUp() {
		dragging = false;
	}
</script>

{#if $magnifierEnabled && targetEl}
	{@const rect = targetEl.getBoundingClientRect()}

	<div
		class="lens"
		style:width={`${lensSize}px`}
		style:height={`${lensSize}px`}
		style:left={`${x - lensSize / 2}px`}
		style:top={`${y - lensSize / 2}px`}
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

		<button class="edge edge-top" type="button" aria-label="Drag magnifier" onpointerdown={pointerDown}></button>
		<button class="edge edge-bottom" type="button" aria-label="Drag magnifier" onpointerdown={pointerDown}></button>
		<button class="edge edge-left" type="button" aria-label="Drag magnifier" onpointerdown={pointerDown}></button>
		<button class="edge edge-right" type="button" aria-label="Drag magnifier" onpointerdown={pointerDown}></button>

		<div class="lens-label">Drag edge</div>
	</div>
{/if}

<style>
	.lens {
		position: fixed;
		z-index: 999999;
		border-radius: 9999px;
		overflow: hidden;
		border: 3px solid black;
		box-shadow: 0 0 0 3px white, 0 10px 30px rgba(0, 0, 0, 0.35);
		background: white;
		pointer-events: none;
	}

	.lens-viewport {
		position: absolute;
		inset: 0;
		overflow: hidden;
		border-radius: inherit;
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

	.edge {
		position: absolute;
		background: transparent;
		border: none;
		padding: 0;
		margin: 0;
		cursor: grab;
		pointer-events: auto;
		z-index: 5;
	}

	.edge:active {
		cursor: grabbing;
	}

	.edge-top {
		top: 0;
		left: 55px;
		right: 55px;
		height: 28px;
	}

	.edge-bottom {
		bottom: 0;
		left: 55px;
		right: 55px;
		height: 28px;
	}

	.edge-left {
		left: 0;
		top: 55px;
		bottom: 55px;
		width: 28px;
	}

	.edge-right {
		right: 0;
		top: 55px;
		bottom: 55px;
		width: 28px;
	}

	.lens-label {
		position: absolute;
		right: 12px;
		bottom: 12px;
		background: rgba(0, 0, 0, 0.7);
		color: white;
		font-size: 0.75rem;
		font-weight: 700;
		padding: 0.25rem 0.5rem;
		border-radius: 999px;
		pointer-events: none;
		z-index: 6;
	}
</style>