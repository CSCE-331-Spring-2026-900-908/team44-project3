<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { magnifierEnabled } from '$lib/stores/magnifier';

	let { targetSelector = '#magnifier-root' } = $props();

	let lensSize = $state(260);
	let zoom = $state(2);

	let x = $state(350);
	let y = $state(250);
	let dragging = $state(false);

	let targetEl = $state<HTMLElement | null>(null);
	let mirrorHtml = $state('');
	let observer: MutationObserver | null = null;

	const edgeSize = 26;
	const visibleMargin = 36;

	function refreshMirror() {
		if (!targetEl) return;
		mirrorHtml = targetEl.outerHTML;
	}

	onMount(async () => {
		await tick();

		targetEl = document.querySelector(targetSelector) as HTMLElement | null;

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

		document.addEventListener('mousemove', mouseMove, true);
		document.addEventListener('mouseup', mouseUp, true);

		return () => {
			observer?.disconnect();
			document.removeEventListener('mousemove', mouseMove, true);
			document.removeEventListener('mouseup', mouseUp, true);
		};
	});

	function clamp(value: number, min: number, max: number) {
		return Math.min(Math.max(value, min), max);
	}

	function moveLens(clientX: number, clientY: number) {
		x = clamp(clientX, visibleMargin, window.innerWidth - visibleMargin);
		y = clamp(clientY, visibleMargin, window.innerHeight - visibleMargin);
	}

	function mouseDown(event: MouseEvent) {
		event.preventDefault();
		event.stopPropagation();
		dragging = true;
		moveLens(event.clientX, event.clientY);
	}

	function mouseMove(event: MouseEvent) {
		if (!dragging) return;
		event.preventDefault();
		moveLens(event.clientX, event.clientY);
	}

	function mouseUp() {
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

		<div class="reticle" aria-hidden="true"></div>

		<button
			type="button"
			class="edge edge-top"
			onmousedown={mouseDown}
			aria-label="Drag magnifier from top edge"
		></button>

		<button
			type="button"
			class="edge edge-bottom"
			onmousedown={mouseDown}
			aria-label="Drag magnifier from bottom edge"
		></button>

		<button
			type="button"
			class="edge edge-left"
			onmousedown={mouseDown}
			aria-label="Drag magnifier from left edge"
		></button>

		<button
			type="button"
			class="edge edge-right"
			onmousedown={mouseDown}
			aria-label="Drag magnifier from right edge"
		></button>

		<div class="drag-label" aria-hidden="true">drag edge</div>
	</div>
{/if}

<style>
	.lens {
		position: fixed;
		z-index: 999999;
		border-radius: 12px;
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

	.reticle {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 14px;
		height: 14px;
		transform: translate(-50%, -50%);
		border: 1.5px solid rgba(0, 0, 0, 0.8);
		border-radius: 50%;
		pointer-events: none;
		z-index: 10;
	}

	.reticle::before,
	.reticle::after {
		content: '';
		position: absolute;
		background: rgba(0, 0, 0, 0.8);
	}

	.reticle::before {
		left: 50%;
		top: -5px;
		width: 1px;
		height: 22px;
		transform: translateX(-50%);
	}

	.reticle::after {
		top: 50%;
		left: -5px;
		width: 22px;
		height: 1px;
		transform: translateY(-50%);
	}

	.edge {
		position: absolute;
		z-index: 20;
		border: none;
		padding: 0;
		margin: 0;
		background: transparent;
		cursor: grab;
		pointer-events: auto;
	}

	.edge:active {
		cursor: grabbing;
	}

	.edge-top {
		top: 0;
		left: 0;
		right: 0;
		height: 26px;
	}

	.edge-bottom {
		bottom: 0;
		left: 0;
		right: 0;
		height: 26px;
	}

	.edge-left {
		left: 0;
		top: 26px;
		bottom: 26px;
		width: 26px;
	}

	.edge-right {
		right: 0;
		top: 26px;
		bottom: 26px;
		width: 26px;
	}

	.drag-label {
		position: absolute;
		right: 8px;
		top: 8px;
		z-index: 21;
		padding: 0.2rem 0.45rem;
		border-radius: 999px;
		background: rgba(0, 0, 0, 0.75);
		color: white;
		font-size: 0.65rem;
		font-weight: 800;
		pointer-events: none;
	}
</style>