package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.result.R;
import com.energy.audit.model.entity.enterprise.EntRegistration;
import com.energy.audit.service.enterprise.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public Registration", description = "Public enterprise registration endpoint (no auth)")
@RestController
@RequestMapping("/public/registration")
public class PublicRegistrationController {

    private final RegistrationService registrationService;

    public PublicRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Operation(summary = "Submit registration application (public, no auth required)")
    @PostMapping
    public R<Void> submit(@RequestBody EntRegistration registration) {
        registrationService.submit(registration);
        return R.ok();
    }
}
