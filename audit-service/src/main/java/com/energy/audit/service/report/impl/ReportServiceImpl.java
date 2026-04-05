package com.energy.audit.service.report.impl;

import com.energy.audit.dao.mapper.report.ArReportMapper;
import com.energy.audit.model.entity.report.ArReport;
import com.energy.audit.service.report.ReportService;
import com.energy.audit.service.report.WordReportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    private ArReportMapper reportMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.report.upload-dir:upload/report}")
    private String uploadDir;

    @Override
    public ArReport generateReport(Long enterpriseId, Integer auditYear, String username) {
        ArReport existing = reportMapper.selectByEnterpriseAndYear(enterpriseId, auditYear, 1);
        if (existing != null && existing.getStatus() == 1) {
            throw new RuntimeException("报告正在生成中，请稍候");
        }

        if (existing != null) {
            existing.setStatus(1);
            existing.setUpdateBy(username);
            reportMapper.update(existing);
        }

        Map<String, Object> reportData = collectReportData(enterpriseId, auditYear);

        String enterpriseName = (String) reportData.getOrDefault("enterpriseName", "企业");
        String reportName = enterpriseName + " " + auditYear + "年度能源审计报告";

        Path dirPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new RuntimeException("无法创建报告目录", e);
        }

        String fileName = "report_" + enterpriseId + "_" + auditYear + "_" + System.currentTimeMillis() + ".docx";
        Path filePath = dirPath.resolve(fileName);

        try {
            WordReportBuilder.buildReport(filePath, reportName, auditYear, reportData);
        } catch (Exception e) {
            log.error("Report generation failed for enterprise={} year={}", enterpriseId, auditYear, e);
            throw new RuntimeException("报告生成失败: " + e.getMessage(), e);
        }

        if (existing != null) {
            existing.setStatus(2);
            existing.setReportName(reportName);
            existing.setGeneratedFilePath(filePath.toString());
            existing.setGenerateTime(LocalDateTime.now());
            existing.setUpdateBy(username);
            reportMapper.update(existing);
            return reportMapper.selectById(existing.getId());
        } else {
            ArReport report = new ArReport();
            report.setEnterpriseId(enterpriseId);
            report.setAuditYear(auditYear);
            report.setReportName(reportName);
            report.setReportType(1);
            report.setStatus(2);
            report.setGeneratedFilePath(filePath.toString());
            report.setGenerateTime(LocalDateTime.now());
            report.setCreateBy(username);
            report.setUpdateBy(username);
            reportMapper.insert(report);
            return reportMapper.selectById(report.getId());
        }
    }

    @Override
    public List<ArReport> listReports(Long enterpriseId, Integer auditYear) {
        return reportMapper.selectByEnterprise(enterpriseId, auditYear);
    }

    @Override
    public ArReport getReport(Long id) {
        return reportMapper.selectById(id);
    }

    @Override
    public byte[] downloadReport(Long id) {
        ArReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("报告不存在");
        }
        String path = report.getGeneratedFilePath();
        if (path == null || path.isEmpty()) {
            path = report.getUploadedFilePath();
        }
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("报告文件尚未生成");
        }
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> collectReportData(Long enterpriseId, Integer auditYear) {
        Map<String, Object> data = new HashMap<>();

        try {
            Map<String, Object> enterprise = jdbcTemplate.queryForMap(
                "SELECT enterprise_name, credit_code, contact_person, contact_email, contact_phone, remark " +
                "FROM ent_enterprise WHERE id = ? AND deleted = 0", enterpriseId);
            data.put("enterpriseName", enterprise.getOrDefault("ENTERPRISE_NAME",
                enterprise.getOrDefault("enterprise_name", "")));
            data.put("enterprise", enterprise);
        } catch (Exception e) {
            log.warn("Failed to load enterprise info for id={}", enterpriseId, e);
            data.put("enterpriseName", "企业");
            data.put("enterprise", new HashMap<>());
        }

        try {
            List<Map<String, Object>> indicators = jdbcTemplate.queryForList(
                "SELECT * FROM de_tech_indicator WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY indicator_year", enterpriseId, auditYear);
            data.put("techIndicators", indicators);
        } catch (Exception e) {
            data.put("techIndicators", List.of());
        }

        try {
            List<Map<String, Object>> balance = jdbcTemplate.queryForList(
                "SELECT * FROM de_energy_balance WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY standard_coal_equiv DESC", enterpriseId, auditYear);
            data.put("energyBalance", balance);
        } catch (Exception e) {
            data.put("energyBalance", List.of());
        }

        try {
            List<Map<String, Object>> products = jdbcTemplate.queryForList(
                "SELECT * FROM de_product_unit_consumption WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY product_name, year_type", enterpriseId, auditYear);
            data.put("productConsumption", products);
        } catch (Exception e) {
            data.put("productConsumption", List.of());
        }

        try {
            List<Map<String, Object>> ghg = jdbcTemplate.queryForList(
                "SELECT * FROM de_ghg_emission WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY emission_type, annual_emission DESC", enterpriseId, auditYear);
            data.put("ghgEmission", ghg);
        } catch (Exception e) {
            data.put("ghgEmission", List.of());
        }

        try {
            List<Map<String, Object>> flows = jdbcTemplate.queryForList(
                "SELECT * FROM de_energy_flow WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY flow_stage, seq_no", enterpriseId, auditYear);
            data.put("energyFlow", flows);
        } catch (Exception e) {
            data.put("energyFlow", List.of());
        }

        return data;
    }
}
