package com.energy.audit.model.dto;

import lombok.Data;

@Data
public class DiscoveredField {

    private String tagName;
    private String sourceType;
    private String mappingType;
    private Integer sheetIndex;
    private String sheetName;
    private Integer row;
    private Integer col;
    private Integer rowCount;
    private Integer colCount;
    private String cellRange;

    public static DiscoveredField namedRangeScalar(String name, int sheetIndex, String sheetName, int row, int col) {
        DiscoveredField f = new DiscoveredField();
        f.tagName = name;
        f.sourceType = "NAMED_RANGE";
        f.mappingType = "SCALAR";
        f.sheetIndex = sheetIndex;
        f.sheetName = sheetName;
        f.row = row;
        f.col = col;
        f.rowCount = 1;
        f.colCount = 1;
        return f;
    }

    public static DiscoveredField namedRangeTable(String name, int sheetIndex, String sheetName, int row, int col,
                                                   int rowCount, int colCount) {
        DiscoveredField f = new DiscoveredField();
        f.tagName = name;
        f.sourceType = "NAMED_RANGE";
        f.mappingType = "TABLE";
        f.sheetIndex = sheetIndex;
        f.sheetName = sheetName;
        f.row = row;
        f.col = col;
        f.rowCount = rowCount;
        f.colCount = colCount;
        f.cellRange = colToLetter(col) + (row + 1) + ":" + colToLetter(col + colCount - 1) + (row + rowCount);
        return f;
    }

    public static DiscoveredField cellTag(String name, int sheetIndex, String sheetName) {
        DiscoveredField f = new DiscoveredField();
        f.tagName = name;
        f.sourceType = "CELL_TAG";
        f.mappingType = "SCALAR";
        f.sheetIndex = sheetIndex;
        f.sheetName = sheetName;
        return f;
    }

    private static String colToLetter(int col) {
        StringBuilder sb = new StringBuilder();
        int c = col;
        while (c >= 0) {
            sb.insert(0, (char) ('A' + (c % 26)));
            c = c / 26 - 1;
        }
        return sb.toString();
    }
}
