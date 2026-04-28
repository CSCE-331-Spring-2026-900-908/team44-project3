<script lang="ts">
    import type { ProductSalesData, PaymentMethodSummary, ZReportResult } from '$lib/types';
    import {
        getProductSales,
        getHourlyPaymentSummary,
        hasZRunToday,
        runZReport
    } from '$lib/api';
    import { formatCurrency, firstOfMonthISO, todayISO } from '$lib/utils';
    import { getDisplayName } from '$lib/stores/auth.svelte';

    type Tab = 'sales' | 'xreport' | 'zreport';

    let activeTab = $state<Tab>('sales');

    // Sales Report state
    let salesStart = $state(firstOfMonthISO());
    let salesEnd = $state(todayISO());
    let salesData = $state<ProductSalesData[]>([]);
    let salesLoading = $state(false);

    // X-Report state
    let xHour = $state(new Date().getHours());
    let xData = $state<PaymentMethodSummary[]>([]);
    let xLoading = $state(false);

    // Z-Report state
    let zResult = $state<ZReportResult | null>(null);
    let zAlreadyRun = $state(false);
    let zLoading = $state(false);
    let zError = $state('');

    async function loadSales() {
        salesLoading = true;
        try {
            salesData = await getProductSales(salesStart, salesEnd);
        } catch {
            salesData = [];
        } finally {
            salesLoading = false;
        }
    }

    async function loadXReport() {
        xLoading = true;
        try {
            xData = await getHourlyPaymentSummary(xHour);
        } catch {
            xData = [];
        } finally {
            xLoading = false;
        }
    }

    async function loadZReport() {
        zLoading = true;
        zError = '';
        try {
            const alreadyRun = await hasZRunToday();
            if (alreadyRun) {
                zAlreadyRun = true;
                return;
            }
            zResult = await runZReport(getDisplayName());
        } catch {
            zError = 'Failed to generate Z-Report.';
        } finally {
            zLoading = false;
        }
    }

    let salesTotals = $derived({
        qty: salesData.reduce((s, d) => s + d.quantity, 0),
        revenue: salesData.reduce((s, d) => s + d.totalPrice, 0)
    });

    let xTotals = $derived({
        orders: xData.reduce((s, d) => s + d.orderCount, 0),
        sales: xData.reduce((s, d) => s + d.totalSales, 0)
    });
</script>

<div class="page-header">
    <h1 class="page-title">Reports</h1>
</div>

<div class="tabs">
    <button class="tab" class:active={activeTab === 'sales'} onclick={() => (activeTab = 'sales')}>
        Sales Report
    </button>
    <button
        class="tab"
        class:active={activeTab === 'xreport'}
        onclick={() => (activeTab = 'xreport')}
    >
        X-Report
    </button>
    <button
        class="tab"
        class:active={activeTab === 'zreport'}
        onclick={() => (activeTab = 'zreport')}
    >
        Z-Report
    </button>
</div>

{#if activeTab === 'sales'}
    <div class="toolbar">
        <div class="form-group">
            <label for="sales-start">From</label>
            <input id="sales-start" type="date" bind:value={salesStart} />
        </div>
        <div class="form-group">
            <label for="sales-end">To</label>
            <input id="sales-end" type="date" bind:value={salesEnd} />
        </div>
        <button class="btn-primary" onclick={loadSales} disabled={salesLoading}>
            {salesLoading ? 'Loading...' : 'Generate'}
        </button>
    </div>

    {#if salesData.length > 0}
        <table>
            <thead>
                <tr>
                    <th>Product</th>
                    <th>Quantity Sold</th>
                    <th>Total Revenue</th>
                </tr>
            </thead>
            <tbody>
                {#each salesData as row, i (i)}
                    <tr>
                        <td>{row.productName}</td>
                        <td>{row.quantity}</td>
                        <td>{formatCurrency(row.totalPrice)}</td>
                    </tr>
                {/each}
            </tbody>
            <tfoot>
                <tr>
                    <th>Totals</th>
                    <th>{salesTotals.qty}</th>
                    <th>{formatCurrency(salesTotals.revenue)}</th>
                </tr>
            </tfoot>
        </table>
    {:else if !salesLoading}
        <p class="no-data">Select a date range and click Generate.</p>
    {/if}

{:else if activeTab === 'xreport'}
    <div class="toolbar">
        <div class="form-group">
            <label for="x-hour">Hour (0-23)</label>
            <input id="x-hour" type="number" min="0" max="23" bind:value={xHour} />
        </div>
        <button class="btn-primary" onclick={loadXReport} disabled={xLoading}>
            {xLoading ? 'Loading...' : 'Generate'}
        </button>
    </div>

    {#if xData.length > 0}
        <table>
            <thead>
                <tr>
                    <th>Payment Method</th>
                    <th>Orders</th>
                    <th>Total Sales</th>
                </tr>
            </thead>
            <tbody>
                {#each xData as row (row.paymentMethod)}
                    <tr>
                        <td>{row.paymentMethod}</td>
                        <td>{row.orderCount}</td>
                        <td>{formatCurrency(row.totalSales)}</td>
                    </tr>
                {/each}
            </tbody>
            <tfoot>
                <tr>
                    <th>Totals</th>
                    <th>{xTotals.orders}</th>
                    <th>{formatCurrency(xTotals.sales)}</th>
                </tr>
            </tfoot>
        </table>
    {:else if !xLoading}
        <p class="no-data">Enter an hour and click Generate.</p>
    {/if}

{:else}
    <div class="z-section">
        {#if zAlreadyRun}
            <div class="card warning-card">
                <p>Z-Report has already been run today.</p>
            </div>
        {:else if zResult}
            <div class="card z-result">
                <h3>End-of-Day Summary</h3>
                <div class="z-totals">
                    <div class="z-stat">
                        <span class="z-label">Gross Sales</span>
                        <span class="z-value">{formatCurrency(zResult.totals.grossSales)}</span>
                    </div>
                    <div class="z-stat">
                        <span class="z-label">Tips</span>
                        <span class="z-value">{formatCurrency(zResult.totals.tips)}</span>
                    </div>
                </div>

                {#if zResult.paymentBreakdown.length > 0}
                    <h4>Payment Breakdown</h4>
                    <table>
                        <thead>
                            <tr>
                                <th>Method</th>
                                <th>Orders</th>
                                <th>Sales</th>
                            </tr>
                        </thead>
                        <tbody>
                            {#each zResult.paymentBreakdown as row (row.paymentMethod)}
                                <tr>
                                    <td>{row.paymentMethod}</td>
                                    <td>{row.orderCount}</td>
                                    <td>{formatCurrency(row.totalSales)}</td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                {/if}

                {#if zResult.signedEmployeeIds.length > 0}
                    <p class="signed-by">
                        Signed by employee IDs: {zResult.signedEmployeeIds.join(', ')}
                    </p>
                {/if}
            </div>
        {:else}
            <div class="card z-prompt">
                <p>
                    Running the Z-Report will close out today's totals. This can only be done once
                    per day.
                </p>
                <button class="btn-primary btn-lg" onclick={loadZReport} disabled={zLoading}>
                    {zLoading ? 'Running...' : 'Run Z-Report'}
                </button>
            </div>
        {/if}

        {#if zError}
            <p class="error-text">{zError}</p>
        {/if}
    </div>
{/if}

<style>
    .tabs {
        display: flex;
        gap: 0;
        margin-bottom: 1.5rem;
        border-bottom: 2px solid var(--color-border);
    }

    .tab {
        padding: 0.6rem 1.25rem;
        background: transparent;
        border-radius: 0;
        font-weight: 500;
        color: var(--color-text-muted);
        border-bottom: 2px solid transparent;
        margin-bottom: -2px;
    }

    .tab:hover {
        color: var(--color-text);
    }

    .tab.active {
        color: var(--color-primary);
        border-bottom-color: var(--color-primary);
    }

    .toolbar {
        margin-bottom: 1.25rem;
    }

    tfoot tr {
        background: #f1f5f9;
    }

    tfoot th {
        background: transparent;
        font-size: 0.875rem;
    }

    .no-data {
        text-align: center;
        color: var(--color-text-muted);
        padding: 2rem;
    }

    .z-section {
        max-width: 600px;
    }

    .warning-card {
        background: #fef3c7;
        border: 1px solid #f59e0b;
        color: #92400e;
    }

    .z-result h3 {
        font-size: 1.125rem;
        margin-bottom: 1rem;
    }

    .z-result h4 {
        font-size: 0.875rem;
        margin: 1rem 0 0.5rem;
    }

    .z-totals {
        display: flex;
        gap: 2rem;
        margin-bottom: 1rem;
    }

    .z-stat {
        display: flex;
        flex-direction: column;
    }

    .z-label {
        font-size: 0.75rem;
        text-transform: uppercase;
        color: var(--color-text-muted);
    }

    .z-value {
        font-size: 1.5rem;
        font-weight: 700;
    }

    .signed-by {
        margin-top: 1rem;
        font-size: 0.8rem;
        color: var(--color-text-muted);
    }

    .z-prompt {
        text-align: center;
    }

    .z-prompt p {
        margin-bottom: 1rem;
        color: var(--color-text-muted);
    }
</style>
