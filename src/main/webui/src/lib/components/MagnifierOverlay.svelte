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
		const half = lensSize / 2;
		x = clamp(clientX, half, window.innerWidth - half);
		y = clamp(clientY, half, window.innerHeight - half);
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
	</div>

	<button
		type="button"
		class="drag-handle"
		style:left={`${x - 45}px`}
		style:top={`${y + lensSize / 2 - 22}px`}
		onmousedown={mouseDown}
	>
		Drag
	</button>
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

	.drag-handle {
		position: fixed;
		z-index: 1000000;
		width: 90px;
		height: 44px;
		border-radius: 999px;
		border: 3px solid white;
		background: rgba(0, 0, 0, 0.85);
		color: white;
		font-weight: 800;
		cursor: grab;
		user-select: none;
	}

	.drag-handle:active {
		cursor: grabbing;
	}
</style>