import { writable } from 'svelte/store';

export const magnifierEnabled = writable(false);
export const magnifierZoom = writable(1.8);