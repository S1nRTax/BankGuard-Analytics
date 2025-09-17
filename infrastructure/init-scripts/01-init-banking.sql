-- infrastructure/init-scripts/01-init-banking.sql
-- Updated Banking Platform Database Initialization
-- Compatible with all microservices: transaction-generator, stream-processor, notification-service, customer-service

-- Create database if it doesn't exist (handled by docker-compose)

-- Connect to the database
\connect banking_analytics;

-- Create extensions (keeping your original extensions)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";


-- =======================
-- CUSTOMER-SERVICE TABLES
-- =======================

-- Enhanced customers table for customer-service
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
    risk_score DECIMAL(3,2) DEFAULT 0.00, -- Changed to decimal for compatibility
    kyc_status VARCHAR(20) DEFAULT 'PENDING',
    preferred_language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'Africa/Casablanca',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Enhanced accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'MAD', -- Changed to MAD for Morocco
    status VARCHAR(20) DEFAULT 'ACTIVE',
    interest_rate DECIMAL(5,4) DEFAULT 0.0000,
    overdraft_limit DECIMAL(15,2) DEFAULT 0.00,
    last_transaction_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- =======================
-- STREAM-PROCESSOR TABLES (Enhanced for compatibility)
-- =======================

-- Transactions table (compatible with stream-processor)
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(255) UNIQUE NOT NULL, -- Increased length for generator format
    account_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL, -- Renamed from transaction_type for consistency
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'MAD',
    merchant_name VARCHAR(255),
    merchant_category VARCHAR(100),
    description TEXT,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    source_location VARCHAR(255), -- Renamed from location
    timestamp TIMESTAMP NOT NULL,

    -- Additional fields for fraud detection (from stream-processor)
    ip_address VARCHAR(45),
    device_id VARCHAR(255),
    is_international BOOLEAN DEFAULT FALSE,
    risk_score DOUBLE PRECISION,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Legacy fields
    reference_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
);

-- Customer summaries table (from stream-processor)
CREATE TABLE IF NOT EXISTS customer_summaries (
    customer_id VARCHAR(50) PRIMARY KEY,
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- Enhanced fraud_alerts table (compatible with stream-processor)
CREATE TABLE IF NOT EXISTS fraud_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id VARCHAR(255) UNIQUE NOT NULL, -- Increased for stream-processor format
    transaction_id VARCHAR(255), -- Made nullable and increased length
    customer_id VARCHAR(50) NOT NULL,
    reason VARCHAR(50) NOT NULL, -- Renamed from alert_type
    description TEXT NOT NULL, -- Renamed from reason
    severity DOUBLE PRECISION NOT NULL, -- Changed to double precision
    amount DECIMAL(15,2),
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'NEW', -- Changed values to match stream-processor
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Legacy fields for backward compatibility
    risk_score INTEGER,
    investigated_by VARCHAR(100),
    investigated_at TIMESTAMP,
    resolution TEXT,

    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- Transaction metrics table (from stream-processor)
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

-- =======================
-- NOTIFICATION-SERVICE TABLES
-- =======================

-- Notifications table (enhanced for notification-service)
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(255) PRIMARY KEY, -- Using VARCHAR for UUID compatibility
    customer_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL, -- Renamed from notification_type
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    message TEXT,
    html_message TEXT,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'PENDING',
    recipient VARCHAR(255), -- email, phone, etc.
    external_id VARCHAR(255), -- ID from external service
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    scheduled_time TIMESTAMP,
    sent_at TIMESTAMP,
    source_alert_id VARCHAR(255), -- For fraud alerts
    transaction_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Legacy fields
    delivery_status VARCHAR(50),
    metadata JSONB,
    notification_id VARCHAR(50), -- Legacy field

    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- Customer contacts table (from notification-service)
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

-- Notification templates table (from notification-service)
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

-- =======================
-- INDEXES FOR PERFORMANCE
-- =======================

-- Transaction indexes
CREATE INDEX IF NOT EXISTS idx_transactions_customer_id ON transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);
CREATE INDEX IF NOT EXISTS idx_transactions_account_number ON transactions(account_number);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_amount ON transactions(amount);
CREATE INDEX IF NOT EXISTS idx_transactions_merchant_category ON transactions(merchant_category);

-- Fraud alert indexes
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_customer_id ON fraud_alerts(customer_id);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_transaction_id ON fraud_alerts(transaction_id);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_severity ON fraud_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_timestamp ON fraud_alerts(timestamp);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_reason ON fraud_alerts(reason);

-- Customer indexes
CREATE INDEX IF NOT EXISTS idx_customers_customer_id ON customers(customer_id);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(account_status);

-- Account indexes
CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);

-- Notification indexes
CREATE INDEX IF NOT EXISTS idx_notifications_customer_id ON notifications(customer_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled_time ON notifications(scheduled_time);

-- Metrics indexes
CREATE INDEX IF NOT EXISTS idx_transaction_metrics_window_start ON transaction_metrics(window_start);

-- Template indexes
CREATE INDEX IF NOT EXISTS idx_notification_templates_type_channel_lang ON notification_templates(type, channel, language);

-- =======================
-- VIEWS FOR ANALYTICS
-- =======================

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
LEFT JOIN transactions t ON fa.transaction_id = t.transaction_id
JOIN customers c ON fa.customer_id = c.customer_id
WHERE fa.status IN ('NEW', 'INVESTIGATING')
ORDER BY fa.created_at DESC;

-- High risk customers view
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

-- Notification analytics view
CREATE OR REPLACE VIEW notification_analytics AS
SELECT
    DATE(created_at) as notification_date,
    type,
    channel,
    status,
    COUNT(*) as count,
    COUNT(CASE WHEN status = 'SENT' THEN 1 END) as successful_count,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count,
    ROUND(
        COUNT(CASE WHEN status = 'SENT' THEN 1 END) * 100.0 / COUNT(*),
        2
    ) as success_rate
FROM notifications
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at), type, channel, status
ORDER BY notification_date DESC, type, channel;

-- =======================
-- SAMPLE DATA POPULATION
-- =======================

-- Insert sample customers (enhanced for all services)
INSERT INTO customers (customer_id, first_name, last_name, email, phone, address, city, country, date_of_birth, risk_score)
VALUES
    ('CUST000001', 'Ahmed', 'Benali', 'ahmed.benali@email.com', '+212600000001', '123 Hassan II Ave, Casablanca', 'Casablanca', 'Morocco', '1985-01-15', 0.15),
    ('CUST000002', 'Fatima', 'Alami', 'fatima.alami@email.com', '+212600000002', '456 Mohammed V St, Rabat', 'Rabat', 'Morocco', '1990-05-22', 0.25),
    ('CUST000003', 'Youssef', 'Tahiri', 'youssef.tahiri@email.com', '+212600000003', '789 Atlas Blvd, Marrakech', 'Marrakech', 'Morocco', '1978-11-08', 0.35),
    ('CUST000004', 'Aicha', 'Idrissi', 'aicha.idrissi@email.com', '+212600000004', '321 Almohades Ave, Fez', 'Fez', 'Morocco', '1992-03-12', 0.10),
    ('CUST000005', 'Omar', 'Nejjar', 'omar.nejjar@email.com', '+212600000005', '654 Corniche Rd, Agadir', 'Agadir', 'Morocco', '1988-07-30', 0.45)
ON CONFLICT (customer_id) DO UPDATE SET
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    updated_at = CURRENT_TIMESTAMP;

-- Generate customers for transaction generator (CUST000001 to CUST001000)
INSERT INTO customers (customer_id, first_name, last_name, email, risk_score, city, country)
SELECT
    'CUST' || LPAD(gs::TEXT, 6, '0') as customer_id,
    'Customer_' || gs AS first_name,
    'User_' || gs AS last_name,
    'cust' || LPAD(gs::TEXT, 6, '0') || '@bankingplatform.ma' AS email,
    (random() * 0.5)::DECIMAL(3,2) as risk_score, -- Random risk score 0.00-0.50
    CASE (gs % 10)
        WHEN 0 THEN 'Casablanca'
        WHEN 1 THEN 'Rabat'
        WHEN 2 THEN 'Marrakech'
        WHEN 3 THEN 'Fez'
        WHEN 4 THEN 'Tangier'
        WHEN 5 THEN 'Agadir'
        WHEN 6 THEN 'Meknes'
        WHEN 7 THEN 'Oujda'
        WHEN 8 THEN 'Kenitra'
        ELSE 'Tetouan'
    END as city,
    'Morocco' as country
FROM generate_series(1, 1000) as gs
ON CONFLICT (customer_id) DO NOTHING;

-- Insert sample accounts (compatible with transaction generator)
INSERT INTO accounts (account_number, customer_id, account_type, balance, currency)
VALUES
    ('ACC00000001', 'CUST000001', 'CHECKING', 5000.00, 'MAD'),
    ('ACC00000002', 'CUST000001', 'SAVINGS', 15000.00, 'MAD'),
    ('ACC00000003', 'CUST000002', 'CHECKING', 3500.00, 'MAD'),
    ('ACC00000004', 'CUST000003', 'CHECKING', 7200.00, 'MAD'),
    ('ACC00000005', 'CUST000003', 'SAVINGS', 25000.00, 'MAD')
ON CONFLICT (account_number) DO UPDATE SET
    balance = EXCLUDED.balance,
    updated_at = CURRENT_TIMESTAMP;

-- Generate accounts for transaction generator (ACC00000001 to ACC00001500)
INSERT INTO accounts (account_number, customer_id, account_type, balance, currency)
SELECT
    'ACC' || LPAD(gs::TEXT, 8, '0') as account_number,
    'CUST' || LPAD(((gs - 1) / 2 + 1)::TEXT, 6, '0') as customer_id,
    CASE WHEN gs % 2 = 0 THEN 'SAVINGS' ELSE 'CHECKING' END as account_type,
    (random() * 50000 + 1000)::DECIMAL(15,2) as balance,
    'MAD' as currency
FROM generate_series(1, 1500) as gs
ON CONFLICT (account_number) DO NOTHING;

-- Insert customer contacts (for notification service)
INSERT INTO customer_contacts (customer_id, email, phone_number, preferred_language, fraud_alerts_enabled, transaction_alerts_enabled)
SELECT
    customer_id,
    email,
    phone,
    CASE WHEN customer_id IN ('CUST000004') THEN 'fr' ELSE 'en' END,
    TRUE,
    TRUE
FROM customers
WHERE customer_id LIKE 'CUST%'
ON CONFLICT (customer_id) DO UPDATE SET
    email = EXCLUDED.email,
    phone_number = EXCLUDED.phone_number,
    updated_at = CURRENT_TIMESTAMP;

-- Insert notification templates
INSERT INTO notification_templates (id, template_id, type, channel, language, subject, body_template, html_template, description) VALUES
(uuid_generate_v4()::text, 'FRAUD_ALERT_EMAIL_EN', 'FRAUD_ALERT', 'EMAIL', 'en',
'🚨 URGENT: Fraud Alert - ${alertId}',
'Dear ${customerName},

FRAUD ALERT DETECTED
Alert ID: ${alertId}
Transaction: ${transactionId}
Reason: ${reason}
Description: ${description}
Amount: ${amount} MAD
Severity: ${severity}
Time: ${timestamp}

Action Required: ${actionRequired}

If you did not authorize this transaction, please contact us immediately.

Best regards,
Banking Platform Security Team',
'<html><body><h2 style="color: #e74c3c;">🚨 FRAUD ALERT</h2><p>Dear ${customerName},</p><div style="background: #f8f9fa; padding: 15px;"><p><strong>Alert ID:</strong> ${alertId}</p><p><strong>Amount:</strong> ${amount} MAD</p><p><strong>Reason:</strong> ${reason}</p></div></body></html>',
'Fraud alert email template in English')
ON CONFLICT (template_id) DO NOTHING;

-- =======================
-- TRIGGERS AND FUNCTIONS
-- =======================

-- Update timestamp trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
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

-- =======================
-- PERMISSIONS
-- =======================

-- Grant permissions to banking_user (in public schema)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO banking_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO banking_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO banking_user;

-- =======================
-- HEALTH CHECK VIEW
-- =======================

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
FROM notifications
UNION ALL
SELECT
    'customers' as table_name,
    COUNT(*) as record_count,
    MAX(created_at) as last_record
FROM customers;

-- Success message
DO $
BEGIN
    RAISE NOTICE 'Enhanced Banking Analytics database initialized successfully!';
    RAISE NOTICE 'Database: banking_analytics';
    RAISE NOTICE 'Compatible with: transaction-generator, stream-processor, notification-service, customer-service';
    RAISE NOTICE 'Schema: public (default)';
    RAISE NOTICE 'Sample customers: CUST000001 to CUST001000';
    RAISE NOTICE 'Sample accounts: ACC00000001 to ACC00001500';
END $;