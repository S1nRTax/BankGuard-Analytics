SHELL := /bin/bash
.DEFAULT_GOAL := help

# Colors
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
NC := \033[0m

# Project directories
SCRIPTS_DIR := scripts
INFRA_DIR := infrastructure

.PHONY: help setup infrastructure dev-up dev-down service-up build logs status clean

help:
	@echo -e "$(BLUE)Banking Microservices Development Commands$(NC)"
	@echo -e "$(BLUE)===========================================$(NC)"
	@echo ""
	@echo -e "$(GREEN)Quick Start:$(NC)"
	@echo "  make setup              - Setup project (create directories, make scripts executable)"
	@echo "  make dev-full           - Start everything (infrastructure + all services)"
	@echo ""
	@echo -e "$(GREEN)Infrastructure:$(NC)"
	@echo "  make infrastructure     - Start infrastructure services"
	@echo "  make infra-down        - Stop infrastructure services"
	@echo "  make infra-logs        - Show infrastructure logs"
	@echo ""
	@echo -e "$(GREEN)Development:$(NC)"
	@echo "  make dev-up            - Start all application services"
	@echo "  make dev-down          - Stop all application services"
	@echo "  make build             - Build all services"
	@echo ""
	@echo -e "$(GREEN)Individual Services:$(NC)"
	@echo "  make transaction       - Start transaction-generator"
	@echo "  make stream           - Start stream-processor"
	@echo "  make customer         - Start customer-service"
	@echo "  make notification     - Start notification-service"
	@echo ""
	@echo -e "$(GREEN)Utilities:$(NC)"
	@echo "  make logs SERVICE=<n>  - Show logs for specific service"
	@echo "  make status           - Show status of all services"
	@echo "  make clean            - Clean up everything"
	@echo "  make health           - Check health of all services"
	@echo ""
	@echo -e "$(GREEN)Alternative (using scripts):$(NC)"
	@echo "  ./scripts/dev-workflow.sh {infrastructure|service <name>|all|stop|status}"

# Setup project
setup:
	@echo -e "$(YELLOW)Setting up project structure...$(NC)"
	@mkdir -p $(SCRIPTS_DIR)/utils
	@chmod +x $(SCRIPTS_DIR)/*.sh 2>/dev/null || true
	@echo -e "$(GREEN)Project setup complete!$(NC)"

# Infrastructure management
infrastructure:
	@echo -e "$(YELLOW)Starting infrastructure services...$(NC)"
	@cd $(INFRA_DIR) && docker-compose up -d
	@echo -e "$(YELLOW)Waiting for services to be ready...$(NC)"
	@sleep 15
	@$(MAKE) infra-status

infra-down:
	@echo -e "$(YELLOW)Stopping infrastructure services...$(NC)"
	@cd $(INFRA_DIR) && docker-compose down

infra-logs:
	@cd $(INFRA_DIR) && docker-compose logs -f

infra-status:
	@echo -e "$(BLUE)Infrastructure Services Status:$(NC)"
	@cd $(INFRA_DIR) && docker-compose ps

# Development commands
dev-up:
	@echo -e "$(YELLOW)Starting application services...$(NC)"
	@docker-compose -f docker-compose.dev.yml up -d
	@$(MAKE) dev-status

dev-down:
	@echo -e "$(YELLOW)Stopping application services...$(NC)"
	@docker-compose -f docker-compose.dev.yml down

dev-full: infrastructure
	@echo -e "$(YELLOW)Starting all application services...$(NC)"
	@sleep 5
	@docker-compose -f docker-compose.dev.yml up -d
	@$(MAKE) status

dev-status:
	@echo -e "$(BLUE)Application Services Status:$(NC)"
	@docker-compose -f docker-compose.dev.yml ps

# Individual service commands
transaction:
	@if [ -f $(SCRIPTS_DIR)/dev-workflow.sh ]; then \
		$(SCRIPTS_DIR)/dev-workflow.sh service transaction-generator; \
	else \
		$(MAKE) check-infrastructure && docker-compose -f docker-compose.dev.yml up --build transaction-generator; \
	fi

stream:
	@if [ -f $(SCRIPTS_DIR)/dev-workflow.sh ]; then \
		$(SCRIPTS_DIR)/dev-workflow.sh service stream-processor; \
	else \
		$(MAKE) check-infrastructure && docker-compose -f docker-compose.dev.yml up --build stream-processor; \
	fi

customer:
	@if [ -f $(SCRIPTS_DIR)/dev-workflow.sh ]; then \
		$(SCRIPTS_DIR)/dev-workflow.sh service customer-service; \
	else \
		$(MAKE) check-infrastructure && docker-compose -f docker-compose.dev.yml up --build customer-service; \
	fi

notification:
	@if [ -f $(SCRIPTS_DIR)/dev-workflow.sh ]; then \
		$(SCRIPTS_DIR)/dev-workflow.sh service notification-service; \
	else \
		$(MAKE) check-infrastructure && docker-compose -f docker-compose.dev.yml up --build notification-service; \
	fi

# Build commands
build:
	@echo -e "$(YELLOW)Building all application services...$(NC)"
	@docker-compose -f docker-compose.dev.yml build

build-service:
ifndef SERVICE
	@echo -e "$(RED)Please specify SERVICE=<service-name>$(NC)"
	@echo "Example: make build-service SERVICE=transaction-generator"
else
	@echo -e "$(YELLOW)Building $(SERVICE)...$(NC)"
	@docker-compose -f docker-compose.dev.yml build $(SERVICE)
endif

# Utility commands
logs:
ifdef SERVICE
	@docker-compose -f docker-compose.dev.yml logs -f $(SERVICE)
else
	@echo -e "$(RED)Please specify SERVICE=<service-name>$(NC)"
	@echo "Example: make logs SERVICE=transaction-generator"
	@echo "Available services: transaction-generator, stream-processor, customer-service, notification-service"
endif

status:
	@echo -e "$(BLUE)=== Infrastructure Services ===$(NC)"
	@cd $(INFRA_DIR) && docker-compose ps
	@echo ""
	@echo -e "$(BLUE)=== Application Services ===$(NC)"
	@docker-compose -f docker-compose.dev.yml ps

health:
	@if [ -f $(SCRIPTS_DIR)/utils/health-check.sh ]; then \
		$(SCRIPTS_DIR)/utils/health-check.sh; \
	else \
		echo -e "$(YELLOW)Running basic health check...$(NC)"; \
		$(MAKE) status; \
	fi

clean:
	@echo -e "$(YELLOW)Cleaning up all services, networks, and volumes...$(NC)"
	@docker-compose -f docker-compose.dev.yml down -v --remove-orphans 2>/dev/null || true
	@cd $(INFRA_DIR) && docker-compose down -v --remove-orphans 2>/dev/null || true
	@docker system prune -f
	@echo -e "$(GREEN)Cleanup complete!$(NC)"

# Internal helper
check-infrastructure:
	@if ! docker ps --format "table {{.Names}}" | grep -q "kafka\|postgres\|redis"; then \
		echo -e "$(YELLOW)Infrastructure not running. Starting infrastructure...$(NC)"; \
		$(MAKE) infrastructure; \
	else \
		echo -e "$(GREEN)Infrastructure is running.$(NC)"; \
	fi

# Development shortcuts
restart:
ifndef SERVICE
	@echo -e "$(RED)Please specify SERVICE=<service-name>$(NC)"
else
	@echo -e "$(YELLOW)Restarting $(SERVICE)...$(NC)"
	@docker-compose -f docker-compose.dev.yml restart $(SERVICE)
endif

# Quick service testing
test-service:
ifndef SERVICE
	@echo -e "$(RED)Please specify SERVICE=<service-name>$(NC)"
else
	@echo -e "$(YELLOW)Testing $(SERVICE)...$(NC)"
	@if [ -f $(SCRIPTS_DIR)/test.sh ]; then \
		$(SCRIPTS_DIR)/test.sh $(SERVICE); \
	else \
		echo "No test script found. Please create $(SCRIPTS_DIR)/test.sh"; \
	fi
endif