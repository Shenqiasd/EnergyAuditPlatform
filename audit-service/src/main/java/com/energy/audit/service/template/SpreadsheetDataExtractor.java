package com.energy.audit.service.template;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.model.dto.DiscoveredField;
import com.energy.audit.model.entity.template.TplTagMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpreadsheetDataExtractor {

    private static final Logger log = LoggerFactory.getLogger(SpreadsheetDataExtractor.class);
    private static final Pattern CELL_RANGE_PATTERN = Pattern.compile("([A-Z]+)(\\d+)(?::([A-Z]+)(\\d+))?");

    private final ObjectMapper objectMapper;

    public SpreadsheetDataExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> extractData(String spreadjsJson, List<TplTagMapping> tagMappings) {
        Map<String, Object> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(spreadjsJson);
            JsonNode sheets = root.get("sheets");
            if (sheets == null || !sheets.isObject()) {
                log.warn("No sheets found in SpreadJS JSON");
                return result;
            }

            List<String> sheetNameList = new ArrayList<>();
            sheets.fieldNames().forEachRemaining(sheetNameList::add);

            Map<String, JsonNode> namedRanges = parseNamedRanges(root);
            Map<String, JsonNode> cellTags = parseCellTags(sheets);

            // Parse _dynamicRanges from submission JSON (add-row feature)
            // Key: tag mapping ID (string) → {startRow, endRow, startCol, endCol}
            Map<Long, int[]> dynamicRanges = parseDynamicRanges(root);

            for (TplTagMapping mapping : tagMappings) {
                String tagName = mapping.getTagName();
                String mappingType = mapping.getMappingType() != null ? mapping.getMappingType() : "SCALAR";

                if ("EQUIPMENT_BENCHMARK".equalsIgnoreCase(mappingType)) {
                    List<Map<String, Object>> benchmarkData = extractEquipmentBenchmark(
                            sheets, sheetNameList, namedRanges, mapping, dynamicRanges);

                    if (mapping.getRequired() != null && mapping.getRequired() == 1 && benchmarkData.isEmpty()) {
                        throw new BusinessException("必填表格 [" + mapping.getFieldName() + "] 无有效数据行");
                    }

                    result.put(mapping.getFieldName(), benchmarkData);
                    log.debug("Extracted EQUIPMENT_BENCHMARK: {} tag: {} rows: {}", mapping.getFieldName(), tagName,
                            benchmarkData.size());
                } else if ("TABLE".equalsIgnoreCase(mappingType)) {
                    List<Map<String, Object>> tableData = extractTableData(
                            sheets, sheetNameList, namedRanges, mapping, dynamicRanges);

                    if (mapping.getRequired() != null && mapping.getRequired() == 1 && tableData.isEmpty()) {
                        throw new BusinessException("必填表格 [" + mapping.getFieldName() + "] 无有效数据行");
                    }

                    result.put(mapping.getFieldName(), tableData);
                    log.debug("Extracted TABLE: {} tag: {} rows: {}", mapping.getFieldName(), tagName,
                            tableData.size());
                } else {
                    Object value = null;
                    if (namedRanges.containsKey(tagName)) {
                        value = extractFromNamedRange(sheets, sheetNameList, namedRanges.get(tagName), mapping.getSheetName());
                    } else if (cellTags.containsKey(tagName)) {
                        value = extractCellValue(cellTags.get(tagName));
                    } else if (mapping.getCellRange() != null && !mapping.getCellRange().isBlank()) {
                        value = extractFromCellRange(sheets, sheetNameList, mapping);
                    }

                    value = convertType(value, mapping.getDataType());

                    if (mapping.getRequired() != null && mapping.getRequired() == 1
                            && (value == null || value.toString().isBlank())) {
                        throw new BusinessException("必填字段 [" + mapping.getFieldName() + "] 未填写");
                    }

                    putExtractedValue(result, mapping.getFieldName(), value);
                    log.debug("Extracted SCALAR: {} tag: {} value: {}", mapping.getFieldName(), tagName, value);
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to extract data from SpreadJS JSON", e);
            throw new BusinessException("解析电子表格数据失败: " + e.getMessage());
        }
        return result;
    }

    public List<DiscoveredField> discoverFields(String templateJson) {
        if (templateJson == null || templateJson.isBlank()) {
            throw new BusinessException("模板 JSON 不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(templateJson);
            List<DiscoveredField> fields = new ArrayList<>();

            List<String> sheetNameList = new ArrayList<>();
            JsonNode sheets = root.get("sheets");
            if (sheets != null && sheets.isObject()) {
                sheets.fieldNames().forEachRemaining(sheetNameList::add);
            }

            JsonNode names = root.get("names");
            if (names != null && names.isArray()) {
                for (JsonNode nameNode : names) {
                    String name = nameNode.has("name") ? nameNode.get("name").asText() : null;
                    if (name == null || name.isBlank()) continue;

                    int sheetIdx = nameNode.path("sheetIndex").asInt(0);
                    String sName = sheetIdx >= 0 && sheetIdx < sheetNameList.size()
                            ? sheetNameList.get(sheetIdx) : null;
                    int row = nameNode.path("row").asInt(0);
                    int col = nameNode.path("col").asInt(0);
                    int rowCount = nameNode.path("rowCount").asInt(1);
                    int colCount = nameNode.path("colCount").asInt(1);

                    if (rowCount > 1 || colCount > 1) {
                        fields.add(DiscoveredField.namedRangeTable(name, sheetIdx, sName, row, col, rowCount, colCount));
                    } else {
                        fields.add(DiscoveredField.namedRangeScalar(name, sheetIdx, sName, row, col));
                    }
                }
            }

            if (sheets != null && sheets.isObject()) {
                int sheetIdx = 0;
                var it = sheets.fieldNames();
                while (it.hasNext()) {
                    String sheetName = it.next();
                    JsonNode dataTable = sheets.get(sheetName).path("data").path("dataTable");
                    if (dataTable.isObject()) {
                        var rowIt = dataTable.fields();
                        while (rowIt.hasNext()) {
                            var rowEntry = rowIt.next();
                            var colIt = rowEntry.getValue().fields();
                            while (colIt.hasNext()) {
                                var colEntry = colIt.next();
                                JsonNode cell = colEntry.getValue();
                                JsonNode tagNode = cell.get("tag");
                                if (tagNode == null) continue;
                                if (tagNode.isTextual()) {
                                    String tagValue = tagNode.asText();
                                    if (!tagValue.isBlank()) {
                                        fields.add(DiscoveredField.cellTag(tagValue, sheetIdx, sheetName));
                                    }
                                }
                            }
                        }
                    }
                    sheetIdx++;
                }
            }

            return fields;
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("discoverFields: failed to parse templateJson — {}", e.getMessage());
            throw new BusinessException("模板 JSON 解析失败，字段发现已中止: " + e.getMessage());
        }
    }

    @Deprecated
    public java.util.Set<String> discoverTagNames(String templateJson) {
        java.util.Set<String> result = new java.util.HashSet<>();
        for (DiscoveredField f : discoverFields(templateJson)) {
            result.add(f.getTagName());
        }
        return result;
    }

    // ── Sheet resolution ────────────────────────────────────────────────────

    private String resolveSheetName(JsonNode sheets, List<String> sheetNameList,
                                     String preferredName, int fallbackIndex) {
        if (preferredName != null && !preferredName.isBlank() && sheets.has(preferredName)) {
            return preferredName;
        }
        if (fallbackIndex >= 0 && fallbackIndex < sheetNameList.size()) {
            return sheetNameList.get(fallbackIndex);
        }
        return null;
    }

    // ── TABLE extraction ────────────────────────────────────────────────────

    private List<Map<String, Object>> extractTableData(
            JsonNode sheets, List<String> sheetNameList,
            Map<String, JsonNode> namedRanges, TplTagMapping mapping,
            Map<Long, int[]> dynamicRanges) {

        List<Map<String, Object>> rows = new ArrayList<>();
        String tagName = mapping.getTagName();

        int startRow, startCol, rowCount, colCount;
        int sheetIdx = mapping.getSheetIndex() != null ? mapping.getSheetIndex() : 0;
        String preferredSheetName = mapping.getSheetName();

        // Priority 1: _dynamicRanges from submission JSON (add-row adjusted)
        int[] dynRange = mapping.getId() != null ? dynamicRanges.get(mapping.getId()) : null;
        if (dynRange != null) {
            startRow = dynRange[0];
            startCol = dynRange[2];
            rowCount = dynRange[1] - dynRange[0] + 1;
            colCount = dynRange[3] - dynRange[2] + 1;
            log.debug("TABLE '{}' using dynamicRange: startRow={}, rowCount={}", tagName, startRow, rowCount);
        // Priority 2: Named Range from SpreadJS JSON
        } else {
            JsonNode rangeNode = namedRanges.get(tagName);
            if (rangeNode != null) {
                int rangeSheetIdx = rangeNode.path("sheetIndex").asInt(sheetIdx);
                preferredSheetName = rangeSheetIdx >= 0 && rangeSheetIdx < sheetNameList.size()
                        ? sheetNameList.get(rangeSheetIdx) : preferredSheetName;
                sheetIdx = rangeSheetIdx;
                startRow = rangeNode.path("row").asInt(0);
                startCol = rangeNode.path("col").asInt(0);
                rowCount = rangeNode.path("rowCount").asInt(1);
                colCount = rangeNode.path("colCount").asInt(1);
            // Priority 3: static cellRange from DB
            } else if (mapping.getCellRange() != null && !mapping.getCellRange().isBlank()) {
                int[] parsed = parseCellRange(mapping.getCellRange());
                startRow = parsed[0];
                startCol = parsed[1];
                rowCount = parsed[2] - parsed[0] + 1;
                colCount = parsed[3] - parsed[1] + 1;
            } else {
                log.warn("TABLE mapping '{}' has no Named Range and no cellRange configured", tagName);
                return rows;
            }
        }

        String sheetName = resolveSheetName(sheets, sheetNameList, preferredSheetName, sheetIdx);
        if (sheetName == null) {
            log.warn("extractTableData: cannot resolve sheet for tag '{}' (sheetName={}, sheetIndex={})",
                    tagName, preferredSheetName, sheetIdx);
            return rows;
        }
        JsonNode dataTable = sheets.get(sheetName).path("data").path("dataTable");

        int headerRowAbs = mapping.getHeaderRow() != null ? (startRow + mapping.getHeaderRow()) : -1;
        Integer rowKeyCol = mapping.getRowKeyColumn();
        List<ColumnMapping> colMaps = parseColumnMappings(mapping.getColumnMappings());

        for (int r = startRow; r < startRow + rowCount; r++) {
            if (r == headerRowAbs) continue;

            JsonNode rowNode = dataTable.path(String.valueOf(r));
            if (rowNode.isMissingNode()) continue;

            boolean hasAnyValue = false;
            Map<String, Object> rowData = new HashMap<>();
            rowData.put("_rowIndex", r - startRow);

            if (rowKeyCol != null) {
                Object keyVal = extractCellValue(rowNode.path(String.valueOf(startCol + rowKeyCol)));
                if (keyVal != null) {
                    rowData.put("_rowKey", keyVal.toString());
                }
            }

            if (!colMaps.isEmpty()) {
                for (ColumnMapping cm : colMaps) {
                    int absCol = startCol + cm.col;
                    Object val = extractCellValue(rowNode.path(String.valueOf(absCol)));
                    val = convertType(val, cm.type);
                    if (val != null) hasAnyValue = true;
                    rowData.put(cm.field, val);
                }
            } else {
                for (int c = startCol; c < startCol + colCount; c++) {
                    Object val = extractCellValue(rowNode.path(String.valueOf(c)));
                    if (val != null) hasAnyValue = true;
                    rowData.put("col_" + (c - startCol), val);
                }
            }

            if (hasAnyValue) {
                rows.add(rowData);
            }
        }

        return rows;
    }

    // ── EQUIPMENT_BENCHMARK extraction ─────────────────────────────────────────

    /**
     * Custom extractor for Audit06 "重点用能设备能效对标表".
     * Scans the data area row-by-row, using column B (sectionHeaderCol) to detect
     * device-type section headers (e.g. "水泵", "空压机"). For each data row,
     * applies the device-type-specific columnMappings from the extended JSON format.
     *
     * columnMappings JSON format:
     * {
     *   "sectionHeaders": {"水泵": "PUMP", "空压机": "COMPRESSOR", ...},
     *   "sectionHeaderCol": 1,
     *   "commonColumns": [{col, field, type}, ...],
     *   "typeColumns": {"PUMP": [{col, field, type}, ...], ...}
     * }
     */
    private List<Map<String, Object>> extractEquipmentBenchmark(
            JsonNode sheets, List<String> sheetNameList,
            Map<String, JsonNode> namedRanges, TplTagMapping mapping,
            Map<Long, int[]> dynamicRanges) {

        List<Map<String, Object>> rows = new ArrayList<>();

        // 1. Resolve cell range — prefer dynamicRange, fallback to static cellRange
        int startRow, startCol, rowCount, colCount;
        int sheetIdx = mapping.getSheetIndex() != null ? mapping.getSheetIndex() : 0;
        String preferredSheetName = mapping.getSheetName();

        int[] dynRange = mapping.getId() != null ? dynamicRanges.get(mapping.getId()) : null;
        if (dynRange != null) {
            startRow = dynRange[0];
            startCol = dynRange[2];
            rowCount = dynRange[1] - dynRange[0] + 1;
            colCount = dynRange[3] - dynRange[2] + 1;
            log.debug("EQUIPMENT_BENCHMARK '{}' using dynamicRange: startRow={}, rowCount={}",
                    mapping.getTagName(), startRow, rowCount);
        } else if (mapping.getCellRange() != null && !mapping.getCellRange().isBlank()) {
            int[] parsed = parseCellRange(mapping.getCellRange());
            startRow = parsed[0];
            startCol = parsed[1];
            rowCount = parsed[2] - parsed[0] + 1;
            colCount = parsed[3] - parsed[1] + 1;
        } else {
            log.warn("EQUIPMENT_BENCHMARK mapping '{}' has no cellRange configured", mapping.getTagName());
            return rows;
        }

        String sheetName = resolveSheetName(sheets, sheetNameList, preferredSheetName, sheetIdx);
        if (sheetName == null) {
            log.warn("extractEquipmentBenchmark: cannot resolve sheet (sheetName={}, sheetIndex={})",
                    preferredSheetName, sheetIdx);
            return rows;
        }
        JsonNode dataTable = sheets.get(sheetName).path("data").path("dataTable");

        // 2. Parse extended columnMappings JSON
        String colMapJson = mapping.getColumnMappings();
        if (colMapJson == null || colMapJson.isBlank()) {
            log.warn("EQUIPMENT_BENCHMARK mapping '{}' has no columnMappings", mapping.getTagName());
            return rows;
        }

        JsonNode colMapRoot;
        try {
            colMapRoot = objectMapper.readTree(colMapJson);
        } catch (Exception e) {
            throw new BusinessException("EQUIPMENT_BENCHMARK columnMappings JSON 格式无效: " + e.getMessage());
        }

        // Parse sectionHeaders: {"水泵": "PUMP", ...}
        Map<String, String> sectionHeaders = new HashMap<>();
        JsonNode shNode = colMapRoot.get("sectionHeaders");
        if (shNode != null && shNode.isObject()) {
            shNode.fields().forEachRemaining(entry -> sectionHeaders.put(entry.getKey(), entry.getValue().asText()));
        }

        int sectionHeaderCol = colMapRoot.path("sectionHeaderCol").asInt(1); // default col B

        // Parse commonColumns
        List<ColumnMapping> commonCols = parseColumnMappingsFromNode(colMapRoot.get("commonColumns"));

        // Parse typeColumns: {"PUMP": [...], ...}
        Map<String, List<ColumnMapping>> typeCols = new HashMap<>();
        JsonNode tcNode = colMapRoot.get("typeColumns");
        if (tcNode != null && tcNode.isObject()) {
            tcNode.fields().forEachRemaining(entry ->
                    typeCols.put(entry.getKey(), parseColumnMappingsFromNode(entry.getValue())));
        }

        // 3. Scan rows, detect sections, extract data
        // Section headers are identified by column A containing "序号" (all device-type
        // header rows in the Audit06 template use "序号" in column A).
        // Data rows have a numeric sequence number in column A.
        // Template reference data rows (e.g. "数据源") are skipped by checking column A.
        String currentEquipmentType = null;

        for (int r = startRow; r < startRow + rowCount; r++) {
            JsonNode rowNode = dataTable.path(String.valueOf(r));
            if (rowNode.isMissingNode()) continue;

            // Read column A value to distinguish header rows from data rows
            Object colAVal = extractCellValue(rowNode.path(String.valueOf(startCol)));
            String colAStr = colAVal != null ? colAVal.toString().trim() : "";

            // Stop processing at template reference data marker
            if (colAStr.contains("数据源") || colAStr.contains("以下为")) {
                log.debug("Hit data-source marker at row {}: '{}', stopping extraction", r, colAStr);
                break;
            }

            // Read the section header column value
            int absHeaderCol = startCol + sectionHeaderCol;
            Object headerVal = extractCellValue(rowNode.path(String.valueOf(absHeaderCol)));
            String headerStr = headerVal != null ? headerVal.toString().trim() : "";

            // Section header rows are identified by "序号" in column A + keyword match in header col
            boolean isSectionHeader = false;
            if ("序号".equals(colAStr) && !headerStr.isEmpty()) {
                for (Map.Entry<String, String> entry : sectionHeaders.entrySet()) {
                    if (headerStr.contains(entry.getKey())) {
                        currentEquipmentType = entry.getValue();
                        isSectionHeader = true;
                        log.debug("Detected section header at row {}: '{}' → {}", r, headerStr, currentEquipmentType);
                        break;
                    }
                }
            }

            if (isSectionHeader) continue;
            if (currentEquipmentType == null) continue;

            // Check if this is a data row (has any non-empty cell beyond the header col)
            boolean hasAnyValue = false;
            Map<String, Object> rowData = new HashMap<>();
            rowData.put("equipment_type", currentEquipmentType);

            // Apply common columns
            for (ColumnMapping cm : commonCols) {
                int absCol = startCol + cm.col;
                Object val = extractCellValue(rowNode.path(String.valueOf(absCol)));
                val = convertType(val, cm.type);
                if (val != null) hasAnyValue = true;
                rowData.put(cm.field, val);
            }

            // Apply type-specific columns
            List<ColumnMapping> specificCols = typeCols.get(currentEquipmentType);
            if (specificCols != null) {
                for (ColumnMapping cm : specificCols) {
                    int absCol = startCol + cm.col;
                    Object val = extractCellValue(rowNode.path(String.valueOf(absCol)));
                    val = convertType(val, cm.type);
                    if (val != null) hasAnyValue = true;
                    rowData.put(cm.field, val);
                }
            }

            if (hasAnyValue) {
                rows.add(rowData);
            }
        }

        log.info("extractEquipmentBenchmark: extracted {} rows across {} device types from sheet '{}'",
                rows.size(), typeCols.size(), sheetName);
        return rows;
    }

    private List<ColumnMapping> parseColumnMappingsFromNode(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<ColumnMapping> result = new ArrayList<>();
        for (JsonNode item : node) {
            ColumnMapping cm = new ColumnMapping();
            cm.col = item.path("col").asInt(0);
            cm.field = item.path("field").asText(null);
            cm.type = item.path("type").asText(null);
            if (cm.field != null && !cm.field.isBlank()) {
                result.add(cm);
            }
        }
        return result;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Parse _dynamicRanges from submission JSON (add-row feature).
     * Format: { "_dynamicRanges": { "tagId": { "startRow":0, "endRow":10, "startCol":0, "endCol":5 }, ... } }
     * Returns Map&lt;tagId, int[]{startRow, endRow, startCol, endCol}&gt;.
     * If _dynamicRanges is absent or malformed, returns an empty map (backward compatible).
     */
    private Map<Long, int[]> parseDynamicRanges(JsonNode root) {
        Map<Long, int[]> map = new HashMap<>();
        JsonNode dr = root.get("_dynamicRanges");
        if (dr == null || !dr.isObject()) return map;
        dr.fields().forEachRemaining(entry -> {
            try {
                long id = Long.parseLong(entry.getKey());
                JsonNode range = entry.getValue();
                if (range.isObject()
                        && range.has("startRow") && range.has("endRow")
                        && range.has("startCol") && range.has("endCol")) {
                    map.put(id, new int[]{
                            range.get("startRow").asInt(),
                            range.get("endRow").asInt(),
                            range.get("startCol").asInt(),
                            range.get("endCol").asInt()
                    });
                }
            } catch (NumberFormatException ignored) {
                log.debug("parseDynamicRanges: skipping non-numeric key '{}'", entry.getKey());
            }
        });
        if (!map.isEmpty()) {
            log.info("Loaded {} dynamicRanges from submission JSON", map.size());
        }
        return map;
    }

    private Map<String, JsonNode> parseNamedRanges(JsonNode root) {
        Map<String, JsonNode> map = new HashMap<>();
        JsonNode names = root.get("names");
        if (names != null && names.isArray()) {
            for (JsonNode nameNode : names) {
                String name = nameNode.has("name") ? nameNode.get("name").asText() : null;
                if (name != null && !name.isBlank()) {
                    map.put(name, nameNode);
                }
            }
        }
        return map;
    }

    private Map<String, JsonNode> parseCellTags(JsonNode sheets) {
        Map<String, JsonNode> map = new HashMap<>();
        sheets.fieldNames().forEachRemaining(sheetName -> {
            JsonNode dataTable = sheets.get(sheetName).path("data").path("dataTable");
            if (dataTable.isObject()) {
                dataTable.fields().forEachRemaining(rowEntry ->
                    rowEntry.getValue().fields().forEachRemaining(colEntry -> {
                        JsonNode cell = colEntry.getValue();
                        JsonNode tagNode = cell.get("tag");
                        if (tagNode == null) return;
                        if (tagNode.isTextual()) {
                            String tagValue = tagNode.asText();
                            if (!tagValue.isBlank()) {
                                map.put(tagValue, cell);
                            }
                        } else {
                            log.debug("parseCellTags: non-text tag ignored — sheet={} type={}",
                                    sheetName, tagNode.getNodeType());
                        }
                    })
                );
            }
        });
        return map;
    }

    private Object extractFromNamedRange(JsonNode sheets, List<String> sheetNameList,
                                          JsonNode rangeNode, String preferredSheetName) {
        int sheetIdx = rangeNode.path("sheetIndex").asInt(0);
        int row = rangeNode.path("row").asInt(0);
        int col = rangeNode.path("col").asInt(0);

        // For named ranges, use rangeNode's own sheetIndex to resolve name if available
        String resolved = sheetIdx >= 0 && sheetIdx < sheetNameList.size()
                ? sheetNameList.get(sheetIdx) : preferredSheetName;
        String sheetName = resolveSheetName(sheets, sheetNameList, resolved, sheetIdx);
        if (sheetName == null) {
            log.warn("extractFromNamedRange: sheetIndex {} out of range (sheets={})", sheetIdx, sheetNameList.size());
            return null;
        }
        JsonNode cellNode = sheets.get(sheetName)
                .path("data").path("dataTable")
                .path(String.valueOf(row)).path(String.valueOf(col));
        return extractCellValue(cellNode);
    }

    private Object extractFromCellRange(JsonNode sheets, List<String> sheetNameList, TplTagMapping mapping) {
        int[] parsed = parseCellRange(mapping.getCellRange());
        int sheetIdx = mapping.getSheetIndex() != null ? mapping.getSheetIndex() : 0;
        String sheetName = resolveSheetName(sheets, sheetNameList, mapping.getSheetName(), sheetIdx);
        if (sheetName == null) {
            log.warn("extractFromCellRange: cannot resolve sheet (sheetName={}, sheetIndex={})",
                    mapping.getSheetName(), sheetIdx);
            return null;
        }
        JsonNode cellNode = sheets.get(sheetName)
                .path("data").path("dataTable")
                .path(String.valueOf(parsed[0])).path(String.valueOf(parsed[1]));
        return extractCellValue(cellNode);
    }

    private void putExtractedValue(Map<String, Object> result, String fieldName, Object value) {
        if (value != null || !result.containsKey(fieldName)) {
            result.put(fieldName, value);
        }
    }

    private Object extractCellValue(JsonNode cellNode) {
        JsonNode val = cellNode.path("value");
        if (val.isMissingNode() || val.isNull()) return null;
        // SpreadJS stores formula errors (e.g. #DIV/0!) as objects like {"_calcError":"#DIV/0!","_code":7}
        if (val.isObject()) {
            if (val.has("_calcError")) {
                log.debug("Skipping calc error cell: {}", val.path("_calcError").asText());
                return null;
            }
            return null;
        }
        if (val.isNumber()) return val.numberValue();
        return val.asText(null);
    }

    private Object convertType(Object value, String dataType) {
        if (value == null || dataType == null) return value;
        if (value instanceof Number && !"STRING".equalsIgnoreCase(dataType)) {
            Number num = (Number) value;
            return switch (dataType.toUpperCase()) {
                case "NUMBER" -> num;
                case "DATE"   -> num.toString();
                case "DICT"   -> num.toString();
                default       -> num.toString();
            };
        }
        String str = value.toString();
        try {
            return switch (dataType.toUpperCase()) {
                case "NUMBER" -> str.contains(".") ? Double.parseDouble(str) : Long.parseLong(str);
                case "DATE"   -> str;
                case "DICT"   -> str;
                default       -> str;
            };
        } catch (NumberFormatException e) {
            log.warn("Failed to convert value '{}' to type {}", str, dataType);
            return str;
        }
    }

    private int[] parseCellRange(String cellRange) {
        Matcher m = CELL_RANGE_PATTERN.matcher(cellRange.toUpperCase().trim());
        if (!m.matches()) {
            throw new BusinessException("无效的单元格范围格式: " + cellRange);
        }
        int startCol = letterToCol(m.group(1));
        int startRow = Integer.parseInt(m.group(2)) - 1;
        int endCol = m.group(3) == null ? startCol : letterToCol(m.group(3));
        int endRow = m.group(4) == null ? startRow : Integer.parseInt(m.group(4)) - 1;
        return new int[]{startRow, startCol, endRow, endCol};
    }

    private int letterToCol(String letters) {
        int col = 0;
        for (char c : letters.toCharArray()) {
            col = col * 26 + (c - 'A' + 1);
        }
        return col - 1;
    }

    private List<ColumnMapping> parseColumnMappings(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<ColumnMapping> mappings = objectMapper.readValue(json, new TypeReference<List<ColumnMapping>>() {});
            for (int i = 0; i < mappings.size(); i++) {
                ColumnMapping cm = mappings.get(i);
                if (cm.field == null || cm.field.isBlank()) {
                    throw new BusinessException("columnMappings 中第 " + (i + 1) + " 项 (col=" + cm.col + ") 缺少 field 属性");
                }
            }
            return mappings;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("列映射 JSON 格式无效: " + e.getMessage());
        }
    }

    public static class ColumnMapping {
        public int col;
        public String field;
        public String type;
    }
}
