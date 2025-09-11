#!/bin/bash

# Log management utility
# Usage: ./scripts/utils/logs.sh [service-name] [lines]

SERVICE=${1:-"all"}
LINES=${2:-50}

if [ "$SERVICE" = "all" ]; then
    echo "=== Infrastructure Logs ==="
    cd infrastructure && docker-compose logs --tail=$LINES
    echo ""
    echo "=== Application Logs ==="
    cd .. && docker-compose -f docker-compose.dev.yml logs --tail=$LINES
else
    # Check if it's an infrastructure or application service
    if docker-compose -f infrastructure/docker-compose.yml ps | grep -q "$SERVICE"; then
        echo "=== $SERVICE Logs (Infrastructure) ==="
        cd infrastructure && docker-compose logs --tail=$LINES $SERVICE
    elif docker-compose -f docker-compose.dev.yml ps | grep -q "$SERVICE"; then
        echo "=== $SERVICE Logs (Application) ==="
        docker-compose -f docker-compose.dev.yml logs --tail=$LINES $SERVICE
    else
        echo "Service '$SERVICE' not found"
        exit 1
    fi
fi