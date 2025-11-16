# Setup Guide - Dynamic XSD Service Generation Platform

This guide provides detailed step-by-step instructions for setting up the Dynamic XSD Service Generation Platform on your local machine or server.

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Installation Steps](#installation-steps)
3. [Database Configuration](#database-configuration)
4. [Backend Setup](#backend-setup)
5. [Frontend Setup](#frontend-setup)
6. [Docker Setup](#docker-setup)
7. [Verification](#verification)
8. [Common Issues](#common-issues)

---

## System Requirements

### Hardware Requirements

**Minimum:**
- CPU: 2 cores
- RAM: 4 GB
- Disk Space: 10 GB

**Recommended:**
- CPU: 4+ cores
- RAM: 8+ GB
- Disk Space: 20+ GB
- SSD storage

### Software Requirements

| Software | Version | Download Link |
|----------|---------|---------------|
| Java (JDK) | 21 | https://adoptium.net/ |
| Node.js | 18+ | https://nodejs.org/ |
| PostgreSQL | 15+ | https://www.postgresql.org/download/ |
| Maven | 3.8+ | https://maven.apache.org/download.cgi |
| Git | Latest | https://git-scm.com/downloads |

### Optional (for Docker deployment)
| Software | Version | Download Link |
|----------|---------|---------------|
| Docker | Latest | https://www.docker.com/get-started |
| Docker Compose | Latest | Included with Docker Desktop |

---

## Installation Steps

### Step 1: Install Java 21

#### On Ubuntu/Debian:
```bash
# Install OpenJDK 21
sudo apt update
sudo apt install openjdk-21-jdk -y

# Verify installation
java -version
javac -version
```

#### On macOS (using Homebrew):
```bash
# Install via Homebrew
brew install openjdk@21

# Link Java
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk

# Verify installation
java -version
```

#### On Windows:
1. Download from https://adoptium.net/
2. Run installer
3. Add to PATH: `C:\Program Files\Eclipse Adoptium\jdk-21.x.x\bin`
4. Verify in Command Prompt: `java -version`

---

### Step 2: Install Node.js 18+

#### On Ubuntu/Debian:
```bash
# Using NodeSource repository
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Verify installation
node -v
npm -v
```

#### On macOS (using Homebrew):
```bash
brew install node@18
node -v
npm -v
```

#### On Windows:
1. Download from https://nodejs.org/
2. Run installer (choose LTS version)
3. Verify in Command Prompt: `node -v` and `npm -v`

---

### Step 3: Install PostgreSQL 15+

#### On Ubuntu/Debian:
```bash
# Add PostgreSQL repository
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -

# Install PostgreSQL
sudo apt update
sudo apt install postgresql-15 postgresql-contrib-15 -y

# Start PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify installation
sudo -u postgres psql --version
```

#### On macOS (using Homebrew):
```bash
brew install postgresql@15
brew services start postgresql@15
psql --version
```

#### On Windows:
1. Download from https://www.postgresql.org/download/windows/
2. Run installer
3. Remember the password you set for `postgres` user
4. Verify: Open SQL Shell (psql) and check version

---

### Step 4: Install Maven 3.8+

#### On Ubuntu/Debian:
```bash
sudo apt install maven -y
mvn -version
```

#### On macOS (using Homebrew):
```bash
brew install maven
mvn -version
```

#### On Windows:
1. Download from https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH: `C:\Program Files\Apache\maven\bin`
4. Verify: `mvn -version`

---

### Step 5: Install Git

#### On Ubuntu/Debian:
```bash
sudo apt install git -y
git --version
```

#### On macOS (using Homebrew):
```bash
brew install git
git --version
```

#### On Windows:
1. Download from https://git-scm.com/download/win
2. Run installer (use default options)
3. Verify: `git --version`

---

## Database Configuration

### Create Database and User

#### Option 1: Using psql Command Line

```bash
# Switch to postgres user
sudo -u postgres psql

# In psql prompt, run:
CREATE DATABASE xsd_platform;
CREATE USER xsd_user WITH PASSWORD 'YourSecurePassword123!';
GRANT ALL PRIVILEGES ON DATABASE xsd_platform TO xsd_user;

# Grant schema permissions (PostgreSQL 15+)
\c xsd_platform
GRANT ALL ON SCHEMA public TO xsd_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO xsd_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO xsd_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO xsd_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO xsd_user;

# Exit
\q
```

#### Option 2: Using createdb and createuser commands

```bash
# Create database
sudo -u postgres createdb xsd_platform

# Create user
sudo -u postgres psql -c "CREATE USER xsd_user WITH PASSWORD 'YourSecurePassword123!';"

# Grant privileges
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE xsd_platform TO xsd_user;"
```

### Test Database Connection

```bash
# Test connection
psql -h localhost -U xsd_user -d xsd_platform

# If successful, you'll see:
# xsd_platform=>

# Exit with \q
```

### Configure PostgreSQL for Network Access (Optional)

If you're running PostgreSQL on a different server:

1. Edit `postgresql.conf`:
```bash
sudo nano /etc/postgresql/15/main/postgresql.conf

# Find and update:
listen_addresses = '*'  # Or specific IP
```

2. Edit `pg_hba.conf`:
```bash
sudo nano /etc/postgresql/15/main/pg_hba.conf

# Add line:
host    xsd_platform    xsd_user    0.0.0.0/0    md5
```

3. Restart PostgreSQL:
```bash
sudo systemctl restart postgresql
```

---

## Backend Setup

### Step 1: Clone Repository

```bash
git clone https://github.com/your-org/elastic-cluster-had.git
cd elastic-cluster-had/backend
```

### Step 2: Configure Application

Edit `src/main/resources/application.yml`:

```bash
nano src/main/resources/application.yml
```

Update database credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/xsd_platform
    username: xsd_user
    password: YourSecurePassword123!
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
      continue-on-error: false

server:
  port: 8080

app:
  xsd:
    upload-dir: ./uploads
    generated-src-dir: ./generated-sources
    compiled-classes-dir: ./compiled-classes
    max-file-size: 5MB

  rate-limit:
    default-max-tokens: 100
    default-refill-rate: 10
```

### Step 3: Create Required Directories

```bash
# Create directories
mkdir -p uploads
mkdir -p generated-sources
mkdir -p compiled-classes

# Set permissions
chmod 755 uploads generated-sources compiled-classes
```

### Step 4: Build Backend

```bash
# Clean and build
mvn clean install

# If you want to skip tests:
mvn clean install -DskipTests
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time:  2:35 min
[INFO] Finished at: 2024-11-16T10:00:00Z
```

### Step 5: Initialize Database

The database schema and seed data will be automatically created when you first run the application (thanks to `spring.sql.init.mode=always`).

Alternatively, you can manually run the SQL scripts:

```bash
# Run schema.sql
psql -h localhost -U xsd_user -d xsd_platform -f src/main/resources/schema.sql

# Run data.sql
psql -h localhost -U xsd_user -d xsd_platform -f src/main/resources/data.sql
```

### Step 6: Run Backend

```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using JAR file
java -jar target/xsd-platform-backend-1.0.0.jar

# Option 3: With custom profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Verify Backend is Running:**
```bash
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## Frontend Setup

### Step 1: Navigate to Frontend Directory

```bash
cd ../frontend
```

### Step 2: Install Dependencies

```bash
# Install all dependencies
npm install

# If you encounter errors, try:
npm install --legacy-peer-deps
```

**Expected Output:**
```
added 120 packages, and audited 121 packages in 15s
found 0 vulnerabilities
```

### Step 3: Configure Environment

Create `.env` file:

```bash
nano .env
```

Add configuration:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

### Step 4: Run Development Server

```bash
# Start Vite dev server
npm run dev
```

**Expected Output:**
```
VITE v7.2.2  ready in 425 ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
```

### Step 5: Build for Production (Optional)

```bash
# Create production build
npm run build

# Output will be in dist/ directory
# Preview production build:
npm run preview
```

---

## Docker Setup

### Prerequisites

- Docker installed and running
- Docker Compose installed

### Step 1: Review docker-compose.yml

```bash
cd elastic-cluster-had
cat docker-compose.yml
```

### Step 2: Build and Start Services

```bash
# Build images and start containers
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Step 3: Verify Services

```bash
# Check running containers
docker-compose ps

# Expected output shows all services as "Up"
```

### Step 4: Access Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **PostgreSQL**: localhost:5432

### Step 5: Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## Verification

### Backend Verification

1. **Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

2. **Swagger UI:**
Open http://localhost:8080/swagger-ui.html in browser

3. **Database Connection:**
```bash
# Check logs for successful connection
tail -f backend/logs/application.log | grep "HikariPool"
```

4. **Test Upload Endpoint:**
```bash
curl -X GET http://localhost:8080/api/v1/schemas
```

### Frontend Verification

1. **Open Application:**
Open http://localhost:5173 in browser

2. **Check Console:**
Open browser DevTools (F12) and check for errors

3. **Navigate Pages:**
- Dashboard: http://localhost:5173/
- Schema Management: http://localhost:5173/schemas
- Services: http://localhost:5173/services

4. **API Connection:**
Check Network tab in DevTools to see API calls

---

## Common Issues

### Issue 1: Port Already in Use

**Error:**
```
Web server failed to start. Port 8080 was already in use.
```

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080
# or on Windows:
netstat -ano | findstr :8080

# Kill the process
kill -9 <PID>
# or on Windows:
taskkill /PID <PID> /F

# Or change port in application.yml:
server:
  port: 8081
```

### Issue 2: Database Connection Failed

**Error:**
```
Unable to create initial connections of pool
```

**Solution:**
1. Check PostgreSQL is running:
```bash
sudo systemctl status postgresql
```

2. Verify credentials in `application.yml`

3. Test connection manually:
```bash
psql -h localhost -U xsd_user -d xsd_platform
```

4. Check `pg_hba.conf` for authentication settings

### Issue 3: npm install Fails

**Error:**
```
npm ERR! code ERESOLVE
```

**Solution:**
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Reinstall with legacy peer deps
npm install --legacy-peer-deps
```

### Issue 4: Maven Build Fails

**Error:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**Solution:**
1. Check Java version:
```bash
java -version  # Must be 21+
```

2. Clear Maven cache:
```bash
rm -rf ~/.m2/repository
```

3. Rebuild:
```bash
mvn clean install -U
```

### Issue 5: CORS Errors in Browser

**Error:**
```
Access to fetch at 'http://localhost:8080/api/...' from origin 'http://localhost:5173' has been blocked by CORS policy
```

**Solution:**
Check `WebMvcConfig.java` has correct origins:
```java
.allowedOrigins("http://localhost:3000", "http://localhost:5173")
```

### Issue 6: File Upload Directory Not Found

**Error:**
```
java.nio.file.NoSuchFileException: ./uploads
```

**Solution:**
```bash
# Create directories
mkdir -p backend/uploads
mkdir -p backend/generated-sources
mkdir -p backend/compiled-classes

# Set permissions
chmod 755 backend/uploads backend/generated-sources backend/compiled-classes
```

---

## Next Steps

After successful setup:

1. **Read API Documentation**: See `API_DOCUMENTATION.md`
2. **Test XSD Upload**: Upload a sample XSD file
3. **Deploy a Service**: Deploy the uploaded schema
4. **Access Endpoints**: Test the generated REST/SOAP endpoints
5. **Monitor Dashboard**: View real-time statistics

---

## Support

If you encounter any issues not covered here:

1. Check the main `README.md`
2. Check GitHub Issues
3. Contact support team

---

Last updated: 2024-11-16
