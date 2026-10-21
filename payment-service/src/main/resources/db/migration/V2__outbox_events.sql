CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    topic        VARCHAR(255) NOT NULL,
    payload      TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_payment_outbox_unpublished
    ON outbox_events (created_at)
    WHERE published_at IS NULL;
