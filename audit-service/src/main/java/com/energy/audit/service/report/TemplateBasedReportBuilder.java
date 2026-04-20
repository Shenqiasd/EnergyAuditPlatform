package com.energy.audit.service.report;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Template-based report builder that reads a Word (.docx) template with batch annotations (批注),
 * finds each annotation position, and inserts SpreadJS sheet data as Word tables or images.
 *
 * Annotation mapping (from the 0417.doc template):
 *   ID 0: 报告年份 (text replacement)
 *   ID 1: 企业唯一编号 (text replacement)
 *   ID 2: 企业名称 (text replacement)
 *   ID 3: 表1 → Sheet "1.企业概况"
 *   ID 4: 表13 → Sheet "13.企业产品能源成本表"
 *   ID 5: 表15的企业碳排放汇总表 → Sheet "15.温室气体排放排放汇总"
 *   ID 6: 表12 → Sheet "12.单位产品能耗数据"
 *   ID 7: 表21 → Sheet "21.十五五期间节能目标"
 *   ID 8: 表2 → Sheet "2.主要技术指标"
 *   ID 9: 表8 → Sheet "8.重点用能设备能效对标"
 *   ID 10: 能源流向图 → Image insert (AntV X6 screenshot)
 *   ID 11: 表4表5 → Sheet "4.能源计量器具汇总" + Sheet "5.能源计量器具配备率"
 *   ID 12: 表15逐一输出 → Sheet "15.温室气体排放排放汇总" (detailed rows)
 *   ID 13: 平衡表 → Sheet "11.1 能源购入、消费、存储"
 *   ID 14: 表7 → Sheet "7.能源管理制度"
 *   ID 15: 表10 → Sheet "10.淘汰产品、设备和装置等目录"
 *   ID 16: 表13 → Sheet "13.企业产品能源成本表" (duplicate)
 *   ID 17: 表14 → Sheet "14.节能量计算数据"
 *   ID 18: 表9 → Sheet "9.重点设备测试数据"
 *   ID 19: 表16 → Sheet "16.节能潜力明细"
 *   ID 20: 表18 → Sheet "18.能源管理改进建议"
 *   ID 21: 表18 → Sheet "19.节能技术改造建议汇总" (annotation text says 表18 but maps to table 19)
 */
public class TemplateBasedReportBuilder {

    private static final Logger log = LoggerFactory.getLogger(TemplateBasedReportBuilder.class);

    /** Annotation ID → sheet name mapping */
    private static final Map<Integer, String> ANNOTATION_SHEET_MAP = new LinkedHashMap<>();
    static {
        // Text replacements (handled specially)
        // ID 0 → year, ID 1 → enterprise code, ID 2 → enterprise name

        // Table inserts: annotation ID → SpreadJS sheet name (partial match)
        ANNOTATION_SHEET_MAP.put(3, "1.企业概况");
        ANNOTATION_SHEET_MAP.put(4, "13.企业产品能源成本表");
        ANNOTATION_SHEET_MAP.put(5, "15.温室气体排放");
        ANNOTATION_SHEET_MAP.put(6, "12.单位产品能耗数据");
        ANNOTATION_SHEET_MAP.put(7, "21.");  // 十五五期间节能目标
        ANNOTATION_SHEET_MAP.put(8, "2.主要技术指标");
        ANNOTATION_SHEET_MAP.put(9, "8.重点用能设备能效对标");
        // ID 10 = energy flow diagram (image)
        ANNOTATION_SHEET_MAP.put(11, "4.能源计量器具汇总");  // + 表5
        ANNOTATION_SHEET_MAP.put(12, "15.温室气体排放");      // detailed
        ANNOTATION_SHEET_MAP.put(13, "11.1");                 // 能源购入、消费、存储
        ANNOTATION_SHEET_MAP.put(14, "7.能源管理制度");
        ANNOTATION_SHEET_MAP.put(15, "10.淘汰");              // 淘汰产品、设备和装置等目录
        ANNOTATION_SHEET_MAP.put(16, "13.企业产品能源成本表"); // duplicate of ID 4
        ANNOTATION_SHEET_MAP.put(17, "14.节能量计算");
        ANNOTATION_SHEET_MAP.put(18, "9.重点设备测试数据");
        ANNOTATION_SHEET_MAP.put(19, "16.节能潜力");
        ANNOTATION_SHEET_MAP.put(20, "18.能源管理改进建议");
        ANNOTATION_SHEET_MAP.put(21, "19.节能技术改造建议");
    }

    /** Additional sheet for annotation 11 (表4表5 → 2 tables) */
    private static final String ANNOTATION_11_EXTRA_SHEET = "5.能源计量器具配备率";

    /**
     * Build a report by filling a Word template with SpreadJS data.
     *
     * @param templateInputStream  the Word template (.docx) input stream
     * @param submissionJson       the raw SpreadJS JSON from tpl_submission
     * @param flowChartImage       energy flow diagram image bytes (PNG), may be null
     * @param metadata             map with keys: "year", "enterpriseCode", "enterpriseName"
     * @return the filled document as a byte array (.docx)
     */
    public static byte[] buildReport(InputStream templateInputStream,
                                     String submissionJson,
                                     byte[] flowChartImage,
                                     Map<String, String> metadata) throws Exception {
        XWPFDocument doc = new XWPFDocument(templateInputStream);

        // Step 1: Find all comment anchor positions in the document body
        Map<Integer, Integer> commentPositions = findCommentPositions(doc);
        log.info("[ReportBuilder] Found {} comment positions in template", commentPositions.size());

        // Step 2: Process annotations in reverse order (to avoid position shifting)
        List<Integer> sortedIds = new ArrayList<>(commentPositions.keySet());
        sortedIds.sort(Collections.reverseOrder());

        for (int annotationId : sortedIds) {
            int bodyIndex = commentPositions.get(annotationId);
            try {
                processAnnotation(doc, annotationId, bodyIndex, submissionJson, flowChartImage, metadata);
            } catch (Exception e) {
                log.warn("[ReportBuilder] Failed to process annotation ID={}: {}", annotationId, e.getMessage());
            }
        }

        // Step 3: Remove all comments from the document
        removeAllComments(doc);

        // Step 4: Write to byte array
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return out.toByteArray();
    }

    /**
     * Find all commentRangeStart positions in the document body.
     * Returns map: commentId → body element index
     */
    private static Map<Integer, Integer> findCommentPositions(XWPFDocument doc) {
        Map<Integer, Integer> positions = new LinkedHashMap<>();
        CTBody body = doc.getDocument().getBody();
        List<Object> bodyElements = body.getPArray().length > 0 ?
            new ArrayList<>() : new ArrayList<>();

        // Scan the XML for comment range starts
        try {
            org.w3c.dom.NodeList children = body.getDomNode().getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node child = children.item(i);
                findCommentRangeStarts(child, i, positions);
            }
        } catch (Exception e) {
            log.warn("[ReportBuilder] Error scanning for comment positions", e);
        }

        return positions;
    }

    private static void findCommentRangeStarts(org.w3c.dom.Node node, int bodyIndex,
                                                Map<Integer, Integer> positions) {
        if (node == null) return;
        String localName = node.getLocalName();
        if ("commentRangeStart".equals(localName)) {
            org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
            if (attrs != null) {
                org.w3c.dom.Node idAttr = attrs.getNamedItemNS(
                    "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "id");
                if (idAttr != null) {
                    try {
                        int commentId = Integer.parseInt(idAttr.getNodeValue());
                        positions.put(commentId, bodyIndex);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        // Recurse into children
        org.w3c.dom.NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                findCommentRangeStarts(children.item(i), bodyIndex, positions);
            }
        }
    }

    /**
     * Process a single annotation: insert table, text, or image.
     */
    private static void processAnnotation(XWPFDocument doc, int annotationId, int bodyIndex,
                                           String submissionJson, byte[] flowChartImage,
                                           Map<String, String> metadata) throws Exception {
        switch (annotationId) {
            case 0 -> replaceAnnotatedText(doc, bodyIndex, metadata.getOrDefault("year", "202X"));
            case 1 -> replaceAnnotatedText(doc, bodyIndex, metadata.getOrDefault("enterpriseCode", ""));
            case 2 -> replaceAnnotatedText(doc, bodyIndex, metadata.getOrDefault("enterpriseName", ""));
            case 10 -> {
                if (flowChartImage != null && flowChartImage.length > 0) {
                    insertImageAfter(doc, bodyIndex, flowChartImage);
                }
            }
            case 11 -> {
                // Special: insert 2 tables (表4 + 表5)
                String sheetName1 = ANNOTATION_SHEET_MAP.get(11);
                String sheetName2 = ANNOTATION_11_EXTRA_SHEET;
                insertSheetAsTable(doc, bodyIndex, submissionJson, sheetName2);
                insertSheetAsTable(doc, bodyIndex, submissionJson, sheetName1);
            }
            default -> {
                String sheetName = ANNOTATION_SHEET_MAP.get(annotationId);
                if (sheetName != null) {
                    insertSheetAsTable(doc, bodyIndex, submissionJson, sheetName);
                }
            }
        }
    }

    /**
     * Replace the text in the paragraph at bodyIndex with new text.
     * Used for text annotations (year, code, enterprise name).
     */
    private static void replaceAnnotatedText(XWPFDocument doc, int bodyIndex, String newText) {
        try {
            CTBody body = doc.getDocument().getBody();
            org.w3c.dom.NodeList children = body.getDomNode().getChildNodes();
            if (bodyIndex >= children.getLength()) return;

            // Find the paragraph and replace placeholder text
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            // Map body index to paragraph index
            int paraCount = 0;
            org.w3c.dom.NodeList bodyChildren = body.getDomNode().getChildNodes();
            for (int i = 0; i <= bodyIndex && i < bodyChildren.getLength(); i++) {
                String localName = bodyChildren.item(i).getLocalName();
                if ("p".equals(localName)) {
                    paraCount++;
                }
            }
            int paraIndex = paraCount - 1;
            if (paraIndex >= 0 && paraIndex < paragraphs.size()) {
                XWPFParagraph para = paragraphs.get(paraIndex);
                String originalText = para.getText();
                // Replace X placeholders in the text
                for (XWPFRun run : para.getRuns()) {
                    String text = run.getText(0);
                    if (text != null && (text.contains("X") || text.contains("x") || text.contains("XXX"))) {
                        run.setText(newText, 0);
                        return;
                    }
                }
                // If no placeholder found, set the first run
                if (!para.getRuns().isEmpty()) {
                    para.getRuns().get(0).setText(newText, 0);
                }
            }
        } catch (Exception e) {
            log.warn("[ReportBuilder] Failed to replace text at bodyIndex={}: {}", bodyIndex, e.getMessage());
        }
    }

    /**
     * Insert a SpreadJS sheet as a Word table after the annotation position.
     */
    private static void insertSheetAsTable(XWPFDocument doc, int bodyIndex,
                                            String submissionJson, String sheetName) {
        List<List<String>> sheetData = SpreadJSJsonParser.extractSheetData(submissionJson, sheetName);
        if (sheetData.isEmpty()) {
            log.info("[ReportBuilder] No data for sheet '{}', skipping", sheetName);
            return;
        }

        // Filter out completely empty rows
        List<List<String>> filteredData = new ArrayList<>();
        for (List<String> row : sheetData) {
            boolean hasValue = row.stream().anyMatch(v -> v != null && !v.trim().isEmpty());
            if (hasValue) {
                filteredData.add(row);
            }
        }

        if (filteredData.isEmpty()) {
            log.info("[ReportBuilder] All rows empty for sheet '{}', skipping", sheetName);
            return;
        }

        // Determine column count (max across all rows)
        int colCount = filteredData.stream().mapToInt(List::size).max().orElse(0);
        if (colCount == 0) return;

        // Cap columns at 15 to avoid extremely wide tables
        colCount = Math.min(colCount, 15);

        // Create the Word table
        XWPFTable table = doc.createTable(filteredData.size(), colCount);
        styleTable(table, colCount);

        // Fill data
        for (int r = 0; r < filteredData.size(); r++) {
            XWPFTableRow tableRow = table.getRow(r);
            List<String> dataRow = filteredData.get(r);
            boolean isHeaderRow = (r == 0); // First row as header
            for (int c = 0; c < colCount; c++) {
                String value = c < dataRow.size() ? dataRow.get(c) : "";
                setCellText(tableRow.getCell(c), value, isHeaderRow);
            }
        }

        // Move the table to the correct position (after the annotation paragraph)
        moveTableAfterBodyIndex(doc, table, bodyIndex);

        log.info("[ReportBuilder] Inserted table for sheet '{}': {}rows × {}cols",
            sheetName, filteredData.size(), colCount);
    }

    /**
     * Insert an image after the annotation position.
     */
    private static void insertImageAfter(XWPFDocument doc, int bodyIndex, byte[] imageBytes) {
        try {
            // Create a new paragraph for the image
            XWPFParagraph imagePara = doc.createParagraph();
            imagePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = imagePara.createRun();

            // Determine image dimensions
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage img = ImageIO.read(bis);
            int widthPx = img != null ? img.getWidth() : 800;
            int heightPx = img != null ? img.getHeight() : 400;

            // Scale to fit page width (~16cm = 6.3 inches)
            double maxWidthEmu = 16 * 360000; // 16cm in EMU
            double scale = maxWidthEmu / (widthPx * 9525.0); // px to EMU
            if (scale > 1) scale = 1;
            int widthEmu = (int) (widthPx * 9525 * scale);
            int heightEmu = (int) (heightPx * 9525 * scale);

            run.addPicture(new ByteArrayInputStream(imageBytes),
                XWPFDocument.PICTURE_TYPE_PNG,
                "energy-flow-diagram.png",
                widthEmu, heightEmu);

            log.info("[ReportBuilder] Inserted energy flow diagram image ({}x{})", widthPx, heightPx);
        } catch (Exception e) {
            log.warn("[ReportBuilder] Failed to insert image: {}", e.getMessage());
        }
    }

    /**
     * Move a table to be positioned after a specific body element index.
     * Since POI appends tables at the end by default, we need to move it.
     */
    private static void moveTableAfterBodyIndex(XWPFDocument doc, XWPFTable table, int targetIndex) {
        try {
            CTBody body = doc.getDocument().getBody();
            CTTbl tbl = table.getCTTbl();

            // The table was appended at the end. We need to move it.
            // Remove it from current position
            body.getDomNode().removeChild(tbl.getDomNode());

            // Find the target position and insert after it
            org.w3c.dom.NodeList children = body.getDomNode().getChildNodes();
            if (targetIndex < children.getLength() - 1) {
                org.w3c.dom.Node refNode = children.item(targetIndex + 1);
                body.getDomNode().insertBefore(tbl.getDomNode(), refNode);
            } else {
                body.getDomNode().appendChild(tbl.getDomNode());
            }
        } catch (Exception e) {
            log.warn("[ReportBuilder] Failed to move table to position {}: {}", targetIndex, e.getMessage());
            // Table stays at end — not ideal but functional
        }
    }

    /**
     * Remove all comments/annotations from the document.
     */
    private static void removeAllComments(XWPFDocument doc) {
        try {
            // Remove comment references from paragraphs
            CTBody body = doc.getDocument().getBody();
            removeCommentNodes(body.getDomNode());

            // Remove the comments part itself
            for (var rel : doc.getPackagePart().getRelationships()) {
                if (rel.getRelationshipType().contains("comments")) {
                    // Mark for removal by clearing the comments content
                    try {
                        var part = doc.getPackagePart().getRelatedPart(rel);
                        if (part != null) {
                            // Write empty comments
                            String emptyComments = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                                "<w:comments xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"/>";
                            try (OutputStream os = part.getOutputStream()) {
                                os.write(emptyComments.getBytes());
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("[ReportBuilder] Partial failure removing comments: {}", e.getMessage());
        }
    }

    /**
     * Recursively remove commentRangeStart, commentRangeEnd, and commentReference nodes.
     */
    private static void removeCommentNodes(org.w3c.dom.Node node) {
        if (node == null) return;
        List<org.w3c.dom.Node> toRemove = new ArrayList<>();
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            String localName = child.getLocalName();
            if ("commentRangeStart".equals(localName) ||
                "commentRangeEnd".equals(localName) ||
                "commentReference".equals(localName)) {
                toRemove.add(child);
            } else {
                removeCommentNodes(child);
            }
        }
        for (org.w3c.dom.Node n : toRemove) {
            node.removeChild(n);
        }
    }

    // ======================== Styling helpers ========================

    private static void styleTable(XWPFTable table, int colCount) {
        // Set table width to full page width
        CTTblWidth tw = table.getCTTbl().addNewTblPr().addNewTblW();
        tw.setType(STTblWidth.DXA);
        tw.setW(BigInteger.valueOf(9000)); // ~16cm

        // Set table borders
        CTTblBorders borders = table.getCTTbl().getTblPr().addNewTblBorders();
        setBorder(borders.addNewTop());
        setBorder(borders.addNewBottom());
        setBorder(borders.addNewLeft());
        setBorder(borders.addNewRight());
        setBorder(borders.addNewInsideH());
        setBorder(borders.addNewInsideV());
    }

    private static void setBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setColor("000000");
        border.setSpace(BigInteger.valueOf(0));
    }

    private static void setCellText(XWPFTableCell cell, String text, boolean isHeader) {
        // Clear existing content
        if (cell.getParagraphs().size() > 0) {
            cell.removeParagraph(0);
        }
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(20);
        p.setSpacingAfter(20);
        XWPFRun run = p.createRun();
        run.setText(text != null ? text : "");
        run.setFontSize(9);
        run.setFontFamily("SimSun");
        run.setBold(isHeader);

        if (isHeader) {
            // Light gray background for header
            CTShd shd = cell.getCTTc().addNewTcPr().addNewShd();
            shd.setVal(STShd.CLEAR);
            shd.setColor("auto");
            shd.setFill("D9E2F3");
        }
    }

    /**
     * Convert the filled document to HTML for TinyMCE editing.
     * Uses a simplified approach: iterate paragraphs and tables → HTML string.
     */
    public static String convertDocxToHtml(byte[] docxBytes) throws Exception {
        XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes));
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:SimSun,serif;font-size:12pt;margin:2cm;line-height:1.6;}");
        html.append("table{border-collapse:collapse;width:100%;margin:10px 0;}");
        html.append("td,th{border:1px solid #333;padding:4px 8px;font-size:9pt;text-align:center;}");
        html.append("th{background:#D9E2F3;font-weight:bold;}");
        html.append("h1{font-size:18pt;text-align:center;}");
        html.append("h2{font-size:14pt;margin-top:20px;}");
        html.append("h3{font-size:12pt;margin-top:16px;}");
        html.append("p{text-indent:2em;margin:6px 0;}");
        html.append(".center{text-align:center;text-indent:0;}");
        html.append("img{max-width:100%;height:auto;display:block;margin:10px auto;}");
        html.append("</style></head><body>");

        for (var element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph para) {
                convertParagraphToHtml(para, html);
            } else if (element instanceof XWPFTable table) {
                convertTableToHtml(table, html);
            }
        }

        html.append("</body></html>");
        doc.close();
        return html.toString();
    }

    private static void convertParagraphToHtml(XWPFParagraph para, StringBuilder html) {
        String text = para.getText().trim();
        if (text.isEmpty()) {
            html.append("<p>&nbsp;</p>\n");
            return;
        }

        // Detect heading level by font size or style
        boolean isBold = false;
        int maxFontSize = 0;
        for (XWPFRun run : para.getRuns()) {
            if (run.isBold()) isBold = true;
            if (run.getFontSizeAsDouble() != null) {
                maxFontSize = Math.max(maxFontSize, run.getFontSizeAsDouble().intValue());
            }
        }

        boolean isCenter = para.getAlignment() == ParagraphAlignment.CENTER;

        if (maxFontSize >= 18 || (isBold && maxFontSize >= 16)) {
            html.append("<h1>").append(escapeHtml(text)).append("</h1>\n");
        } else if (maxFontSize >= 14 || (isBold && maxFontSize >= 13)) {
            html.append("<h2>").append(escapeHtml(text)).append("</h2>\n");
        } else if (isBold && maxFontSize >= 11) {
            html.append("<h3>").append(escapeHtml(text)).append("</h3>\n");
        } else if (isCenter) {
            html.append("<p class='center'>").append(escapeHtml(text)).append("</p>\n");
        } else {
            html.append("<p>");
            for (XWPFRun run : para.getRuns()) {
                String runText = run.getText(0);
                if (runText == null) continue;
                if (run.isBold()) html.append("<strong>");
                html.append(escapeHtml(runText));
                if (run.isBold()) html.append("</strong>");
            }
            html.append("</p>\n");
        }
    }

    private static void convertTableToHtml(XWPFTable table, StringBuilder html) {
        html.append("<table>\n");
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 0; r < rows.size(); r++) {
            html.append("<tr>");
            String cellTag = (r == 0) ? "th" : "td";
            for (XWPFTableCell cell : rows.get(r).getTableCells()) {
                html.append("<").append(cellTag).append(">");
                html.append(escapeHtml(cell.getText()));
                html.append("</").append(cellTag).append(">");
            }
            html.append("</tr>\n");
        }
        html.append("</table>\n");
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
