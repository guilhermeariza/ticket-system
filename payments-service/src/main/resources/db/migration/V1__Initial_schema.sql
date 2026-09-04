-- payments-service database schema

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255),
    processed_at TIMESTAMP,
    CONSTRAINT check_amount_positive CHECK (amount >= 0),
    CONSTRAINT check_status_valid CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

-- Create indexes for faster queries
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_processed_at ON payments(processed_at);
