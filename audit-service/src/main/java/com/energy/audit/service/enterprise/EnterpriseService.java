package com.energy.audit.service.enterprise;

import com.energy.audit.model.entity.enterprise.EntEnterprise;

import java.time.LocalDate;
import java.util.List;

/**
 * Enterprise service interface
 */
public interface EnterpriseService {

    EntEnterprise getById(Long id);

    List<EntEnterprise> list(EntEnterprise query);

    void create(EntEnterprise enterprise);

    void update(EntEnterprise enterprise);

    void delete(Long id);

    void lock(Long id);

    void unlock(Long id);

    void updateExpireDate(Long id, LocalDate expireDate);
}
