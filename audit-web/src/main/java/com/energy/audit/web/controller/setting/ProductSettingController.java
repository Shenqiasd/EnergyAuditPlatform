package com.energy.audit.web.controller.setting;

import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.setting.BsProduct;
import com.energy.audit.service.setting.ProductSettingService;
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

/**
 * Product setting controller
 */
@Tag(name = "Product Settings", description = "Enterprise product CRUD")
@RestController
@RequestMapping("/setting/product")
public class ProductSettingController {

    private final ProductSettingService productSettingService;

    public ProductSettingController(ProductSettingService productSettingService) {
        this.productSettingService = productSettingService;
    }

    @Operation(summary = "Get product by ID (tenant-scoped)")
    @GetMapping("/{id}")
    public R<BsProduct> getById(@PathVariable Long id) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(productSettingService.getByIdForEnterprise(id, enterpriseId));
    }

    @Operation(summary = "List products with pagination")
    @GetMapping
    public R<PageResult<BsProduct>> list(BsProduct query,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BsProduct> list = productSettingService.list(query);
        PageInfo<BsProduct> pageInfo = new PageInfo<>(list);
        return R.ok(PageResult.of(pageInfo.getTotal(), pageInfo.getList()));
    }

    @Operation(summary = "Create product")
    @PostMapping
    public R<Void> create(@RequestBody BsProduct product) {
        productSettingService.create(product);
        return R.ok();
    }

    @Operation(summary = "Update product")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody BsProduct product) {
        product.setId(id);
        productSettingService.update(product);
        return R.ok();
    }

    @Operation(summary = "Delete product")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productSettingService.delete(id);
        return R.ok();
    }
}
