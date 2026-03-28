import type { Employee } from '$lib/types';

let currentEmployee = $state<Employee | null>(null);

export function getEmployee(): Employee | null {
    return currentEmployee;
}

export function setEmployee(employee: Employee | null): void {
    currentEmployee = employee;
}

export function isManager(): boolean {
    return currentEmployee?.role === 'manager';
}

export function getEmployeeId(): number {
    return currentEmployee?.employeeId ?? 0;
}

export function getDisplayName(): string {
    if (!currentEmployee) return '';
    const cap = (s: string) => s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
    return `${cap(currentEmployee.firstName)} ${cap(currentEmployee.lastName)}`;
}
