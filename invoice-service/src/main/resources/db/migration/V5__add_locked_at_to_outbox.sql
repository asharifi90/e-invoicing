ALTER TABLE outbox_events
    ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_outbox_unclaimed
    ON outbox_events (created_at)
    WHERE published_at IS NULL;