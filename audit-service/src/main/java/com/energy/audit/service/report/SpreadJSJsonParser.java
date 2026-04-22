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

            // 1) Exact-name match (cheap, exact path lookup)
            JsonNode sheetNode = sheets.path(sheetName);
            if (!sheetNode.isMissingNode()) {
                return parseSheetNode(sheetNode);
            }

            // 2) Normalized prefix match. The live SpreadJS template sometimes has "15,温室..."
            //    (Chinese comma) while the report builder configures "15.温室...". Treat all of
            //    ,，。 as equivalent to '.', strip quotes/brackets/whitespace, then compare.
            //    Using prefix-match in BOTH directions lets a short config value like "21."
            //    match a full sheet name like "21.“十五五”期间节能目标".
            String target = normalizeSheetName(sheetName);
            if (target.isEmpty()) {
                log.warn("[SpreadJSParser] Empty sheet name after normalization: '{}'", sheetName);
                return List.of();
            }

            JsonNode bestMatch = null;
            String bestMatchKey = null;
            Iterator<Map.Entry<String, JsonNode>> fields = sheets.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String candidate = normalizeSheetName(entry.getKey());
                if (candidate.equals(target)
                        || candidate.startsWith(target)
                        || target.startsWith(candidate)) {
                    // Prefer the one whose normalized form is closest in length to the target
                    if (bestMatch == null
                            || Math.abs(candidate.length() - target.length())
                               < Math.abs(normalizeSheetName(bestMatchKey).length() - target.length())) {
                        bestMatch = entry.getValue();
                        bestMatchKey = entry.getKey();
                    }
                }
            }

            if (bestMatch != null) {
                log.info("[SpreadJSParser] Normalized match: '{}' -> sheet '{}'", sheetName, bestMatchKey);
                return parseSheetNode(bestMatch);
            }

            log.warn("[SpreadJSParser] Sheet '{}' not found in submission_json (normalized='{}')",
                    sheetName, target);
            return List.of();
        } catch (Exception e) {
            log.error("[SpreadJSParser] Failed to parse submission_json for sheet '{}'", sheetName, e);
            return List.of();
        }
    }

    /**
     * Normalize a sheet name so "15.温室气体", "15,温室气体排放汇总", and
     * "15、温室气体" all collapse to a common prefix form. Public for unit testing.
     */
    public static String normalizeSheetName(String name) {
        if (name == null) return "";
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            // Unify separators that commonly drift: ,，、。 → .
            if (c == '，' || c == ',' || c == '、' || c == '。') {
                sb.append('.');
                continue;
            }
            // Strip quotes/brackets/whitespace that don't carry semantic meaning
            if (c == '“' || c == '”' || c == '"' || c == '\''
                    || c == '‘' || c == '’'
                    || c == '（' || c == '）' || c == '(' || c == ')'
                    || c == '【' || c == '】' || c == '[' || c == ']'
                    || Character.isWhitespace(c)) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
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
