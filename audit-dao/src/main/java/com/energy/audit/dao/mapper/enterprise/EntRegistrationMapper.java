package com.energy.audit.dao.mapper.enterprise;

import com.energy.audit.model.entity.enterprise.EntRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Enterprise registration application mapper
 */
@Mapper
public interface EntRegistrationMapper {

    EntRegistration selectById(@Param("id") Long id);

    List<EntRegistration> selectList(EntRegistration query);

    int insert(EntRegistration registration);

    int updateById(EntRegistration registration);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
