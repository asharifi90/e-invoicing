CREATE TABLE invoices (
    id                UUID PRIMARY KEY,
    invoice_number    VARCHAR(255) NOT NULL UNIQUE,
    seller_vat_number VARCHAR(50)  NOT NULL,
    buyer_vat_number  VARCHAR(50)  NOT NULL,
    total_amount      NUMERIC(19, 2) NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    status            VARCHAR(50)  NOT NULL,
    received_at       TIMESTAMPTZ  NOT NULL,
    rejection_reason  TEXT
);

CREATE TABLE invoice_lines (
    id          UUID PRIMARY KEY,
    invoice_id  UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity    INT NOT NULL,
    unit_price  NUMERIC(19, 2) NOT NULL,
    line_total  NUMERIC(19, 2) NOT NULL,
    currency    VARCHAR(3) NOT NULL
);

CREATE INDEX idx_invoice_lines_invoice_id ON invoice_lines(invoice_id);