package com.energy.audit.service.template;

import com.energy.audit.model.entity.template.TplSubmission;

import java.util.List;

/**
 * Service for enterprise template submissions (tpl_submission)
 */
public interface SubmissionService {

    /**
     * Save or update a draft submission (status=0).
     * If an existing draft row for the same (enterpriseId, templateId, auditYear) exists it is updated;
     * otherwise a new row is inserted.
     *
     * @return saved/updated submission
     */
    TplSubmission saveDraft(Long enterpriseId, Long templateId, Integer auditYear,
                            String submissionJson, Integer templateVersion,
                            Long templateVersionId);

    /**
     * Submit the draft:
     *  1. Calls SpreadsheetDataExtractor to extract tag values from submissionJson
     *  2. Stores the extracted key-value map as JSON in extracted_data
     *  3. Sets status=1 and submit_time=now
     *  4. Does NOT write to de_* tables (deferred to Wave 4)
     *
     * @param submissionId  id of the TplSubmission row
     * @param templateVersionId  id of the TplTemplateVersion used to load tag mappings
     */
    void submit(Long submissionId, Long templateVersionId);

    /** Find existing submission by unique business key */
    TplSubmission getByKey(Long enterpriseId, Long templateId, Integer auditYear);

    /** List all submissions for an enterprise */
    List<TplSubmission> listByEnterprise(Long enterpriseId);

    TplSubmission getById(Long id);
}
