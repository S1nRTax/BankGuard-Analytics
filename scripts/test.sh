#!/bin/bash

# Testing script for individual services
# Usage: ./scripts/test.sh [service-name]

SERVICE=${1:-"all"}
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

test_service() {
    local service=$1
    local port=$2

    echo -e "${YELLOW}Testing $service...${NC}"

    # Wait for service to be ready
    echo "Waiting for $service to be ready..."
    sleep 10

    # Test health endpoint
    if curl -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $service health check passed${NC}"
    else
        echo -e "${RED}❌ $service health check failed${NC}"
        return 1
    fi

    # Test info endpoint
    if curl -f http://localhost:$port/actuator/info > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $service info endpoint accessible${NC}"
    else
        echo -e "${YELLOW}⚠️  $service info endpoint not accessible${NC}"
    fi
}

case $SERVICE in
    "transaction-generator")
        test_service "transaction-generator" 8080
        ;;
    "customer-service")
        test_service "customer-service" 8081
        ;;
    "notification-service")
        test_service "notification-service" 8082
        ;;
    "stream-processor")
        test_service "stream-processor" 8083
        ;;
    "all")
        echo -e "${BLUE}Testing all services...${NC}"
        test_service "transaction-generator" 8080
        test_service "customer-service" 8081
        test_service "notification-service" 8082
        test_service "stream-processor" 8083
        ;;
    *)
        echo -e "${RED}Unknown service: $SERVICE${NC}"
        echo "Available services: transaction-generator, customer-service, notification-service, stream-processor"
        exit 1
        ;;
esac