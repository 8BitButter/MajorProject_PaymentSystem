ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS payer_mpin VARCHAR(16) NOT NULL DEFAULT '1111';

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    vpa VARCHAR(128) NOT NULL UNIQUE,
    mpin VARCHAR(16) NOT NULL,
    bank_node_code VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS bank_nodes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS transaction_steps (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    stage VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    next_stage VARCHAR(32),
    branch VARCHAR(16) NOT NULL,
    processing_ms BIGINT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    input_summary VARCHAR(512),
    output_summary VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS failure_config (
    id BIGSERIAL PRIMARY KEY,
    scenario VARCHAR(64) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tx_steps_tx_id ON transaction_steps(transaction_id, id);
CREATE INDEX IF NOT EXISTS idx_tx_steps_stage ON transaction_steps(stage);

INSERT INTO bank_nodes (code, name, role) VALUES
('PSP', 'PSP Bank', 'PSP'),
('SWITCH', 'NPCI Switch', 'SWITCH'),
('ISSUER_BANK', 'Issuer Bank', 'ISSUER'),
('ACQUIRER_BANK', 'Acquirer Bank', 'ACQUIRER')
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (display_name, vpa, mpin, bank_node_code) VALUES
('Payer One', 'payer@issuer', '1111', 'ISSUER_BANK'),
('Payee One', 'payee@acquirer', '1111', 'ACQUIRER_BANK'),
('Payer Two', 'payer2@issuer', '1111', 'ISSUER_BANK'),
('Payee Two', 'payee2@acquirer', '1111', 'ACQUIRER_BANK')
ON CONFLICT (vpa) DO NOTHING;

INSERT INTO failure_config (scenario, enabled) VALUES
('VALIDATION_FAIL', FALSE),
('DEBIT_FAIL', FALSE),
('CREDIT_FAIL', FALSE),
('SWITCH_FAIL', FALSE),
('REVERSAL_FAIL', FALSE)
ON CONFLICT (scenario) DO NOTHING;

