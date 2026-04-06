export function toTitleCase(s: string): string {
    return s.replace(/\b\w/g, (c) => c.toUpperCase());
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
