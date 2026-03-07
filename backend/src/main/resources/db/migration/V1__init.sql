CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    client_request_id VARCHAR(128) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    payer_vpa VARCHAR(128) NOT NULL,
    payee_vpa VARCHAR(128) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    source VARCHAR(32) NOT NULL,
    state VARCHAR(64) NOT NULL,
    terminal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE transaction_events (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    from_state VARCHAR(64),
    to_state VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    vpa VARCHAR(128) UNIQUE NOT NULL,
    bank_type VARCHAR(32) NOT NULL,
    balance NUMERIC(18,2) NOT NULL DEFAULT 0
);

CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    vpa VARCHAR(128) NOT NULL,
    entry_type VARCHAR(32) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    payer_vpa VARCHAR(128) NOT NULL,
    client_request_id VARCHAR(128) NOT NULL,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    request_hash VARCHAR(256) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (payer_vpa, client_request_id)
);

CREATE TABLE sms_message_log (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(128) UNIQUE NOT NULL,
    accepted BOOLEAN NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_payer ON transactions(payer_vpa);
CREATE INDEX idx_transactions_payee ON transactions(payee_vpa);
CREATE INDEX idx_tx_events_tx_id ON transaction_events(transaction_id, id);

INSERT INTO accounts (vpa, bank_type, balance) VALUES
('payer@issuer', 'ISSUER', 5000.00),
('payee@acquirer', 'ACQUIRER', 1000.00),
('payer2@issuer', 'ISSUER', 2500.00),
('payee2@acquirer', 'ACQUIRER', 300.00);

