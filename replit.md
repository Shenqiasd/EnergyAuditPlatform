# Energy Audit Platform (能碳审计管理平台)

## Project Overview

A comprehensive enterprise-level web application for managing energy consumption data, performing energy audits, and generating regulatory reports for industrial enterprises. Targets three user types: Enterprises, Administrators, and Auditors.

## Architecture

### Frontend (Vue 3 + Vite)
- Located in `audit-ui/`
- Vue 3 + TypeScript + Vite (port 5000)
- UI Library: Element Plus
- State Management: Pinia
- Three portals: admin, auditor, enterprise (each with own Layout and router guards)

### Backend (Spring Boot 3)
- Maven multi-module Java project
- Modules: audit-common, audit-model, audit-dao, audit-service, audit-web
- Java 17, Spring Boot 3.2.5, MyBatis, JWT authentication
- **Production**: MySQL 8.0 (`application-prod.yml`, `docker-compose.yml`) — DO NOT CHANGE
- **Development**: H2 in-memory (`application-dev.yml`, `--spring.profiles.active=dev`)

## Development Setup

### Frontend
```bash
cd audit-ui && npm install
npm run dev  # runs on port 5000
```

### Backend (dev profile — H2 in-memory)
```bash
mvn package -DskipTests -pl audit-web -am
java -jar audit-web/target/audit-web-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
# H2 console available at http://localhost:8080/h2-console
# Seed account: admin / admin123 (userType=1, admin portal)
```

### Workflow
- "Start application" workflow: `cd audit-ui && npm run dev` on port 5000

## Database Schemas
- **Production**: `sql/` directory — 55 tables for MySQL 8.0
- **Dev H2**: `audit-web/src/main/resources/schema-h2.sql` (7 core tables)
- **Dev seed**: `audit-web/src/main/resources/data-h2.sql` (admin/admin123)

## Wave 0 — Completed (feat/wave0-complete branch)
- 6 MyBatis Mapper XML files created in `audit-dao/src/main/resources/mapper/`
- H2 dev environment: `application-dev.yml`, `schema-h2.sql`, `data-h2.sql`
- H2 runtime dependency added to `audit-web/pom.xml`
- `ChangePasswordDialog` wired into all 3 Layout components (auto-shows when `needChangePassword=true`)
- Backend smoke-tested: login returns JWT, `passwordChanged=false` triggers force-change dialog

## Key Technologies
- **SpreadJS**: Excel-like data entry interface
- **AntV X6**: Energy flow diagram visualization
- **ECharts**: Data dashboards
- **OnlyOffice**: Online document editing

## Deployment
- Static deployment: builds `audit-ui/dist`
- Build command: `cd audit-ui && npm run build`
- Production MUST use MySQL — `application-prod.yml` and `docker-compose.yml` are off-limits

## GitHub
- Remote: `https://github.com/Shenqiasd/Municipal-Industrial-Energy-Carbon-Audit-Platform`
- Main branch: `master`
- Push to GitHub requires a Personal Access Token set as `GITHUB_TOKEN` secret
