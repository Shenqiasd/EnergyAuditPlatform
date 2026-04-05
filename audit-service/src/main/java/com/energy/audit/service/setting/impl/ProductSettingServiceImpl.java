package com.energy.audit.service.setting.impl;

import com.energy.audit.model.entity.setting.BsProduct;
import com.energy.audit.service.setting.ProductSettingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductSettingServiceImpl implements ProductSettingService {

    @Override
    public BsProduct getById(Long id) {
        return null;
    }

    @Override
    public BsProduct getByIdForEnterprise(Long id, Long enterpriseId) {
        return null;
    }

    @Override
    public List<BsProduct> list(BsProduct query) {
        return new ArrayList<>();
    }

    @Override
    public void create(BsProduct product) {
    }

    @Override
    public void update(BsProduct product) {
    }

    @Override
    public void delete(Long id) {
    }
}
