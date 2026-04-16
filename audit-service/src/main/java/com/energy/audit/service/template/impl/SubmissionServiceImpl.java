package com.energy.audit.service.template.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplSubmissionMapper;
import com.energy.audit.model.entity.template.TplSubmission;
import com.energy.audit.model.entity.template.TplTagMapping;
import com.energy.audit.model.entity.template.TplTemplateVersion;
import com.energy.audit.service.template.DataPersistenceService;
import com.energy.audit.service.template.SpreadsheetDataExtractor;
import com.energy.audit.service.template.SubmissionService;
import com.energy.audit.service.template.TagMappingService;
import com.energy.audit.service.template.TemplateVersionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionServiceImpl.class);

    private final TplSubmissionMapper submissionMapper;
    private final TagMappingService tagMappingService;
    private final SpreadsheetDataExtractor dataExtractor;
    private final TemplateVersionService versionService;
    private final DataPersistenceService dataPersistenceService;
    private final ObjectMapper objectMapper;

    public SubmissionServiceImpl(TplSubmissionMapper submissionMapper,
                                 TagMappingService tagMappingService,
                                 SpreadsheetDataExtractor dataExtractor,
                                 TemplateVersionService versionService,
                                 DataPersistenceService dataPersistenceService,
                                 ObjectMapper objectMapper) {
        this.submissionMapper = submissionMapper;
        this.tagMappingService = tagMappingService;
        this.dataExtractor = dataExtractor;
        this.versionService = versionService;
        this.dataPersistenceService = dataPersistenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TplSubmission saveDraft(Long enterpriseId, Long templateId, Integer auditYear,
                                   String submissionJson, Integer templateVersion,
                                   Long templateVersionId) {
        // NOTE: templateVersionId is accepted but NOT used inside this transaction.
        // Extraction runs in a separate transaction via extractForDraft() called by the controller,
        // so that extraction failures never cause the save to roll back.
        // M-4: validate JSON format before persisting
        validateJson(submissionJson);

        String operator = SecurityUtils.getRequiredCurrentUsername();
        TplSubmission existing = submissionMapper.selectByEnterpriseTemplateYear(
                enterpriseId, templateId, auditYear);
        if (existing != null) {
            // Allow overwriting even after submission — enterprise may need to correct data
            if (existing.getStatus() == 1) {
                // Use dedicated SQL to NULL out submit_time & extracted_data
                // (generic updateById skips null fields due to MyBatis <if> guards)
                submissionMapper.resetToDraft(existing.getId(), operator);
                existing.setStatus(0);
                existing.setSubmitTime(null);
            }
            existing.setSubmissionJson(submissionJson);
            existing.setTemplateVersion(templateVersion);
            existing.setUpdateBy(operator);
            submissionMapper.updateById(existing);
            return existing;
        }
        TplSubmission sub = new TplSubmission();
        sub.setEnterpriseId(enterpriseId);
        sub.setTemplateId(templateId);
        sub.setTemplateVersion(templateVersion);
        sub.setAuditYear(auditYear);
        sub.setSubmissionJson(submissionJson);
        sub.setStatus(0);
        sub.setCreateBy(operator);
        sub.setUpdateBy(operator);
        submissionMapper.insert(sub);
        return sub;
    }

    @Override
    @Transactional
    public void submit(Long submissionId, Long templateVersionId) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        TplSubmission sub = submissionMapper.selectByIdAndEnterprise(submissionId, enterpriseId);
        if (sub == null) {
            throw new BusinessException("填报记录不存在或无权操作: " + submissionId);
        }
        // Allow re-submission: re-extract with latest tag mappings and overwrite previous results

        // M-3: validate that the supplied templateVersionId belongs to the same template
        // as this submission — prevents data extraction with a mismatched mapping set
        TplTemplateVersion version = versionService.getById(templateVersionId);
        if (!version.getTemplateId().equals(sub.getTemplateId())) {
            throw new BusinessException("模板版本与填报记录不匹配");
        }

        List<TplTagMapping> mappings = tagMappingService.listByVersionId(templateVersionId);
        Map<String, Object> extracted = dataExtractor.extractData(sub.getSubmissionJson(), mappings);
        String extractedJson;
        try {
            extractedJson = objectMapper.writeValueAsString(extracted);
        } catch (JsonProcessingException e) {
            throw new BusinessException("数据抽取序列化失败: " + e.getMessage());
        }

        dataPersistenceService.persistExtractedData(
                submissionId, enterpriseId, sub.getAuditYear(), extracted, mappings);

        String operator = SecurityUtils.getRequiredCurrentUsername();
        TplSubmission upd = new TplSubmission();
        upd.setId(submissionId);
        upd.setExtractedData(extractedJson);
        upd.setStatus(1);
        upd.setSubmitTime(LocalDateTime.now());
        upd.setUpdateBy(operator);
        submissionMapper.updateById(upd);
    }

    @Override
    public TplSubmission getByKey(Long enterpriseId, Long templateId, Integer auditYear) {
        return submissionMapper.selectByEnterpriseTemplateYear(enterpriseId, templateId, auditYear);
    }

    @Override
    public List<TplSubmission> listByEnterprise(Long enterpriseId) {
        return submissionMapper.selectListByEnterprise(enterpriseId);
    }

    @Override
    public TplSubmission getById(Long id) {
        TplSubmission sub = submissionMapper.selectById(id);
        if (sub == null) {
            throw new BusinessException("填报记录不存在: " + id);
        }
        return sub;
    }

    /**
     * Best-effort data extraction for a draft submission.
     * Runs in its OWN transaction (REQUIRES_NEW) so that failures never cause
     * the preceding saveDraft transaction to roll back.
     * Called from the controller layer AFTER saveDraft() has committed.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void extractForDraft(Long submissionId, Long templateVersionId) {
        if (templateVersionId == null) return;
        try {
            TplSubmission sub = submissionMapper.selectById(submissionId);
            if (sub == null) return;

            List<TplTagMapping> mappings = tagMappingService.listByVersionId(templateVersionId);
            if (mappings.isEmpty()) return;

            Map<String, Object> extracted = dataExtractor.extractData(sub.getSubmissionJson(), mappings);
            String extractedJson = objectMapper.writeValueAsString(extracted);

            dataPersistenceService.persistExtractedData(
                    submissionId, sub.getEnterpriseId(), sub.getAuditYear(), extracted, mappings);

            TplSubmission upd = new TplSubmission();
            upd.setId(submissionId);
            upd.setExtractedData(extractedJson);
            upd.setUpdateBy(SecurityUtils.getRequiredCurrentUsername());
            submissionMapper.updateById(upd);

            log.info("Draft extraction succeeded for submission {} (versionId={})",
                    submissionId, templateVersionId);
        } catch (Exception e) {
            log.warn("Draft extraction failed (non-blocking) for submission {}: {}",
                    submissionId, e.getMessage());
        }
    }

    /** M-4: ensure submissionJson is well-formed JSON before persisting */
    private void validateJson(String submissionJson) {
        if (submissionJson == null || submissionJson.isBlank()) {
            throw new BusinessException("填报数据不能为空");
        }
        try {
            objectMapper.readTree(submissionJson);
        } catch (JsonProcessingException e) {
            throw new BusinessException("填报数据 JSON 格式无效: " + e.getOriginalMessage());
        }
    }
}
