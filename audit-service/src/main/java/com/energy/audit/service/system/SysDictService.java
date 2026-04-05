package com.energy.audit.service.system;

import com.energy.audit.model.entity.system.SysDictData;
import com.energy.audit.model.entity.system.SysDictType;

import java.util.List;

public interface SysDictService {

    List<SysDictType> listTypes(SysDictType query);

    SysDictType getTypeById(Long id);

    void createType(SysDictType type);

    void updateType(SysDictType type);

    void deleteType(Long id);

    List<SysDictData> listDataByType(String dictType);

    List<SysDictData> getDataByType(String dictType);

    SysDictData getDataById(Long id);

    List<SysDictData> listData(SysDictData query);

    void createData(SysDictData data);

    void updateData(SysDictData data);

    void deleteData(Long id);
}
