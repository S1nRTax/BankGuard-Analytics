#!/bin/bash

# Health check utility script
# Usage: ./scripts/utils/health-check.sh

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Health Check Report ===${NC}"
echo -e "Timestamp: $(date)"
echo ""

# Check infrastructure services
echo -e "${BLUE}Infrastructure Services:${NC}"
services=("kafka" "postgres" "redis" "zookeeper")

for service in "${services[@]}"; do
    if docker ps --format "table {{.Names}}" | grep -q "$service"; then
        # Check if service is healthy
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "$service" 2>/dev/null)
        if [ "$health_status" = "healthy" ]; then
            echo -e "  ✅ $service: ${GREEN}healthy${NC}"
        elif [ "$health_status" = "unhealthy" ]; then
            echo -e "  ❌ $service: ${RED}unhealthy${NC}"
        else
            echo -e "  ⚠️  $service: ${YELLOW}running (no health check)${NC}"
        fi
    else
        echo -e "  ❌ $service: ${RED}not running${NC}"
    fi
done

echo ""

# Check application services
echo -e "${BLUE}Application Services:${NC}"
app_services=("transaction-generator" "stream-processor" "customer-service" "notification-service")

for service in "${app_services[@]}"; do
    if docker ps --format "table {{.Names}}" | grep -q "$service"; then
        # Check service health via HTTP if ports are exposed
        case $service in
            "transaction-generator")
                if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
                    echo -e "  ✅ $service: ${GREEN}healthy (port 8080)${NC}"
                else
                    echo -e "  ⚠️  $service: ${YELLOW}running but health check failed${NC}"
                fi
                ;;
            "customer-service")
                if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
                    echo -e "  ✅ $service: ${GREEN}healthy (port 8081)${NC}"
                else
                    echo -e "  ⚠️  $service: ${YELLOW}running but health check failed${NC}"
                fi
                ;;
            "notification-service")
                if curl -s http://localhost:8082/actuator/health > /dev/null 2>&1; then
                    echo -e "  ✅ $service: ${GREEN}healthy (port 8082)${NC}"
                else
                    echo -e "  ⚠️  $service: ${YELLOW}running but health check failed${NC}"
                fi
                ;;
            "stream-processor")
                if curl -s http://localhost:8083/actuator/health > /dev/null 2>&1; then
                    echo -e "  ✅ $service: ${GREEN}healthy (port 8083)${NC}"
                else
                    echo -e "  ⚠️  $service: ${YELLOW}running but health check failed${NC}"
                fi
                ;;
        esac
    else
        echo -e "  ❌ $service: ${RED}not running${NC}"
    fi
done

echo ""
echo -e "${BLUE}=== Network Status ===${NC}"
if docker network ls | grep -q "infrastructure_banking-platform-network"; then
    echo -e "  ✅ banking-network: ${GREEN}exists${NC}"
else
    echo -e "  ❌ banking-network: ${RED}missing${NC}"
fi