import type { Employee } from '$lib/types';

let currentEmployee = $state<Employee | null>(null);
let sessionToken = $state<string | null>(null);
let customerMode = $state(false);

export function getEmployee(): Employee | null {
    return currentEmployee;
}

export function getToken(): string | null {
    return sessionToken;
}

export function setSession(token: string, employee: Employee): void {
    sessionToken = token;
    currentEmployee = employee;
    sessionStorage.setItem('auth_token', token);
    sessionStorage.setItem('auth_employee', JSON.stringify(employee));
}

export function clearSession(): void {
    sessionToken = null;
    currentEmployee = null;
    sessionStorage.removeItem('auth_token');
    sessionStorage.removeItem('auth_employee');
}

export function restoreSession(): { token: string; employee: Employee } | null {
    const token = sessionStorage.getItem('auth_token');
    const empJson = sessionStorage.getItem('auth_employee');
    if (!token || !empJson) return null;

    try {
        const employee = JSON.parse(empJson) as Employee;
        sessionToken = token;
        currentEmployee = employee;
        return { token, employee };
    } catch {
        clearSession();
        return null;
    }
}

/** @deprecated Use setSession instead for new login flows */
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

export function isCustomerMode(): boolean {
    return customerMode;
}

export function setCustomerMode(val: boolean): void {
    customerMode = val;
}
