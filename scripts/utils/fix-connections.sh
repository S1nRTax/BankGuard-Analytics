#!/bin/bash
# fix-connections.sh - Fix Redis and Database connection issues

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

print_status "INFO" "🔧 Fixing connection issues for stream-processor..."

# Step 1: Check if Redis is accessible from the network
print_status "INFO" "📡 Testing Redis connectivity..."
if docker exec stream-processor sh -c 'nc -zv redis 6379' 2>/dev/null; then
    print_status "SUCCESS" "Redis is accessible from stream-processor"
else
    print_status "ERROR" "Redis is NOT accessible from stream-processor"
    print_status "INFO" "Checking Redis container status..."
    docker ps | grep redis || print_status "ERROR" "Redis container not running"
fi

# Step 2: Check if PostgreSQL is accessible
print_status "INFO" "🐘 Testing PostgreSQL connectivity..."
if docker exec stream-processor sh -c 'nc -zv postgresql 5432' 2>/dev/null; then
    print_status "SUCCESS" "PostgreSQL is accessible from stream-processor"
else
    print_status "ERROR" "PostgreSQL is NOT accessible from stream-processor"
    print_status "INFO" "Checking PostgreSQL container status..."
    docker ps | grep postgresql || print_status "ERROR" "PostgreSQL container not running"
fi

# Step 3: Check network connectivity
print_status "INFO" "🌐 Checking network connectivity..."
NETWORK_NAME="banking-platform-network"

# Get containers connected to the network
print_status "INFO" "Containers in $NETWORK_NAME network:"
docker network inspect $NETWORK_NAME --format='{{range .Containers}}{{.Name}} {{end}}' | tr ' ' '\n' | grep -v '^$' | while read container; do
    if [ ! -z "$container" ]; then
        print_status "INFO" "  ✓ $container"
    fi
done

# Step 4: Test actual connections
print_status "INFO" "🔍 Testing actual connections from stream-processor..."

# Test Redis connection
print_status "INFO" "Testing Redis connection with authentication..."
if docker exec redis redis-cli -a redis_pass ping >/dev/null 2>&1; then
    print_status "SUCCESS" "Redis ping successful"

    # Test from stream-processor container
    if docker exec stream-processor sh -c 'timeout 5 bash -c "</dev/tcp/redis/6379"' 2>/dev/null; then
        print_status "SUCCESS" "Redis port 6379 is reachable from stream-processor"
    else
        print_status "ERROR" "Redis port 6379 is NOT reachable from stream-processor"
    fi
else
    print_status "ERROR" "Redis ping failed"
fi

# Test PostgreSQL connection
print_status "INFO" "Testing PostgreSQL connection..."
if docker exec postgresql pg_isready -h localhost -U banking_user -d banking_analytics >/dev/null 2>&1; then
    print_status "SUCCESS" "PostgreSQL is ready"

    # Test from stream-processor container
    if docker exec stream-processor sh -c 'timeout 5 bash -c "</dev/tcp/postgresql/5432"' 2>/dev/null; then
        print_status "SUCCESS" "PostgreSQL port 5432 is reachable from stream-processor"
    else
        print_status "ERROR" "PostgreSQL port 5432 is NOT reachable from stream-processor"
    fi
else
    print_status "ERROR" "PostgreSQL is not ready"
fi

# Step 5: Check environment variables in stream-processor
print_status "INFO" "🔧 Checking environment variables in stream-processor..."
echo "Environment variables:"
docker exec stream-processor env | grep -E "(REDIS|POSTGRES|KAFKA)" | sort

# Step 6: Restart stream-processor with proper wait
print_status "INFO" "🔄 Restarting stream-processor..."
docker-compose -f docker-compose.dev.yml stop stream-processor
sleep 5

print_status "INFO" "Waiting for dependencies to be ready..."
# Wait for Redis
echo -n "Waiting for Redis"
while ! docker exec redis redis-cli -a redis_pass ping >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
print_status "SUCCESS" "Redis is ready"

# Wait for PostgreSQL
echo -n "Waiting for PostgreSQL"
while ! docker exec postgresql pg_isready -h localhost -U banking_user -d banking_analytics >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
print_status "SUCCESS" "PostgreSQL is ready"

# Wait for Kafka
echo -n "Waiting for Kafka"
while ! docker exec kafka kafka-topics --bootstrap-server localhost:29092 --list >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
print_status "SUCCESS" "Kafka is ready"

print_status "INFO" "Starting stream-processor..."
docker-compose -f docker-compose.dev.yml up -d stream-processor

print_status "INFO" "Waiting for stream-processor to start..."
sleep 10

# Check logs for errors
print_status "INFO" "📋 Checking recent logs..."
docker logs stream-processor --tail=20

# Step 7: Test connections after restart
print_status "INFO" "🧪 Final connection tests..."

# Test if application started successfully
if docker ps | grep stream-processor | grep -q "Up"; then
    print_status "SUCCESS" "Stream-processor container is running"

    # Check health endpoint
    sleep 5
    if docker exec stream-processor curl -s http://localhost:8082/actuator/health >/dev/null 2>&1; then
        print_status "SUCCESS" "Stream-processor health endpoint is responding"
    else
        print_status "WARNING" "Stream-processor health endpoint not yet ready"
    fi
else
    print_status "ERROR" "Stream-processor container failed to start"
fi

print_status "INFO" "✅ Connection fix script completed!"
print_status "INFO" "Monitor logs with: docker logs stream-processor -f"