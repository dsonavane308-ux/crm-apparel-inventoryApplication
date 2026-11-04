const API_BASE = 'http://localhost:8080/api/v1';

async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const response = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...options });
  const data = response.status !== 204 ? await response.json().catch(() => ({})) : null;
  if (!response.ok) {
    const error = new Error(data?.message || `HTTP Error ${response.status}`);
    error.status = response.status;
    error.payload = data;
    throw error;
  }
  return { status: response.status, data };
}

export const inventoryApi = {
  getItems: (search = '') => request(`/items?page=0&size=100${search ? `&search=${encodeURIComponent(search)}` : ''}`),
  getItemByBarcode: (code) => request(`/items/barcode/${encodeURIComponent(code)}`),
  createItem: (payload) => request('/items', { method: 'POST', body: JSON.stringify(payload) }),
  getWarehouses: () => request('/warehouses?activeOnly=false'),
  createWarehouse: (payload) => request('/warehouses', { method: 'POST', body: JSON.stringify(payload) }),
  getStockLevels: () => request('/stock?page=0&size=100'),
  getItemStockBreakdown: (itemId) => request(`/stock/items/${itemId}`),
  getLowStockAlerts: () => request('/stock/low-stock-alerts'),
  executeStockOp: (opType, payload) => request(`/stock/operations/${opType}`, { method: 'POST', body: JSON.stringify(payload) }),
  getLedger: () => request('/stock/ledger?page=0&size=50'),
  getPosInvoices: () => request('/pos/invoices'),
  getCustomers: () => request('/pos/customers'),
  posCheckout: (payload) => request('/pos/checkout', { method: 'POST', body: JSON.stringify(payload) }),
  getAnalytics: () => request('/analytics/dashboard'),
};
