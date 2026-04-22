import type {
    Employee,
    MenuItem,
    MenuItemContent,
    Inventory,
    Customer,
    CartItem,
    Order,
    RestockOrder,
    ProductSalesData,
    DailySalesSummary,
    PaymentMethodSummary,
    ZReportResult,
    InventoryUsageData
} from './types';
import { getToken, setSession, clearSession } from '$lib/stores/auth.svelte';

const BASE = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`${BASE}${path}`, {
        headers,
        ...options
    });
    if (!res.ok) throw new Error(`API error: ${res.status} ${res.statusText}`);
    return res.json() as Promise<T>;
}

// Auth
export async function authenticate(
    email: string,
    password: string
): Promise<Employee | null> {
    const result = await request<{ token: string; employee: Employee } | null>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
    });
    if (result) {
        setSession(result.token, result.employee);
        return result.employee;
    }
    return null;
}

export async function logout(): Promise<void> {
    try {
        await request<void>('/auth/logout', { method: 'POST' });
    } finally {
        clearSession();
    }
}

export async function getCurrentUser(): Promise<Employee | null> {
    try {
        return await request<Employee>('/auth/me');
    } catch {
        clearSession();
        return null;
    }
}

// Employees
export async function getAllEmployees(): Promise<Employee[]> {
    return request<Employee[]>('/employees');
}

export async function addEmployee(employee: Omit<Employee, 'employeeId'>): Promise<Employee> {
    return request<Employee>('/employees', {
        method: 'POST',
        body: JSON.stringify(employee)
    });
}

export async function updateEmployee(employee: Employee): Promise<Employee> {
    return request<Employee>(`/employees/${String(employee.employeeId)}`, {
        method: 'PUT',
        body: JSON.stringify(employee)
    });
}

export async function deleteEmployee(id: number): Promise<void> {
    await request<void>(`/employees/${String(id)}`, { method: 'DELETE' });
}

// Menu
export async function getCategories(): Promise<string[]> {
    return request<string[]>('/menu/categories');
}

export async function getAllMenuItems(): Promise<MenuItem[]> {
    return request<MenuItem[]>('/menu/items');
}

export async function getItemsByCategory(category: string): Promise<MenuItem[]> {
    return request<MenuItem[]>(`/menu/items?category=${encodeURIComponent(category)}`);
}

export async function addMenuItem(item: Omit<MenuItem, 'menuItemId'>): Promise<number> {
    return request<number>('/menu/items', {
        method: 'POST',
        body: JSON.stringify(item)
    });
}

export async function updateMenuItem(item: MenuItem): Promise<void> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`${BASE}/menu/items/${String(item.menuItemId)}`, {
        method: 'PUT',
        headers,
        body: JSON.stringify(item)
    });
    if (!res.ok) throw new Error(`API error: ${res.status} ${res.statusText}`);
}

export async function deleteMenuItem(id: number): Promise<void> {
    await request<void>(`/menu/items/${String(id)}`, { method: 'DELETE' });
}

export async function getSizesForItem(menuItemId: number): Promise<string[]> {
    return request<string[]>(`/menu/items/${String(menuItemId)}/sizes`);
}

export async function getAddOns(): Promise<MenuItem[]> {
    return request<MenuItem[]>('/menu/addons');
}

export async function getAllInventoryNames(): Promise<string[]> {
    return request<string[]>('/inventory/names');
}

export async function getOrAddInventoryItem(
    name: string,
    category: string,
    currentQuantity: number,
    unit: string,
    reorderThreshold: number,
    supplierCost: number
): Promise<number> {
    return request<number>('/inventory/get-or-add', {
        method: 'POST',
        body: JSON.stringify({ name, category, currentQuantity, unit, reorderThreshold, supplierCost })
    });
}

export async function addMenuItemContent(content: MenuItemContent): Promise<void> {
    await request<void>('/menu/item-content', {
        method: 'POST',
        body: JSON.stringify(content)
    });
}

// Inventory
export async function getAllInventory(): Promise<Inventory[]> {
    return request<Inventory[]>('/inventory');
}

export async function addInventoryItem(item: Omit<Inventory, 'inventoryId'>): Promise<void> {
    await request<void>('/inventory', {
        method: 'POST',
        body: JSON.stringify(item)
    });
}

export async function updateStock(inventoryId: number, newQuantity: number): Promise<void> {
    await request<void>(`/inventory/${String(inventoryId)}/stock`, {
        method: 'PUT',
        body: JSON.stringify({ quantity: newQuantity })
    });
}

export async function deleteInventoryItem(id: number): Promise<void> {
    await request<void>(`/inventory/${String(id)}`, { method: 'DELETE' });
}

// Customers
export async function findCustomerByPhone(phone: string): Promise<Customer | null> {
    const res = await fetch(`${BASE}/customers/lookup?phone=${encodeURIComponent(phone)}`, {
        headers: {
            'Content-Type': 'application/json',
            ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {})
        }
    });
    if (res.status === 404) return null;
    if (!res.ok) throw new Error(`API error: ${res.status} ${res.statusText}`);
    return res.json() as Promise<Customer>;
}

// Customers
export async function customerCheckin(email: string): Promise<Customer> {
    return request<Customer>('/customers/checkin', {
        method: 'POST',
        body: JSON.stringify({ email })
    });
}

// Orders
export async function submitOrder(
    employeeId: number | null,
    customerId: number | null,
    paymentMethod: string,
    tipAmount: number,
    cart: CartItem[],
    redeemedIndices: number[] = []
): Promise<Order | null> {
    return request<Order | null>('/orders', {
        method: 'POST',
        body: JSON.stringify({ employeeId, customerId, paymentMethod, tipAmount, cart, redeemedIndices })
    });
}

// Reports
export async function getProductSales(
    startDate: string,
    endDate: string
): Promise<ProductSalesData[]> {
    return request<ProductSalesData[]>(
        `/reports/product-sales?start=${encodeURIComponent(startDate)}&end=${encodeURIComponent(endDate)}`
    );
}

export async function getDailySalesSummary(
    startDate: string,
    endDate: string
): Promise<DailySalesSummary[]> {
    return request<DailySalesSummary[]>(
        `/reports/daily-summary?start=${encodeURIComponent(startDate)}&end=${encodeURIComponent(endDate)}`
    );
}

export async function getHourlyPaymentSummary(hour: number): Promise<PaymentMethodSummary[]> {
    return request<PaymentMethodSummary[]>(`/reports/x-report?hour=${String(hour)}`);
}

export async function hasZRunToday(): Promise<boolean> {
    return request<boolean>('/reports/z-report/status');
}

export async function runZReport(signedBy: string): Promise<ZReportResult> {
    return request<ZReportResult>('/reports/z-report', {
        method: 'POST',
        body: JSON.stringify({ signedBy })
    });
}

export async function getInventoryUsage(
    startDate: string,
    endDate: string
): Promise<InventoryUsageData[]> {
    return request<InventoryUsageData[]>(
        `/reports/inventory-usage?start=${encodeURIComponent(startDate)}&end=${encodeURIComponent(endDate)}`
    );
}

export async function createRestockOrder(
    employeeId: number,
    inventoryId: number,
    quantity: number
): Promise<RestockOrder | null> {
    return request<RestockOrder | null>('/inventory/restock', {
        method: 'POST',
        body: JSON.stringify({ employeeId, inventoryId, quantity })
    });
}

// Display

export async function getKitchenOrders(): Promise<any[]> {
    return request<any[]>('/display/kitchen');
}

export async function getPickupOrders(): Promise<any[]> {
    return request<any[]>('/display/pickup');
}

export async function getDisplayMenu(): Promise<MenuItem[]> {
    return request<any[]>('/display/menu');
}