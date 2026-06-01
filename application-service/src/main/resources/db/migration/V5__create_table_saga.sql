CREATE TABLE saga_log (
     id UUID PRIMARY KEY,
     saga_id UUID NOT NULL,
     application_id BIGINT NOT NULL,
     step VARCHAR(100) NOT NULL,
     status VARCHAR(50) NOT NULL,
     message TEXT,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_saga_log_saga_id
    ON saga_log(saga_id);

CREATE INDEX idx_saga_log_application_id
    ON saga_log(application_id);

CREATE INDEX idx_saga_log_created_at
    ON saga_log(created_at);