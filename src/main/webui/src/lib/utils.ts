export function toTitleCase(s: string): string {
    return s.replace(/(^|[\s-])(\w)/g, (_, pre, c) => pre + c.toUpperCase());
}

export function formatCurrency(amount: number): string {
    return `$${amount.toFixed(2)}`;
}

export const TAX_RATE = 0.0825;

export function todayISO(): string {
    return new Date().toISOString().split('T')[0] ?? '';
}

export function firstOfMonthISO(): string {
    const d = new Date();
    d.setDate(1);
    return d.toISOString().split('T')[0] ?? '';
}

const SIZE_ORDER: Record<string, number> = {
    small: 0,
    regular: 1,
    medium: 2,
    large: 3
};

export function sizeRank(size: string | null | undefined): number {
    return SIZE_ORDER[(size ?? '').toLowerCase()] ?? 99;
}

export function compareSizes(a: string | null | undefined, b: string | null | undefined): number {
    return sizeRank(a) - sizeRank(b);
}
