package com.energy.audit.service.template.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplSubmissionMapper;
import com.energy.audit.model.entity.template.TplSubmission;
import com.energy.audit.model.entity.template.TplTagMapping;
import com.energy.audit.service.template.SpreadsheetDataExtractor;
import com.energy.audit.service.template.SubmissionService;
import com.energy.audit.service.template.TagMappingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final TplSubmissionMapper submissionMapper;
    private final TagMappingService tagMappingService;
    private final SpreadsheetDataExtractor dataExtractor;
    private final ObjectMapper objectMapper;

    public SubmissionServiceImpl(TplSubmissionMapper submissionMapper,
                                 TagMappingService tagMappingService,
                                 SpreadsheetDataExtractor dataExtractor,
                                 ObjectMapper objectMapper) {
        this.submissionMapper = submissionMapper;
        this.tagMappingService = tagMappingService;
        this.dataExtractor = dataExtractor;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TplSubmission saveDraft(Long enterpriseId, Long templateId, Integer auditYear,
                                   String submissionJson, Integer templateVersion) {
        String operator = SecurityUtils.getRequiredCurrentUsername();
        TplSubmission existing = submissionMapper.selectByEnterpriseTemplateYear(
                enterpriseId, templateId, auditYear);
        if (existing != null) {
            if (existing.getStatus() == 1) {
                throw new BusinessException("该年度数据已提交，无法继续编辑草稿");
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
        // Tenant-scoped lookup: throws BusinessException when submission belongs to another enterprise
        TplSubmission sub = submissionMapper.selectByIdAndEnterprise(submissionId, enterpriseId);
        if (sub == null) {
            throw new BusinessException("填报记录不存在或无权操作: " + submissionId);
        }
        if (sub.getStatus() == 1) {
            throw new BusinessException("该填报已提交，请勿重复提交");
        }
        List<TplTagMapping> mappings = tagMappingService.listByVersionId(templateVersionId);
        Map<String, Object> extracted = dataExtractor.extractData(sub.getSubmissionJson(), mappings);
        String extractedJson;
        try {
            extractedJson = objectMapper.writeValueAsString(extracted);
        } catch (JsonProcessingException e) {
            throw new BusinessException("数据抽取序列化失败: " + e.getMessage());
        }
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
}
