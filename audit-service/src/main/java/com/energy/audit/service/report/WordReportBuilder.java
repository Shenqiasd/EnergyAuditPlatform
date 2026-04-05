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

            addBasicInfoTable(doc, data);
            addPageBreak(doc);

            addChapter1(doc, data, auditYear);
            addPageBreak(doc);
            addChapter2(doc, data, auditYear);
            addPageBreak(doc);
            addChapter3(doc, data, auditYear);
            addPageBreak(doc);
            addChapter4(doc, data, auditYear);
            addPageBreak(doc);
            addChapter5(doc, data, auditYear);
            addPageBreak(doc);
            addChapter6(doc, data, auditYear);
            addPageBreak(doc);
            addChapter7(doc, data, auditYear);

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                doc.write(fos);
            }
        }
    }

    @SuppressWarnings("unchecked")
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
        Map<String, Object> setting = (Map<String, Object>) data.getOrDefault("enterpriseSetting", Map.of());
        String compilerName = mapStr(setting, "COMPILER_NAME", "compiler_name");

        addCenterLine(doc, "被审计企业：" + enterpriseName, 14);
        if (!compilerName.isEmpty()) {
            addCenterLine(doc, "编制单位：" + compilerName, 14);
        }
        addCenterLine(doc, "报告生成日期：" + java.time.LocalDate.now(), 12);
    }

    private static void addTableOfContents(XWPFDocument doc) {
        addChapterHeading(doc, "目 录");
        String[] chapters = {
            "能源审计基本信息表",
            "第一章 审计事项说明",
            "第二章 企业基本情况",
            "第三章 企业能源管理运行状况分析",
            "第四章 企业能源统计和温室气体排放数据审核",
            "第五章 企业用能情况分析",
            "第六章 主要用能设备及系统节能分析",
            "第七章 审计结论与节能降碳建议",
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
    private static void addBasicInfoTable(XWPFDocument doc, Map<String, Object> data) {
        addChapterHeading(doc, "能源审计基本信息表");
        Map<String, Object> ent = (Map<String, Object>) data.getOrDefault("enterprise", Map.of());
        Map<String, Object> setting = (Map<String, Object>) data.getOrDefault("enterpriseSetting", Map.of());

        String[][] infoRows = {
            {"企业名称", mapStr(ent, "ENTERPRISE_NAME", "enterprise_name")},
            {"统一社会信用代码", mapStr(ent, "CREDIT_CODE", "credit_code")},
            {"法人代表", mapStr(setting, "LEGAL_REPRESENTATIVE", "legal_representative")},
            {"地址", mapStr(setting, "ENTERPRISE_ADDRESS", "enterprise_address")},
            {"邮编", mapStr(setting, "POSTAL_CODE", "postal_code")},
            {"传真", mapStr(setting, "FAX", "fax")},
            {"联系人", mapStr(ent, "CONTACT_PERSON", "contact_person")},
            {"联系电话", mapStr(ent, "CONTACT_PHONE", "contact_phone")},
            {"行业类别", mapStr(setting, "INDUSTRY_CATEGORY", "industry_category")},
            {"行业代码", mapStr(setting, "INDUSTRY_CODE", "industry_code")},
            {"编制单位", mapStr(setting, "COMPILER_NAME", "compiler_name")},
        };

        XWPFTable table = doc.createTable(infoRows.length, 2);
        setTableWidth(table, 9000);
        for (int i = 0; i < infoRows.length; i++) {
            setCellText(table.getRow(i).getCell(0), infoRows[i][0], true);
            setCellText(table.getRow(i).getCell(1), infoRows[i][1], false);
        }
        addEmptyLines(doc, 1);
    }

    private static void addChapter1(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        addChapterHeading(doc, "第一章 审计事项说明");

        addSectionHeading(doc, "1.1 任务来源");
        addBodyText(doc, "（待填写：审计任务来源说明）");

        addSectionHeading(doc, "1.2 审计目的");
        addBodyText(doc, "（待填写：审计目的）");

        addSectionHeading(doc, "1.3 审计依据");
        addBodyText(doc, "（待填写：审计依据和相关法规标准列表）");

        addSectionHeading(doc, "1.4 审计期、范围和内容");
        addBodyText(doc, "审计期：" + auditYear + "年1月1日至" + auditYear + "年12月31日。");
        addBodyText(doc, "（待填写：审计范围和内容说明）");

        addSectionHeading(doc, "1.5 审计情况说明");
        addBodyText(doc, "（待填写：审计情况说明及已实施节能技改项目）");
    }

    @SuppressWarnings("unchecked")
    private static void addChapter2(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        addChapterHeading(doc, "第二章 企业基本情况");

        addSectionHeading(doc, "2.1 企业简况");
        addBodyText(doc, "（待填写：企业简介）");

        addSectionHeading(doc, "表2. 企业基本情况及主要经济技术指标");
        addTable2(doc, data, auditYear);

        addSectionHeading(doc, "2.2 主要产品生产工艺概况");
        addBodyText(doc, "（待填写：主要工艺、装置说明）");

        addSectionHeading(doc, "2.3 主要供能或耗能工质系统情况");
        addBodyText(doc, "（待填写：电力/供热/制冷/压缩空气/循环水等系统说明）");

        addSectionHeading(doc, "2.4 能源流程图");
        addEnergyFlowTable(doc, data);
    }

    @SuppressWarnings("unchecked")
    private static void addTable2(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        Map<String, Object> ent = (Map<String, Object>) data.getOrDefault("enterprise", Map.of());
        Map<String, Object> setting = (Map<String, Object>) data.getOrDefault("enterpriseSetting", Map.of());
        Map<String, Object> overview = (Map<String, Object>) data.getOrDefault("companyOverview", Map.of());
        List<Map<String, Object>> indicators = (List<Map<String, Object>>) data.getOrDefault("techIndicators", List.of());

        addBodyText(doc, "上半部分 — 企业基本情况：");
        String[][] upperRows = {
            {"1. 单位名称", mapStr(ent, "ENTERPRISE_NAME", "enterprise_name")},
            {"2. 法人代表", mapStr(setting, "LEGAL_REPRESENTATIVE", "legal_representative")},
            {"3. 节能主管领导", mapStr(overview, "ENERGY_LEADER_NAME", "energy_leader_name") + " / " + mapStr(overview, "ENERGY_LEADER_POSITION", "energy_leader_position")},
            {"4. 节能主管部门", mapStr(overview, "ENERGY_DEPT_NAME", "energy_dept_name")},
            {"5. 部门负责人", mapStr(overview, "ENERGY_DEPT_LEADER", "energy_dept_leader")},
            {"6. 专职管理人数", mapStr(overview, "FULLTIME_STAFF_COUNT", "fulltime_staff_count")},
            {"7. 兼职管理人数", mapStr(overview, "PARTTIME_STAFF_COUNT", "parttime_staff_count")},
            {"8. 十四五节能目标名称", mapStr(overview, "FIVE_YEAR_TARGET_NAME", "five_year_target_name")},
            {"9. 十四五节能目标值", formatNum(overview, "FIVE_YEAR_TARGET_VALUE", "five_year_target_value")},
            {"10. 目标下达部门", mapStr(overview, "FIVE_YEAR_TARGET_DEPT", "five_year_target_dept")},
        };

        XWPFTable upperTable = doc.createTable(upperRows.length, 2);
        setTableWidth(upperTable, 9000);
        for (int i = 0; i < upperRows.length; i++) {
            setCellText(upperTable.getRow(i).getCell(0), upperRows[i][0], true);
            setCellText(upperTable.getRow(i).getCell(1), upperRows[i][1], false);
        }
        addEmptyLines(doc, 1);

        if (indicators.isEmpty()) {
            addBodyText(doc, "暂无技术经济指标数据。");
            return;
        }

        addBodyText(doc, "下半部分 — 主要经济技术指标（多年对比）：");

        String[] headers = {"指标项目", "单位"};
        int yearCount = indicators.size();
        String[] allHeaders = new String[2 + yearCount];
        allHeaders[0] = "指标项目";
        allHeaders[1] = "单位";
        for (int i = 0; i < yearCount; i++) {
            allHeaders[2 + i] = mapStr(indicators.get(i), "INDICATOR_YEAR", "indicator_year") + "年";
        }

        String[][] indicatorDefs = {
            {"工业总产值(现价)", "万元", "GROSS_OUTPUT", "gross_output"},
            {"销售收入", "万元", "SALES_REVENUE", "sales_revenue"},
            {"上缴利税", "万元", "TAX_PAID", "tax_paid"},
            {"综合能耗(当量值)", "tce", "TOTAL_ENERGY_EQUIV", "total_energy_equiv"},
            {"综合能耗(等价值)", "tce", "TOTAL_ENERGY_EQUAL", "total_energy_equal"},
            {"综合能耗(剔除原料)", "tce", "TOTAL_ENERGY_EXCL_MATERIAL", "total_energy_excl_material"},
            {"单位产值综合能耗", "tce/万元", "UNIT_OUTPUT_ENERGY", "unit_output_energy"},
            {"生产总成本", "万元", "PRODUCTION_COST", "production_cost"},
            {"能源消费成本", "万元", "ENERGY_TOTAL_COST", "energy_total_cost"},
            {"能源成本占比", "%", "ENERGY_COST_RATIO", "energy_cost_ratio"},
            {"节能项目数目", "个", "SAVING_PROJECT_COUNT", "saving_project_count"},
            {"节能项目投资总量", "万元", "SAVING_INVEST_TOTAL", "saving_invest_total"},
            {"节能项目节能能力", "tce", "SAVING_CAPACITY", "saving_capacity"},
            {"节能项目经济效益", "万元", "SAVING_BENEFIT", "saving_benefit"},
            {"煤炭消费总量目标", "tce", "COAL_TARGET", "coal_target"},
            {"煤炭消费实际完成", "tce", "COAL_ACTUAL", "coal_actual"},
        };

        XWPFTable indTable = doc.createTable(1, allHeaders.length);
        setTableWidth(indTable, 9000);
        XWPFTableRow headerRow = indTable.getRow(0);
        for (int i = 0; i < allHeaders.length; i++) {
            setCellText(headerRow.getCell(i), allHeaders[i], true);
        }

        for (String[] def : indicatorDefs) {
            XWPFTableRow tr = indTable.createRow();
            setCellText(tr.getCell(0), def[0], false);
            setCellText(tr.getCell(1), def[1], false);
            for (int i = 0; i < yearCount; i++) {
                setCellText(tr.getCell(2 + i), formatNum(indicators.get(i), def[2], def[3]), false);
            }
        }
        addEmptyLines(doc, 1);
    }

    private static void addChapter3(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        addChapterHeading(doc, "第三章 企业能源管理运行状况分析");

        addSectionHeading(doc, "3.1 企业能源管理方针和目标");
        addBodyText(doc, "（待填写：能源管理方针和目标）");

        addSectionHeading(doc, "3.2 企业能源管理机构和职权");
        addBodyText(doc, "（待填写：能源管理机构和职权说明）");

        addSectionHeading(doc, "3.3 企业能源文件管理");
        addBodyText(doc, "（待填写：能源管理制度清单及说明）");

        addSectionHeading(doc, "3.4 企业能源计量管理");
        addBodyText(doc, "（待填写：计量器具配备情况及计量管理说明）");

        addSectionHeading(doc, "3.5 企业能源统计管理");
        addBodyText(doc, "（待填写：能源统计工作介绍及建议）");
    }

    @SuppressWarnings("unchecked")
    private static void addChapter4(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        addChapterHeading(doc, "第四章 企业能源统计和温室气体排放数据审核");

        addSectionHeading(doc, "4.1 对能源数据和温室气体排放源数据进行审核");
        addBodyText(doc, "（待填写：审核说明）");

        addSectionHeading(doc, "4.2 对与能耗相关数据审核");
        addTechIndicatorSummary(doc, data, auditYear);

        addSectionHeading(doc, "4.3 对采用的能源折标系数和相关参数的审核");
        addBodyText(doc, "（待填写：折标系数审核说明）");

        addSectionHeading(doc, "4.4 对温室气体排放量数据进行审核");
        addGhgEmission(doc, data);
    }

    @SuppressWarnings("unchecked")
    private static void addTechIndicatorSummary(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        List<Map<String, Object>> indicators = (List<Map<String, Object>>) data.getOrDefault("techIndicators", List.of());
        if (indicators.isEmpty()) {
            addBodyText(doc, "暂无技术经济指标数据。");
            return;
        }

        addBodyText(doc, "表8. 工业总产值和产量审核表：");

        String[] headers = {"指标项目", "单位"};
        int yearCount = indicators.size();
        String[] allHeaders = new String[2 + yearCount];
        allHeaders[0] = "指标项目";
        allHeaders[1] = "单位";
        for (int i = 0; i < yearCount; i++) {
            allHeaders[2 + i] = mapStr(indicators.get(i), "INDICATOR_YEAR", "indicator_year") + "年";
        }

        String[][] defs = {
            {"工业总产值", "万元", "GROSS_OUTPUT", "gross_output"},
            {"销售收入", "万元", "SALES_REVENUE", "sales_revenue"},
            {"利税", "万元", "TAX_PAID", "tax_paid"},
            {"综合能耗(等价值)", "tce", "TOTAL_ENERGY_EQUAL", "total_energy_equal"},
        };

        XWPFTable table = doc.createTable(1, allHeaders.length);
        setTableWidth(table, 9000);
        XWPFTableRow hr = table.getRow(0);
        for (int i = 0; i < allHeaders.length; i++) {
            setCellText(hr.getCell(i), allHeaders[i], true);
        }
        for (String[] def : defs) {
            XWPFTableRow tr = table.createRow();
            setCellText(tr.getCell(0), def[0], false);
            setCellText(tr.getCell(1), def[1], false);
            for (int i = 0; i < yearCount; i++) {
                setCellText(tr.getCell(2 + i), formatNum(indicators.get(i), def[2], def[3]), false);
            }
        }
        addEmptyLines(doc, 1);

        if (indicators.size() >= 2) {
            String enterpriseName = str(data, "enterpriseName");
            Map<String, Object> current = indicators.get(indicators.size() - 1);
            Map<String, Object> previous = indicators.get(indicators.size() - 2);
            String curYear = mapStr(current, "INDICATOR_YEAR", "indicator_year");
            String prevYear = mapStr(previous, "INDICATOR_YEAR", "indicator_year");
            double curEnergy = parseDouble(current, "TOTAL_ENERGY_EQUAL", "total_energy_equal");
            double prevEnergy = parseDouble(previous, "TOTAL_ENERGY_EQUAL", "total_energy_equal");
            double change = prevEnergy != 0 ? (curEnergy - prevEnergy) / prevEnergy * 100 : 0;
            String trend = change < 0 ? "降低" : "增长";
            addBodyText(doc, String.format("%s %s年综合能耗为%.1ftce，比%s年的%.1ftce%s了%.1f%%。",
                enterpriseName, curYear, curEnergy, prevYear, prevEnergy, trend, Math.abs(change)));
        }
    }

    @SuppressWarnings("unchecked")
    private static void addChapter5(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        addChapterHeading(doc, "第五章 企业用能情况分析");

        addSectionHeading(doc, "5.1 能源消费结构");
        addBodyText(doc, "表14. 能源消费汇总表：");
        addEnergyBalance(doc, data);

        addSectionHeading(doc, "5.2 企业能源加工转换");
        addBodyText(doc, "（待填写：加工转换汇总数据）");

        addSectionHeading(doc, "5.3 各单元能源消费");
        addBodyText(doc, "（待填写：能源消费平衡表）");

        addSectionHeading(doc, "5.4 产品能源成本分析");
        addBodyText(doc, "（待填写：产品能源成本数据）");

        addSectionHeading(doc, "5.5 综合能耗计算");
        addBodyText(doc, "（待填写：节能量计算数据）");
    }

    @SuppressWarnings("unchecked")
    private static void addChapter6(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        addChapterHeading(doc, "第六章 主要用能设备及系统节能分析");

        addSectionHeading(doc, "6.1 主要用能设备分析");
        addBodyText(doc, "（待填写：主要用能设备运行效率数据）");

        addSectionHeading(doc, "6.2 淘汰设备情况");
        addBodyText(doc, "（待填写：淘汰设备清单）");

        addSectionHeading(doc, "6.3 产品单位能耗分析");
        addBodyText(doc, "表21. 主要产品单位能耗情况表：");
        addProductConsumption(doc, data);
    }

    @SuppressWarnings("unchecked")
    private static void addChapter7(XWPFDocument doc, Map<String, Object> data, int auditYear) {
        addChapterHeading(doc, "第七章 审计结论与节能降碳建议");

        addSectionHeading(doc, "7.1 节能潜力分析");
        addBodyText(doc, "（待填写：节能潜力分析表及说明）");

        addSectionHeading(doc, "7.2 能源管理改进建议");
        addBodyText(doc, "（待填写：管理改进建议清单）");

        addSectionHeading(doc, "7.3 节能技改建议");
        addBodyText(doc, "（待填写：技改建议清单）");

        addSectionHeading(doc, "7.4 整改措施");
        addBodyText(doc, "（待填写：整改措施清单）");

        addSectionHeading(doc, "7.5 十四五节能目标");
        addBodyText(doc, "（待填写：十四五期间能源消费与节能目标表）");

        addSectionHeading(doc, "7.6 节能措施项目汇总");
        addBodyText(doc, "（待填写：拟实施的节能措施项目清单）");

        addSectionHeading(doc, "审计结论总结");
        addBodyText(doc, "（待填写：审计结论总结）");
    }

    @SuppressWarnings("unchecked")
    private static void addEnergyBalance(XWPFDocument doc, Map<String, Object> data) {
        List<Map<String, Object>> balance = (List<Map<String, Object>>) data.getOrDefault("energyBalance", List.of());
        if (balance.isEmpty()) {
            addBodyText(doc, "暂无能源消费结构数据。");
            return;
        }

        String[] headers = {"能源名称", "计量单位", "购入量", "消费量", "折标量(tce)", "占比(%)"};
        XWPFTable table = doc.createTable(1, headers.length);
        setTableWidth(table, 9000);
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            setCellText(headerRow.getCell(i), headers[i], true);
        }

        double total = 0;
        for (Map<String, Object> row : balance) {
            total += parseDouble(row, "STANDARD_COAL_EQUIV", "standard_coal_equiv");
        }

        for (Map<String, Object> row : balance) {
            XWPFTableRow tr = table.createRow();
            setCellText(tr.getCell(0), mapStr(row, "ENERGY_NAME", "energy_name"), false);
            setCellText(tr.getCell(1), mapStr(row, "MEASUREMENT_UNIT", "measurement_unit"), false);
            setCellText(tr.getCell(2), formatNum(row, "PURCHASE_AMOUNT", "purchase_amount"), false);
            setCellText(tr.getCell(3), formatNum(row, "CONSUMPTION_AMOUNT", "consumption_amount"), false);
            String val = formatNum(row, "STANDARD_COAL_EQUIV", "standard_coal_equiv");
            setCellText(tr.getCell(4), val, false);
            double rowVal = parseDouble(row, "STANDARD_COAL_EQUIV", "standard_coal_equiv");
            String pct = total > 0 ? String.format("%.1f", rowVal / total * 100) : "0";
            setCellText(tr.getCell(5), pct, false);
        }

        XWPFTableRow totalRow = table.createRow();
        setCellText(totalRow.getCell(0), "合计", true);
        setCellText(totalRow.getCell(1), "", false);
        setCellText(totalRow.getCell(2), "", false);
        setCellText(totalRow.getCell(3), "", false);
        setCellText(totalRow.getCell(4), String.format("%.1f", total), true);
        setCellText(totalRow.getCell(5), "100.0", true);

        addEmptyLines(doc, 1);
    }

    @SuppressWarnings("unchecked")
    private static void addProductConsumption(XWPFDocument doc, Map<String, Object> data) {
        List<Map<String, Object>> products = (List<Map<String, Object>>) data.getOrDefault("productConsumption", List.of());
        if (products.isEmpty()) {
            addBodyText(doc, "暂无产品单位能耗数据。");
            return;
        }

        String[] headers = {"产品名称", "年度类型", "计量单位", "产量", "能耗(tce)", "单耗(tce/t)"};
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
            setCellText(tr.getCell(2), mapStr(row, "MEASUREMENT_UNIT", "measurement_unit"), false);
            setCellText(tr.getCell(3), formatNum(row, "OUTPUT", "output"), false);
            setCellText(tr.getCell(4), formatNum(row, "ENERGY_CONSUMPTION", "energy_consumption"), false);
            setCellText(tr.getCell(5), formatNum(row, "UNIT_CONSUMPTION", "unit_consumption"), false);
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

        addBodyText(doc, "表12. 温室气体排放量表：");

        String[] headers = {"排放类型", "能源品种", "主要设备/部门", "活动数据", "年排放量(tCO\u2082)"};
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
            setCellText(tr.getCell(3), formatNum(row, "ACTIVITY_DATA", "activity_data"), false);
            String val = formatNum(row, "ANNUAL_EMISSION", "annual_emission");
            setCellText(tr.getCell(4), val, false);
            total += parseDouble(row, "ANNUAL_EMISSION", "annual_emission");
        }

        XWPFTableRow totalRow = table.createRow();
        setCellText(totalRow.getCell(0), "合计", true);
        setCellText(totalRow.getCell(1), "", false);
        setCellText(totalRow.getCell(2), "", false);
        setCellText(totalRow.getCell(3), "", false);
        setCellText(totalRow.getCell(4), String.format("%.1f", total), true);

        addEmptyLines(doc, 1);
    }

    @SuppressWarnings("unchecked")
    private static void addEnergyFlowTable(XWPFDocument doc, Map<String, Object> data) {
        List<Map<String, Object>> flows = (List<Map<String, Object>>) data.getOrDefault("energyFlow", List.of());
        if (flows.isEmpty()) {
            addBodyText(doc, "暂无能源流向数据。（该部分可通过能源流程图模块导出图片插入。）");
            return;
        }

        addBodyText(doc, "企业能源流向明细如下（完整流程图请参见能源流程图模块导出图片）：");

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

    private static void addChapterHeading(XWPFDocument doc, String text) {
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

    private static void addSectionHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(100);
        p.setSpacingAfter(50);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(13);
        r.setFontFamily("SimHei");
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
