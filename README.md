# BankGuard-Analytics

BankGuard-Analytics is a platform for managing and analyzing financial data.

---

## Usage:
### -Infrastructure Management
   - Start infrastructure services only: `make infrastructure`  
   - Stop infrastructure services: `make infra-down`                  
   - Show infrastructure logs: `make infra-logs`                   
   - Show infrastructure status: `make infra-status`                 

### -Development Workflow
  - Start infrastructure + all application services: `make dev-full`
  - Start application services (requires infrastructure): `make dev-up`
  - Stop application services: `make dev-down` 
  - Build all application services: `make build`

### -Utilities
  - Show logs for a specific service: `make logs SERVICE=<name>`
  - Show status of all services: `make status`                       
  - Run comprehensive health check: `make health`                       
  - Remove containers, volumes, `networks: make clean`                   
  - Restart a specific service: `make restart SERVICE=<name>`     
  - Build a specific service: `make build-service SERVICE=<name>`

### -Testing
  - Run tests for a specific service: `make test-service SERVICE=<name>` 
