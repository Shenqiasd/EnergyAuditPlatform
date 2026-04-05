package com.energy.audit.service.report;

import com.energy.audit.model.entity.report.ArReport;

import java.util.List;

public interface ReportService {

    ArReport generateReport(Long enterpriseId, Integer auditYear, String username);

    List<ArReport> listReports(Long enterpriseId, Integer auditYear);

    ArReport getReport(Long id);

    byte[] downloadReport(Long id);
}
