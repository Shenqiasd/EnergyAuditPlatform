package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.model.entity.enterprise.EntRegistration;
import com.energy.audit.service.enterprise.RegistrationService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Registration application controller
 */
@Tag(name = "Registration", description = "Enterprise registration application")
@RestController
@RequestMapping("/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Operation(summary = "Get registration by ID")
    @GetMapping("/{id}")
    public R<EntRegistration> getById(@PathVariable Long id) {
        return R.ok(registrationService.getById(id));
    }

    @Operation(summary = "List registrations with pagination")
    @GetMapping
    public R<PageResult<EntRegistration>> list(EntRegistration query,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<EntRegistration> list = registrationService.list(query);
        PageInfo<EntRegistration> pageInfo = new PageInfo<>(list);
        return R.ok(PageResult.of(pageInfo.getTotal(), pageInfo.getList()));
    }

    @Operation(summary = "Submit registration application")
    @PostMapping
    public R<Void> submit(@RequestBody EntRegistration registration) {
        registrationService.submit(registration);
        return R.ok();
    }

    @Operation(summary = "Approve registration")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String auditRemark = body != null ? body.getOrDefault("auditRemark", "") : "";
        registrationService.approve(id, auditRemark);
        return R.ok();
    }

    @Operation(summary = "Reject registration")
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String auditRemark = body != null ? body.getOrDefault("auditRemark", "") : "";
        registrationService.reject(id, auditRemark);
        return R.ok();
    }
}
