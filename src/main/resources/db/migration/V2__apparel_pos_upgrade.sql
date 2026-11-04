-- 1. Add Clothing / Apparel specific columns to items
ALTER TABLE items ADD COLUMN IF NOT EXISTS size VARCHAR(32);
ALTER TABLE items ADD COLUMN IF NOT EXISTS color VARCHAR(64);
ALTER TABLE items ADD COLUMN IF NOT EXISTS brand VARCHAR(100);
ALTER TABLE items ADD COLUMN IF NOT EXISTS barcode VARCHAR(64) UNIQUE;

CREATE INDEX IF NOT EXISTS idx_items_barcode ON items(barcode);

-- 2. Customers CRM Table
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mobile VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    notes TEXT,
    total_spent NUMERIC(12, 2) NOT NULL DEFAULT 0,
    visit_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customers_mobile ON customers(mobile);

-- 3. POS Invoices Table
CREATE TABLE IF NOT EXISTS pos_invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    customer_name VARCHAR(150),
    customer_mobile VARCHAR(20),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    subtotal NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(12, 2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL DEFAULT 'CASH',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. POS Invoice Line Items Table
CREATE TABLE IF NOT EXISTS pos_invoice_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID NOT NULL REFERENCES pos_invoices(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    sku VARCHAR(64) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    size VARCHAR(32),
    color VARCHAR(64),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL
);
