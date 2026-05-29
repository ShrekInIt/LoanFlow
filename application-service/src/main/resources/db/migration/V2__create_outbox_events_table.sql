CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,

    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,

    payload TEXT NOT NULL,

    status VARCHAR(50) NOT NULL DEFAULT 'NEW',

    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 5,

    error_message TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

CREATE INDEX idx_outbox_events_status_created_at
    ON outbox_events(status, created_at);

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events(aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_events_event_type
    ON outbox_events(event_type);

CREATE INDEX idx_outbox_events_topic
    ON outbox_events(topic);

CREATE INDEX idx_outbox_events_published_at
    ON outbox_events(published_at);