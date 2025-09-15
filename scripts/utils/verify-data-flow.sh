#!/bin/bash
# verify-data-flow.sh - Comprehensive Data Flow Verification

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
KAFKA_CONTAINER="kafka"
POSTGRES_CONTAINER="postgresql"
REDIS_CONTAINER="redis"

print_status() {
    local status=$1
    local message=$2
    case $status in
        "INFO") echo -e "${BLUE}[INFO]${NC} $message" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS]${NC} $message" ;;
        "WARNING") echo -e "${YELLOW}[WARNING]${NC} $message" ;;
        "ERROR") echo -e "${RED}[ERROR]${NC} $message" ;;
    esac
}

# Function to generate test transaction
generate_test_transaction() {
    local tx_id="TEST-$(date +%s)-$(shuf -i 1000-9999 -n 1)"
    local customer_id="CUST00$(shuf -i 1-5 -n 1)"
    local amount=$(shuf -i 10-5000 -n 1)
    local timestamp=$(date -Iseconds)

    cat <<EOF
{
    "transactionId": "$tx_id",
    "customerId": "$customer_id",
    "accountNumber": "ACC${customer_id}001",
    "amount": $amount.00,
    "currency": "USD",
    "transactionType": "PURCHASE",
    "merchantName": "Test Merchant",
    "merchantCategory": "GROCERY",
    "description": "Test transaction for verification",
    "location": "Test City, Test State",
    "timestamp": "$timestamp",
    "status": "COMPLETED"
}
EOF
}

# Step 1: Verify Kafka Topics
verify_kafka_topics() {
    print_status "INFO" "Step 1: Verifying Kafka topics..."

    local required_topics=("transactions" "fraud-alerts" "notifications")
    local missing_topics=()

    print_status "INFO" "Listing existing topics:"
    local existing_topics=$(docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server localhost:29092 --list)
    echo "$existing_topics"

    for topic in "${required_topics[@]}"; do
        if ! echo "$existing_topics" | grep -q "^$topic$"; then
            missing_topics+=("$topic")
        fi
    done

    if [ ${#missing_topics[@]} -eq 0 ]; then
        print_status "SUCCESS" "All required Kafka topics exist"
    else
        print_status "WARNING" "Missing topics: ${missing_topics[*]}"
        print_status "INFO" "Creating missing topics..."

        for topic in "${missing_topics[@]}"; do
            docker exec $KAFKA_CONTAINER kafka-topics \
                --bootstrap-server localhost:29092 \
                --create --if-not-exists \
                --topic "$topic" \
                --partitions 3 \
                --replication-factor 1
        done
        print_status "SUCCESS" "Topics created successfully"
    fi
}

# Step 2: Test Transaction Generation
test_transaction_generation() {
    print_status "INFO" "Step 2: Testing transaction generation..."

    # Generate and send test transaction
    local test_transaction=$(generate_test_transaction)
    local tx_id=$(echo "$test_transaction" | jq -r '.transactionId')

    print_status "INFO" "Generating test transaction: $tx_id"

    # Send to Kafka
    echo "$test_transaction" | docker exec -i $KAFKA_CONTAINER \
        kafka-console-producer --bootstrap-server localhost:29092 --topic transactions

    print_status "SUCCESS" "Test transaction sent to Kafka"

    # Wait a moment for processing
    sleep 2

    # Verify message is in Kafka
    print_status "INFO" "Verifying message in Kafka..."
    local kafka_messages=$(timeout 10s docker exec $KAFKA_CONTAINER \
        kafka-console-consumer --bootstrap-server localhost:29092 \
        --topic transactions --from-beginning --max-messages 5 2>/dev/null | grep "$tx_id" || echo "")

    if [ -n "$kafka_messages" ]; then
        print_status "SUCCESS" "Test transaction found in Kafka"
    else
        print_status "WARNING" "Test transaction not found in recent Kafka messages"
    fi

    echo "$tx_id"
}

# Step 3: Verify Database Processing
verify_database_processing() {
    local test_tx_id=$1
    print_status "INFO" "Step 3: Verifying database processing..."

    # Wait for stream processor to handle the message
    print_status "INFO" "Waiting 10 seconds for stream processing..."
    sleep 10

    # Check if transaction exists in database
    local tx_count=$(docker exec $POSTGRES_CONTAINER \
        psql -U banking_user -d banking_analytics -t \
        -c "SELECT COUNT(*) FROM transactions WHERE transaction_id = '$test_tx_id';" \
        2>/dev/null | xargs || echo "0")

    if [ "$tx_count" -gt 0 ]; then
        print_status "SUCCESS" "Test transaction found in database"

        # Get transaction details
        docker exec $POSTGRES_CONTAINER \
            psql -U banking_user -d banking_analytics \
            -c "SELECT transaction_id, customer_id, amount, timestamp FROM transactions WHERE transaction_id = '$test_tx_id';" \
            2>/dev/null
    else
        print_status "WARNING" "Test transaction not found in database"
    fi

    # Show total transaction count
    local total_transactions=$(docker exec $POSTGRES_CONTAINER \
        psql -U banking_user -d banking_analytics -t \
        -c "SELECT COUNT(*) FROM transactions;" \
        2>/dev/null | xargs || echo "0")

    print_status "INFO" "Total transactions in database: $total_transactions"
}

# Step 4: Test Fraud Detection
test_fraud_detection() {
    print_status "INFO" "Step 4: Testing fraud detection..."

    # Generate a high-value transaction that should trigger fraud detection
    local high_value_tx=$(cat <<EOF
{
    "transactionId": "FRAUD-TEST-$(date +%s)",
    "customerId": "CUST001",
    "accountNumber": "ACC001001",
    "amount": 9999.99,
    "currency": "USD",
    "transactionType": "PURCHASE",
    "merchantName": "Suspicious Merchant",
    "merchantCategory": "UNKNOWN",
    "description": "High value test transaction",
    "location": "Unknown Location",
    "timestamp": "$(date -Iseconds)",
    "status": "COMPLETED"
}
EOF
)

    local fraud_tx_id=$(echo "$high_value_tx" | jq -r '.transactionId')
    print_status "INFO" "Generating high-value transaction: $fraud_tx_id"

    # Send to Kafka
    echo "$high_value_tx" | docker exec -i $KAFKA_CONTAINER \
        kafka-console-producer --bootstrap-server localhost:29092 --topic transactions

    # Wait for processing
    sleep 5

    # Check for fraud alerts
    local fraud_alerts=$(docker exec $POSTGRES_CONTAINER \
        psql -U banking_user -d banking_analytics -t \
        -c "SELECT COUNT(*) FROM fraud_alerts WHERE transaction_id = '$fraud_tx_id';" \
        2>/dev/null | xargs || echo "0")

    if [ "$fraud_alerts" -gt 0 ]; then
        print_status "SUCCESS" "Fraud alert generated for high-value transaction"

        # Show fraud alert details
        docker exec $POSTGRES_CONTAINER \
            psql -U banking_user -d banking_analytics \
            -c "SELECT alert_type, severity, reason FROM fraud_alerts WHERE transaction_id = '$fraud_tx_id';" \
            2>/dev/null
    else
        print_status "WARNING" "No fraud alert generated (may be normal depending on rules)"
    fi
}

# Step 5: Test Redis Caching
test_redis_caching() {
    print_status "INFO" "Step 5: Testing Redis caching..."

    # Test basic Redis operations
    docker exec $REDIS_CONTAINER redis-cli -a redis_pass set "test:verification" "$(date)" >/dev/null 2>&1
    local redis_value=$(docker exec $REDIS_CONTAINER redis-cli -a redis_pass get "test:verification" 2>/dev/null)

    if [ -n "$redis_value" ]; then
        print_status "SUCCESS" "Redis caching is working"
        print_status "INFO" "Test value stored: $redis_value"
    else
        print_status "ERROR" "Redis caching is not working"
        return 1
    fi

    # Show Redis info
    local redis_info=$(docker exec $REDIS_CONTAINER redis-cli -a redis_pass info memory 2>/dev/null | grep "used_memory_human")
    print_status "INFO" "Redis memory usage: $redis_info"

    # Clean up test key
    docker exec $REDIS_CONTAINER redis-cli -a redis_pass del "test:verification" >/dev/null 2>&1
}

# Step 6: Test Notifications
test_notifications() {
    print_status "INFO" "Step 6: Testing notification system..."

    # Check for any notifications in the database
    local notification_count=$(docker exec $POSTGRES_CONTAINER \
        psql -U banking_user -d banking_analytics -t \
        -c "SELECT COUNT(*) FROM notifications;" \
        2>/dev/null | xargs || echo "0")

    print_status "INFO" "Total notifications in database: $notification_count"

    # Check Kafka notifications topic
    print_status "INFO" "Checking notifications topic..."
    local notification_messages=$(timeout 5s docker exec $KAFKA_CONTAINER \
        kafka-console-consumer --bootstrap-server localhost:29092 \
        --topic notifications --from-beginning --max-messages 1 2>/dev/null || echo "")

    if [ -n "$notification_messages" ]; then
        print_status "SUCCESS" "Messages found in notifications topic"
    else
        print_status "INFO" "No messages in notifications topic (may be normal)"
    fi
}

# Step 7: Performance Metrics
show_performance_metrics() {
    print_status "INFO" "Step 7: Performance metrics..."

    # Kafka topics info
    print_status "INFO" "Kafka topic details:"
    docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server localhost:29092 --describe --topic transactions

    # Database statistics
    print_status "INFO" "Database table sizes:"
    docker exec $POSTGRES_CONTAINER psql -U banking_user -d banking_analytics \
        -c "SELECT
                schemaname,
                tablename,
                n_tup_ins as inserts,
                n_tup_upd as updates,
                n_tup_del as deletes
            FROM pg_stat_user_tables
            ORDER BY n_tup_ins DESC;" 2>/dev/null || true

    # Redis statistics
    print_status "INFO" "Redis statistics:"
    docker exec $REDIS_CONTAINER redis-cli -a redis_pass info stats 2>/dev/null | grep -E "(total_commands_processed|total_connections_received|keyspace)"
}

# Main execution
main() {
    print_status "INFO" "Starting comprehensive data flow verification..."
    echo "================================================================"

    # Run all verification steps
    verify_kafka_topics
    echo ""

    local test_tx_id=$(test_transaction_generation)
    echo ""

    verify_database_processing "$test_tx_id"
    echo ""

    test_fraud_detection
    echo ""

    test_redis_caching
    echo ""

    test_notifications
    echo ""

    show_performance_metrics

    echo "================================================================"
    print_status "SUCCESS" "Data flow verification completed!"
    print_status "INFO" "Check the logs for any errors: docker-compose -f docker-compose.dev.yml logs"
}

# Execute main function
main "$@"