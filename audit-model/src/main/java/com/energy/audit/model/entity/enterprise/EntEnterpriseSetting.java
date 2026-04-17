package com.energy.audit.model.entity.enterprise;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Enterprise detailed setting entity — matches ent_enterprise_setting production schema.
 * One row per enterprise (unique on enterprise_id).
 * Fields align with the "1.企业概况" spreadsheet template.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EntEnterpriseSetting extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** FK -> ent_enterprise.id (unique) */
    private Long enterpriseId;

    // ── 基本信息 (从 ent_enterprise / ent_registration 联查) ──

    /** 单位详细名称 (non-persistent, populated from ent_enterprise for prefill) */
    private transient String enterpriseName;

    /** 统一社会信用代码 (non-persistent, populated from ent_registration for prefill) */
    private transient String creditCode;

    // ── 地区 / 行业 ──

    /** 所属地区 */
    private String region;

    /** 所属领域 */
    private String industryField;

    /** 行业分类名称 */
    private String industryName;

    /** 单位类型 */
    private String unitNature;

    // ── 工商注册 ──

    /** 单位注册日期 */
    private LocalDate registeredDate;

    /** 注册资本（万元） */
    private BigDecimal registeredCapital;

    // ── 法人 / 联系人 ──

    /** 法定代表人姓名 */
    private String legalRepresentative;

    /** 法定代表人联系电话（区号） */
    private String legalPhone;

    /** 是否央企 (0=否, 1=是) */
    private Integer isCentralEnterprise;

    /** 所属集团名称 */
    private String groupName;

    // ── 地址 / 通讯 ──

    /** 单位地址 */
    private String enterpriseAddress;

    /** 单位地址(备用) */
    private String unitAddress;

    /** 邮政编码 */
    private String postalCode;

    /** 行政区划代码 */
    private String adminDivisionCode;

    /** 电子邮箱 */
    private String enterpriseEmail;

    /** 传真（区号） */
    private String fax;

    // ── 能源管理 ──

    /** 能源管理机构名称 */
    private String energyMgmtOrg;

    /** 单位主管节能领导姓名 */
    private String energyLeaderName;

    /** 节能领导联系电话 */
    private String energyLeaderPhone;

    /** 能源管理负责人姓名 */
    private String energyManagerName;

    /** 能源管理负责人手机 */
    private String energyManagerMobile;

    /** 能源管理师证号 */
    private String energyManagerCert;

    /** 能源部门负责人电话 */
    private String energyDeptLeaderPhone;

    // ── 能源认证 ──

    /** 是否通过能源管理体系认证 (0=否, 1=是) */
    private Integer energyCert;

    /** 认证通过日期 */
    private LocalDate certPassDate;

    /** 认证机构 */
    private String certAuthority;

    /** 是否建设能源管理中心 (0=否, 1=是) */
    private Integer hasEnergyCenter;

    // ── 其他 ──

    /** 企业联系人 */
    private String enterpriseContact;

    /** 企业联系手机 */
    private String enterpriseMobile;

    /** 编制人联系人 */
    private String compilerContact;

    /** 编制人姓名 */
    private String compilerName;

    /** 编制人手机 */
    private String compilerMobile;

    /** 编制人邮箱 */
    private String compilerEmail;

    /** 行业大类 */
    private String industryCategory;

    /** 行业代码 */
    private String industryCode;

    /** 上级主管部门 */
    private String superiorDepartment;

    /** 用能企业类型 */
    private String energyEnterpriseType;

    /** 备注 */
    private String remark;
}
