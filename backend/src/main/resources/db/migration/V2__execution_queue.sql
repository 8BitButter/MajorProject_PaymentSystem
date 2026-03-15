CREATE TABLE execution_queue (
    transaction_id UUID PRIMARY KEY REFERENCES transactions(id) ON DELETE CASCADE,
    priority_score INTEGER NOT NULL,
    queue_status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    locked_by VARCHAR(128),
    locked_at TIMESTAMPTZ,
    last_error_code VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_execution_queue_status
        CHECK (queue_status IN ('PENDING', 'PROCESSING', 'DEAD_LETTER')),
    CONSTRAINT chk_execution_queue_priority_non_negative
        CHECK (priority_score >= 0),
    CONSTRAINT chk_execution_queue_attempt_non_negative
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_execution_queue_poll
    ON execution_queue(queue_status, next_attempt_at, priority_score, created_at);

