-- infrastructure/init-scripts/01-init-banking.sql
-- Banking Analytics Database Initialization

-- Create database if it doesn't exist (handled by docker-compose)
-- CREATE DATABASE IF NOT EXISTS banking_analytics ;

-- Connect to the database
\connect banking_analytics;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    date_of_birth DATE,
    account_status VARCHAR(20) DEFAULT 'ACTIVE',
    risk_score INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    description TEXT,
    merchant_name VARCHAR(255),
    merchant_category VARCHAR(100),
    location VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    reference_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
);

-- Create fraud_alerts table
CREATE TABLE IF NOT EXISTS fraud_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id VARCHAR(50) UNIQUE NOT NULL,
    transaction_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    alert_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    risk_score INTEGER NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    investigated_by VARCHAR(100),
    investigated_at TIMESTAMP,
    resolution TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Create transaction_metrics table for aggregated data
CREATE TABLE IF NOT EXISTS transaction_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_date DATE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    total_transactions INTEGER DEFAULT 0,
    total_amount DECIMAL(15,2) DEFAULT 0.00,
    average_amount DECIMAL(15,2) DEFAULT 0.00,
    max_amount DECIMAL(15,2) DEFAULT 0.00,
    min_amount DECIMAL(15,2) DEFAULT 0.00,
    unique_merchants INTEGER DEFAULT 0,
    high_risk_transactions INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    UNIQUE(metric_date, customer_id)
);

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notification_id VARCHAR(50) UNIQUE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    notification_type VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL, -- EMAIL, SMS, PUSH, IN_APP
    subject VARCHAR(255),
    message TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    delivery_status VARCHAR(50),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_transactions_customer_id ON transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);
CREATE INDEX IF NOT EXISTS idx_transactions_account_number ON transactions(account_number);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_amount ON transactions(amount);

CREATE INDEX IF NOT EXISTS idx_fraud_alerts_customer_id ON fraud_alerts(customer_id);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_transaction_id ON fraud_alerts(transaction_id);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_severity ON fraud_alerts(severity);

CREATE INDEX IF NOT EXISTS idx_customers_customer_id ON customers(customer_id);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(account_status);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);

CREATE INDEX IF NOT EXISTS idx_metrics_date_customer ON transaction_metrics(metric_date, customer_id);
CREATE INDEX IF NOT EXISTS idx_metrics_date ON transaction_metrics(metric_date);

CREATE INDEX IF NOT EXISTS idx_notifications_customer_id ON notifications(customer_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(notification_type);

-- Create views for common queries
CREATE OR REPLACE VIEW daily_transaction_summary AS
SELECT
    DATE(timestamp) as transaction_date,
    COUNT(*) as total_transactions,
    SUM(amount) as total_amount,
    AVG(amount) as average_amount,
    COUNT(DISTINCT customer_id) as unique_customers,
    COUNT(CASE WHEN amount > 1000 THEN 1 END) as high_value_transactions
FROM transactions
WHERE status = 'COMPLETED'
GROUP BY DATE(timestamp)
ORDER BY transaction_date DESC;

CREATE OR REPLACE VIEW customer_transaction_summary AS
SELECT
    t.customer_id,
    c.first_name,
    c.last_name,
    c.email,
    COUNT(t.*) as total_transactions,
    SUM(t.amount) as total_amount,
    AVG(t.amount) as average_amount,
    MAX(t.timestamp) as last_transaction,
    COUNT(fa.id) as fraud_alerts_count
FROM customers c
LEFT JOIN transactions t ON c.customer_id = t.customer_id
LEFT JOIN fraud_alerts fa ON c.customer_id = fa.customer_id
GROUP BY t.customer_id, c.first_name, c.last_name, c.email
ORDER BY total_amount DESC NULLS LAST;

CREATE OR REPLACE VIEW active_fraud_alerts AS
SELECT
    fa.*,
    t.amount as transaction_amount,
    t.merchant_name,
    c.first_name,
    c.last_name,
    c.email
FROM fraud_alerts fa
JOIN transactions t ON fa.transaction_id = t.transaction_id
JOIN customers c ON fa.customer_id = c.customer_id
WHERE fa.status IN ('OPEN', 'INVESTIGATING')
ORDER BY fa.created_at DESC;

-- Insert sample customers for testing
INSERT INTO customers (customer_id, first_name, last_name, email, phone, address, date_of_birth, risk_score)
VALUES
    ('CUST001', 'John', 'Doe', 'john.doe@email.com', '+1234567890', '123 Main St, City, State', '1985-01-15', 0),
    ('CUST002', 'Jane', 'Smith', 'jane.smith@email.com', '+1234567891', '456 Oak Ave, City, State', '1990-05-22', 0),
    ('CUST003', 'Bob', 'Johnson', 'bob.johnson@email.com', '+1234567892', '789 Pine St, City, State', '1978-11-08', 0),
    ('CUST004', 'Alice', 'Williams', 'alice.williams@email.com', '+1234567893', '321 Elm St, City, State', '1992-03-12', 0),
    ('CUST005', 'Charlie', 'Brown', 'charlie.brown@email.com', '+1234567894', '654 Cedar Ave, City, State', '1988-07-30', 0)
ON CONFLICT (customer_id) DO NOTHING;

-- Insert sample accounts
INSERT INTO accounts (account_number, customer_id, account_type, balance)
VALUES
    ('ACC001001', 'CUST001', 'CHECKING', 5000.00),
    ('ACC001002', 'CUST001', 'SAVINGS', 15000.00),
    ('ACC002001', 'CUST002', 'CHECKING', 3500.00),
    ('ACC003001', 'CUST003', 'CHECKING', 7200.00),
    ('ACC003002', 'CUST003', 'SAVINGS', 25000.00),
    ('ACC004001', 'CUST004', 'CHECKING', 2800.00),
    ('ACC005001', 'CUST005', 'CHECKING', 4600.00)
ON CONFLICT (account_number) DO NOTHING;

-- Create trigger function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transaction_metrics_updated_at BEFORE UPDATE ON transaction_metrics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Grant permissions to banking_user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO banking_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO banking_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO banking_user;

-- Create monitoring queries for health checks
CREATE OR REPLACE VIEW system_health AS
SELECT
    'transactions' as table_name,
    COUNT(*) as record_count,
    MAX(created_at) as last_record
FROM transactions
UNION ALL
SELECT
    'fraud_alerts' as table_name,
    COUNT(*) as record_count,
    MAX(created_at) as last_record
FROM fraud_alerts
UNION ALL
SELECT
    'notifications' as table_name,
    COUNT(*) as record_count,
    MAX(created_at) as last_record
FROM notifications;

-- Success message
DO $$
BEGIN
    RAISE NOTICE 'Banking Analytics database initialized successfully!';
END $$;