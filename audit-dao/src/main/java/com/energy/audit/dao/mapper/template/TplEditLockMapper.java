package com.energy.audit.dao.mapper.template;

import com.energy.audit.model.entity.template.TplEditLock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface TplEditLockMapper {

    TplEditLock selectByKeyForUpdate(@Param("enterpriseId") Long enterpriseId,
                                     @Param("templateId") Long templateId,
                                     @Param("auditYear") Integer auditYear);

    TplEditLock selectByKey(@Param("enterpriseId") Long enterpriseId,
                            @Param("templateId") Long templateId,
                            @Param("auditYear") Integer auditYear);

    int insert(TplEditLock lock);

    int updateByKey(@Param("enterpriseId") Long enterpriseId,
                    @Param("templateId") Long templateId,
                    @Param("auditYear") Integer auditYear,
                    @Param("lockUserId") Long lockUserId,
                    @Param("lockTime") LocalDateTime lockTime,
                    @Param("expireTime") LocalDateTime expireTime,
                    @Param("updateBy") String updateBy);

    int deleteByKey(@Param("enterpriseId") Long enterpriseId,
                    @Param("templateId") Long templateId,
                    @Param("auditYear") Integer auditYear,
                    @Param("updateBy") String updateBy);
}
