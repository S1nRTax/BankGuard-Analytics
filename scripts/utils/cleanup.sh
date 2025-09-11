#!/bin/bash

# Cleanup utility script
# Usage: ./scripts/utils/cleanup.sh [--force]

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

FORCE=${1:-""}

if [ "$FORCE" != "--force" ]; then
    echo -e "${YELLOW}This will stop all services and remove containers, networks, and volumes.${NC}"
    echo -e "${RED}This action cannot be undone!${NC}"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cleanup cancelled."
        exit 1
    fi
fi

echo -e "${YELLOW}Stopping and removing all services...${NC}"

# Stop application services
docker-compose -f docker-compose.dev.yml down -v --remove-orphans 2>/dev/null || true

# Stop infrastructure services
cd infrastructure && docker-compose down -v --remove-orphans 2>/dev/null || true

# Clean up Docker system
echo -e "${YELLOW}Cleaning up Docker system...${NC}"
docker system prune -f

# Remove dangling images
echo -e "${YELLOW}Removing dangling images...${NC}"
docker image prune -f

echo -e "${GREEN}Cleanup complete!${NC}"