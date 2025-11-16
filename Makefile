.PHONY: help build up down restart logs clean test backup restore

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
NC := \033[0m # No Color

## help: Display this help message
help:
	@echo "$(BLUE)XSD Service Generation Platform - Docker Commands$(NC)"
	@echo ""
	@echo "$(GREEN)Available targets:$(NC)"
	@awk 'BEGIN {FS = ":.*##"; printf ""} /^[a-zA-Z_-]+:.*?##/ { printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(BLUE)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Development

## dev: Start development environment
dev:
	@echo "$(GREEN)Starting development environment...$(NC)"
	docker-compose --profile development up -d
	@echo "$(GREEN)Services started:$(NC)"
	@echo "  Frontend:  http://localhost:3000"
	@echo "  Backend:   http://localhost:8080"
	@echo "  Swagger:   http://localhost:8080/swagger-ui.html"
	@echo "  pgAdmin:   http://localhost:5050"

## dev-build: Build and start development environment
dev-build:
	@echo "$(GREEN)Building development environment...$(NC)"
	docker-compose --profile development build
	docker-compose --profile development up -d

## dev-logs: Follow development logs
dev-logs:
	docker-compose logs -f backend frontend

##@ Production

## prod: Start production environment
prod:
	@echo "$(GREEN)Starting production environment...$(NC)"
	docker-compose --profile production up -d
	@echo "$(GREEN)Services started:$(NC)"
	@echo "  Application: http://localhost"
	@echo "  Backend API: http://localhost/api"

## prod-build: Build and start production environment
prod-build:
	@echo "$(GREEN)Building production environment...$(NC)"
	docker-compose --profile production build
	docker-compose --profile production up -d

##@ Docker Operations

## build: Build all services
build:
	@echo "$(GREEN)Building all services...$(NC)"
	docker-compose build

## up: Start all services
up:
	@echo "$(GREEN)Starting all services...$(NC)"
	docker-compose up -d

## down: Stop all services
down:
	@echo "$(YELLOW)Stopping all services...$(NC)"
	docker-compose down

## restart: Restart all services
restart: down up

## ps: Show service status
ps:
	docker-compose ps

## logs: View logs
logs:
	docker-compose logs -f

## stats: Show resource usage
stats:
	docker stats

##@ Specific Services

## backend-logs: View backend logs
backend-logs:
	docker-compose logs -f backend

## frontend-logs: View frontend logs
frontend-logs:
	docker-compose logs -f frontend

## db-logs: View database logs
db-logs:
	docker-compose logs -f postgres

## backend-shell: Access backend container shell
backend-shell:
	docker-compose exec backend sh

## db-shell: Access PostgreSQL shell
db-shell:
	docker-compose exec postgres psql -U postgres xsdplatform

##@ Maintenance

## clean: Stop services and remove volumes
clean:
	@echo "$(YELLOW)Stopping services and removing volumes...$(NC)"
	docker-compose down -v
	@echo "$(GREEN)Cleanup complete$(NC)"

## prune: Clean up Docker system
prune:
	@echo "$(YELLOW)Cleaning up Docker system...$(NC)"
	docker system prune -f
	docker volume prune -f
	@echo "$(GREEN)System cleanup complete$(NC)"

## reset: Complete reset (stop, clean, rebuild)
reset: clean
	@echo "$(YELLOW)Performing complete reset...$(NC)"
	docker-compose build --no-cache
	docker-compose up -d
	@echo "$(GREEN)Reset complete$(NC)"

##@ Database

## db-backup: Backup database
db-backup:
	@echo "$(GREEN)Backing up database...$(NC)"
	@mkdir -p backups
	docker-compose exec -T postgres pg_dump -U postgres xsdplatform > backups/backup-$$(date +%Y%m%d-%H%M%S).sql
	@echo "$(GREEN)Database backup created$(NC)"

## db-restore: Restore database from latest backup
db-restore:
	@echo "$(YELLOW)Restoring database from latest backup...$(NC)"
	@LATEST=$$(ls -t backups/*.sql | head -1); \
	if [ -z "$$LATEST" ]; then \
		echo "$(YELLOW)No backup files found$(NC)"; \
	else \
		echo "Restoring from $$LATEST"; \
		docker-compose exec -T postgres psql -U postgres xsdplatform < $$LATEST; \
		echo "$(GREEN)Database restored$(NC)"; \
	fi

##@ Testing

## test: Run tests
test:
	@echo "$(GREEN)Running backend tests...$(NC)"
	cd backend && ./mvnw test

## health: Check service health
health:
	@echo "$(GREEN)Checking service health...$(NC)"
	@echo -n "Backend: "
	@curl -sf http://localhost:8080/actuator/health > /dev/null && echo "$(GREEN)✓$(NC)" || echo "$(YELLOW)✗$(NC)"
	@echo -n "Frontend: "
	@curl -sf http://localhost:3000 > /dev/null && echo "$(GREEN)✓$(NC)" || echo "$(YELLOW)✗$(NC)"
	@echo -n "Database: "
	@docker-compose exec -T postgres pg_isready -U postgres > /dev/null && echo "$(GREEN)✓$(NC)" || echo "$(YELLOW)✗$(NC)"

##@ Setup

## init: Initialize project (copy env, build, start)
init:
	@echo "$(GREEN)Initializing project...$(NC)"
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(GREEN)Created .env file from .env.example$(NC)"; \
		echo "$(YELLOW)Please review and update .env file$(NC)"; \
	fi
	@$(MAKE) dev-build
	@echo "$(GREEN)Initialization complete!$(NC)"
