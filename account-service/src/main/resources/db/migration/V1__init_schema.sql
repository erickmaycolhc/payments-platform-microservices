CREATE TABLE customer (
    id_customer UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_document UNIQUE (document_type, document_number)
);

CREATE TABLE account (
    id_account UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customer(id_customer),
    account_number VARCHAR(50) NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_customer_id ON account(customer_id);

CREATE TABLE account_movement (
    id_account_movement BIGSERIAL PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES account(id_account),
    transfer_id UUID,
    type VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_transfer_account_type UNIQUE (transfer_id, account_id, type)
);

CREATE INDEX idx_movement_account_id ON account_movement(account_id);

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