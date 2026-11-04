-- 1. Add cost price and area/pincode tracking to invoices and items
ALTER TABLE pos_invoices ADD COLUMN IF NOT EXISTS customer_pincode VARCHAR(10);
ALTER TABLE pos_invoice_items ADD COLUMN IF NOT EXISTS cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE pos_invoice_items ADD COLUMN IF NOT EXISTS profit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE pos_invoice_items ADD COLUMN IF NOT EXISTS profit_percentage NUMERIC(8, 2) NOT NULL DEFAULT 0;

-- 2. Customer preference mapping table for seasonal & monthly tracking
CREATE TABLE IF NOT EXISTS customer_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    brand VARCHAR(100),
    category_id UUID,
    purchase_month INT NOT NULL, -- 1 to 12
    purchase_year INT NOT NULL,
    total_quantity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cust_pref_month ON customer_preferences(purchase_month, purchase_year);
