# Energy Audit Platform — Testing Skills

## Production Environment
- **URL**: `https://sjs.ben-china.org.cn` (SpreadJS license bound to this domain)
- **Railway direct URL**: `audit-ui-production-*.up.railway.app` (SpreadJS will NOT work here — license domain mismatch)
- **Backend API**: `https://sjs.ben-china.org.cn/api/`

## Test Accounts
- **Enterprise**: `910100100101010` / password stored in session secrets
- **Admin**: stored in `AUDIT_ADMIN_USERNAME` / `AUDIT_ADMIN_PASSWORD` secrets
- **Enterprise (alternate)**: `enterprise` / `${AUDIT_ADMIN_PASSWORD}`
- **E2E test**: `e2etest0412` / `${AUDIT_ADMIN_PASSWORD}`

## Railway Deployment
- Use GraphQL API `https://backboard.railway.app/graphql/v2` + `Authorization: Bearer $RAILWAY_API_TOKEN`
- Railway CLI `railway status` does NOT work with the token — only GraphQL works
- To get MySQL connection info: query `variables` on the MySQL service via GraphQL
- After PR merge, Railway auto-deploys both frontend and backend
- If frontend deploys before backend finishes, nginx may cache stale DNS → redeploy frontend

## SQL Migrations
- Always execute migrations via Railway GraphQL → get MySQL TCP proxy → `mysql` client
- Check `_migration_history` table to avoid re-running old migrations
- For Tencent Cloud: use `deploy-tencent/run-migrations.sh`

## Industry Cascading Selector Testing
- Component: `el-cascader` on `/enterprise/settings/company` page
- Data: GB/T 4754-2017 (8 门类 / 45 大类 / 169 中类)
- API endpoint: `GET /api/enterprise/setting` returns `industryCode`, `industryName`, `industryCategory`
- Clear icon selector: `.el-cascader .icon-circle-close` (may need JavaScript click)
- Verify round-trip: select → save → reload → verify cascader shows full path

## SpreadJS Testing
- MUST use `https://sjs.ben-china.org.cn` domain (license bound)
- License expires ~2026-04-30, check for renewal
- `setValue()` via console does NOT trigger dirty flag → data won't serialize on save
- Use `sheet.getCell(row,col).locked()` to verify protection status
- Console logs: `[protection]` prefix for cell protection diagnostics
- Console logs: `[config-prefill]` prefix for CONFIG_PREFILL diagnostics

## CONFIG_PREFILL Testing
- Tags with `mappingType=CONFIG_PREFILL` auto-fill data from `bs_energy`/`bs_product`/`bs_unit`
- `mode: "dropdown_only"` → only inject dropdowns, no value prefill
- `linkedTo` → column value auto-derived from another column's selection
- `dropdown: false` → numeric columns skip dropdown injection
- Verify via console: `[config-prefill] found N CONFIG_PREFILL tags`

## Performance Testing
- Use `performance.getEntriesByType('resource')` to verify API parallelization
- Check `listTags` call count (should be 1, not 3)
- Gzip only works in production nginx, not dev mode

## Common Pitfalls
- After backend redeploy, frontend nginx may cache old backend IP → redeploy frontend
- `tpl_tag_mapping` column names must match actual MySQL columns (not canonical schema)
- TABLE/EQUIPMENT_BENCHMARK tags are manually configured — `syncFromTemplateJson` preserves them (PR #106)
- Submitted templates (status=1) are readonly; status=3 (rejected) allows re-editing

## Devin Secrets Needed
- `AUDIT_ADMIN_USERNAME` — ${AUDIT_ADMIN_USERNAME} login username
- `AUDIT_ADMIN_PASSWORD` — ${AUDIT_ADMIN_USERNAME} login password  
- `RAILWAY_API_TOKEN` — Railway GraphQL API access
