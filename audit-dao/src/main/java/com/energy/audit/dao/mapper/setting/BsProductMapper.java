package com.energy.audit.dao.mapper.setting;

import com.energy.audit.model.entity.setting.BsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Product setting mapper
 */
@Mapper
public interface BsProductMapper {

    BsProduct selectById(@Param("id") Long id);

    BsProduct selectByIdAndEnterprise(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);

    List<BsProduct> selectList(BsProduct query);

    int insert(BsProduct product);

    int updateById(BsProduct product);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
