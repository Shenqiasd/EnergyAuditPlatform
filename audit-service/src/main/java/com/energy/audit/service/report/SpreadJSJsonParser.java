package com.energy.audit.service.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parses SpreadJS JSON (submission_json) and extracts sheet data as 2D arrays.
 * Used by TemplateBasedReportBuilder to convert SpreadJS sheets into Word tables.
 */
public class SpreadJSJsonParser {

    private static final Logger log = LoggerFactory.getLogger(SpreadJSJsonParser.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Extract a specific sheet's data from submission_json by sheet name.
     *
     * @param submissionJson the raw SpreadJS JSON string
     * @param sheetName      the sheet name to find (e.g., "1.企业概况")
     * @return 2D list of cell values (rows × columns), empty list if sheet not found
     */
    public static List<List<String>> extractSheetData(String submissionJson, String sheetName) {
        try {
            JsonNode root = mapper.readTree(submissionJson);
            JsonNode sheets = root.path("sheets");
            if (sheets.isMissingNode() || !sheets.isObject()) {
                log.warn("[SpreadJSParser] No 'sheets' object in submission_json");
                return List.of();
            }

            // Try exact match first, then prefix match (startsWith) to avoid false positives
            // e.g., "1.企业概况" should NOT match "11.企业概况详细"
            JsonNode sheetNode = sheets.path(sheetName);
            if (sheetNode.isMissingNode()) {
                // Prefix match: find sheet whose name starts with the search string, or vice versa
                Iterator<Map.Entry<String, JsonNode>> fields = sheets.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    if (entry.getKey().startsWith(sheetName) || sheetName.startsWith(entry.getKey())) {
                        sheetNode = entry.getValue();
                        log.info("[SpreadJSParser] Prefix matched sheet '{}' for query '{}'", entry.getKey(), sheetName);
                        break;
                    }
                }
            }

            if (sheetNode.isMissingNode()) {
                log.warn("[SpreadJSParser] Sheet '{}' not found in submission_json", sheetName);
                return List.of();
            }

            return parseSheetNode(sheetNode);
        } catch (Exception e) {
            log.error("[SpreadJSParser] Failed to parse submission_json for sheet '{}'", sheetName, e);
            return List.of();
        }
    }

    /**
     * Extract all sheet names from submission_json.
     */
    public static List<String> getSheetNames(String submissionJson) {
        try {
            JsonNode root = mapper.readTree(submissionJson);
            JsonNode sheets = root.path("sheets");
            if (sheets.isMissingNode() || !sheets.isObject()) return List.of();
            List<String> names = new ArrayList<>();
            sheets.fieldNames().forEachRemaining(names::add);
            return names;
        } catch (Exception e) {
            log.error("[SpreadJSParser] Failed to get sheet names", e);
            return List.of();
        }
    }

    /**
     * Parse a sheet node's dataTable into a 2D list.
     * SpreadJS structure: sheet.data.dataTable[rowIndex][colIndex].value
     */
    private static List<List<String>> parseSheetNode(JsonNode sheetNode) {
        JsonNode dataTable = sheetNode.path("data").path("dataTable");
        if (dataTable.isMissingNode() || !dataTable.isObject()) {
            // Alternative path: some versions use sheet.rows directly
            dataTable = sheetNode.path("dataTable");
        }
        if (dataTable.isMissingNode() || !dataTable.isObject()) {
            return List.of();
        }

        // Find the bounds of the data
        int maxRow = -1;
        int maxCol = -1;
        Iterator<Map.Entry<String, JsonNode>> rowIterator = dataTable.fields();
        while (rowIterator.hasNext()) {
            Map.Entry<String, JsonNode> rowEntry = rowIterator.next();
            int rowIdx = parseIntSafe(rowEntry.getKey());
            if (rowIdx > maxRow) maxRow = rowIdx;
            if (rowEntry.getValue().isObject()) {
                Iterator<String> colNames = rowEntry.getValue().fieldNames();
                while (colNames.hasNext()) {
                    int colIdx = parseIntSafe(colNames.next());
                    if (colIdx > maxCol) maxCol = colIdx;
                }
            }
        }

        if (maxRow < 0 || maxCol < 0) return List.of();

        // Build the 2D array
        List<List<String>> result = new ArrayList<>();
        for (int r = 0; r <= maxRow; r++) {
            List<String> row = new ArrayList<>();
            JsonNode rowNode = dataTable.path(String.valueOf(r));
            for (int c = 0; c <= maxCol; c++) {
                if (rowNode.isMissingNode() || !rowNode.isObject()) {
                    row.add("");
                } else {
                    JsonNode cellNode = rowNode.path(String.valueOf(c));
                    row.add(extractCellValue(cellNode));
                }
            }
            result.add(row);
        }

        return result;
    }

    /**
     * Extract cell value from a SpreadJS cell node.
     * Handles: {value: "xxx"}, {value: 123}, {formula: "=A1+B1", value: 5},
     * {_calcError: "#DIV/0!", _code: 7}
     */
    private static String extractCellValue(JsonNode cellNode) {
        if (cellNode == null || cellNode.isMissingNode() || !cellNode.isObject()) {
            return "";
        }

        // Check for calc error objects
        if (cellNode.has("_calcError")) {
            return ""; // Formula error, treat as empty
        }

        JsonNode valueNode = cellNode.path("value");
        if (!valueNode.isMissingNode()) {
            if (valueNode.isNull()) return "";
            if (valueNode.isObject()) {
                // Could be a calc error embedded in value
                if (valueNode.has("_calcError")) return "";
                return valueNode.toString();
            }
            if (valueNode.isNumber()) {
                // Preserve number precision
                if (valueNode.isInt() || valueNode.isLong()) {
                    return String.valueOf(valueNode.longValue());
                }
                double d = valueNode.doubleValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            }
            if (valueNode.isBoolean()) {
                return valueNode.booleanValue() ? "是" : "否";
            }
            return valueNode.asText("");
        }

        return "";
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
