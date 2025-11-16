#!/bin/bash

# PostgreSQL Database Setup Script
# This script helps you set up PostgreSQL for the XSD Service Platform

set -e

echo "=================================================="
echo "  XSD Service Platform - PostgreSQL Setup"
echo "=================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed.${NC}"
    echo "Please install Docker first: https://docs.docker.com/get-docker/"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not installed.${NC}"
    echo "Please install Docker Compose first: https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✓ Docker and Docker Compose are installed${NC}"
echo ""

# Check if PostgreSQL is already running
if docker ps | grep -q xsd-platform-postgres; then
    echo -e "${YELLOW}PostgreSQL container is already running${NC}"
    read -p "Do you want to restart it? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Stopping existing container..."
        docker-compose down
    else
        echo "Keeping existing container running"
        exit 0
    fi
fi

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo -e "${YELLOW}Creating .env file from .env.example...${NC}"
    cp .env.example .env
    echo -e "${GREEN}✓ .env file created${NC}"
    echo -e "${YELLOW}Note: Please update the .env file with your actual values before production use${NC}"
    echo ""
fi

# Start PostgreSQL
echo "Starting PostgreSQL with Docker Compose..."
docker-compose up -d postgres

echo ""
echo "Waiting for PostgreSQL to be ready..."
sleep 5

# Check if PostgreSQL is healthy
RETRIES=30
while [ $RETRIES -gt 0 ]; do
    if docker exec xsd-platform-postgres pg_isready -U postgres > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PostgreSQL is ready!${NC}"
        break
    fi
    echo "Waiting for PostgreSQL to start... ($RETRIES retries left)"
    sleep 2
    RETRIES=$((RETRIES - 1))
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${RED}Error: PostgreSQL failed to start${NC}"
    echo "Check logs with: docker-compose logs postgres"
    exit 1
fi

echo ""
echo "=================================================="
echo -e "${GREEN}PostgreSQL Setup Complete!${NC}"
echo "=================================================="
echo ""
echo "Database Connection Details:"
echo "  Host:     localhost"
echo "  Port:     5432"
echo "  Database: xsdplatform"
echo "  Username: postgres"
echo "  Password: postgres"
echo ""
echo "pgAdmin (Database Management UI):"
echo "  URL:      http://localhost:5050"
echo "  Email:    admin@admin.com"
echo "  Password: admin"
echo ""
echo "Commands:"
echo "  Start:    docker-compose up -d postgres"
echo "  Stop:     docker-compose down"
echo "  Logs:     docker-compose logs -f postgres"
echo "  Connect:  docker exec -it xsd-platform-postgres psql -U postgres -d xsdplatform"
echo ""
echo "You can now run the Spring Boot application!"
echo "  cd backend"
echo "  mvn spring-boot:run"
echo ""
