\connect banking_analytics;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- CUSTOMER-SERVICE TABLES
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100) DEFAULT 'Morocco',
    postal_code VARCHAR(20),
    date_of_birth DATE,
    account_status VARCHAR(20) DEFAULT 'ACTIVE',
    risk_score DECIMAL(3,2) DEFAULT 0.00,
    kyc_status VARCHAR(20) DEFAULT 'PENDING',
    preferred_language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'Africa/Casablanca',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'MAD',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    interest_rate DECIMAL(5,4) DEFAULT 0.0000,
    overdraft_limit DECIMAL(15,2) DEFAULT 0.00,
    last_transaction_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- STREAM-PROCESSOR TABLES
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(255) UNIQUE NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'MAD',
    merchant_name VARCHAR(255),
    merchant_category VARCHAR(100),
    description TEXT,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    source_location VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    device_id VARCHAR(255),
    is_international BOOLEAN DEFAULT FALSE,
    risk_score DOUBLE PRECISION,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reference_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS customer_summaries (
    customer_id VARCHAR(50) PRIMARY KEY,
    total_transactions BIGINT DEFAULT 0,
    total_amount DECIMAL(15,2) DEFAULT 0,
    avg_amount DECIMAL(15,2) DEFAULT 0,
    most_frequent_merchant_category VARCHAR(100),
    preferred_location VARCHAR(255),
    last_transaction_time TIMESTAMP,
    avg_risk_score DOUBLE PRECISION,
    transactions_last_1_hour BIGINT DEFAULT 0,
    amount_last1_hour DECIMAL(15,2) DEFAULT 0,
    transactions_last24_hours BIGINT DEFAULT 0,
    amount_last24_hours DECIMAL(15,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id VARCHAR(255) UNIQUE NOT NULL,
    transaction_id VARCHAR(255),
    customer_id VARCHAR(50) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    severity DOUBLE PRECISION NOT NULL,
    amount DECIMAL(15,2),
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    risk_score INTEGER,
    investigated_by VARCHAR(100),
    investigated_at TIMESTAMP,
    resolution TEXT,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction_metrics (
    id SERIAL PRIMARY KEY,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    total_transactions BIGINT DEFAULT 0,
    total_amount DECIMAL(15,2) DEFAULT 0,
    avg_amount DECIMAL(15,2) DEFAULT 0,
    transactions_by_type TEXT,
    transactions_by_status TEXT,
    transactions_by_location TEXT,
    alerts_generated BIGINT DEFAULT 0,
    avg_risk_score DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- NOTIFICATION-SERVICE TABLES
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    message TEXT,
    html_message TEXT,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'PENDING',
    recipient VARCHAR(255),
    external_id VARCHAR(255),
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    scheduled_time TIMESTAMP,
    sent_at TIMESTAMP,
    source_alert_id VARCHAR(255),
    transaction_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivery_status VARCHAR(50),
    metadata JSONB,
    notification_id VARCHAR(50),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS customer_contacts (
    customer_id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255),
    phone_number VARCHAR(20),
    push_token VARCHAR(500),
    preferred_language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'Africa/Casablanca',
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT TRUE,
    push_enabled BOOLEAN DEFAULT TRUE,
    fraud_alerts_enabled BOOLEAN DEFAULT TRUE,
    transaction_alerts_enabled BOOLEAN DEFAULT TRUE,
    marketing_enabled BOOLEAN DEFAULT FALSE,
    alternate_email VARCHAR(255),
    alternate_phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification_templates (
    id VARCHAR(255) PRIMARY KEY,
    template_id VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    language VARCHAR(10) DEFAULT 'en',
    subject VARCHAR(500),
    body_template TEXT,
    html_template TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_transactions_customer_id ON transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);
CREATE INDEX IF NOT EXISTS idx_transactions_account_number ON transactions(account_number);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_customer_id ON fraud_alerts(customer_id);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_severity ON fraud_alerts(severity);

-- DROP DEPENDENT VIEWS
DROP VIEW IF EXISTS active_fraud_alerts CASCADE;
DROP VIEW IF EXISTS high_risk_customers CASCADE;

-- ALTER COLUMN
ALTER TABLE IF EXISTS fraud_alerts
ALTER COLUMN severity TYPE VARCHAR(20);

-- RECREATE VIEWS
CREATE OR REPLACE VIEW active_fraud_alerts AS
SELECT
    fa.*,
    t.amount AS transaction_amount,
    t.merchant_name,
    c.first_name,
    c.last_name,
    c.email
FROM fraud_alerts fa
LEFT JOIN transactions t ON fa.transaction_id = t.transaction_id
JOIN customers c ON fa.customer_id = c.customer_id
WHERE fa.status IN ('NEW', 'INVESTIGATING')
ORDER BY fa.created_at DESC;

CREATE OR REPLACE VIEW high_risk_customers AS
SELECT
    cs.*,
    fa.alert_count
FROM customer_summaries cs
LEFT JOIN (
    SELECT customer_id, COUNT(*) AS alert_count
    FROM fraud_alerts
    WHERE created_at >= NOW() - INTERVAL '30 days'
    GROUP BY customer_id
) fa ON cs.customer_id = fa.customer_id
WHERE cs.avg_risk_score > 0.7 OR fa.alert_count > 0;

-- TRIGGERS
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

DROP TRIGGER IF EXISTS update_customers_updated_at ON customers;
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_accounts_updated_at ON accounts;
CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_notifications_updated_at ON notifications;
CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_customer_contacts_updated_at ON customer_contacts;
CREATE TRIGGER update_customer_contacts_updated_at BEFORE UPDATE ON customer_contacts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- PERMISSIONS
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO banking_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO banking_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO banking_user;

