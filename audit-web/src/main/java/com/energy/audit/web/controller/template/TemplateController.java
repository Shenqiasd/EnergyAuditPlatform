package com.energy.audit.web.controller.template;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.dto.SaveDraftRequest;
import com.energy.audit.model.entity.template.TplEditLock;
import com.energy.audit.model.entity.template.TplSubmission;
import com.energy.audit.model.entity.template.TplTagMapping;
import com.energy.audit.model.entity.template.TplTemplate;
import com.energy.audit.model.entity.template.TplTemplateVersion;
import com.energy.audit.service.template.EditLockService;
import com.energy.audit.service.template.SubmissionService;
import com.energy.audit.service.template.TagMappingService;
import com.energy.audit.service.template.TemplateService;
import com.energy.audit.service.template.TemplateVersionService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Template management controller — covers template CRUD, versioning,
 * tag mapping, enterprise submission, and edit-lock management.
 *
 * Role isolation:
 *   - Admin (userType=1)     : template/version/tag write operations
 *   - Enterprise (userType=3): submission and edit-lock operations
 *   - Any authenticated user : template and version read operations
 */
@Tag(name = "Template", description = "模板管理：CRUD/版本/标签映射/填报/编辑锁")
@RestController
@RequestMapping("/template")
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    private final TemplateService templateService;
    private final TemplateVersionService versionService;
    private final TagMappingService tagMappingService;
    private final SubmissionService submissionService;
    private final EditLockService editLockService;

    public TemplateController(TemplateService templateService,
                              TemplateVersionService versionService,
                              TagMappingService tagMappingService,
                              SubmissionService submissionService,
                              EditLockService editLockService) {
        this.templateService = templateService;
        this.versionService = versionService;
        this.tagMappingService = tagMappingService;
        this.submissionService = submissionService;
        this.editLockService = editLockService;
    }

    // =========================================================================
    // Role guards
    // =========================================================================

    private void requireAdmin() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 1) {
            throw new BusinessException(403, "该操作仅管理员可执行");
        }
    }

    private void requireEnterprise() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new BusinessException(403, "该操作仅企业用户可执行");
        }
    }

    // =========================================================================
    // Template CRUD (read: any authenticated; write: admin only)
    // =========================================================================

    @Operation(summary = "分页查询模板列表")
    @GetMapping
    public R<PageResult<TplTemplate>> list(TplTemplate query,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TplTemplate> list = templateService.list(query);
        PageInfo<TplTemplate> pageInfo = new PageInfo<>(list);
        return R.ok(PageResult.of(pageInfo.getTotal(), pageInfo.getList()));
    }

    @Operation(summary = "按 ID 查询模板")
    @GetMapping("/{id}")
    public R<TplTemplate> getById(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    @Operation(summary = "按 code 查询模板")
    @GetMapping("/code/{code}")
    public R<TplTemplate> getByCode(@PathVariable String code) {
        return R.ok(templateService.getByCode(code));
    }

    @Operation(summary = "新建模板（自动创建 version=1 草稿版本）")
    @PostMapping
    public R<Void> create(@RequestBody TplTemplate template) {
        requireAdmin();
        templateService.create(template);
        return R.ok();
    }

    @Operation(summary = "更新模板基本信息")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody TplTemplate template) {
        requireAdmin();
        template.setId(id);
        templateService.update(template);
        return R.ok();
    }

    @Operation(summary = "删除模板（软删）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        requireAdmin();
        templateService.delete(id);
        return R.ok();
    }

    @Operation(summary = "发布模板最新草稿版本（便捷接口）")
    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        requireAdmin();
        templateService.publish(id);
        return R.ok();
    }

    // =========================================================================
    // Template Version Management (read: any; write: admin only)
    // =========================================================================

    @Operation(summary = "查询模板版本列表（不含 JSON 内容）")
    @GetMapping("/{templateId}/versions")
    public R<List<TplTemplateVersion>> listVersions(@PathVariable Long templateId) {
        return R.ok(versionService.listVersionsMeta(templateId));
    }

    @Operation(summary = "按 versionId 获取版本详情（含 SpreadJS JSON，仅管理员）")
    @GetMapping("/versions/{versionId}")
    public R<TplTemplateVersion> getVersionById(@PathVariable Long versionId) {
        requireAdmin();
        return R.ok(versionService.getById(versionId));
    }

    @Operation(summary = "获取已发布版本（含 SpreadJS JSON）")
    @GetMapping("/{templateId}/version/published")
    public R<TplTemplateVersion> getPublishedVersion(@PathVariable Long templateId) {
        return R.ok(versionService.getPublished(templateId));
    }

    @Operation(summary = "为模板创建新草稿版本")
    @PostMapping("/{templateId}/versions")
    public R<TplTemplateVersion> createVersion(@PathVariable Long templateId) {
        requireAdmin();
        return R.ok(versionService.createDraftVersion(templateId));
    }

    @Operation(summary = "上传/更新草稿版本的 SpreadJS JSON")
    @PutMapping("/versions/{versionId}/json")
    public R<Void> saveVersionJson(@PathVariable Long versionId,
                                   @RequestBody Map<String, String> body) {
        requireAdmin();
        String json = body.get("templateJson");
        String changeLog = body.get("changeLog");
        String protectionStr = body.get("protectionEnabled");
        Integer protectionEnabled = protectionStr != null ? Integer.valueOf(protectionStr) : null;
        versionService.saveJson(versionId, json, changeLog, protectionEnabled);
        return R.ok();
    }

    @Operation(summary = "发布指定版本")
    @PostMapping("/{templateId}/versions/{versionId}/publish")
    public R<Void> publishVersion(@PathVariable Long templateId,
                                  @PathVariable Long versionId) {
        requireAdmin();
        versionService.publish(templateId, versionId);
        return R.ok();
    }

    @Operation(summary = "删除指定版本（已发布版本不可删除）")
    @DeleteMapping("/{templateId}/versions/{versionId}")
    public R<Void> deleteVersion(@PathVariable Long templateId,
                                 @PathVariable Long versionId) {
        requireAdmin();
        versionService.deleteVersion(templateId, versionId);
        return R.ok();
    }

    // =========================================================================
    // Tag Mapping Management (read: any authenticated; write: admin only)
    // =========================================================================

    @Operation(summary = "查询版本的标签映射列表（读操作，企业用户也需要用于预填充）")
    @GetMapping("/versions/{versionId}/tags")
    public R<List<TplTagMapping>> listTags(@PathVariable Long versionId) {
        // No requireAdmin() — enterprise users need tag mappings for SpreadJS pre-fill
        return R.ok(tagMappingService.listByVersionId(versionId));
    }

    @Operation(summary = "从已保存的 templateJson 重新发现并同步 Tag 映射（管理员打开设计器时调用）")
    @PostMapping("/versions/{versionId}/tags/sync")
    public R<Void> syncTagsFromJson(@PathVariable Long versionId) {
        requireAdmin();
        TplTemplateVersion version = versionService.getById(versionId);
        if (version.getTemplateJson() != null && !version.getTemplateJson().isBlank()) {
            tagMappingService.syncFromTemplateJson(versionId, version.getTemplateJson());
        }
        return R.ok();
    }

    @Operation(summary = "替换版本的全部标签映射（先删后插）")
    @PutMapping("/versions/{versionId}/tags")
    public R<Void> replaceTags(@PathVariable Long versionId,
                               @RequestBody List<TplTagMapping> mappings) {
        requireAdmin();
        tagMappingService.replaceAll(versionId, mappings);
        return R.ok();
    }

    // =========================================================================
    // Enterprise Submission (企业端专属 — enterpriseId 从安全上下文取)
    // =========================================================================

    @Operation(summary = "查询当前企业的填报记录（按模板+年度）")
    @GetMapping("/submission")
    public R<TplSubmission> getSubmission(@RequestParam Long templateId,
                                          @RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(submissionService.getByKey(enterpriseId, templateId, auditYear));
    }

    @Operation(summary = "查询当前企业所有填报记录")
    @GetMapping("/submissions")
    public R<List<TplSubmission>> listSubmissions() {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(submissionService.listByEnterprise(enterpriseId));
    }

    @Operation(summary = "保存或更新草稿填报")
    @PostMapping("/submission/draft")
    public R<TplSubmission> saveDraft(@RequestBody @Valid SaveDraftRequest req) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        TplSubmission saved = submissionService.saveDraft(
                enterpriseId, req.getTemplateId(), req.getAuditYear(),
                req.getSubmissionJson(), req.getTemplateVersion(),
                req.getTemplateVersionId());

        // Best-effort extraction in a separate transaction — failures never roll back the save
        try {
            submissionService.extractForDraft(saved.getId(), req.getTemplateVersionId());
        } catch (Exception e) {
            log.warn("Draft extraction failed (non-blocking) for submission {}: {}",
                    saved.getId(), e.getMessage());
        }

        return R.ok(saved);
    }

    @Operation(summary = "提交填报（抽取 Tag 数据存入 extracted_data，status→1）")
    @PostMapping("/submission/{submissionId}/submit")
    public R<Void> submit(@PathVariable Long submissionId,
                          @RequestParam Long templateVersionId) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        submissionService.submit(submissionId, templateVersionId);
        return R.ok();
    }

    // =========================================================================
    // Edit Lock Management (企业端专属)
    // =========================================================================

    @Operation(summary = "获取编辑锁（enterpriseId 从安全上下文取）")
    @PostMapping("/lock")
    public R<TplEditLock> acquireLock(@RequestParam Long templateId,
                                      @RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(editLockService.acquireLock(enterpriseId, templateId, auditYear));
    }

    @Operation(summary = "释放编辑锁（enterpriseId 从安全上下文取）")
    @DeleteMapping("/lock")
    public R<Void> releaseLock(@RequestParam Long templateId,
                               @RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        editLockService.releaseLock(enterpriseId, templateId, auditYear);
        return R.ok();
    }

    @Operation(summary = "检查编辑锁状态（enterpriseId 从安全上下文取）")
    @GetMapping("/lock")
    public R<TplEditLock> checkLock(@RequestParam Long templateId,
                                    @RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(editLockService.checkLock(enterpriseId, templateId, auditYear));
    }

    @Operation(summary = "续约编辑锁（心跳，每5分钟调用一次）")
    @PutMapping("/lock")
    public R<TplEditLock> renewLock(@RequestParam Long templateId,
                                    @RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(editLockService.renewLock(enterpriseId, templateId, auditYear));
    }
}
