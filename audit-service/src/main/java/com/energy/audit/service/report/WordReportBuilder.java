package com.energy.audit.service.report;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class WordReportBuilder {

    private WordReportBuilder() {}

    public static void buildReport(Path outputPath, String title, int auditYear,
                                   Map<String, Object> data) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            addCoverPage(doc, title, auditYear, data);
            addPageBreak(doc);
            addTableOfContents(doc);
            addPageBreak(doc);

            addChapter(doc, "一、企业基本情况");
            addEnterpriseInfo(doc, data);

            addChapter(doc, "二、主要技术经济指标");
            addTechIndicators(doc, auditYear, data);

            addChapter(doc, "三、能源消费结构分析");
            addEnergyBalance(doc, data);

            addChapter(doc, "四、产品单位能耗分析");
            addProductConsumption(doc, data);

            addChapter(doc, "五、温室气体排放分析");
            addGhgEmission(doc, data);

            addChapter(doc, "六、能源流向分析");
            addEnergyFlow(doc, data);

            addChapter(doc, "七、节能建议与措施");
            addSuggestions(doc);

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                doc.write(fos);
            }
        }
    }

    private static void addCoverPage(XWPFDocument doc, String title, int auditYear,
                                     Map<String, Object> data) {
        addEmptyLines(doc, 6);

        XWPFParagraph titlePara = doc.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(title);
        titleRun.setBold(true);
        titleRun.setFontSize(22);
        titleRun.setFontFamily("SimHei");
        titleRun.setColor("00897B");

        addEmptyLines(doc, 2);

        XWPFParagraph subtitlePara = doc.createParagraph();
        subtitlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun subtitleRun = subtitlePara.createRun();
        subtitleRun.setText("审计期间：" + auditYear + "年1月 — " + auditYear + "年12月");
        subtitleRun.setFontSize(14);
        subtitleRun.setFontFamily("SimSun");

        addEmptyLines(doc, 4);

        String enterpriseName = str(data, "enterpriseName");
        addCenterLine(doc, "被审计企业：" + enterpriseName, 14);
        addCenterLine(doc, "报告生成日期：" + java.time.LocalDate.now(), 12);
    }

    private static void addTableOfContents(XWPFDocument doc) {
        addChapter(doc, "目 录");
        String[] chapters = {
            "一、企业基本情况",
            "二、主要技术经济指标",
            "三、能源消费结构分析",
            "四、产品单位能耗分析",
            "五、温室气体排放分析",
            "六、能源流向分析",
            "七、节能建议与措施",
        };
        for (String ch : chapters) {
            XWPFParagraph p = doc.createParagraph();
            p.setIndentationLeft(720);
            XWPFRun r = p.createRun();
            r.setText(ch);
            r.setFontSize(12);
            r.setFontFamily("SimSun");
        }
    }

    @SuppressWarnings("unchecked")
    private static void addEnterpriseInfo(XWPFDocument doc, Map<String, Object> data) {
        Map<String, Object> ent = (Map<String, Object>) data.getOrDefault("enterprise", Map.of());
        String[][] infoRows = {
            {"企业名称", mapStr(ent, "ENTERPRISE_NAME", "enterprise_name")},
            {"统一社会信用代码", mapStr(ent, "CREDIT_CODE", "credit_code")},
            {"法定代表人", mapStr(ent, "LEGAL_PERSON", "legal_person")},
            {"企业地址", mapStr(ent, "ADDRESS", "address")},
            {"所属行业", mapStr(ent, "INDUSTRY", "industry")},
        };

        XWPFTable table = doc.createTable(infoRows.length, 2);
        setTableWidth(table, 9000);
        for (int i = 0; i < infoRows.length; i++) {
            setCellText(table.getRow(i).getCell(0), infoRows[i][0], true);
            setCellText(table.getRow(i).getCell(1), infoRows[i][1], false);
        }
        addEmptyLines(doc, 1);
    }

    @SuppressWarnings("unchecked")
    private static void addTechIndicators(XWPFDocument doc, int auditYear, Map<String, Object> data) {
        List<Map<String, Object>> indicators = (List<Map<String, Object>>) data.getOrDefault("techIndicators", List.of());
        if (indicators.isEmpty()) {
            addBodyText(doc, "暂无技术经济指标数据。");
            return;
        }

        addBodyText(doc, "以下为企业近年主要技术经济指标对比：");

        String[] headers = {"年度", "综合能耗当量(tce)", "综合能耗等价(tce)", "工业总产值(万元)", "单位产值能耗(tce/万元)"};
        XWPFTable table = doc.createTable(1, headers.length);
        setTableWidth(table, 9000);
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            setCellText(headerRow.getCell(i), headers[i], true);
        }

        for (Map<String, Object> row : indicators) {
            XWPFTableRow tr = table.createRow();
            setCellText(tr.getCell(0), mapStr(row, "INDICATOR_YEAR", "indicator_year"), false);
            setCellText(tr.getCell(1), formatNum(row, "TOTAL_ENERGY_EQUIV", "total_energy_equiv"), false);
            setCellText(tr.getCell(2), formatNum(row, "TOTAL_ENERGY_EQUAL", "total_energy_equal"), false);
            setCellText(tr.getCell(3), formatNum(row, "GROSS_OUTPUT", "gross_output"), false);
            setCellText(tr.getCell(4), formatNum(row, "UNIT_OUTPUT_ENERGY", "unit_output_energy"), false);
        }
        addEmptyLines(doc, 1);
    }

    @SuppressWarnings("unchecked")
    private static void addEnergyBalance(XWPFDocument doc, Map<String, Object> data) {
        List<Map<String, Object>> balance = (List<Map<String, Object>>) data.getOrDefault("energyBalance", List.of());
        if (balance.isEmpty()) {
            addBodyText(doc, "暂无能源消费结构数据。");
            return;
        }

        addBodyText(doc, "企业能源消费结构如下表所示：");

        String[] headers = {"能源品种", "能源类别", "消费量(折标煤tce)"};
        XWPFTable table = doc.createTable(1, headers.length);
        setTableWidth(table, 9000);
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            setCellText(headerRow.getCell(i), headers[i], true);
        }

        double total = 0;
        for (Map<String, Object> row : balance) {
            XWPFTableRow tr = table.createRow();
            setCellText(tr.getCell(0), mapStr(row, "ENERGY_NAME", "energy_name"), false);
            setCellText(tr.getCell(1), mapStr(row, "ENERGY_CATEGORY", "energy_category"), false);
            String val = formatNum(row, "STANDARD_COAL_EQUIV", "standard_coal_equiv");
            setCellText(tr.getCell(2), val, false);
            total += parseDouble(row, "STANDARD_COAL_EQUIV", "standard_coal_equiv");
        }

        XWPFTableRow totalRow = table.createRow();
        setCellText(totalRow.getCell(0), "合计", true);
        setCellText(totalRow.getCell(1), "", false);
        setCellText(totalRow.getCell(2), String.format("%.1f", total), true);

        addEmptyLines(doc, 1);
    }

    @SuppressWarnings("unchecked")
    private static void addProductConsumption(XWPFDocument doc, Map<String, Object> data) {
        List<Map<String, Object>> products = (List<Map<String, Object>>) data.getOrDefault("productConsumption", List.of());
        if (products.isEmpty()) {
            addBodyText(doc, "暂无产品单位能耗数据。");
            return;
        }

        addBodyText(doc, "主要产品单位能耗对比分析：");

        String[] headers = {"产品名称", "年度类型", "产量", "能耗(tce)", "单耗(tce/t)"};
        XWPFTable table = doc.createTable(1, headers.length);
        setTableWidth(table, 9000);
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            setCellText(headerRow.getCell(i), headers[i], true);
        }

        for (Map<String, Object> row : products) {
            XWPFTableRow tr = table.createRow();
            setCellText(tr.getCell(0), mapStr(row, "PRODUCT_NAME", "product_name"), false);
            setCellText(tr.getCell(1), mapStr(row, "YEAR_TYPE", "year_type"), false);
            setCellText(tr.getCell(2), formatNum(row, "OUTPUT", "output"), false);
            setCellText(tr.getCell(3), formatNum(row, "ENERGY_CONSUMPTION", "energy_consumption"), false);
            setCellText(tr.getCell(4), formatNum(row, "UNIT_CONSUMPTION", "unit_consumption"), false);
        }
        addEmptyLines(doc, 1);
    }

    @SuppressWarnings("unchecked")
    private static void addGhgEmission(XWPFDocument doc, Map<String, Object> data) {
        List<Map<String, Object>> ghg = (List<Map<String, Object>>) data.getOrDefault("ghgEmission", List.of());
        if (ghg.isEmpty()) {
            addBodyText(doc, "暂无温室气体排放数据。");
            return;
        }

        addBodyText(doc, "温室气体排放清单如下：");

        String[] headers = {"排放类型", "能源品种", "主要设备", "年排放量(tCO\u2082)"};
        XWPFTable table = doc.createTable(1, headers.length);
        setTableWidth(table, 9000);
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            setCellText(headerRow.getCell(i), headers[i], true);
        }

        double total = 0;
        for (Map<String, Object> row : ghg) {
            XWPFTableRow tr = table.createRow();
            setCellText(tr.getCell(0), mapStr(row, "EMISSION_TYPE", "emission_type"), false);
            setCellText(tr.getCell(1), mapStr(row, "ENERGY_NAME", "energy_name"), false);
            setCellText(tr.getCell(2), mapStr(row, "MAIN_EQUIPMENT", "main_equipment"), false);
            String val = formatNum(row, "ANNUAL_EMISSION", "annual_emission");
            setCellText(tr.getCell(3), val, false);
            total += parseDouble(row, "ANNUAL_EMISSION", "annual_emission");
        }

        XWPFTableRow totalRow = table.createRow();
        setCellText(totalRow.getCell(0), "合计", true);
        setCellText(totalRow.getCell(1), "", false);
        setCellText(totalRow.getCell(2), "", false);
        setCellText(totalRow.getCell(3), String.format("%.1f", total), true);

        addEmptyLines(doc, 1);
    }

    @SuppressWarnings("unchecked")
    private static void addEnergyFlow(XWPFDocument doc, Map<String, Object> data) {
        List<Map<String, Object>> flows = (List<Map<String, Object>>) data.getOrDefault("energyFlow", List.of());
        if (flows.isEmpty()) {
            addBodyText(doc, "暂无能源流向数据。");
            return;
        }

        addBodyText(doc, "企业能源流向明细如下：");

        String[] headers = {"流向阶段", "序号", "源单元", "目标单元", "能源品种", "实物量", "折标量(tce)"};
        XWPFTable table = doc.createTable(1, headers.length);
        setTableWidth(table, 9000);
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            setCellText(headerRow.getCell(i), headers[i], true);
        }

        for (Map<String, Object> row : flows) {
            XWPFTableRow tr = table.createRow();
            setCellText(tr.getCell(0), mapStr(row, "FLOW_STAGE", "flow_stage"), false);
            setCellText(tr.getCell(1), mapStr(row, "SEQ_NO", "seq_no"), false);
            setCellText(tr.getCell(2), mapStr(row, "SOURCE_UNIT", "source_unit"), false);
            setCellText(tr.getCell(3), mapStr(row, "TARGET_UNIT", "target_unit"), false);
            setCellText(tr.getCell(4), mapStr(row, "ENERGY_PRODUCT", "energy_product"), false);
            setCellText(tr.getCell(5), formatNum(row, "PHYSICAL_QUANTITY", "physical_quantity"), false);
            setCellText(tr.getCell(6), formatNum(row, "STANDARD_QUANTITY", "standard_quantity"), false);
        }
        addEmptyLines(doc, 1);
    }

    private static void addSuggestions(XWPFDocument doc) {
        addBodyText(doc, "根据上述能源审计分析，提出以下节能建议：");
        String[] suggestions = {
            "1. 优化能源结构，逐步提高清洁能源使用比例，减少煤炭等高碳能源消费。",
            "2. 加强用能设备管理，推广高效节能设备，淘汰高耗能落后设备。",
            "3. 完善能源计量体系，实现重点用能设备和关键工序的能源计量全覆盖。",
            "4. 建立能源管理体系，持续改进能源绩效，降低单位产值综合能耗。",
            "5. 加强余热余压回收利用，提高能源综合利用效率。",
            "6. 制定碳减排目标和路径，推进企业低碳绿色转型发展。",
        };
        for (String s : suggestions) {
            addBodyText(doc, s);
        }
    }

    private static void addChapter(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        p.setSpacingAfter(100);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(16);
        r.setFontFamily("SimHei");
        r.setColor("00897B");
    }

    private static void addBodyText(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationFirstLine(480);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(12);
        r.setFontFamily("SimSun");
    }

    private static void addCenterLine(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(fontSize);
        r.setFontFamily("SimSun");
    }

    private static void addEmptyLines(XWPFDocument doc, int count) {
        for (int i = 0; i < count; i++) {
            doc.createParagraph();
        }
    }

    private static void addPageBreak(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.addBreak(BreakType.PAGE);
    }

    private static void setTableWidth(XWPFTable table, int widthTwips) {
        CTTblWidth tw = table.getCTTbl().addNewTblPr().addNewTblW();
        tw.setType(STTblWidth.DXA);
        tw.setW(java.math.BigInteger.valueOf(widthTwips));
    }

    private static void setCellText(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text != null ? text : "");
        r.setFontSize(10);
        r.setFontFamily("SimSun");
        r.setBold(bold);
    }

    private static String str(Map<String, Object> data, String key) {
        Object v = data.get(key);
        return v != null ? v.toString() : "";
    }

    private static String mapStr(Map<String, Object> row, String upperKey, String lowerKey) {
        Object v = row.get(upperKey);
        if (v == null) v = row.get(lowerKey);
        return v != null ? v.toString() : "";
    }

    private static String formatNum(Map<String, Object> row, String upperKey, String lowerKey) {
        Object v = row.get(upperKey);
        if (v == null) v = row.get(lowerKey);
        if (v == null) return "0";
        if (v instanceof BigDecimal bd) {
            return bd.setScale(1, RoundingMode.HALF_UP).toPlainString();
        }
        if (v instanceof Number n) {
            return String.format("%.1f", n.doubleValue());
        }
        return v.toString();
    }

    private static double parseDouble(Map<String, Object> row, String upperKey, String lowerKey) {
        Object v = row.get(upperKey);
        if (v == null) v = row.get(lowerKey);
        if (v instanceof Number n) return n.doubleValue();
        return 0;
    }
}
