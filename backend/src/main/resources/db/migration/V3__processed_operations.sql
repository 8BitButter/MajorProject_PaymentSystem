CREATE TABLE processed_operations (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    operation_type VARCHAR(32) NOT NULL,
    result VARCHAR(16) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_processed_operations_tx_operation UNIQUE (transaction_id, operation_type),
    CONSTRAINT chk_processed_operations_type CHECK (operation_type IN ('DEBIT', 'CREDIT', 'REVERSAL')),
    CONSTRAINT chk_processed_operations_result CHECK (result IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_processed_operations_tx_id ON processed_operations(transaction_id);
