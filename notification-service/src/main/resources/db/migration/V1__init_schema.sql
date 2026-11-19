CREATE TABLE notification (
    id_notification BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    transfer_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notification_event_account UNIQUE (event_id, account_number)
);

CREATE INDEX idx_notification_account_created ON notification(account_number, created_at DESC);