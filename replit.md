# Energy Audit Platform (能碳审计管理平台)

## Project Overview

A comprehensive enterprise-level web application for managing energy consumption data, performing energy audits, and generating regulatory reports for industrial enterprises. Targets three user types: Enterprises, Administrators, and Auditors.

**Core philosophy**: SpreadJS templates define all data collection/calculation logic; software handles main workflow. Data entry is template-driven — no independent CRUD pages for audit data.

## Architecture

### Frontend (Vue 3 + Vite)
- Located in `audit-ui/`
- Vue 3 + TypeScript + Vite (port 5000)
- UI Library: Element Plus (theme color `#00897B`)
- State Management: Pinia
- Three portals: admin, auditor, enterprise (each with own Layout and router guards)

### Backend (Spring Boot 3)
- Maven multi-module Java project
- Modules: audit-common, audit-model, audit-dao, audit-service, audit-web
- Java 17, Spring Boot 3.2.5, MyBatis, JWT authentication
- **Production**: MySQL 8.0 (`application-prod.yml`, `docker-compose.yml`) — DO NOT CHANGE
- **Development**: H2 in-memory (`application-dev.yml`, `--spring.profiles.active=dev`)

## Development Progress

| Wave | Topic | Status |
|------|-------|--------|
| Wave 0 | Infrastructure & Auth | ✅ Completed |
| Wave 1 | Admin Basic Functions | ✅ Completed |
| Wave 2 | Enterprise Basic Settings | ✅ Completed |
| Wave 3 | SpreadJS Template Engine | ✅ Completed |
| Wave 4 | Template-Driven Data Extraction | ✅ Completed |
| Wave 5 | Menu Restructure & Extracted Data Overview | ✅ Completed |
| Wave 6-9 | Flow Diagram, Charts, Reports, Workflow | Planned |
| Wave 10 | Carbon Management & Platform Integration | Partial (emission factor CRUD done) |
| Wave 11 | Optimization & Testing | Planned |

**Current stage**: Wave 5 complete — deleted 24 placeholder entry pages, reorganized enterprise menu (工作台/基础设置/数据填报/图表分析/报告管理), built extracted data overview page with backend API

## Development Setup

### Frontend
```bash
cd audit-ui && npm install
npm run dev  # runs on port 5000
```

### Backend (dev profile — H2 in-memory)
```bash
mvn package -DskipTests -pl audit-web -am -P dev
java -jar audit-web/target/audit-web-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
# H2 console available at http://localhost:8080/api/h2-console
# Seed account: admin / admin123 (userType=1, admin portal)

# Or run directly:
mvn install -DskipTests -pl audit-web -am -P dev
cd audit-web && mvn spring-boot:run -P dev
```

### Production build (MySQL, no H2)
```bash
mvn package -DskipTests -pl audit-web -am   # no -P dev → H2 excluded
```

### Workflow
- "Start application" workflow: `cd audit-ui && npm run dev` on port 5000

## Database Schemas
- **Production**: `sql/` directory — 55+ tables for MySQL 8.0; `sql/02-wave4-data-extraction.sql` adds 12 de_* tables + tpl_tag_mapping ALTER; `sql/03-wave6-schema-expansion.sql` adds 14 new de_* tables + 29 new columns across 6 existing tables
- **Dev H2**: `audit-web/src/main/resources/schema-h2.sql` (46 tables including 24 de_* business tables + 2 generic)
- **Dev seed**: `audit-web/src/main/resources/data-h2.sql` (admin/admin123)

## Wave 4 — Template-Driven Data Extraction (SCALAR + TABLE Dual Mapping Engine)
- **Core concept**: SpreadJS templates define all data collection/calculation logic; software handles workflow
- **tpl_tag_mapping** extended: mapping_type (SCALAR/TABLE), source_type (NAMED_RANGE/CELL_TAG), row_key_column, column_mappings (JSON), header_row
- **DiscoveredField DTO**: Auto-detects Named Range (single→SCALAR, multi→TABLE) vs Cell Tag (default SCALAR, manual TABLE)
- **SpreadsheetDataExtractor**: Supports both SCALAR extraction (cell value) and TABLE extraction (row-by-row with column_mappings)
- **DataPersistenceService**: Routes extracted data to generic de_submission_field / de_submission_table tables
- **BusinessTablePersister**: Dynamic SQL with column name whitelist (`^[a-z][a-z0-9_]{0,63}$`), camelToSnake conversion
- **26 de_* tables**: 24 key business tables (company_overview, tech_indicator, energy_consumption, energy_conversion, product_unit_consumption, equipment_detail, carbon_emission, energy_balance, energy_flow, five_year_target, tech_reform_history, saving_project, product_output, meter_instrument, meter_config_rate, obsolete_equipment, product_energy_cost, saving_calculation, management_policy, saving_potential, management_suggestion, tech_reform_suggestion, rectification, report_text) + 2 generic (de_submission_field, de_submission_table)
- **Frontend**: Admin template designer sidebar renamed to "字段映射配置", shows source type badges, mapping type selector, TABLE-specific config panel (cellRange, headerRow, columnMappings JSON, rowKeyColumn)

## Key Technologies
- **SpreadJS v18.2.5**: Excel-like data entry interface
  - Files served locally from `audit-ui/public/spreadjs/` (CDN cdn.grapecity.com is blocked in Replit)
  - npm packages: `@grapecity/spread-sheets{,-designer,-designer-resources-en,-io,-shapes,-charts,-print,-barcode}@18.2.5`
  - V18 file layout differs from V17: CSS in `styles/`, designer is `designer.all.min.js`
  - License key stored in `VITE_SPREADJS_LICENSE` env var, initialized via `src/utils/spreadjs-license.ts`
  - V18 API change: `GC.Spread.Sheets.Designer` is the constructor directly (not `Designer.Designer`)
  - `SpreadDesigner/index.vue` uses `resolveDesignerConstructor()` to handle both V17 and V18 patterns
- **AntV X6**: Energy flow diagram visualization (FlowEditor.vue — skeleton placeholder)
- **ECharts**: Data dashboards (ChartCard.vue — not yet created)
- **OnlyOffice**: Online document editing (DocEditor.vue — skeleton placeholder)

## Key Backend Patterns
- **SecurityUtils**: Always use `getRequired*` variants
- **Axios interceptor**: `request.ts` already extracts `res.data` — never add extra `.then((r: any) => r.data)`
- **Tag mapping rules**: Named Range(1x1)→SCALAR; Named Range(multi)→TABLE auto; Cell Tag→SCALAR default, TABLE requires cellRange+headerRow
- **BusinessTablePersister**: `NamedParameterJdbcTemplate`; column names validated via `^[a-z][a-z0-9_]{0,63}$`; `camelToSnake()` lowercases mixed-case with underscores
- **de_* tables**: delete by `submission_id` (NOT enterprise_id+audit_year) for isolation
- **PageResult**: Backend returns `rows` (not `list`). `setting.ts` is canonical pattern.
- **Backend login**: `POST /api/auth/login`; dev: `admin/admin123` (userType=1), `enterprise/admin123` (userType=3, enterpriseId=1)
- **MyBatis mapper XML**: `audit-dao/src/main/resources/mapper/{module}/`; MapperScan: `com.energy.audit.dao.mapper`

## Important Files
- `DEVELOPMENT_PLAN.md` — full development roadmap (Wave 0-11)
- `audit-ui/src/config/menus.ts` — enterprise/admin/auditor menu definitions
- `audit-ui/src/components/SpreadDesigner/index.vue` — V18 API compat with resolveDesignerConstructor()
- `audit-ui/src/utils/spreadjs-license.ts` — centralized SpreadJS license initialization
- `audit-ui/src/types/spreadjs.d.ts` — SpreadJS TypeScript declarations (V18 shape)
- `audit-web/src/main/java/com/energy/audit/web/controller/data/ExtractedDataController.java` — extracted data overview API (GET /tables + GET /{tableName}), 24 table labels
- `audit-ui/src/api/extracted-data.ts` — frontend API for extracted data queries
- `audit-ui/src/views/enterprise/data/overview/index.vue` — extracted data overview page with year filter + 24 el-tabs with column labels
- `audit-service/src/main/java/com/energy/audit/service/template/BusinessTablePersister.java` — dynamic business table routing
- `audit-service/src/main/java/com/energy/audit/service/template/SpreadsheetDataExtractor.java` — SCALAR+TABLE extraction
- `audit-service/src/main/java/com/energy/audit/service/template/impl/DataPersistenceServiceImpl.java` — dispatch logic
- `audit-web/src/main/resources/schema-h2.sql` — H2 dev schema (all de_* tables)

## Deployment
- Static deployment: builds `audit-ui/dist`
- Build command: `cd audit-ui && npm run build`
- Production MUST use MySQL — `application-prod.yml` and `docker-compose.yml` are off-limits
- MySQL migration: `sql/02-wave4-data-extraction.sql` — run against prod before Wave 4 features go live

## GitHub
- Remote: `https://github.com/Shenqiasd/Municipal-Industrial-Energy-Carbon-Audit-Platform`
- Main branch: `master`
- Push to GitHub requires a Personal Access Token set as `GITHUB_TOKEN` secret
