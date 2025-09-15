-- init-stream-processor.sql
-- Database initialization script for Stream Processor

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS banking;

-- Set search path
SET search_path TO banking;

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    account_number VARCHAR(255),
    type VARCHAR(50),
    amount DECIMAL(15,2),
    currency VARCHAR(10),
    merchant_name VARCHAR(255),
    merchant_category VARCHAR(100),
    description TEXT,
    status VARCHAR(50),
    source_location VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    device_id VARCHAR(255),
    is_international BOOLEAN,
    risk_score DOUBLE PRECISION,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for transactions table
CREATE INDEX IF NOT EXISTS idx_customer_id ON transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_timestamp ON transactions(timestamp);
CREATE INDEX IF NOT EXISTS idx_merchant_category ON transactions(merchant_category);
CREATE INDEX IF NOT EXISTS idx_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_type ON transactions(type);

-- Create customer_summaries table
CREATE TABLE IF NOT EXISTS customer_summaries (
    customer_id VARCHAR(255) PRIMARY KEY,
    total_transactions BIGINT DEFAULT 0,
    total_amount DECIMAL(15,2) DEFAULT 0,
    avg_amount DECIMAL(15,2) DEFAULT 0,
    most_frequent_merchant_category VARCHAR(100),
    preferred_location VARCHAR(255),
    last_transaction_time TIMESTAMP,
    avg_risk_score DOUBLE PRECISION,
    transactions_last1_hour BIGINT DEFAULT 0,
    amount_last1_hour DECIMAL(15,2) DEFAULT 0,
    transactions_last24_hours BIGINT DEFAULT 0,
    amount_last24_hours DECIMAL(15,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create fraud_alerts table
CREATE TABLE IF NOT EXISTS fraud_alerts (
    alert_id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    reason VARCHAR(50),
    description TEXT,
    severity DOUBLE PRECISION,
    amount DECIMAL(15,2),
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for fraud_alerts table
CREATE INDEX IF NOT EXISTS idx_customer_id_alert ON fraud_alerts(customer_id);
CREATE INDEX IF NOT EXISTS idx_timestamp_alert ON fraud_alerts(timestamp);
CREATE INDEX IF NOT EXISTS idx_status_alert ON fraud_alerts(status);
CREATE INDEX IF NOT EXISTS idx_reason ON fraud_alerts(reason);

-- Create transaction_metrics table
CREATE TABLE IF NOT EXISTS transaction_metrics (
    id SERIAL PRIMARY KEY,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    total_transactions BIGINT DEFAULT 0,
    total_amount DECIMAL(15,2) DEFAULT 0,
    avg_amount DECIMAL(15,2) DEFAULT 0,
    transactions_by_type TEXT, -- JSON string
    transactions_by_status TEXT, -- JSON string
    transactions_by_location TEXT, -- JSON string
    alerts_generated BIGINT DEFAULT 0,
    avg_risk_score DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for transaction_metrics table
CREATE INDEX IF NOT EXISTS idx_window_start ON transaction_metrics(window_start);

-- Create some utility views for analytics
CREATE OR REPLACE VIEW high_risk_customers AS
SELECT
    cs.*,
    fa.alert_count
FROM customer_summaries cs
LEFT JOIN (
    SELECT
        customer_id,
        COUNT(*) as alert_count
    FROM fraud_alerts
    WHERE created_at >= NOW() - INTERVAL '30 days'
    GROUP BY customer_id
) fa ON cs.customer_id = fa.customer_id
WHERE cs.avg_risk_score > 0.7 OR fa.alert_count > 0;

CREATE OR REPLACE VIEW transaction_summary_today AS
SELECT
    COUNT(*) as total_transactions,
    SUM(CASE WHEN status = 'COMPLETED' THEN amount ELSE 0 END) as total_amount,
    AVG(CASE WHEN status = 'COMPLETED' THEN amount ELSE NULL END) as avg_amount,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_transactions,
    AVG(risk_score) as avg_risk_score
FROM transactions
WHERE DATE(timestamp) = CURRENT_DATE;

CREATE OR REPLACE VIEW fraud_alerts_today AS
SELECT
    COUNT(*) as total_alerts,
    COUNT(CASE WHEN status = 'NEW' THEN 1 END) as new_alerts,
    AVG(severity) as avg_severity,
    reason,
    COUNT(*) as count_by_reason
FROM fraud_alerts
WHERE DATE(created_at) = CURRENT_DATE
GROUP BY reason;

-- Insert some sample data for testing
-- todo: remove in production
INSERT INTO customer_summaries (customer_id, total_transactions, total_amount, avg_amount, updated_at)
VALUES
    ('CUST000001', 0, 0, 0, CURRENT_TIMESTAMP),
    ('CUST000002', 0, 0, 0, CURRENT_TIMESTAMP),
    ('CUST000003', 0, 0, 0, CURRENT_TIMESTAMP)
ON CONFLICT (customer_id) DO NOTHING;

Grant permissions (adjust as needed for your setup)
GRANT ALL PRIVILEGES ON SCHEMA banking TO banking_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA banking TO banking_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA banking TO banking_user;