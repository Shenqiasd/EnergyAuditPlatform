package com.energy.audit.web.controller.system;

import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.model.entity.system.SysDictData;
import com.energy.audit.model.entity.system.SysDictType;
import com.energy.audit.service.system.SysDictService;
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
 * System dictionary controller
 * /system/dicts/type  → dict type CRUD
 * /system/dicts/data  → dict data CRUD
 * /system/dicts/data/{dictType} → cached lookup by type
 */
@Tag(name = "System Dictionaries", description = "Dictionary CRUD operations")
@RestController
@RequestMapping("/system/dicts")
public class SysDictController {

    private final SysDictService sysDictService;

    public SysDictController(SysDictService sysDictService) {
        this.sysDictService = sysDictService;
    }

    // ===== Dict Type =====

    @Operation(summary = "List dict types with pagination")
    @GetMapping("/type")
    public R<PageResult<SysDictType>> listTypes(SysDictType query,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<SysDictType> list = sysDictService.listTypes(query);
        PageInfo<SysDictType> pageInfo = new PageInfo<>(list);
        return R.ok(PageResult.of(pageInfo.getTotal(), pageInfo.getList()));
    }

    @Operation(summary = "Get dict type by ID")
    @GetMapping("/type/{id}")
    public R<SysDictType> getTypeById(@PathVariable Long id) {
        return R.ok(sysDictService.getTypeById(id));
    }

    @Operation(summary = "Create dict type")
    @PostMapping("/type")
    public R<Void> createType(@RequestBody SysDictType dictType) {
        sysDictService.createType(dictType);
        return R.ok();
    }

    @Operation(summary = "Update dict type")
    @PutMapping("/type/{id}")
    public R<Void> updateType(@PathVariable Long id, @RequestBody SysDictType dictType) {
        dictType.setId(id);
        sysDictService.updateType(dictType);
        return R.ok();
    }

    @Operation(summary = "Delete dict type (also deletes associated data)")
    @DeleteMapping("/type/{id}")
    public R<Void> deleteType(@PathVariable Long id) {
        sysDictService.deleteType(id);
        return R.ok();
    }

    // ===== Dict Data =====

    @Operation(summary = "List dict data with pagination")
    @GetMapping("/data")
    public R<PageResult<SysDictData>> listData(SysDictData query,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<SysDictData> list = sysDictService.listData(query);
        PageInfo<SysDictData> pageInfo = new PageInfo<>(list);
        return R.ok(PageResult.of(pageInfo.getTotal(), pageInfo.getList()));
    }

    @Operation(summary = "Get dict data by type (cached)")
    @GetMapping("/data/{dictType}")
    public R<List<SysDictData>> getDataByType(@PathVariable String dictType) {
        return R.ok(sysDictService.getDataByType(dictType));
    }

    @Operation(summary = "Get dict data by ID")
    @GetMapping("/data/item/{id}")
    public R<SysDictData> getDataById(@PathVariable Long id) {
        return R.ok(sysDictService.getDataById(id));
    }

    @Operation(summary = "Create dict data")
    @PostMapping("/data")
    public R<Void> createData(@RequestBody SysDictData dictData) {
        sysDictService.createData(dictData);
        return R.ok();
    }

    @Operation(summary = "Update dict data")
    @PutMapping("/data/{id}")
    public R<Void> updateData(@PathVariable Long id, @RequestBody SysDictData dictData) {
        dictData.setId(id);
        sysDictService.updateData(dictData);
        return R.ok();
    }

    @Operation(summary = "Delete dict data")
    @DeleteMapping("/data/{id}")
    public R<Void> deleteData(@PathVariable Long id) {
        sysDictService.deleteData(id);
        return R.ok();
    }
}
