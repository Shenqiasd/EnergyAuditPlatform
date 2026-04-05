package com.energy.audit.service.setting;

import com.energy.audit.model.entity.setting.BsProduct;

import java.util.List;

public interface ProductSettingService {

    BsProduct getById(Long id);

    BsProduct getByIdForEnterprise(Long id, Long enterpriseId);

    List<BsProduct> list(BsProduct query);

    void create(BsProduct product);

    void update(BsProduct product);

    void delete(Long id);
}
