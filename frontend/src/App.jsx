import React, { useState, useEffect } from 'react';
import { inventoryApi } from './services/api';
import {
  ShoppingCart, LayoutDashboard, Package, Warehouse, ArrowRightLeft,
  History, Users, RefreshCw, AlertTriangle, Terminal, Printer, Trash2, Barcode, Plus
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState('pos');
  const [items, setItems] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [stockLevels, setStockLevels] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [ledger, setLedger] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(false);

  // Clothing Catalog Filter
  const [sizeFilter, setSizeFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // POS Billing Counter State
  const [posWarehouseId, setPosWarehouseId] = useState('');
  const [barcodeInput, setBarcodeInput] = useState('');
  const [cart, setCart] = useState([]);
  const [customerName, setCustomerName] = useState('');
  const [customerMobile, setCustomerMobile] = useState('');
  const [discountAmount, setDiscountAmount] = useState(0);
  const [taxPercent, setTaxPercent] = useState(5); // Default 5% GST for apparel
  const [paymentMode, setPaymentMode] = useState('UPI');
  const [lastInvoice, setLastInvoice] = useState(null);

  // API Inspector State
  const [apiLog, setApiLog] = useState({
    method: 'GET',
    endpoint: '/api/v1/pos/invoices',
    status: 200,
    response: { status: 'Offline Apparel POS & Inventory Ready' }
  });

  // New Garment Form State
  const [newItem, setNewItem] = useState({
    sku: '', name: '', brand: '', size: 'L', color: '', barcode: '',
    unitPrice: '', costPrice: '', reorderThreshold: 5
  });

  // New Warehouse Form State
  const [newWarehouse, setNewWarehouse] = useState({ code: '', name: '', address: '' });

  // Stock Op State
  const [opForm, setOpForm] = useState({
    opType: 'receive', warehouseId: '', toWarehouseId: '', itemId: '',
    quantity: 10, quantityDelta: -1, unitCost: 450,
    poReference: 'SUP-BILL-2026', crmReferenceId: 'ALTERATION-HOLD-01',
    reason: 'Display damage', notes: 'New festive stock', wasReserved: false
  });

  const loadAllData = async () => {
    setLoading(true);
    try {
      const [itemsRes, whRes, stockRes, alertsRes, ledgerRes, invRes, custRes] = await Promise.all([
        inventoryApi.getItems(),
        inventoryApi.getWarehouses(),
        inventoryApi.getStockLevels(),
        inventoryApi.getLowStockAlerts(),
        inventoryApi.getLedger(),
        inventoryApi.getPosInvoices(),
        inventoryApi.getCustomers()
      ]);

      const itemList = itemsRes.data?.content || [];
      const whList = whRes.data || [];
      setItems(itemList);
      setWarehouses(whList);
      setStockLevels(stockRes.data?.content || []);
      setAlerts(alertsRes.data || []);
      setLedger(ledgerRes.data?.content || []);
      setInvoices(invRes.data || []);
      setCustomers(custRes.data || []);

      if (whList.length > 0 && !posWarehouseId) setPosWarehouseId(whList[0].id);
      if (whList.length > 0 && !opForm.warehouseId) setOpForm(p => ({ ...p, warehouseId: whList[0].id }));
      if (itemList.length > 0 && !opForm.itemId) setOpForm(p => ({ ...p, itemId: itemList[0].id }));
    } catch (err) {
      setApiLog({
        method: 'GET', endpoint: '/api/v1/*',
        status: err.status || 500, response: err.payload || { error: err.message }
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, []);

  // Helper to get available stock for an item in a warehouse
  const getAvailableStock = (itemId, warehouseId) => {
    const record = stockLevels.find(s => s.itemId === itemId && s.warehouseId === warehouseId);
    return record ? (record.quantityOnHand - record.quantityReserved) : 0;
  };

  // Add item to POS Cart
  const addToCart = (item) => {
    setCart(prev => {
      const existing = prev.find(c => c.itemId === item.id);
      if (existing) {
        return prev.map(c => c.itemId === item.id ? { ...c, quantity: c.quantity + 1 } : c);
      }
      return [...prev, {
        itemId: item.id,
        sku: item.sku,
        name: item.name,
        size: item.size || '-',
        color: item.color || '-',
        unitPrice: Number(item.unitPrice),
        quantity: 1
      }];
    });
  };

  // Barcode scan handler
  const handleBarcodeSubmit = async (e) => {
    e.preventDefault();
    if (!barcodeInput.trim()) return;
    try {
      const res = await inventoryApi.getItemByBarcode(barcodeInput.trim());
      addToCart(res.data);
      setBarcodeInput('');
      setApiLog({ method: 'GET', endpoint: `/api/v1/items/barcode/${barcodeInput}`, status: 200, response: res.data });
    } catch (err) {
      setApiLog({ method: 'GET', endpoint: `/api/v1/items/barcode/${barcodeInput}`, status: 404, response: err.payload });
    }
  };

  // Complete POS Checkout
  const handlePosCheckout = async (e) => {
    e.preventDefault();
    if (cart.length === 0) return;
    const payload = {
      warehouseId: posWarehouseId,
      customerName,
      customerMobile,
      discountAmount: Number(discountAmount) || 0,
      taxPercent: Number(taxPercent) || 0,
      paymentMode,
      items: cart.map(c => ({ itemId: c.itemId, quantity: Number(c.quantity), wasReserved: false }))
    };

    try {
      const res = await inventoryApi.posCheckout(payload);
      setLastInvoice(res.data);
      setCart([]);
      setCustomerName('');
      setCustomerMobile('');
      setDiscountAmount(0);
      setApiLog({ method: 'POST', endpoint: '/api/v1/pos/checkout', status: res.status, response: res.data });
      loadAllData();
    } catch (err) {
      setApiLog({ method: 'POST', endpoint: '/api/v1/pos/checkout', status: err.status || 409, response: err.payload });
    }
  };

  const handleCreateItem = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...newItem,
        barcode: newItem.barcode || newItem.sku,
        unitPrice: parseFloat(newItem.unitPrice),
        costPrice: parseFloat(newItem.costPrice),
        reorderThreshold: parseInt(newItem.reorderThreshold, 10)
      };
      const res = await inventoryApi.createItem(payload);
      setApiLog({ method: 'POST', endpoint: '/api/v1/items', status: res.status, response: res.data });
      setNewItem({ sku: '', name: '', brand: '', size: 'L', color: '', barcode: '', unitPrice: '', costPrice: '', reorderThreshold: 5 });
      loadAllData();
    } catch (err) {
      setApiLog({ method: 'POST', endpoint: '/api/v1/items', status: err.status || 400, response: err.payload });
    }
  };

  const handleCreateWarehouse = async (e) => {
    e.preventDefault();
    try {
      const res = await inventoryApi.createWarehouse(newWarehouse);
      setApiLog({ method: 'POST', endpoint: '/api/v1/warehouses', status: res.status, response: res.data });
      setNewWarehouse({ code: '', name: '', address: '' });
      loadAllData();
    } catch (err) {
      setApiLog({ method: 'POST', endpoint: '/api/v1/warehouses', status: err.status || 400, response: err.payload });
    }
  };

  const handleExecuteOperation = async (e) => {
    e.preventDefault();
    const { opType } = opForm;
    let payload = {};
    if (opType === 'receive') {
      payload = { warehouseId: opForm.warehouseId, itemId: opForm.itemId, quantity: Number(opForm.quantity), unitCost: Number(opForm.unitCost), poReference: opForm.poReference, notes: opForm.notes };
    } else if (opType === 'reserve' || opType === 'release') {
      payload = { warehouseId: opForm.warehouseId, itemId: opForm.itemId, quantity: Number(opForm.quantity), crmReferenceId: opForm.crmReferenceId };
    } else if (opType === 'deduct') {
      payload = { warehouseId: opForm.warehouseId, itemId: opForm.itemId, quantity: Number(opForm.quantity), crmReferenceId: opForm.crmReferenceId, wasReserved: Boolean(opForm.wasReserved) };
    } else if (opType === 'transfer') {
      payload = { fromWarehouseId: opForm.warehouseId, toWarehouseId: opForm.toWarehouseId, itemId: opForm.itemId, quantity: Number(opForm.quantity), reason: opForm.reason };
    } else if (opType === 'adjust') {
      payload = { warehouseId: opForm.warehouseId, itemId: opForm.itemId, quantityDelta: Number(opForm.quantityDelta), reason: opForm.reason };
    }

    try {
      const res = await inventoryApi.executeStockOp(opType, payload);
      setApiLog({ method: 'POST', endpoint: `/api/v1/stock/operations/${opType}`, status: res.status, response: res.data });
      loadAllData();
    } catch (err) {
      setApiLog({ method: 'POST', endpoint: `/api/v1/stock/operations/${opType}`, status: err.status || 409, response: err.payload });
    }
  };

  // Cart Calculations
  const cartSubtotal = cart.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
  const cartTaxable = Math.max(0, cartSubtotal - (Number(discountAmount) || 0));
  const cartTax = (cartTaxable * (Number(taxPercent) || 0)) / 100;
  const cartGrandTotal = cartTaxable + cartTax;

  const filteredItems = items.filter(i => {
    const matchesSize = sizeFilter === 'ALL' || (i.size && i.size.toUpperCase() === sizeFilter);
    const matchesSearch = !searchQuery ||
      i.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.sku?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.color?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.brand?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesSize && matchesSearch;
  });

  const totalOnHand = stockLevels.reduce((acc, s) => acc + (s.quantityOnHand || 0), 0);
  const totalReserved = stockLevels.reduce((acc, s) => acc + (s.quantityReserved || 0), 0);

  return (
    <div className="min-h-screen flex bg-slate-950 text-slate-100">
      {/* SIDEBAR */}
      <aside className="w-64 border-r border-slate-800 bg-slate-900/70 flex flex-col justify-between p-4 print:hidden">
        <div>
          <div className="flex items-center gap-3 px-2 py-3 border-b border-slate-800">
            <div className="p-2 rounded-lg bg-indigo-600 text-white font-bold">POS</div>
            <div>
              <h1 className="font-bold text-sm">Apparel POS & CRM</h1>
              <p className="text-xs text-emerald-400">100% Offline Localhost</p>
            </div>
          </div>

          <nav className="mt-5 space-y-1">
            {[
              { id: 'pos', label: 'POS Billing Counter', icon: ShoppingCart },
              { id: 'items', label: 'Garment Catalog (Size/Color)', icon: Package },
              { id: 'operations', label: 'Godown & Hold/Alteration', icon: ArrowRightLeft },
              { id: 'customers', label: 'Customer CRM & Bills', icon: Users },
              { id: 'dashboard', label: 'Stock Matrix & Alerts', icon: LayoutDashboard },
              { id: 'warehouses', label: 'Shop Floor / Godown', icon: Warehouse },
              { id: 'ledger', label: 'Stock Audit Ledger', icon: History },
            ].map(tab => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-semibold transition ${
                    activeTab === tab.id
                      ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/20'
                      : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  {tab.label}
                </button>
              );
            })}
          </nav>
        </div>

        <button
          onClick={loadAllData}
          className="flex items-center justify-center gap-2 w-full py-2 px-3 bg-slate-800 hover:bg-slate-700 rounded-lg text-xs font-medium text-slate-300"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Local DB
        </button>
      </aside>

      {/* MAIN CONTENT AREA */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {/* TOP METRICS */}
        <header className="border-b border-slate-800 bg-slate-900/40 px-6 py-3 grid grid-cols-4 gap-4 print:hidden">
          <div className="bg-slate-900 border border-slate-800 p-3 rounded-xl">
            <span className="text-xs text-slate-400">Total Designs / SKUs</span>
            <p className="text-xl font-bold">{items.length}</p>
          </div>
          <div className="bg-slate-900 border border-slate-800 p-3 rounded-xl">
            <span className="text-xs text-slate-400">Total Pieces in Shop + Godown</span>
            <p className="text-xl font-bold text-indigo-400">{totalOnHand}</p>
          </div>
          <div className="bg-slate-900 border border-slate-800 p-3 rounded-xl">
            <span className="text-xs text-slate-400">Held for Alteration / Customer</span>
            <p className="text-xl font-bold text-amber-400">{totalReserved}</p>
          </div>
          <div className="bg-slate-900 border border-slate-800 p-3 rounded-xl">
            <span className="text-xs text-slate-400">Registered Shop Customers</span>
            <p className="text-xl font-bold text-emerald-400">{customers.length}</p>
          </div>
        </header>

        <div className="flex-1 grid grid-cols-12 overflow-hidden">
          <div className="col-span-8 p-6 overflow-y-auto space-y-6">

            {/* ==========================================
                TAB 1: CLOTHING SHOP POS BILLING COUNTER
               ========================================== */}
            {activeTab === 'pos' && (
              <div className="space-y-5">
                <div className="flex items-center justify-between bg-slate-900 border border-slate-800 p-4 rounded-xl">
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-bold uppercase text-slate-400">Billing Location:</span>
                    <select
                      value={posWarehouseId}
                      onChange={e => setPosWarehouseId(e.target.value)}
                      className="bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-sm font-semibold text-indigo-300"
                    >
                      {warehouses.map(w => (
                        <option key={w.id} value={w.id}>{w.code} - {w.name}</option>
                      ))}
                    </select>
                  </div>

                  <form onSubmit={handleBarcodeSubmit} className="flex items-center gap-2">
                    <div className="relative">
                      <Barcode className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                      <input
                        placeholder="Scan Barcode or Enter SKU..."
                        value={barcodeInput}
                        onChange={e => setBarcodeInput(e.target.value)}
                        className="pl-9 pr-3 py-1.5 bg-slate-950 border border-slate-700 rounded-lg text-sm w-64 focus:border-indigo-500 outline-none"
                      />
                    </div>
                    <button type="submit" className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 rounded-lg text-xs font-bold">
                      + Add
                    </button>
                  </form>
                </div>

                <div className="grid grid-cols-12 gap-5">
                  {/* Quick Garment Selector */}
                  <div className="col-span-6 bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col h-[480px]">
                    <div className="flex items-center justify-between mb-3">
                      <h3 className="font-bold text-sm">Select Garments to Bill</h3>
                      <div className="flex gap-1">
                        {['ALL', 'S', 'M', 'L', 'XL', 'XXL'].map(sz => (
                          <button
                            key={sz}
                            type="button"
                            onClick={() => setSizeFilter(sz)}
                            className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                              sizeFilter === sz ? 'bg-indigo-600 text-white' : 'bg-slate-800 text-slate-400'
                            }`}
                          >
                            {sz}
                          </button>
                        ))}
                      </div>
                    </div>

                    <input
                      placeholder="Filter by Shirt, Kurta, Jeans, Color, Brand..."
                      value={searchQuery}
                      onChange={e => setSearchQuery(e.target.value)}
                      className="mb-3 bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs"
                    />

                    <div className="flex-1 overflow-y-auto space-y-2 pr-1">
                      {filteredItems.map(item => {
                        const avail = getAvailableStock(item.id, posWarehouseId);
                        return (
                          <div
                            key={item.id}
                            className="p-3 rounded-lg bg-slate-950 border border-slate-800 flex items-center justify-between hover:border-slate-700"
                          >
                            <div>
                              <div className="flex items-center gap-2">
                                <span className="font-bold text-sm">{item.name}</span>
                                {item.size && (
                                  <span className="px-1.5 py-0.5 bg-indigo-950 text-indigo-300 border border-indigo-800 rounded text-[10px] font-bold">
                                    Size: {item.size}
                                  </span>
                                )}
                                {item.color && (
                                  <span className="px-1.5 py-0.5 bg-slate-800 text-slate-300 rounded text-[10px]">
                                    {item.color}
                                  </span>
                                )}
                              </div>
                              <p className="text-xs text-slate-400 font-mono mt-0.5">
                                {item.sku} • ₹{item.unitPrice} • <span className={avail > 0 ? 'text-emerald-400' : 'text-rose-400'}>Stock: {avail}</span>
                              </p>
                            </div>
                            <button
                              disabled={avail <= 0}
                              onClick={() => addToCart(item)}
                              className="px-3 py-1.5 bg-emerald-600 disabled:bg-slate-800 disabled:text-slate-600 hover:bg-emerald-500 text-slate-950 font-bold rounded-lg text-xs"
                            >
                              + Bill
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* Active Customer Bill & Checkout */}
                  <form onSubmit={handlePosCheckout} className="col-span-6 bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col justify-between h-[480px]">
                    <div>
                      <h3 className="font-bold text-sm border-b border-slate-800 pb-2">Customer Shopping Bag ({cart.length} items)</h3>
                      <div className="grid grid-cols-2 gap-2 mt-3">
                        <input
                          placeholder="Customer Name (Walk-in)"
                          value={customerName}
                          onChange={e => setCustomerName(e.target.value)}
                          className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs"
                        />
                        <input
                          placeholder="Mobile No (10 digits for CRM)"
                          value={customerMobile}
                          onChange={e => setCustomerMobile(e.target.value)}
                          className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs"
                        />
                      </div>

                      <div className="mt-3 max-h-48 overflow-y-auto divide-y divide-slate-800">
                        {cart.map(c => (
                          <div key={c.itemId} className="py-2 flex items-center justify-between text-xs">
                            <div>
                              <p className="font-semibold">{c.name} <span className="text-indigo-400">({c.size}/{c.color})</span></p>
                              <p className="text-slate-400">₹{c.unitPrice} × {c.quantity}</p>
                            </div>
                            <div className="flex items-center gap-2">
                              <input
                                type="number" min="1"
                                value={c.quantity}
                                onChange={e => setCart(cart.map(x => x.itemId === c.itemId ? { ...x, quantity: Number(e.target.value) } : x))}
                                className="w-14 bg-slate-950 border border-slate-700 rounded px-2 py-1 text-center"
                              />
                              <span className="font-bold w-16 text-right">₹{(c.unitPrice * c.quantity).toFixed(2)}</span>
                              <button
                                type="button"
                                onClick={() => setCart(cart.filter(x => x.itemId !== c.itemId))}
                                className="text-rose-400 hover:text-rose-300"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </div>
                          </div>
                        ))}
                        {cart.length === 0 && (
                          <p className="text-center text-slate-500 py-10 text-xs">Scan a barcode or click "+ Bill" on any garment.</p>
                        )}
                      </div>
                    </div>

                    <div className="border-t border-slate-800 pt-3 space-y-2 text-xs">
                      <div className="grid grid-cols-3 gap-2">
                        <div>
                          <label className="text-[10px] text-slate-400">Discount (₹)</label>
                          <input
                            type="number" min="0"
                            value={discountAmount}
                            onChange={e => setDiscountAmount(e.target.value)}
                            className="w-full bg-slate-950 border border-slate-800 rounded px-2 py-1"
                          />
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-400">GST %</label>
                          <input
                            type="number" min="0"
                            value={taxPercent}
                            onChange={e => setTaxPercent(e.target.value)}
                            className="w-full bg-slate-950 border border-slate-800 rounded px-2 py-1"
                          />
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-400">Payment Mode</label>
                          <select
                            value={paymentMode}
                            onChange={e => setPaymentMode(e.target.value)}
                            className="w-full bg-slate-950 border border-slate-800 rounded px-2 py-1"
                          >
                            <option value="UPI">UPI / GPay</option>
                            <option value="CASH">Cash</option>
                            <option value="CARD">Card</option>
                          </select>
                        </div>
                      </div>

                      <div className="flex justify-between items-center pt-2 text-sm font-bold">
                        <span>Net Payable Total:</span>
                        <span className="text-xl text-emerald-400">₹{cartGrandTotal.toFixed(2)}</span>
                      </div>

                      <button
                        type="submit"
                        disabled={cart.length === 0}
                        className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-800 rounded-lg font-bold text-white shadow-lg"
                      >
                        Complete Bill & Deduct Stock
                      </button>
                    </div>
                  </form>
                </div>

                {/* Printable Receipt Preview */}
                {lastInvoice && (
                  <div className="bg-slate-900 border border-emerald-700/60 rounded-xl p-5 flex justify-between items-start">
                    <div className="space-y-1 text-xs">
                      <span className="px-2 py-0.5 bg-emerald-950 text-emerald-400 rounded font-bold">Bill Generated: {lastInvoice.invoiceNumber}</span>
                      <h4 className="font-bold text-sm pt-1">Customer: {lastInvoice.customerName} ({lastInvoice.customerMobile || 'Walk-in'})</h4>
                      <p className="text-slate-400">
                        Subtotal: ₹{lastInvoice.subtotal} | Discount: ₹{lastInvoice.discountAmount} | GST: ₹{lastInvoice.taxAmount} | <strong>Paid ({lastInvoice.paymentMode}): ₹{lastInvoice.grandTotal}</strong>
                      </p>
                    </div>
                    <button
                      onClick={() => window.print()}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold rounded-lg text-xs"
                    >
                      <Printer className="w-4 h-4" /> Print Receipt
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* ==========================================
                TAB 2: GARMENT CATALOG (WITH SIZE / COLOR / BRAND)
               ========================================== */}
            {activeTab === 'items' && (
              <div className="space-y-6">
                <form onSubmit={handleCreateItem} className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-4">
                  <h3 className="font-bold text-sm text-indigo-400 uppercase">Add New Garment / Apparel SKU</h3>
                  <div className="grid grid-cols-4 gap-3 text-sm">
                    <input required placeholder="SKU (e.g. KURTA-BLU-L)" value={newItem.sku} onChange={e => setNewItem({ ...newItem, sku: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    <input required placeholder="Garment Name (e.g. Cotton Kurta)" value={newItem.name} onChange={e => setNewItem({ ...newItem, name: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    <select value={newItem.size} onChange={e => setNewItem({ ...newItem, size: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2">
                      {['XS', 'S', 'M', 'L', 'XL', 'XXL', '3XL', 'Free Size'].map(s => <option key={s} value={s}>Size: {s}</option>)}
                    </select>
                    <input placeholder="Color (e.g. Royal Blue)" value={newItem.color} onChange={e => setNewItem({ ...newItem, color: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    <input placeholder="Brand (e.g. Manyavar / In-House)" value={newItem.brand} onChange={e => setNewItem({ ...newItem, brand: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    <input placeholder="Barcode (Leave blank to use SKU)" value={newItem.barcode} onChange={e => setNewItem({ ...newItem, barcode: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    <input required type="number" step="0.01" placeholder="Selling MRP (₹)" value={newItem.unitPrice} onChange={e => setNewItem({ ...newItem, unitPrice: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    <input required type="number" step="0.01" placeholder="Wholesale Cost (₹)" value={newItem.costPrice} onChange={e => setNewItem({ ...newItem, costPrice: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                  </div>
                  <button type="submit" className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 rounded-lg text-xs font-bold">
                    + Save Garment to Catalog
                  </button>
                </form>

                <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400 uppercase bg-slate-950/50">
                        <th className="p-3">SKU / Barcode</th>
                        <th className="p-3">Garment</th>
                        <th className="p-3">Size / Color</th>
                        <th className="p-3">Brand</th>
                        <th className="p-3">Wholesale Cost</th>
                        <th className="p-3">Selling Price</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {items.map(item => (
                        <tr key={item.id} className="hover:bg-slate-800/40">
                          <td className="p-3 font-mono text-indigo-400">{item.sku}</td>
                          <td className="p-3 font-semibold">{item.name}</td>
                          <td className="p-3">{item.size || '-'} / {item.color || '-'}</td>
                          <td className="p-3 text-slate-400">{item.brand || '-'}</td>
                          <td className="p-3 text-slate-400">₹{item.costPrice}</td>
                          <td className="p-3 font-bold text-emerald-400">₹{item.unitPrice}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* ==========================================
                TAB 3: GODOWN STOCK & ALTERATION HOLD
               ========================================== */}
            {activeTab === 'operations' && (
              <form onSubmit={handleExecuteOperation} className="bg-slate-900 border border-slate-800 p-6 rounded-xl space-y-5">
                <h2 className="text-lg font-bold">Wholesale Inward, Godown Transfer & Alteration Hold</h2>
                <div className="flex gap-2 flex-wrap">
                  {[
                    { id: 'receive', label: '1. Receive Wholesale Stock' },
                    { id: 'transfer', label: '2. Godown → Shop Floor Transfer' },
                    { id: 'reserve', label: '3. Hold for Alteration / Customer' },
                    { id: 'release', label: '4. Cancel Hold' },
                    { id: 'adjust', label: '5. Damage / Audit Adjust' }
                  ].map(t => (
                    <button
                      type="button"
                      key={t.id}
                      onClick={() => setOpForm({ ...opForm, opType: t.id })}
                      className={`px-3 py-2 rounded-lg text-xs font-bold ${
                        opForm.opType === t.id ? 'bg-indigo-600 text-white' : 'bg-slate-950 text-slate-400 border border-slate-800'
                      }`}
                    >
                      {t.label}
                    </button>
                  ))}
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">Garment SKU</label>
                    <select value={opForm.itemId} onChange={e => setOpForm({ ...opForm, itemId: e.target.value })} className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2">
                      {items.map(i => <option key={i.id} value={i.id}>{i.sku} - {i.name} ({i.size || 'Std'})</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">{opForm.opType === 'transfer' ? 'From Location (e.g. Godown)' : 'Location'}</label>
                    <select value={opForm.warehouseId} onChange={e => setOpForm({ ...opForm, warehouseId: e.target.value })} className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2">
                      {warehouses.map(w => <option key={w.id} value={w.id}>{w.code} - {w.name}</option>)}
                    </select>
                  </div>
                  {opForm.opType === 'transfer' && (
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">To Location (e.g. Main Shop Floor)</label>
                      <select value={opForm.toWarehouseId} onChange={e => setOpForm({ ...opForm, toWarehouseId: e.target.value })} className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2">
                        <option value="">-- Select Target Location --</option>
                        {warehouses.map(w => <option key={w.id} value={w.id}>{w.code} - {w.name}</option>)}
                      </select>
                    </div>
                  )}
                  {opForm.opType !== 'adjust' ? (
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">Pieces Quantity</label>
                      <input type="number" min="1" required value={opForm.quantity} onChange={e => setOpForm({ ...opForm, quantity: e.target.value })} className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    </div>
                  ) : (
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">Quantity Delta (+ or -)</label>
                      <input type="number" required value={opForm.quantityDelta} onChange={e => setOpForm({ ...opForm, quantityDelta: e.target.value })} className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    </div>
                  )}
                  {(opForm.opType === 'reserve' || opForm.opType === 'release') && (
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">Customer Mobile / Alteration Slip No.</label>
                      <input required value={opForm.crmReferenceId} onChange={e => setOpForm({ ...opForm, crmReferenceId: e.target.value })} className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    </div>
                  )}
                </div>

                <button type="submit" className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold rounded-lg text-sm">
                  Update Stock Ledger
                </button>
              </form>
            )}

            {/* ==========================================
                TAB 4: CUSTOMER CRM & PAST BILLS
               ========================================== */}
            {activeTab === 'customers' && (
              <div className="space-y-6">
                <h3 className="font-bold text-sm text-indigo-400 uppercase">Customer Loyalty & Repeat Visits</h3>
                <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400 uppercase bg-slate-950/50">
                        <th className="p-3">Customer Name</th>
                        <th className="p-3">Mobile Number</th>
                        <th className="p-3">Visits</th>
                        <th className="p-3">Lifetime Spend</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {customers.map(c => (
                        <tr key={c.id}>
                          <td className="p-3 font-bold">{c.name}</td>
                          <td className="p-3 font-mono text-indigo-400">{c.mobile}</td>
                          <td className="p-3">{c.visitCount}</td>
                          <td className="p-3 font-bold text-emerald-400">₹{c.totalSpent}</td>
                        </tr>
                      ))}
                      {customers.length === 0 && (
                        <tr><td colSpan="4" className="p-4 text-center text-slate-500">Customers are automatically saved when you enter their mobile number during POS billing.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>

                <h3 className="font-bold text-sm text-indigo-400 uppercase">Recent POS Counter Invoices</h3>
                <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400 uppercase bg-slate-950/50">
                        <th className="p-3">Invoice No</th>
                        <th className="p-3">Customer</th>
                        <th className="p-3">Items</th>
                        <th className="p-3">Payment</th>
                        <th className="p-3">Grand Total</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {invoices.map(inv => (
                        <tr key={inv.id}>
                          <td className="p-3 font-mono text-indigo-400">{inv.invoiceNumber}</td>
                          <td className="p-3">{inv.customerName} ({inv.customerMobile || 'Walk-in'})</td>
                          <td className="p-3">{inv.items?.length || 0} garments</td>
                          <td className="p-3">{inv.paymentMode}</td>
                          <td className="p-3 font-bold text-emerald-400">₹{inv.grandTotal}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* TAB 5: STOCK MATRIX & LOW STOCK ALERTS */}
            {activeTab === 'dashboard' && (
              <div className="space-y-6">
                <h3 className="font-bold text-sm">Real-Time Shop Floor vs Godown Stock</h3>
                <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400 uppercase bg-slate-950/50">
                        <th className="p-3">Garment SKU</th>
                        <th className="p-3">Location</th>
                        <th className="p-3">On-Hand</th>
                        <th className="p-3">Held/Alteration</th>
                        <th className="p-3">Available for Sale</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {stockLevels.map(sl => {
                        const itm = items.find(i => i.id === sl.itemId);
                        const wh = warehouses.find(w => w.id === sl.warehouseId);
                        return (
                          <tr key={sl.id}>
                            <td className="p-3 font-semibold">{itm ? `${itm.sku} - ${itm.name} (${itm.size || 'Std'})` : sl.itemId}</td>
                            <td className="p-3">{wh ? wh.name : sl.warehouseId}</td>
                            <td className="p-3">{sl.quantityOnHand}</td>
                            <td className="p-3 text-amber-400">{sl.quantityReserved}</td>
                            <td className="p-3 font-bold text-emerald-400">{sl.quantityOnHand - sl.quantityReserved}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* TAB 6: WAREHOUSES */}
            {activeTab === 'warehouses' && (
              <div className="space-y-6">
                <form onSubmit={handleCreateWarehouse} className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-4">
                  <h3 className="font-bold text-sm text-indigo-400 uppercase">Add Shop Floor or Godown Location</h3>
                  <div className="grid grid-cols-3 gap-3 text-sm">
                    <input required placeholder="Code (e.g. WH-SHOP-01)" value={newWarehouse.code} onChange={e => setNewWarehouse({ ...newWarehouse, code: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    <input required placeholder="Name (e.g. Main Display Floor)" value={newWarehouse.name} onChange={e => setNewWarehouse({ ...newWarehouse, name: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                    <input placeholder="Address / Floor" value={newWarehouse.address} onChange={e => setNewWarehouse({ ...newWarehouse, address: e.target.value })} className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2" />
                  </div>
                  <button type="submit" className="px-4 py-2 bg-indigo-600 rounded-lg text-xs font-bold">+ Save Location</button>
                </form>
              </div>
            )}

            {/* TAB 7: AUDIT LEDGER */}
            {activeTab === 'ledger' && (
              <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400 uppercase bg-slate-950/50">
                      <th className="p-3">Operation</th>
                      <th className="p-3">Qty</th>
                      <th className="p-3">Bill / Reference No</th>
                      <th className="p-3">Timestamp</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {ledger.map(tx => (
                      <tr key={tx.id}>
                        <td className="p-3 font-bold text-indigo-400">{tx.type}</td>
                        <td className="p-3 font-bold">{tx.quantity}</td>
                        <td className="p-3 font-mono">{tx.referenceId || '-'}</td>
                        <td className="p-3 text-slate-500">{tx.createdAt}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* RIGHT SIDEBAR: API INSPECTOR */}
          <div className="col-span-4 border-l border-slate-800 bg-slate-900/90 p-5 flex flex-col overflow-hidden print:hidden">
            <div className="flex items-center gap-2 pb-3 border-b border-slate-800">
              <Terminal className="w-4 h-4 text-emerald-400" />
              <h3 className="font-bold text-xs uppercase tracking-wider text-slate-300">Spring Boot POS API Inspector</h3>
            </div>
            <div className="mt-4 flex items-center justify-between text-xs bg-slate-950 p-3 rounded-lg border border-slate-800">
              <span className="font-mono font-bold text-indigo-400">{apiLog.method} {apiLog.endpoint}</span>
              <span className="px-2 py-0.5 rounded font-bold bg-emerald-950 text-emerald-400">HTTP {apiLog.status}</span>
            </div>
            <pre className="mt-3 flex-1 bg-slate-950 border border-slate-800 rounded-xl p-4 text-xs font-mono text-emerald-300 overflow-auto">
              {JSON.stringify(apiLog.response, null, 2)}
            </pre>
          </div>
        </div>
      </main>
    </div>
  );
}
