package com.energy.audit.dao.mapper.setting;

import com.energy.audit.model.entity.setting.BsEnergyCatalog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Global energy catalog mapper
 */
@Mapper
public interface BsEnergyCatalogMapper {

    BsEnergyCatalog selectById(@Param("id") Long id);

    List<BsEnergyCatalog> selectList(BsEnergyCatalog query);

    /** Fetch all active catalog entries for a given category (for admin grouping / import-picker) */
    List<BsEnergyCatalog> selectByCategory(@Param("category") String category);

    int insert(BsEnergyCatalog catalog);

    int updateById(BsEnergyCatalog catalog);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
