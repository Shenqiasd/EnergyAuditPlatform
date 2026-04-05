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

## Development Progress

| Wave | Topic | Status |
|------|-------|--------|
| Wave 0 | Infrastructure & Auth | ✅ Completed |
| Wave 1 | Admin Basic Functions | ✅ Completed |
| Wave 2 | Enterprise Basic Settings | ✅ Completed |
| Wave 3 | SpreadJS Template Engine | ✅ Completed |
| Wave 4 | Template-Driven Data Extraction | ✅ Completed |
| Wave 5 | Menu Restructure & Extracted Data Overview | ✅ Completed |
| Wave 6 | Schema (24 de_* tables) | ✅ Completed |
| Wave 9.1 | Audit Task Management (3-portal workflow) | ✅ Completed |
| Wave 9.2 | Rectification Tracking & Overdue Warning | ✅ Completed |
| Wave 7 | Energy Flow Diagram (AntV X6) | ✅ Completed |
| Wave 8 | ECharts Charts + Report Generation | Planned |
| Wave 10 | Carbon Management & Platform Integration | Partial (emission factor CRUD done) |
| Wave 11 | Optimization & Testing | Planned |

**Current stage**: Wave 7 complete — Energy Flow Diagram with AntV X6 (PR #19)

## Wave 7 — Energy Flow Diagram (AntV X6)
- **Backend**: `EnergyFlowController` (GET /list, POST /save-batch, DELETE /{id}), `EnergyFlowService`, `DeEnergyFlowMapper`
- **Frontend**: `FlowEditor/index.vue` — AntV X6 canvas with 6 node types (purchased=#409EFF, conversion=#E6A23C, distribution=#67C23A, terminal=#909399, product=#F56C6C, non_production=#C0C4CC), auto-layout, edge labels, export PNG
- **Page**: `enterprise/charts/energy-flow/index.vue` — load/save/export/sample-data actions
- **API**: `energyFlow.ts` — getFlowData, saveBatch, deleteFlow
- **Menu**: Energy flow entry in enterprise charts section
- **Seed**: 9 energy flow records (purchased→conversion→distribution→terminal) for enterprise 1, year 2024
- **Dev login**: enterprise1/admin123 (userType=3, enterpriseId=1), admin/admin123 (userType=1)
- **GitHub PR**: #19 feat/wave7-energy-flow-diagram

## Wave 9.1 — Audit Task Management
- **Tables**: `aw_audit_task`, `aw_audit_log` (H2 + MySQL schema)
- **Entities**: `AwAuditTask`, `AwAuditLog` with transient `enterpriseName`, `assigneeName`
- **Mapper**: XML with JOIN queries, `selectList` dynamic conditions, `clearResult` for resubmit
- **Service**: `AuditTaskServiceImpl` — submitForAudit (auto-assign round-robin), assign, approve, reject, comment
- **Controller**: `AuditTaskController` — object-level access control (enterprise=own tasks, auditor=assigned tasks, admin=all)
- **ExtractedDataController**: Updated to allow auditor/admin access with `enterpriseId` param
- **Frontend API**: `audit-task.ts` with status maps and action labels
- **Pages**: Enterprise generate/index.vue (submit-audit), admin audit-manage (task list + assign dialog + detail drawer), auditor dashboard/tasks/review
- **Seed user**: `auditor/admin123` (userType=2, id=3)

## Wave 9.2 — Rectification Tracking & Overdue Warning
- **Table**: `aw_rectification_track` (H2 + MySQL schema) — status: 0=未启动, 1=进行中, 2=已完成, 3=超期
- **Entity**: `AwRectificationTrack` with transient `enterpriseName`, `taskTitle`
- **Mapper**: XML with JOIN queries, `selectOverdueCandidates` for deadline check, `batchUpdateStatus`
- **Service**: `RectificationServiceImpl` — createItems (batch), updateProgress (enterprise), acceptItem (auditor, status=1 only), markOverdueItems (scheduled)
- **Controller**: `RectificationController` at `/audit/rectification` — IDOR-safe access control per endpoint
- **Scheduled Job**: `RectificationOverdueJob` — daily at 01:00, marks overdue items (status 0/1 past deadline → 3)
- **Frontend API**: `rectification.ts` with status maps
- **Auditor review**: Added 整改管理 card with table + create dialog (batch add items with name/requirement/deadline) + 验收 button
- **Enterprise dashboard**: Replaced mock todos with real rectification items from API, with update progress dialog
- **Admin audit-manage**: Added 超期 column (red badge) + 仅超期 filter checkbox, batch query overdue counts
- **Security**: Role-based + ownership checks prevent IDOR — auditors can only see/act on assigned tasks

## Development Setup

### Frontend
```bash
cd audit-ui && npm install
npm run dev  # runs on port 5000
```

### Backend (dev profile — H2 in-memory)
```bash
# Build with H2 included (-P dev activates H2 profile; -am resolves sibling modules)
mvn package -DskipTests -pl audit-web -am -P dev
java -jar audit-web/target/audit-web-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
# H2 console available at http://localhost:8080/api/h2-console
# Seed account: admin / admin123 (userType=1, admin portal)

# Or run directly without packaging (multi-module: install first, then run from module dir):
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
- **Production**: `sql/` directory — 55 tables for MySQL 8.0; `sql/02-wave4-data-extraction.sql` adds 12 de_* tables + tpl_tag_mapping ALTER
- **Dev H2**: `audit-web/src/main/resources/schema-h2.sql` (~32 tables including de_* extraction tables)
- **Dev seed**: `audit-web/src/main/resources/data-h2.sql` (admin/admin123)

## Wave 4 — Template-Driven Data Extraction (SCALAR + TABLE Dual Mapping Engine)
- **Core concept**: SpreadJS templates define all data collection/calculation logic; software handles workflow
- **tpl_tag_mapping** extended: mapping_type (SCALAR/TABLE), source_type (NAMED_RANGE/CELL_TAG), row_key_column, column_mappings (JSON), header_row
- **DiscoveredField DTO**: Auto-detects Named Range (single→SCALAR, multi→TABLE) vs Cell Tag (default SCALAR, manual TABLE)
- **SpreadsheetDataExtractor**: Supports both SCALAR extraction (cell value) and TABLE extraction (row-by-row with column_mappings)
- **DataPersistenceService**: Routes extracted data to generic de_submission_field / de_submission_table tables
- **12 de_* tables**: 10 key business tables (company_overview, tech_indicator, energy_consumption, energy_conversion, product_unit_consumption, equipment_detail, carbon_emission, energy_balance, energy_flow, five_year_target) + 2 generic (de_submission_field, de_submission_table)
- **Frontend**: Admin template designer sidebar renamed to "字段映射配置", shows source type badges, mapping type selector, TABLE-specific config panel (cellRange, headerRow, columnMappings JSON, rowKeyColumn)

## Wave 0 — Completed (feat/wave0-complete branch)
- 6 MyBatis Mapper XML files created in `audit-dao/src/main/resources/mapper/`
- H2 dev environment: `application-dev.yml`, `schema-h2.sql`, `data-h2.sql`
- H2 runtime dependency added to `audit-web/pom.xml`
- `ChangePasswordDialog` wired into all 3 Layout components (auto-shows when `needChangePassword=true`)
- Backend smoke-tested: login returns JWT, `passwordChanged=false` triggers force-change dialog

## Key Technologies
- **SpreadJS v18.2.5**: Excel-like data entry interface
  - Files served locally from `audit-ui/public/spreadjs/` (CDN cdn.grapecity.com is blocked in Replit)
  - npm packages: `@grapecity/spread-sheets{,-designer,-designer-resources-en,-io,-shapes,-charts,-print,-barcode}@18.2.5`
  - V18 file layout differs from V17: CSS in `styles/`, designer is `designer.all.min.js`
  - License key stored in `VITE_SPREADJS_LICENSE` env var, initialized via `src/utils/spreadjs-license.ts`
  - V18 API change: `GC.Spread.Sheets.Designer` is the constructor directly (not `Designer.Designer`)
  - `SpreadDesigner/index.vue` uses `resolveDesignerConstructor()` to handle both V17 and V18 patterns
- **AntV X6 v2**: Energy flow diagram visualization (FlowEditor.vue — fully implemented)
- **ECharts**: Data dashboards (planned)
- **OnlyOffice**: Online document editing (planned)

## Key Backend Patterns
- **SecurityUtils**: Always use `getRequired*` variants
- **Axios interceptor**: `request.ts` already extracts `res.data` — never add extra `.then((r: any) => r.data)`
- **Tag mapping rules**: Named Range(1x1)→SCALAR; Named Range(multi)→TABLE auto; Cell Tag→SCALAR default, TABLE requires cellRange+headerRow
- **BusinessTablePersister**: `NamedParameterJdbcTemplate`; column names validated via `^[a-z][a-z0-9_]{0,63}$`; `camelToSnake()` lowercases mixed-case with underscores
- **de_* tables**: delete by `submission_id` (NOT enterprise_id+audit_year) for isolation
- **PageResult**: Backend returns `rows` (not `list`). `setting.ts` is canonical pattern.
- **Backend login**: `POST /api/auth/login`; dev: `admin/admin123` (userType=1), `enterprise/admin123` (userType=3, enterpriseId=1), `auditor/admin123` (userType=2)
- **MyBatis mapper XML**: `audit-dao/src/main/resources/mapper/{module}/`; MapperScan: `com.energy.audit.dao.mapper`

## Important Files
- `DEVELOPMENT_PLAN.md` — full development roadmap (Wave 0-11)
- `audit-ui/src/config/menus.ts` — enterprise/admin/auditor menu definitions
- `audit-ui/src/components/SpreadDesigner/index.vue` — V18 API compat with resolveDesignerConstructor()
- `audit-ui/src/utils/spreadjs-license.ts` — centralized SpreadJS license initialization
- `audit-ui/src/types/spreadjs.d.ts` — SpreadJS TypeScript declarations (V18 shape)
- `audit-web/src/main/java/com/energy/audit/web/controller/data/ExtractedDataController.java` — extracted data overview API (GET /tables + GET /{tableName})
- `audit-ui/src/api/extracted-data.ts` — frontend API for extracted data queries
- `audit-ui/src/views/enterprise/data/overview/index.vue` — extracted data overview page with year filter + 10 el-tabs
- `audit-service/src/main/java/com/energy/audit/service/template/BusinessTablePersister.java` — dynamic business table routing
- `audit-service/src/main/java/com/energy/audit/service/template/SpreadsheetDataExtractor.java` — SCALAR+TABLE extraction
- `audit-service/src/main/java/com/energy/audit/service/template/impl/DataPersistenceServiceImpl.java` — dispatch logic
- `audit-web/src/main/resources/schema-h2.sql` — H2 dev schema (all de_* tables)

## Deployment
- Static deployment: builds `audit-ui/dist`
- Build command: `cd audit-ui && npm run build`
- Production MUST use MySQL — `application-prod.yml` and `docker-compose.yml` are off-limits

## GitHub
- Remote: `https://github.com/Shenqiasd/Municipal-Industrial-Energy-Carbon-Audit-Platform`
- Main branch: `master`
- Push to GitHub requires a Personal Access Token set as `GITHUB_TOKEN` secret
