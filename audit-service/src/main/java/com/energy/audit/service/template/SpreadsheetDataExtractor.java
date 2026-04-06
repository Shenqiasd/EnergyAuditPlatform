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
    private static final Pattern CELL_RANGE_PATTERN = Pattern.compile("([A-Z]+)(\\d+):([A-Z]+)(\\d+)");

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

            for (TplTagMapping mapping : tagMappings) {
                String tagName = mapping.getTagName();
                String mappingType = mapping.getMappingType() != null ? mapping.getMappingType() : "SCALAR";

                if ("TABLE".equalsIgnoreCase(mappingType)) {
                    List<Map<String, Object>> tableData = extractTableData(
                            sheets, sheetNameList, namedRanges, mapping);

                    if (mapping.getRequired() != null && mapping.getRequired() == 1 && tableData.isEmpty()) {
                        throw new BusinessException("必填表格 [" + mapping.getFieldName() + "] 无有效数据行");
                    }

                    result.put(mapping.getFieldName(), tableData);
                    log.debug("Extracted TABLE: {} tag: {} rows: {}", mapping.getFieldName(), tagName,
                            tableData.size());
                } else {
                    Object value = null;
                    if (namedRanges.containsKey(tagName)) {
                        value = extractFromNamedRange(sheets, sheetNameList, namedRanges.get(tagName));
                    } else if (cellTags.containsKey(tagName)) {
                        value = extractCellValue(cellTags.get(tagName));
                    }

                    value = convertType(value, mapping.getDataType());

                    if (mapping.getRequired() != null && mapping.getRequired() == 1
                            && (value == null || value.toString().isBlank())) {
                        throw new BusinessException("必填字段 [" + mapping.getFieldName() + "] 未填写");
                    }

                    result.put(mapping.getFieldName(), value);
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
            Map<String, JsonNode> namedRanges, TplTagMapping mapping) {

        List<Map<String, Object>> rows = new ArrayList<>();
        String tagName = mapping.getTagName();

        int startRow, startCol, rowCount, colCount;
        int sheetIdx = mapping.getSheetIndex() != null ? mapping.getSheetIndex() : 0;
        String mappingSheetName = mapping.getSheetName();

        JsonNode rangeNode = namedRanges.get(tagName);
        if (rangeNode != null) {
            sheetIdx = rangeNode.path("sheetIndex").asInt(sheetIdx);
            startRow = rangeNode.path("row").asInt(0);
            startCol = rangeNode.path("col").asInt(0);
            rowCount = rangeNode.path("rowCount").asInt(1);
            colCount = rangeNode.path("colCount").asInt(1);
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

        String sheetName = resolveSheetName(sheets, sheetNameList, mappingSheetName, sheetIdx);
        if (sheetName == null) {
            log.warn("extractTableData: cannot resolve sheet for tag '{}' (sheetName={}, sheetIndex={})",
                    tagName, mappingSheetName, sheetIdx);
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

    // ── Private helpers ────────────────────────────────────────────────────────

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

    private Object extractFromNamedRange(JsonNode sheets, List<String> sheetNameList, JsonNode rangeNode) {
        int sheetIdx = rangeNode.path("sheetIndex").asInt(0);
        int row = rangeNode.path("row").asInt(0);
        int col = rangeNode.path("col").asInt(0);

        String sheetName = resolveSheetName(sheets, sheetNameList, null, sheetIdx);
        if (sheetName == null) {
            log.warn("extractFromNamedRange: sheetIndex {} out of range (sheets={})", sheetIdx, sheetNameList.size());
            return null;
        }
        JsonNode cellNode = sheets.get(sheetName)
                .path("data").path("dataTable")
                .path(String.valueOf(row)).path(String.valueOf(col));
        return extractCellValue(cellNode);
    }

    private Object extractCellValue(JsonNode cellNode) {
        JsonNode val = cellNode.path("value");
        if (val.isMissingNode() || val.isNull()) return null;
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
        int endCol = letterToCol(m.group(3));
        int endRow = Integer.parseInt(m.group(4)) - 1;
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
