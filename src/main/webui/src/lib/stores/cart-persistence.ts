import type { CartItem } from '$lib/types';

export type CartScope = 'order' | 'cashier';

interface PersistedCart {
    v: 1;
    cart: CartItem[];
    redeemedCounts: Array<[number, number]>;
}

function key(scope: CartScope): string {
    return `kioskCart:${scope}:v1`;
}

function isAvailable(): boolean {
    return typeof sessionStorage !== 'undefined';
}

export function loadCartState(
    scope: CartScope
): { cart: CartItem[]; redeemedCounts: Map<number, number> } | null {
    if (!isAvailable()) return null;
    const raw = sessionStorage.getItem(key(scope));
    if (!raw) return null;
    try {
        const parsed = JSON.parse(raw) as PersistedCart;
        if (parsed?.v !== 1 || !Array.isArray(parsed.cart)) {
            sessionStorage.removeItem(key(scope));
            return null;
        }
        const counts = new Map<number, number>(
            Array.isArray(parsed.redeemedCounts) ? parsed.redeemedCounts : []
        );
        return { cart: parsed.cart, redeemedCounts: counts };
    } catch {
        sessionStorage.removeItem(key(scope));
        return null;
    }
}

export function saveCartState(
    scope: CartScope,
    state: { cart: CartItem[]; redeemedCounts: Map<number, number> }
): void {
    if (!isAvailable()) return;
    if (state.cart.length === 0 && state.redeemedCounts.size === 0) {
        sessionStorage.removeItem(key(scope));
        return;
    }
    const payload: PersistedCart = {
        v: 1,
        cart: state.cart,
        redeemedCounts: Array.from(state.redeemedCounts.entries())
    };
    try {
        sessionStorage.setItem(key(scope), JSON.stringify(payload));
    } catch {
        // Quota or serialization failure - cart still works in memory.
    }
}

export function clearCartState(scope: CartScope): void {
    if (!isAvailable()) return;
    sessionStorage.removeItem(key(scope));
}
