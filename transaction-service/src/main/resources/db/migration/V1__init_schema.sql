CREATE TABLE transfer (
    id_transfer UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(100) NOT NULL,
    source_account VARCHAR(50) NOT NULL,
    target_account VARCHAR(50) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_transfer_idempotency_key UNIQUE(idempotency_key)
);


CREATE TABLE transfer_status_history (
    id_transfer_status_history BIGSERIAL PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES transfer(id_transfer),
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    change_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_event (
    id_outbox_event BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX idx_outbox_published_at ON outbox_event(published_at) WHERE published_at IS NULL;

CREATE TABLE processed_message (
    message_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);