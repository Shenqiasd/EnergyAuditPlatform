package com.energy.audit.dao.mapper.template;

import com.energy.audit.model.entity.template.TplTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Template mapper
 */
@Mapper
public interface TplTemplateMapper {

    TplTemplate selectById(@Param("id") Long id);

    TplTemplate selectByCode(@Param("templateCode") String templateCode);

    List<TplTemplate> selectList(TplTemplate query);

    int insert(TplTemplate template);

    int updateById(TplTemplate template);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
