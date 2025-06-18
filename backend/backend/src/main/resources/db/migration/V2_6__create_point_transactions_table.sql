-- Create point transactions table for point system
CREATE TABLE point_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(10) NOT NULL CHECK (type IN ('EARNED', 'SPENT')),
    source VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    description VARCHAR(200),
    reference_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_point_transactions_user_id ON point_transactions(user_id);
CREATE INDEX idx_point_transactions_type ON point_transactions(type);
CREATE INDEX idx_point_transactions_source ON point_transactions(source);
CREATE INDEX idx_point_transactions_created_at ON point_transactions(created_at);
CREATE INDEX idx_point_transactions_user_type ON point_transactions(user_id, type);

-- Add comment for documentation
COMMENT ON TABLE point_transactions IS 'All point earning and spending transactions';
COMMENT ON COLUMN point_transactions.type IS 'EARNED or SPENT';
COMMENT ON COLUMN point_transactions.source IS 'Source of the transaction (e.g., STACK_TRACE_RESOLVED)';
COMMENT ON COLUMN point_transactions.reference_id IS 'Optional reference to related entity'; 