package com.energy.audit.service.enterprise;

import com.energy.audit.model.entity.enterprise.EntRegistration;

import java.util.List;

/**
 * Registration application service
 */
public interface RegistrationService {

    EntRegistration getById(Long id);

    List<EntRegistration> list(EntRegistration query);

    void submit(EntRegistration registration);

    void approve(Long id, String auditRemark);

    void reject(Long id, String auditRemark);
}
