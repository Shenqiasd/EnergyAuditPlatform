package com.energy.audit.model.dto;

import lombok.Data;

@Data
public class DiscoveredField {
    private String tagName;
    private String fieldName;
    private String valueText;
    private Double valueNumber;
    private String valueDate;
    private Integer rowIndex;
    private String rowKey;
    private String type;
    private Integer sheetIndex;
    private Integer row;
    private Integer col;
    private Integer rowCount;
    private Integer colCount;

    public static DiscoveredField namedRangeTable(String name, int sheetIdx, int row, int col, int rowCount, int colCount) {
        DiscoveredField f = new DiscoveredField();
        f.setTagName(name);
        f.setType("namedRangeTable");
        f.setSheetIndex(sheetIdx);
        f.setRow(row);
        f.setCol(col);
        f.setRowCount(rowCount);
        f.setColCount(colCount);
        return f;
    }

    public static DiscoveredField namedRangeScalar(String name, int sheetIdx, int row, int col) {
        DiscoveredField f = new DiscoveredField();
        f.setTagName(name);
        f.setType("namedRangeScalar");
        f.setSheetIndex(sheetIdx);
        f.setRow(row);
        f.setCol(col);
        return f;
    }

    public static DiscoveredField cellTag(String tagValue, int sheetIdx) {
        DiscoveredField f = new DiscoveredField();
        f.setTagName(tagValue);
        f.setType("cellTag");
        f.setSheetIndex(sheetIdx);
        return f;
    }
}
