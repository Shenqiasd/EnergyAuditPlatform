package com.energy.audit.service.template.impl;

import com.energy.audit.model.entity.template.TplSubmission;
import com.energy.audit.service.template.SubmissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    @Override
    public TplSubmission getById(Long id) {
        return null;
    }

    @Override
    public TplSubmission getByKey(Long enterpriseId, Long templateId, Integer auditYear) {
        return null;
    }

    @Override
    public TplSubmission getByIdForEnterprise(Long submissionId, Long enterpriseId) {
        return null;
    }

    @Override
    public List<TplSubmission> listByEnterpriseAndYear(Long enterpriseId, Integer auditYear) {
        return new ArrayList<>();
    }

    @Override
    public List<TplSubmission> listByEnterprise(Long enterpriseId) {
        return new ArrayList<>();
    }

    @Override
    public TplSubmission saveDraft(Long enterpriseId, Long templateId, Integer auditYear,
                                   String submissionJson, Integer templateVersion) {
        TplSubmission sub = new TplSubmission();
        sub.setEnterpriseId(enterpriseId);
        sub.setTemplateId(templateId);
        sub.setAuditYear(auditYear);
        return sub;
    }

    @Override
    public void submit(Long submissionId, Long templateVersionId) {
    }
}
