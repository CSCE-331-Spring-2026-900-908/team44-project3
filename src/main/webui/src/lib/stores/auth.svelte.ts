import type { Employee, Customer } from '$lib/types';

let currentEmployee = $state<Employee | null>(null);
let sessionToken = $state<string | null>(null);
let currentCustomer = $state<Customer | null>(null);

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

export function getCustomer(): Customer | null {
    return currentCustomer;
}

export function setCustomer(customer: Customer): void {
    currentCustomer = customer;
    sessionStorage.setItem('auth_customer', JSON.stringify(customer));
}

export function clearCustomer(): void {
    currentCustomer = null;
    sessionStorage.removeItem('auth_customer');
}

export function restoreCustomer(): Customer | null {
    const json = sessionStorage.getItem('auth_customer');
    if (!json) return null;

    try {
        const customer = JSON.parse(json) as Customer;
        currentCustomer = customer;
        return customer;
    } catch {
        clearCustomer();
        return null;
    }
}
