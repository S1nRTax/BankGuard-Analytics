#!/bin/bash
# scripts/dev-workflow.sh

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if infrastructure is running
check_infrastructure() {
    echo -e "${YELLOW}Checking infrastructure status...${NC}"

    # Check if containers are running
    if ! docker ps --format "table {{.Names}}" | grep -q "kafka\|postgres\|redis"; then
        echo -e "${RED}Infrastructure services not found. Starting infrastructure...${NC}"
        return 1
    else
        echo -e "${GREEN}Infrastructure services are running.${NC}"
        return 0
    fi
}

# Function to start infrastructure
start_infrastructure() {
    echo -e "${YELLOW}Starting infrastructure services...${NC}"
    cd infrastructure
    docker-compose up -d
    cd ..

    # Wait for services to be healthy
    echo -e "${YELLOW}Waiting for services to be healthy...${NC}"
    sleep 10

    # Check health
    docker-compose -f infrastructure/docker-compose.yml ps
}

# Function to start individual service
start_service() {
    local service_name=$1

    if [ -z "$service_name" ]; then
        echo -e "${RED}Please specify a service name${NC}"
        echo "Available services: transaction-generator, stream-processor, customer-service, notification-service"
        exit 1
    fi

    # Check if infrastructure is running
    if ! check_infrastructure; then
        start_infrastructure
    fi

    echo -e "${YELLOW}Starting $service_name...${NC}"
    docker-compose -f docker-compose.dev.yml up --build $service_name
}

# Function to start all services
start_all() {
    # Start infrastructure first
    if ! check_infrastructure; then
        start_infrastructure
    fi

    echo -e "${YELLOW}Starting all application services...${NC}"
    docker-compose -f docker-compose.dev.yml up --build
}

# Function to stop everything
stop_all() {
    echo -e "${YELLOW}Stopping all services...${NC}"
    docker-compose -f docker-compose.dev.yml down
    docker-compose -f infrastructure/docker-compose.yml down
}

# Function to restart a specific service
restart_service() {
    local service_name=$1

    if [ -z "$service_name" ]; then
        echo -e "${RED}Please specify a service name${NC}"
        exit 1
    fi

    echo -e "${YELLOW}Restarting $service_name...${NC}"
    docker-compose -f docker-compose.dev.yml stop $service_name
    docker-compose -f docker-compose.dev.yml up --build -d $service_name
}

# Main script logic
case "$1" in
    "infrastructure")
        start_infrastructure
        ;;
    "service")
        start_service $2
        ;;
    "all")
        start_all
        ;;
    "stop")
        stop_all
        ;;
    "restart")
        restart_service $2
        ;;
    "status")
        echo "Infrastructure services:"
        docker-compose -f infrastructure/docker-compose.yml ps
        echo ""
        echo "Application services:"
        docker-compose -f docker-compose.dev.yml ps
        ;;
    *)
        echo "Usage: $0 {infrastructure|service <name>|all|stop|restart <name>|status}"
        echo ""
        echo "Commands:"
        echo "  infrastructure    - Start only infrastructure services"
        echo "  service <name>    - Start a specific application service"
        echo "  all              - Start infrastructure + all application services"
        echo "  stop             - Stop all services"
        echo "  restart <name>   - Restart a specific service"
        echo "  status           - Show status of all services"
        echo ""
        echo "Available services: transaction-generator, stream-processor, customer-service, notification-service"
        exit 1
        ;;
esac