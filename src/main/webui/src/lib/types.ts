export interface Employee {
    employeeId: number;
    firstName: string;
    lastName: string;
    role: string;
    startDate: string;
    email: string;
    passwordHash: string;
    isActive: boolean;
}

export interface MenuItem {
    menuItemId: number;
    name: string;
    category: string;
    size: string;
    basePrice: number;
    isAvailable: boolean;
    isHot: boolean;
    hasImage: boolean;
}

export interface MenuItemGroup {
    name: string;
    category: string;
    isHot: boolean;
    isAvailable: boolean;
    hasImage: boolean;
    variants: MenuItem[];
    minPrice: number;
    maxPrice: number;
}

export interface MenuItemContent {
    menuItemId: number;
    inventoryId: number;
    quantity: number;
}

export interface MenuItemContentWithName {
    menuItemId: number;
    inventoryId: number;
    quantity: number;
    itemName: string;
}

export interface Inventory {
    inventoryId: number;
    itemName: string;
    category: string;
    currentQuantity: number;
    unit: string;
    reorderThreshold: number;
    supplierCost: number;
    lastRestocked: string | null;
}

export interface Customer {
    customerId: number;
    firstName: string;
    lastName: string;
    phone: string;
    email: string;
    rewardPoints: number;
}

export interface CartItem {
    item: MenuItem;
    size: string;
    sweetness: string;
    iceLevel: string;
    addOns: MenuItem[];
    totalPrice: number;
    quantity: number;
}

export interface Order {
    orderId: number;
    employeeId: number | null;
    customerId: number | null;
    paymentMethod: string;
    tipAmount: number;
    totalAmount: number;
    orderDate: string;
    pointsEarned: number;
}

export interface RestockOrder {
    restockOrderId: number;
    employeeId: number;
    inventoryId: number;
    quantity: number;
    orderDate: string;
    status: string;
}

export interface ProductSalesData {
    saleDate: string | null;
    productName: string;
    quantity: number;
    totalPrice: number;
}

export interface DailySalesSummary {
    saleDate: string;
    totalSales: number;
}

export interface PaymentMethodSummary {
    paymentMethod: string;
    orderCount: number;
    totalSales: number;
}

export interface ZReportResult {
    totals: {
        grossSales: number;
        tips: number;
    };
    paymentBreakdown: PaymentMethodSummary[];
    signedEmployeeIds: number[];
}

export interface InventoryUsageData {
    usageDate: string;
    quantityUsed: number;
}

export type SweetnessLevel = '0%' | '25%' | '50%' | '75%' | '100%';
export type IceLevel = 'Hot' | 'No Ice' | 'Less Ice' | 'Regular Ice' | 'Extra Ice';
