CREATE TABLE idempotency_request (
    key          VARCHAR(255) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    invoice_id   UUID NOT NULL REFERENCES invoices(id),
    created_at   TIMESTAMPTZ NOT NULL
);
