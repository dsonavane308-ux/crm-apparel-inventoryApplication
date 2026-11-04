-- Enable UUID generator extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. CATEGORIES TABLE
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. ITEMS / SKU CATALOG TABLE
CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    cost_price NUMERIC(12, 2) NOT NULL CHECK (cost_price >= 0),
    reorder_threshold INT NOT NULL DEFAULT 10 CHECK (reorder_threshold >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_items_sku ON items(sku);
CREATE INDEX idx_items_name ON items(name);

-- 3. WAREHOUSES TABLE
CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. STOCK LEVELS TABLE (Item-Warehouse Mapping & Counters)
CREATE TABLE stock_levels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    quantity_on_hand INT NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved INT NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    quantity_available INT GENERATED ALWAYS AS (quantity_on_hand - quantity_reserved) STORED,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_item_warehouse UNIQUE (item_id, warehouse_id),
    CONSTRAINT chk_reserved_not_exceed_onhand CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE INDEX idx_stock_levels_item ON stock_levels(item_id);
CREATE INDEX idx_stock_levels_warehouse ON stock_levels(warehouse_id);

-- 5. STOCK TRANSACTIONS TABLE (Append-Only Audit Ledger)
CREATE TABLE stock_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id UUID NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    from_warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT,
    to_warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT,
    type VARCHAR(30) NOT NULL CHECK (type IN ('RECEIVE', 'TRANSFER', 'ADJUSTMENT', 'RESERVE', 'RELEASE', 'DEDUCT')),
    quantity INT NOT NULL CHECK (quantity != 0),
    unit_cost NUMERIC(12, 2) CHECK (unit_cost >= 0),
    reference_id VARCHAR(100),
    performed_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_tx_item ON stock_transactions(item_id);
CREATE INDEX idx_stock_tx_reference ON stock_transactions(reference_id);
CREATE INDEX idx_stock_tx_created_at ON stock_transactions(created_at);