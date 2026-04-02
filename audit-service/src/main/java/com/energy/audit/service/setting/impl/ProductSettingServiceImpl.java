package com.energy.audit.service.setting.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.setting.BsProductMapper;
import com.energy.audit.model.entity.setting.BsProduct;
import com.energy.audit.service.setting.ProductSettingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductSettingServiceImpl implements ProductSettingService {

    private final BsProductMapper productMapper;

    public ProductSettingServiceImpl(BsProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public BsProduct getByIdForEnterprise(Long id, Long enterpriseId) {
        BsProduct product = productMapper.selectByIdAndEnterprise(id, enterpriseId);
        if (product == null) {
            throw new BusinessException("Product not found or access denied: " + id);
        }
        return product;
    }

    @Override
    public List<BsProduct> list(BsProduct query) {
        query.setEnterpriseId(SecurityUtils.getRequiredCurrentEnterpriseId());
        return productMapper.selectList(query);
    }

    @Override
    public void create(BsProduct product) {
        String operator = SecurityUtils.getCurrentUsername();
        product.setCreateBy(operator);
        product.setUpdateBy(operator);
        product.setEnterpriseId(SecurityUtils.getRequiredCurrentEnterpriseId());
        productMapper.insert(product);
    }

    @Override
    public void update(BsProduct product) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        getByIdForEnterprise(product.getId(), enterpriseId);
        product.setUpdateBy(SecurityUtils.getCurrentUsername());
        productMapper.updateById(product);
    }

    @Override
    public void delete(Long id) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        getByIdForEnterprise(id, enterpriseId);
        productMapper.deleteById(id, SecurityUtils.getCurrentUsername());
    }
}
