package com.energy.audit.service.template;

import com.energy.audit.model.entity.template.TplSubmission;

import java.util.List;

public interface SubmissionService {

    TplSubmission getById(Long id);

    TplSubmission getByKey(Long enterpriseId, Long templateId, Integer auditYear);

    TplSubmission getByIdForEnterprise(Long submissionId, Long enterpriseId);

    List<TplSubmission> listByEnterpriseAndYear(Long enterpriseId, Integer auditYear);

    List<TplSubmission> listByEnterprise(Long enterpriseId);

    TplSubmission saveDraft(Long enterpriseId, Long templateId, Integer auditYear,
                            String submissionJson, Integer templateVersion);

    void submit(Long submissionId, Long templateVersionId);
}
