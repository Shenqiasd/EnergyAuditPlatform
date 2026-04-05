package com.energy.audit.service.system.impl;

import com.energy.audit.model.entity.system.SysDictData;
import com.energy.audit.model.entity.system.SysDictType;
import com.energy.audit.service.system.SysDictService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SysDictServiceImpl implements SysDictService {

    @Override
    public List<SysDictType> listTypes(SysDictType query) {
        return new ArrayList<>();
    }

    @Override
    public SysDictType getTypeById(Long id) {
        return null;
    }

    @Override
    public void createType(SysDictType type) {
    }

    @Override
    public void updateType(SysDictType type) {
    }

    @Override
    public void deleteType(Long id) {
    }

    @Override
    public List<SysDictData> listDataByType(String dictType) {
        return new ArrayList<>();
    }

    @Override
    public List<SysDictData> getDataByType(String dictType) {
        return new ArrayList<>();
    }

    @Override
    public SysDictData getDataById(Long id) {
        return null;
    }

    @Override
    public List<SysDictData> listData(SysDictData query) {
        return new ArrayList<>();
    }

    @Override
    public void createData(SysDictData data) {
    }

    @Override
    public void updateData(SysDictData data) {
    }

    @Override
    public void deleteData(Long id) {
    }
}
