# Legacy Report Generation Removal — Comprehensive Fix Plan

## 1. Background

The system had two report generation paths:
- **Legacy (reportType=1)**: Code-based using `WordReportBuilder`, generates DOCX only, no HTML
- **Template-based (reportType=2)**: Template-driven using `TemplateBasedReportBuilder`, generates DOCX + HTML

Legacy reports had `reportHtml=NULL`, causing the TinyMCE editor to display "报告内容为空，请先生成报告". The frontend logic would fall back to the legacy path when no approved submissions (status=2) existed, creating reports that could never be edited online.

## 2. What Was Removed

### Backend (Java)

| File | Change | Lines Removed |
|------|--------|--------------|
| `ReportService.java` | Removed `generateReport()` method signature | 2 |
| `ReportServiceImpl.java` | Removed `generateReport()` method + `collectReportData()` helper + `WordReportBuilder` import | ~170 |
| `ReportController.java` | Removed `POST /report/generate` endpoint | 10 |
| `WordReportBuilder.java` | **Deleted entirely** — 657-line legacy DOCX builder | 657 |

### Frontend (TypeScript/Vue)

| File | Change |
|------|--------|
| `report.ts` | Removed `generateReport()` API function (called deleted `/report/generate` endpoint) |
| `generate/index.vue` | Removed `generateReport` import; rewrote `handleGenerateReport()` to ONLY use template-based path; added early-exit with user-friendly message when no approved submissions exist |

### Total: ~840 lines removed, 6 files changed

## 3. How Template-Based Generation Works

1. Enterprise user fills SpreadJS templates and submits (status=0 -> 1)
2. Auditor reviews and approves submissions (status=1 -> 2)
3. Enterprise clicks "生成审计报告" — system checks for status=2 submissions
4. `generateReportFromTemplate()` is called with the approved submission ID
5. Backend loads the active Word template (`ar_report_template` where `is_active=1`)
6. `TemplateBasedReportBuilder` reads submission JSON, extracts SpreadJS data, fills Word template
7. Generated DOCX is converted to HTML and stored in `ar_report.report_html`
8. Enterprise can edit the report in TinyMCE, then submit for review

## 4. Database Migration

### SQL Script: `sql/26-remove-legacy-reports.sql`

```sql
-- Soft-delete all legacy reports (reportType=1)
UPDATE ar_report SET deleted=1, update_by='SYSTEM_MIGRATION', update_time=NOW()
WHERE report_type=1 AND deleted=0;

-- Activate the most recent report template
UPDATE ar_report_template SET is_active=1, update_by='SYSTEM_MIGRATION', update_time=NOW()
WHERE id = (SELECT id FROM (SELECT id FROM ar_report_template WHERE deleted=0 ORDER BY create_time DESC LIMIT 1) AS t)
AND (is_active IS NULL OR is_active=0);
```

**Impact**: Legacy reports (id=2, id=3 in production) will be soft-deleted. They can be recovered by setting `deleted=0` if needed.

## 5. Production Deployment Steps

1. **Merge PR** to master
2. **Execute SQL migration** on production MySQL:
   ```bash
   # Via Railway GraphQL API to get MySQL credentials, then:
   mysql -h <host> -P <port> -u <user> -p<pass> energy_audit < sql/26-remove-legacy-reports.sql
   ```
3. **Verify**: Railway auto-deploys on merge. After deployment:
   - Login as enterprise user
   - Click "生成审计报告" without approved submissions → should show warning message
   - With approved submissions → should generate template-based report with HTML
   - Old legacy reports should no longer appear in the report list

## 6. Testing Checklist

- [ ] Backend compiles without errors (no references to removed methods)
- [ ] Frontend builds without errors (no references to removed `generateReport` function)
- [ ] Report list page loads correctly (legacy reports hidden after migration)
- [ ] "生成审计报告" button shows warning when no approved submissions exist
- [ ] Template-based report generation works end-to-end (approved submission -> DOCX + HTML)
- [ ] TinyMCE editor displays generated HTML content correctly
- [ ] Report download (DOCX) works for template-based reports
- [ ] Report review workflow (submit -> approve/reject) works correctly

## 7. Rollback Plan

If issues arise after deployment:

1. **Restore legacy reports**: `UPDATE ar_report SET deleted=0 WHERE update_by='SYSTEM_MIGRATION';`
2. **Revert code**: Git revert the merge commit
3. **Redeploy**: Push revert to trigger Railway auto-deploy

The legacy `WordReportBuilder.java` will be restored by the git revert, along with all endpoint and service method references.
