package com.energy.audit.web.controller.template;

import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
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
 */
@Tag(name = "Template", description = "模板管理：CRUD/版本/标签映射/填报/编辑锁")
@RestController
@RequestMapping("/template")
public class TemplateController {

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
    // Template CRUD
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
        templateService.create(template);
        return R.ok();
    }

    @Operation(summary = "更新模板基本信息")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody TplTemplate template) {
        template.setId(id);
        templateService.update(template);
        return R.ok();
    }

    @Operation(summary = "删除模板（软删）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.ok();
    }

    @Operation(summary = "发布模板最新草稿版本（便捷接口）")
    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        templateService.publish(id);
        return R.ok();
    }

    // =========================================================================
    // Template Version Management
    // =========================================================================

    @Operation(summary = "查询模板版本列表（不含 JSON 内容）")
    @GetMapping("/{templateId}/versions")
    public R<List<TplTemplateVersion>> listVersions(@PathVariable Long templateId) {
        return R.ok(versionService.listVersions(templateId));
    }

    @Operation(summary = "获取已发布版本（含 SpreadJS JSON）")
    @GetMapping("/{templateId}/version/published")
    public R<TplTemplateVersion> getPublishedVersion(@PathVariable Long templateId) {
        return R.ok(versionService.getPublished(templateId));
    }

    @Operation(summary = "为模板创建新草稿版本")
    @PostMapping("/{templateId}/versions")
    public R<TplTemplateVersion> createVersion(@PathVariable Long templateId) {
        return R.ok(versionService.createDraftVersion(templateId));
    }

    @Operation(summary = "上传/更新草稿版本的 SpreadJS JSON")
    @PutMapping("/versions/{versionId}/json")
    public R<Void> saveVersionJson(@PathVariable Long versionId,
                                   @RequestBody Map<String, String> body) {
        String json = body.get("templateJson");
        String changeLog = body.get("changeLog");
        versionService.saveJson(versionId, json, changeLog);
        return R.ok();
    }

    @Operation(summary = "发布指定版本")
    @PostMapping("/{templateId}/versions/{versionId}/publish")
    public R<Void> publishVersion(@PathVariable Long templateId,
                                  @PathVariable Long versionId) {
        versionService.publish(templateId, versionId);
        return R.ok();
    }

    // =========================================================================
    // Tag Mapping Management
    // =========================================================================

    @Operation(summary = "查询版本的标签映射列表")
    @GetMapping("/versions/{versionId}/tags")
    public R<List<TplTagMapping>> listTags(@PathVariable Long versionId) {
        return R.ok(tagMappingService.listByVersionId(versionId));
    }

    @Operation(summary = "替换版本的全部标签映射（先删后插）")
    @PutMapping("/versions/{versionId}/tags")
    public R<Void> replaceTags(@PathVariable Long versionId,
                               @RequestBody List<TplTagMapping> mappings) {
        tagMappingService.replaceAll(versionId, mappings);
        return R.ok();
    }

    // =========================================================================
    // Enterprise Submission (企业端 — enterpriseId 从安全上下文取，不接受客户端传参)
    // =========================================================================

    @Operation(summary = "查询当前企业的填报记录（按模板+年度）")
    @GetMapping("/submission")
    public R<TplSubmission> getSubmission(@RequestParam Long templateId,
                                          @RequestParam Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(submissionService.getByKey(enterpriseId, templateId, auditYear));
    }

    @Operation(summary = "查询当前企业所有填报记录")
    @GetMapping("/submissions")
    public R<List<TplSubmission>> listSubmissions() {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(submissionService.listByEnterprise(enterpriseId));
    }

    @Operation(summary = "保存或更新草稿填报")
    @PostMapping("/submission/draft")
    public R<TplSubmission> saveDraft(@RequestBody Map<String, Object> body) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        Long templateId = Long.parseLong(body.get("templateId").toString());
        Integer auditYear = Integer.parseInt(body.get("auditYear").toString());
        String submissionJson = (String) body.get("submissionJson");
        Integer templateVersion = Integer.parseInt(body.get("templateVersion").toString());
        return R.ok(submissionService.saveDraft(
                enterpriseId, templateId, auditYear, submissionJson, templateVersion));
    }

    @Operation(summary = "提交填报（抽取 Tag 数据存入 extracted_data，status→1）")
    @PostMapping("/submission/{submissionId}/submit")
    public R<Void> submit(@PathVariable Long submissionId,
                          @RequestParam Long templateVersionId) {
        submissionService.submit(submissionId, templateVersionId);
        return R.ok();
    }

    // =========================================================================
    // Edit Lock Management
    // =========================================================================

    @Operation(summary = "获取编辑锁（enterpriseId 从安全上下文取）")
    @PostMapping("/lock")
    public R<TplEditLock> acquireLock(@RequestParam Long templateId,
                                      @RequestParam Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(editLockService.acquireLock(enterpriseId, templateId, auditYear));
    }

    @Operation(summary = "释放编辑锁（enterpriseId 从安全上下文取）")
    @DeleteMapping("/lock")
    public R<Void> releaseLock(@RequestParam Long templateId,
                               @RequestParam Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        editLockService.releaseLock(enterpriseId, templateId, auditYear);
        return R.ok();
    }

    @Operation(summary = "检查编辑锁状态（enterpriseId 从安全上下文取）")
    @GetMapping("/lock")
    public R<TplEditLock> checkLock(@RequestParam Long templateId,
                                    @RequestParam Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(editLockService.checkLock(enterpriseId, templateId, auditYear));
    }
}
