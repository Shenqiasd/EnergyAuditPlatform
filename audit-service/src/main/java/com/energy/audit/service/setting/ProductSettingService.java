package com.energy.audit.service.setting;

import com.energy.audit.model.entity.setting.BsProduct;

import java.util.List;

/**
 * Product setting service
 */
public interface ProductSettingService {

    /**
     * Tenant-scoped lookup — throws BusinessException when id does not belong to enterpriseId.
     */
    BsProduct getByIdForEnterprise(Long id, Long enterpriseId);

    List<BsProduct> list(BsProduct query);

    void create(BsProduct product);

    void update(BsProduct product);

    void delete(Long id);
}
