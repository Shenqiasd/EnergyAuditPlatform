package com.energy.audit.dao.mapper.enterprise;

import com.energy.audit.model.entity.enterprise.EntEnterprise;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Enterprise mapper
 */
@Mapper
public interface EntEnterpriseMapper {

    EntEnterprise selectById(@Param("id") Long id);

    List<EntEnterprise> selectList(EntEnterprise query);

    int insert(EntEnterprise enterprise);

    int updateById(EntEnterprise enterprise);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
