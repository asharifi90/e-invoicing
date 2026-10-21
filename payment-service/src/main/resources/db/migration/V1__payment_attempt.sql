CREATE TABLE payment_attempt (
    invoice_id       UUID PRIMARY KEY,
    invoice_number   VARCHAR(64),
    status           VARCHAR(32)  NOT NULL,
    provider_reference VARCHAR(128),
    failure_reason   VARCHAR(512),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_payment_attempt_status ON payment_attempt (status);