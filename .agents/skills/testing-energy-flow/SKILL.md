# Testing Energy Flow Diagram

## Overview
The energy flow diagram (`/enterprise/charts/energy-flow`) renders a three-section SVG layout:
- Left: Purchase nodes (hollow circles) — 购入产出环节
- Middle: Conversion nodes (rectangles) — 加工转换环节  
- Right: Terminal/distribution nodes (rectangles) — 分配/最终消费环节

## Test Data Insertion

### Flow Data (de_energy_flow)
- **API**: `POST /api/energy-flow/save?auditYear={year}` with `List<DeEnergyFlow>` body
- **Required fields**: `flowStage` (purchased/conversion/distribution/terminal), `sourceUnit`, `targetUnit`, `energyProduct`, `physicalQuantity`, `standardQuantity`, `submissionId` (set to 0 for manual insert)
- **Auth**: Bearer token from enterprise login (use ENTERPRISE_USERNAME and ENTERPRISE_PASSWORD secrets)
- The `submissionId=0` workaround is needed because the DB has a NOT NULL constraint, but the API doesn't auto-populate it for manual inserts

### Balance Data (de_energy_balance)
- No direct insert API — this data is populated via SpreadJS Audit11.1 submission extraction
- Without balance data, the 等价值/当量值 columns next to purchase nodes show "-"
- To insert balance data, you need direct DB access (requires DB credentials)

## Visual Verification via Console

Since the SVG is rendered by Vue's reactive system, use these console queries to verify element counts:

```javascript
// Count all SVG elements in the diagram
const allGs = document.querySelectorAll('g');
for (const g of allGs) {
  if (g.children.length > 5) {
    const circles = g.querySelectorAll('circle');
    const rects = g.querySelectorAll('rect');
    const texts = g.querySelectorAll('text');
    console.log(`circles=${circles.length}, rects=${rects.length}, texts=${texts.length}`);
  }
}

// Get all text labels
const allTexts = document.querySelectorAll('text');
const labels = [...allTexts].map(t => t.textContent?.trim()).filter(Boolean);
console.log(JSON.stringify(labels));

// Check edge colors
const paths = document.querySelectorAll('path');
const colors = new Set([...paths].map(p => p.getAttribute('stroke')).filter(Boolean));
console.log('Edge colors:', [...colors]);
```

## Zoom Testing

The diagram supports Ctrl+wheel zoom. To programmatically zoom via console:
```javascript
const svgEl = document.querySelectorAll('svg')[1];
const evt = new WheelEvent('wheel', { deltaY: -100, ctrlKey: true, bubbles: true, clientX: 500, clientY: 400 });
svgEl.dispatchEvent(evt);
```

## Dynamic Layout Behavior
- `computeLayout(maxRows)` adjusts sizing based on the largest layer's node count
- <=6 nodes: spacious layout (rowH=90, circleR=34, fontSize=9)
- >6 nodes: shrinks proportionally (min rowH=46, min circleR=16, min fontSize=7)
- For 21 nodes: rowH=46, circleR=19, fontSize=7

## Known Issues
- **导出 PNG** may show "导出失败，图表为空" — this is a pre-existing bug in the SVG-to-canvas export logic that fails to find inner SVG content
- The diagram container may briefly show "暂无数据" while data loads; wait 3-5 seconds for it to render

## Devin Secrets Needed
- `ENTERPRISE_USERNAME` / `ENTERPRISE_PASSWORD` — enterprise login credentials for API auth
- DB credentials (if direct balance data insertion is needed)
