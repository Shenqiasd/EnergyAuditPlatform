# Testing SpreadJS Features (Cell Protection, Required Fields, Tag Mapping, CONFIG_PREFILL)

## Overview
This skill covers end-to-end testing of SpreadJS-based features in the Energy Audit Platform, including cell protection, required field marking, data submission workflows, and CONFIG_PREFILL (configuration-driven row prefill).

## Environment Setup

### Local Dev Stack
- **Backend**: `cd audit-web && mvn spring-boot:run` on port 8080 (H2 in-memory DB)
- **Frontend**: `cd audit-ui && npm run dev` on port 5173 (Vite)
- **Credentials**: admin/Admin@2026 (admin), enterprise/Enterprise@2026 (enterprise)

### Production Environment
- **URL**: https://sjs.ben-china.org.cn
- **Admin credentials**: ${AUDIT_ADMIN_USERNAME} / (use Devin secret PROD_ADMIN_PASSWORD)
- **Enterprise credentials**: AS2335234234252223 / (use Devin secret PROD_ENTERPRISE_PASSWORD)
- Railway auto-deploys on merge to master; frontend is a Vite SPA with lazy-loaded chunks

### Devin Secrets Needed
- No external secrets required for local testing (H2 in-memory DB)
- For production testing: PROD_ADMIN_PASSWORD, PROD_ENTERPRISE_PASSWORD

## Key Testing Techniques

### Element Plus Click Blocking Workaround
Element Plus UI components (el-select, el-button, el-dialog) consistently block Playwright direct clicks due to overlay/wrapper elements intercepting clicks. **Workaround**: Use JavaScript console for ALL Element Plus interactions:

```javascript
// Open dropdown
document.querySelector('.el-select .el-select__wrapper').click();

// Click dropdown option
document.querySelector('[devinid="19"]').click();

// Click buttons by text
const buttons = document.querySelectorAll('button');
for (const btn of buttons) {
  if (btn.textContent.trim() === '提交数据') { btn.click(); break; }
}
```

### Finding SpreadJS Control
The SpreadJS control is NOT found via `canvas.parentElement` hierarchy. Use `gcuielement` attribute:

```javascript
const spread = GC.Spread.Sheets.findControl(document.querySelector('[gcuielement=gcSpread]'));
```

### Navigating to a Specific Sheet
Sheet tabs may be scrolled off-screen. Use JS to switch directly:

```javascript
const spread = GC.Spread.Sheets.findControl(document.querySelector('[gcuielement=gcSpread]'));
spread.setActiveSheetIndex(16); // 0-based index
console.log('Active sheet:', spread.getActiveSheet().name());
```

### Inspecting Cell Protection State
```javascript
const sheet = spread.getSheet(0);
sheet.options.isProtected;     // true = sheet protection active
sheet.getCell(r, c).locked();  // true = cell is locked
sheet.getCell(r, c).backColor(); // e.g. '#FFF3E0' for required fields
sheet.comments.get(r, c);     // Comment object (use .text() to read)
sheet.comments.all();         // All comments on sheet
```

### Setting Cell Values Programmatically
```javascript
sheet.setValue(row, col, 'value');
```

### Reading Cell Values for Verification
```javascript
// getValue uses 0-based indices: getValue(row, col)
sheet.getValue(4, 0); // = cell A5
sheet.getValue(4, 1); // = cell B5
```

## Test Data Setup via API

When using H2 in-memory DB, seed test data via API calls:

1. **Login**: `POST /api/auth/login` with credentials, save token
2. **Set cell tags in designer**: Use SpreadJS API `sheet.setTag(row, col, 'tagName')`
3. **Configure tag mappings**: `PUT /api/template/versions/{id}/tags` with required flags
4. **Publish version**: `POST /api/template/{tplId}/versions/{verId}/publish`
5. **Check protection toggle**: `GET /api/template/versions/{id}` → `protectionEnabled` field

## CONFIG_PREFILL Testing

### What CONFIG_PREFILL Does
Automatically populates SpreadJS table rows from enterprise configuration data (bs_energy / bs_product) when a template is first opened (no prior submission).

### Verifying Deployment After PR Merge
Railway auto-deploys on merge but may take several minutes. To verify:

1. Check if the main bundle hash changed:
   ```bash
   curl -s https://sjs.ben-china.org.cn/ | grep -o '/assets/index-[^"]*\.js'
   ```
2. The SpreadSheet component is in a **lazy-loaded chunk**, not the main bundle. Download and check:
   ```bash
   # Find which chunk contains config-prefill code
   for chunk in $(curl -s https://sjs.ben-china.org.cn/assets/index-MAIN.js | grep -oP '"\./index-[^"]+\.js"' | tr -d '"./'); do
     count=$(curl -s "https://sjs.ben-china.org.cn/assets/${chunk}" | grep -c "config-prefill")
     [ "$count" -gt 0 ] && echo "Found in $chunk"
   done
   ```
3. Look for the diagnostic log `[config-prefill] found` in the chunk to confirm the fix is deployed.

### Testing CONFIG_PREFILL End-to-End

1. **Clear console** before loading the template
2. **Select template** in enterprise UI (模板填报 page)
3. **Check console** for `[config-prefill] found N CONFIG_PREFILL tags`
4. **Navigate to target sheet** using JS: `spread.setActiveSheetIndex(sheetIndex)`
5. **Verify cell values** using JS: `sheet.getValue(row, col)`

### CONFIG_PREFILL Tag Mapping Format
```json
{
  "tagName": "config_prefill_energy_11_1",
  "mappingType": "CONFIG_PREFILL",
  "targetTable": "bs_energy",
  "cellRange": "A5:B50",
  "sheetIndex": 16,
  "sheetName": "11.1 能源购入、消费、存储",
  "columnMappings": "{\"filter\":{\"isActive\":1},\"columns\":[{\"col\":\"A\",\"field\":\"name\"},{\"col\":\"B\",\"field\":\"measurementUnit\"}]}"
}
```

### Config Data API
- `GET /api/enterprise-settings/energy` — returns bs_energy records
- `GET /api/enterprise-settings/product` — returns bs_product records
- These are the data sources for CONFIG_PREFILL

### Common Pitfalls
- **col field format**: `col` can be a letter ("A") for absolute column or a number (0) for relative offset from startCol. Letter format is more intuitive for template designers.
- **Merged cells**: If a sheet uses merged cells, CONFIG_PREFILL writes to the specified cell coordinates but the data may be visually hidden behind merged cell overlays. Verify with `sheet.getValue()` rather than visual inspection alone.
- **First-time-only**: CONFIG_PREFILL only runs when there's no existing submission/draft. To re-test, you may need to delete the submission first.
- **Empty data source**: If the config table has 0 records, CONFIG_PREFILL gracefully skips without errors.

## Common Test Scenarios

### Cell Protection Verification
1. Load template as enterprise user
2. Check `sheet.options.isProtected === true`
3. Verify label cells `locked === true`
4. Verify data-entry cells `locked === false`
5. Verify unmapped cells `locked === true`

### Required Field Marking
1. Check `backColor === '#FFF3E0'` for required cells
2. Check `backColor === undefined` for non-required cells
3. Verify comments exist on required cells with '必填字段' text

### Submission Validation
1. Leave required fields empty, click submit
2. Expect '必填字段未填写' dialog listing missing fields
3. Fill required fields, submit again
4. Expect confirmation dialog, then success message

### Readonly Protection After Submission
1. After successful submission, verify ALL cells are locked
2. Previously unlocked data-entry cells should now be `locked === true`
3. This tests the `applyReadonlyProtection` function

## Browser Session Recovery
After browser restart, Vue Router may infinite-redirect to /login because localStorage is cleared. Fix by setting token first:

```javascript
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({username: 'enterprise', password: 'Enterprise@2026'})
}).then(r => r.json()).then(data => {
  localStorage.setItem('token', data.data.token);
  localStorage.setItem('userInfo', JSON.stringify(data.data));
  window.location.href = '/enterprise/report/input';
});
```

## Gotchas
- SpreadJS `getComment()` is not a method on `sheet` — use `sheet.comments.get(row, col)` instead
- `sheet.comments.all()` returns objects where `.row` and `.col` may be undefined; use `.text()` to read content
- H2 in-memory DB loses data on backend restart — re-seed test data if backend was restarted
- The protection feature is gated by `protectionEnabled` on the template version — ensure it's set to 1 before testing
- When checking deployment status, the main bundle hash may change but lazy-loaded chunks contain the actual component code — always check the specific chunk
