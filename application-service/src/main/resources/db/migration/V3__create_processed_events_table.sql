CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_name VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_processed_events_event_consumer
    ON processed_events(event_id, consumer_name);

CREATE INDEX idx_processed_events_processed_at
    ON processed_events(processed_at);