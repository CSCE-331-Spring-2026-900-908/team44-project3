export function toTitleCase(s: string): string {
    return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}

export function formatCurrency(amount: number): string {
    return `$${amount.toFixed(2)}`;
}

export function todayISO(): string {
    return new Date().toISOString().split('T')[0] ?? '';
}

export function firstOfMonthISO(): string {
    const d = new Date();
    d.setDate(1);
    return d.toISOString().split('T')[0] ?? '';
}
