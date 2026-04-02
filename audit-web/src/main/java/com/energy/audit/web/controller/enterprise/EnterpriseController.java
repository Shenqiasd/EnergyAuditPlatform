package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
import com.energy.audit.service.enterprise.EnterpriseService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Enterprise management controller
 */
@Tag(name = "Enterprise", description = "Enterprise CRUD operations")
@RestController
@RequestMapping("/enterprise")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    public EnterpriseController(EnterpriseService enterpriseService) {
        this.enterpriseService = enterpriseService;
    }

    @Operation(summary = "Get enterprise by ID")
    @GetMapping("/{id}")
    public R<EntEnterprise> getById(@PathVariable Long id) {
        return R.ok(enterpriseService.getById(id));
    }

    @Operation(summary = "List enterprises with pagination")
    @GetMapping
    public R<PageResult<EntEnterprise>> list(EntEnterprise query,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<EntEnterprise> list = enterpriseService.list(query);
        PageInfo<EntEnterprise> pageInfo = new PageInfo<>(list);
        return R.ok(PageResult.of(pageInfo.getTotal(), pageInfo.getList()));
    }

    @Operation(summary = "Create enterprise")
    @PostMapping
    public R<Void> create(@RequestBody EntEnterprise enterprise) {
        enterpriseService.create(enterprise);
        return R.ok();
    }

    @Operation(summary = "Update enterprise")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody EntEnterprise enterprise) {
        enterprise.setId(id);
        enterpriseService.update(enterprise);
        return R.ok();
    }

    @Operation(summary = "Delete enterprise")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        enterpriseService.delete(id);
        return R.ok();
    }

    @Operation(summary = "Lock enterprise account")
    @PutMapping("/{id}/lock")
    public R<Void> lock(@PathVariable Long id) {
        enterpriseService.lock(id);
        return R.ok();
    }

    @Operation(summary = "Unlock enterprise account")
    @PutMapping("/{id}/unlock")
    public R<Void> unlock(@PathVariable Long id) {
        enterpriseService.unlock(id);
        return R.ok();
    }

    @Operation(summary = "Update enterprise expiry date")
    @PutMapping("/{id}/expire")
    public R<Void> updateExpireDate(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String dateStr = body.get("expireDate");
        LocalDate expireDate = dateStr != null && !dateStr.isEmpty() ? LocalDate.parse(dateStr) : null;
        enterpriseService.updateExpireDate(id, expireDate);
        return R.ok();
    }
}
