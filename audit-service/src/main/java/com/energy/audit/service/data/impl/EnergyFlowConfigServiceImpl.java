package com.energy.audit.service.data.impl;

import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.data.DeEnergyFlowDiagramMapper;
import com.energy.audit.dao.mapper.data.DeEnergyFlowEdgeMapper;
import com.energy.audit.dao.mapper.data.DeEnergyFlowMapper;
import com.energy.audit.dao.mapper.data.DeEnergyFlowNodeMapper;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseMapper;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseSettingMapper;
import com.energy.audit.dao.mapper.setting.BsEnergyMapper;
import com.energy.audit.dao.mapper.setting.BsProductMapper;
import com.energy.audit.dao.mapper.setting.BsUnitMapper;
import com.energy.audit.model.dto.DiagramConfigDTO;
import com.energy.audit.model.dto.EnergyFlowConfigDTO;
import com.energy.audit.model.dto.SaveEnergyFlowConfigDTO;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
import com.energy.audit.model.entity.enterprise.EntEnterpriseSetting;
import com.energy.audit.model.entity.extraction.DeEnergyFlow;
import com.energy.audit.model.entity.extraction.DeEnergyFlowDiagram;
import com.energy.audit.model.entity.extraction.DeEnergyFlowEdge;
import com.energy.audit.model.entity.extraction.DeEnergyFlowNode;
import com.energy.audit.model.entity.setting.BsEnergy;
import com.energy.audit.model.entity.setting.BsProduct;
import com.energy.audit.model.entity.setting.BsUnit;
import com.energy.audit.service.data.EnergyFlowConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnergyFlowConfigServiceImpl implements EnergyFlowConfigService {

    private static final Logger log = LoggerFactory.getLogger(EnergyFlowConfigServiceImpl.class);
    private static final int DIAGRAM_TYPE_CONFIG = 3;

    private final DeEnergyFlowMapper flowMapper;
    private final DeEnergyFlowDiagramMapper diagramMapper;
    private final DeEnergyFlowNodeMapper nodeMapper;
    private final DeEnergyFlowEdgeMapper edgeMapper;
    private final EntEnterpriseMapper enterpriseMapper;
    private final EntEnterpriseSettingMapper enterpriseSettingMapper;
    private final BsUnitMapper unitMapper;
    private final BsEnergyMapper energyMapper;
    private final BsProductMapper productMapper;

    public EnergyFlowConfigServiceImpl(DeEnergyFlowMapper flowMapper,
                                        DeEnergyFlowDiagramMapper diagramMapper,
                                        DeEnergyFlowNodeMapper nodeMapper,
                                        DeEnergyFlowEdgeMapper edgeMapper,
                                        EntEnterpriseMapper enterpriseMapper,
                                        EntEnterpriseSettingMapper enterpriseSettingMapper,
                                        BsUnitMapper unitMapper,
                                        BsEnergyMapper energyMapper,
                                        BsProductMapper productMapper) {
        this.flowMapper = flowMapper;
        this.diagramMapper = diagramMapper;
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.enterpriseMapper = enterpriseMapper;
        this.enterpriseSettingMapper = enterpriseSettingMapper;
        this.unitMapper = unitMapper;
        this.energyMapper = energyMapper;
        this.productMapper = productMapper;
    }

    @Override
    public EnergyFlowConfigDTO getConfig(Long enterpriseId, Integer auditYear) {
        EnergyFlowConfigDTO result = new EnergyFlowConfigDTO();

        // Enterprise info
        EntEnterprise ent = enterpriseMapper.selectById(enterpriseId);
        if (ent != null) {
            EnergyFlowConfigDTO.EnterpriseInfoDTO info = new EnergyFlowConfigDTO.EnterpriseInfoDTO();
            info.setId(ent.getId());
            info.setName(ent.getEnterpriseName());
            result.setEnterpriseInfo(info);
        }

        // Units
        BsUnit unitQuery = new BsUnit();
        unitQuery.setEnterpriseId(enterpriseId);
        List<BsUnit> unitList = unitMapper.selectList(unitQuery);
        List<EnergyFlowConfigDTO.UnitInfoDTO> unitDtos = new ArrayList<>();
        for (BsUnit u : unitList) {
            EnergyFlowConfigDTO.UnitInfoDTO dto = new EnergyFlowConfigDTO.UnitInfoDTO();
            dto.setId(u.getId());
            dto.setName(u.getName());
            dto.setUnitType(u.getUnitType());
            dto.setSubCategory(u.getSubCategory());
            unitDtos.add(dto);
        }
        result.setUnits(unitDtos);

        // Energies
        BsEnergy energyQuery = new BsEnergy();
        energyQuery.setEnterpriseId(enterpriseId);
        List<BsEnergy> energyList = energyMapper.selectList(energyQuery);
        List<EnergyFlowConfigDTO.EnergyInfoDTO> energyDtos = new ArrayList<>();
        for (BsEnergy e : energyList) {
            EnergyFlowConfigDTO.EnergyInfoDTO dto = new EnergyFlowConfigDTO.EnergyInfoDTO();
            dto.setId(e.getId());
            dto.setName(e.getName());
            dto.setCategory(e.getCategory());
            dto.setMeasurementUnit(e.getMeasurementUnit());
            dto.setEquivalentValue(e.getEquivalentValue());
            energyDtos.add(dto);
        }
        result.setEnergies(energyDtos);

        // Products
        BsProduct productQuery = new BsProduct();
        productQuery.setEnterpriseId(enterpriseId);
        List<BsProduct> productList = productMapper.selectList(productQuery);
        List<EnergyFlowConfigDTO.ProductInfoDTO> productDtos = new ArrayList<>();
        for (BsProduct p : productList) {
            EnergyFlowConfigDTO.ProductInfoDTO dto = new EnergyFlowConfigDTO.ProductInfoDTO();
            dto.setId(p.getId());
            dto.setName(p.getName());
            dto.setMeasurementUnit(p.getMeasurementUnit());
            dto.setUnitPrice(p.getUnitPrice());
            productDtos.add(dto);
        }
        result.setProducts(productDtos);

        // Flow records
        List<DeEnergyFlow> flows = flowMapper.selectByEnterpriseAndYear(enterpriseId, auditYear);
        result.setFlowRecords(flows);

        // Diagram config
        DeEnergyFlowDiagram diagram = diagramMapper.selectByEnterpriseYearType(
                enterpriseId, auditYear, DIAGRAM_TYPE_CONFIG);
        if (diagram != null) {
            DiagramConfigDTO dConfig = new DiagramConfigDTO();
            dConfig.setId(diagram.getId());
            dConfig.setName(diagram.getName());
            dConfig.setDiagramType(diagram.getDiagramType());
            dConfig.setCanvasWidth(diagram.getCanvasWidth());
            dConfig.setCanvasHeight(diagram.getCanvasHeight());
            dConfig.setBackgroundColor(diagram.getBackgroundColor());

            // Nodes
            List<DeEnergyFlowNode> nodeList = nodeMapper.selectByDiagramId(diagram.getId());
            List<DiagramConfigDTO.FlowNodeDTO> nodeDtos = new ArrayList<>();
            for (DeEnergyFlowNode n : nodeList) {
                DiagramConfigDTO.FlowNodeDTO nd = new DiagramConfigDTO.FlowNodeDTO();
                nd.setId(n.getId());
                nd.setNodeId(n.getNodeId());
                nd.setNodeType(n.getNodeType());
                nd.setRefType(n.getRefType());
                nd.setRefId(n.getRefId());
                nd.setLabel(n.getLabel());
                nd.setPositionX(n.getPositionX());
                nd.setPositionY(n.getPositionY());
                nd.setWidth(n.getWidth());
                nd.setHeight(n.getHeight());
                nd.setColor(n.getColor());
                nd.setVisible(n.getVisible());
                nd.setLocked(n.getLocked());
                nodeDtos.add(nd);
            }
            dConfig.setNodes(nodeDtos);

            // Edges
            List<DeEnergyFlowEdge> edgeList = edgeMapper.selectByDiagramId(diagram.getId());
            List<DiagramConfigDTO.FlowEdgeDTO> edgeDtos = new ArrayList<>();
            for (DeEnergyFlowEdge e : edgeList) {
                DiagramConfigDTO.FlowEdgeDTO ed = new DiagramConfigDTO.FlowEdgeDTO();
                ed.setId(e.getId());
                ed.setEdgeId(e.getEdgeId());
                ed.setSourceNodeId(e.getSourceNodeId());
                ed.setTargetNodeId(e.getTargetNodeId());
                ed.setFlowRecordId(e.getFlowRecordId());
                ed.setItemType(e.getItemType());
                ed.setItemId(e.getItemId());
                ed.setPhysicalQuantity(e.getPhysicalQuantity());
                ed.setCalculatedValue(e.getCalculatedValue());
                ed.setLabelText(e.getLabelText());
                ed.setColor(e.getColor());
                ed.setLineWidth(e.getLineWidth());
                ed.setRoutePoints(e.getRoutePoints());
                ed.setVisible(e.getVisible());
                edgeDtos.add(ed);
            }
            dConfig.setEdges(edgeDtos);
            result.setDiagram(dConfig);
        }

        // Validation
        EnergyFlowConfigDTO.ValidationResultDTO validation = new EnergyFlowConfigDTO.ValidationResultDTO();
        boolean entComplete = checkEnterpriseComplete(ent, enterpriseId);
        validation.setEnterpriseComplete(entComplete);
        validation.setHasUnits(!unitList.isEmpty());
        validation.setHasEnergies(!energyList.isEmpty());
        validation.setHasProducts(!productList.isEmpty());
        validation.setValid(entComplete && !unitList.isEmpty() && !energyList.isEmpty() && !productList.isEmpty());

        List<String> warnings = new ArrayList<>();
        List<String> exportErrors = new ArrayList<>();
        if (!entComplete) {
            List<String> missing = listMissingEnterpriseFields(ent, enterpriseId);
            exportErrors.add("企业信息不完整：" + String.join("、", missing));
        }
        if (unitList.isEmpty()) exportErrors.add("至少需要一个用能单元");
        if (energyList.isEmpty()) exportErrors.add("至少需要一个能源品种");
        if (productList.isEmpty()) exportErrors.add("至少需要一个产品");

        for (DeEnergyFlow f : flows) {
            if ("energy".equals(f.getItemType()) && f.getItemId() != null) {
                BsEnergy energy = energyMapper.selectByIdAndEnterprise(f.getItemId(), enterpriseId);
                if (energy == null) {
                    String msg = "填报记录的能源品种(itemId=" + f.getItemId() + ")在本企业中不存在或已删除（待确认）";
                    warnings.add(msg);
                    exportErrors.add(msg);
                } else if (energy.getEquivalentValue() == null) {
                    String msg = "能源 [" + energy.getName() + "] 缺少折标系数";
                    warnings.add(msg);
                    exportErrors.add(msg);
                }
            } else if ("product".equals(f.getItemType()) && f.getItemId() != null) {
                BsProduct product = productMapper.selectByIdAndEnterprise(f.getItemId(), enterpriseId);
                if (product == null) {
                    String msg = "填报记录的产品(itemId=" + f.getItemId() + ")在本企业中不存在或已删除（待确认）";
                    warnings.add(msg);
                    exportErrors.add(msg);
                } else if (product.getUnitPrice() == null) {
                    String msg = "产品 [" + product.getName() + "] 缺少单价";
                    warnings.add(msg);
                    exportErrors.add(msg);
                }
            } else if (!isBlank(f.getItemType()) && f.getItemId() == null) {
                String msg = "填报记录品目类型为 [" + f.getItemType() + "] 但未选择品目(itemId为空)（待确认）";
                warnings.add(msg);
                exportErrors.add(msg);
            } else if (isBlank(f.getItemType()) && !isBlank(f.getEnergyProduct())) {
                String msg = "填报记录 [" + f.getEnergyProduct() + "] 为旧数据（待确认），请编辑确认品目类型和品目";
                warnings.add(msg);
                exportErrors.add(msg);
            }
        }
        // Validate visible edges: each must bind to an active flow record
        Set<Long> activeFlowIds = flows.stream()
                .filter(f -> f.getId() != null)
                .map(DeEnergyFlow::getId)
                .collect(Collectors.toSet());
        if (result.getDiagram() != null && result.getDiagram().getEdges() != null) {
            for (DiagramConfigDTO.FlowEdgeDTO ed : result.getDiagram().getEdges()) {
                Integer vis = ed.getVisible() != null ? ed.getVisible() : 1;
                if (vis == 0) continue;
                if (ed.getFlowRecordId() == null) {
                    String msg = "连线 [" + ed.getEdgeId() + "] 未绑定到有效的填报记录（待确认）";
                    warnings.add(msg);
                    exportErrors.add(msg);
                } else if (!activeFlowIds.contains(ed.getFlowRecordId())) {
                    String msg = "连线 [" + ed.getEdgeId() + "] 绑定的填报记录(id=" + ed.getFlowRecordId() + ")不存在或已删除（待确认）";
                    warnings.add(msg);
                    exportErrors.add(msg);
                }
            }
        }
        validation.setWarnings(warnings);
        validation.setExportErrors(exportErrors);
        validation.setExportReady(exportErrors.isEmpty());
        result.setValidation(validation);

        return result;
    }

    @Override
    @Transactional
    public void saveConfig(Long enterpriseId, Integer auditYear, SaveEnergyFlowConfigDTO dto) {
        String operator = SecurityUtils.getCurrentUsername();

        // 1. Validate and upsert flow records
        List<DeEnergyFlow> savedRecords = new ArrayList<>();
        if (dto.getFlowRecords() != null) {
            // Server-side validation for each flow record
            for (int i = 0; i < dto.getFlowRecords().size(); i++) {
                DeEnergyFlow flow = dto.getFlowRecords().get(i);
                validateFlowRecord(flow, i, enterpriseId);
            }

            List<DeEnergyFlow> existing = flowMapper.selectByEnterpriseAndYear(enterpriseId, auditYear);
            Set<Long> existingIds = existing.stream()
                    .map(DeEnergyFlow::getId)
                    .collect(Collectors.toSet());
            Set<Long> incomingIds = new HashSet<>();

            for (int i = 0; i < dto.getFlowRecords().size(); i++) {
                DeEnergyFlow flow = dto.getFlowRecords().get(i);
                flow.setEnterpriseId(enterpriseId);
                flow.setAuditYear(auditYear);
                flow.setSeqNo(i + 1);
                if (flow.getSubmissionId() == null) {
                    flow.setSubmissionId(0L);
                }
                calculateFlowValue(flow, enterpriseId);

                if (flow.getId() != null && existingIds.contains(flow.getId())) {
                    flow.setUpdateBy(operator);
                    flowMapper.updateById(flow);
                    incomingIds.add(flow.getId());
                } else {
                    flow.setId(null);
                    flow.setCreateBy(operator);
                    flow.setUpdateBy(operator);
                    flowMapper.insert(flow);
                }
                savedRecords.add(flow);
            }

            // Soft-delete records no longer in the incoming list
            for (Long eid : existingIds) {
                if (!incomingIds.contains(eid)) {
                    flowMapper.softDeleteByIdAndEnterprise(eid, enterpriseId, operator);
                }
            }
        }

        // 2. Save diagram config
        if (dto.getDiagram() != null) {
            DiagramConfigDTO dc = dto.getDiagram();

            // Upsert diagram
            DeEnergyFlowDiagram existing = diagramMapper.selectByEnterpriseYearType(
                    enterpriseId, auditYear, DIAGRAM_TYPE_CONFIG);

            Long diagramId;
            if (existing != null) {
                existing.setName(dc.getName());
                existing.setCanvasWidth(dc.getCanvasWidth());
                existing.setCanvasHeight(dc.getCanvasHeight());
                existing.setBackgroundColor(dc.getBackgroundColor());
                existing.setUpdateBy(operator);
                diagramMapper.update(existing);
                diagramId = existing.getId();
            } else {
                DeEnergyFlowDiagram d = new DeEnergyFlowDiagram();
                d.setEnterpriseId(enterpriseId);
                d.setAuditYear(auditYear);
                d.setDiagramType(DIAGRAM_TYPE_CONFIG);
                d.setName(dc.getName());
                d.setCanvasWidth(dc.getCanvasWidth());
                d.setCanvasHeight(dc.getCanvasHeight());
                d.setBackgroundColor(dc.getBackgroundColor());
                d.setCreateBy(operator);
                d.setUpdateBy(operator);
                diagramMapper.insert(d);
                diagramId = d.getId();
            }

            // Replace nodes
            nodeMapper.deleteByDiagramId(diagramId, operator);
            if (dc.getNodes() != null) {
                for (DiagramConfigDTO.FlowNodeDTO nd : dc.getNodes()) {
                    DeEnergyFlowNode node = new DeEnergyFlowNode();
                    node.setDiagramId(diagramId);
                    node.setNodeId(nd.getNodeId());
                    node.setNodeType(nd.getNodeType());
                    node.setRefType(nd.getRefType());
                    node.setRefId(nd.getRefId());
                    node.setLabel(nd.getLabel());
                    node.setPositionX(nd.getPositionX());
                    node.setPositionY(nd.getPositionY());
                    node.setWidth(nd.getWidth());
                    node.setHeight(nd.getHeight());
                    node.setColor(nd.getColor());
                    node.setVisible(nd.getVisible() != null ? nd.getVisible() : 1);
                    node.setLocked(nd.getLocked() != null ? nd.getLocked() : 0);
                    node.setCreateBy(operator);
                    node.setUpdateBy(operator);
                    nodeMapper.insert(node);
                }
            }

            // Pre-validate edges: resolve flowRecordId and reject invalid visible edges BEFORE deleting
            List<Long> resolvedEdgeRecordIds = new ArrayList<>();
            if (dc.getEdges() != null) {
                for (DiagramConfigDTO.FlowEdgeDTO ed : dc.getEdges()) {
                    Long resolvedRecordId = ed.getFlowRecordId();
                    if (ed.getFlowRecordIndex() != null
                            && ed.getFlowRecordIndex() >= 0
                            && ed.getFlowRecordIndex() < savedRecords.size()) {
                        resolvedRecordId = savedRecords.get(ed.getFlowRecordIndex()).getId();
                    }
                    if (resolvedRecordId != null) {
                        final Long checkId = resolvedRecordId;
                        boolean valid = savedRecords.stream()
                                .anyMatch(r -> r.getId().equals(checkId));
                        if (!valid) {
                            log.warn("Edge {} has flowRecordId={} not in active records",
                                    ed.getEdgeId(), resolvedRecordId);
                            resolvedRecordId = null;
                        }
                    }
                    Integer edgeVisible = ed.getVisible() != null ? ed.getVisible() : 1;
                    if (resolvedRecordId == null && edgeVisible == 1) {
                        throw new IllegalArgumentException(
                                String.format("连线 [%s] 没有绑定到有效的填报记录，无法保存。请先删除该连线或重新绑定填报记录。",
                                        ed.getEdgeId()));
                    }
                    resolvedEdgeRecordIds.add(resolvedRecordId);
                }
            }

            // Now safe to replace edges (all validated)
            edgeMapper.deleteByDiagramId(diagramId, operator);
            if (dc.getEdges() != null) {
                for (int i = 0; i < dc.getEdges().size(); i++) {
                    DiagramConfigDTO.FlowEdgeDTO ed = dc.getEdges().get(i);
                    DeEnergyFlowEdge edge = new DeEnergyFlowEdge();
                    edge.setDiagramId(diagramId);
                    edge.setEdgeId(ed.getEdgeId());
                    edge.setSourceNodeId(ed.getSourceNodeId());
                    edge.setTargetNodeId(ed.getTargetNodeId());
                    edge.setFlowRecordId(resolvedEdgeRecordIds.get(i));
                    edge.setItemType(ed.getItemType());
                    edge.setItemId(ed.getItemId());
                    edge.setPhysicalQuantity(ed.getPhysicalQuantity());
                    edge.setCalculatedValue(ed.getCalculatedValue());
                    edge.setLabelText(ed.getLabelText());
                    edge.setColor(ed.getColor());
                    edge.setLineWidth(ed.getLineWidth());
                    edge.setRoutePoints(ed.getRoutePoints());
                    edge.setVisible(ed.getVisible() != null ? ed.getVisible() : 1);
                    edge.setCreateBy(operator);
                    edge.setUpdateBy(operator);
                    edgeMapper.insert(edge);
                }
            }
        }

        log.info("Energy flow config saved: enterprise={}, year={}", enterpriseId, auditYear);
    }

    /**
     * Check enterprise completeness using both ent_enterprise basic fields
     * and ent_enterprise_setting detail fields (matching the company settings form).
     */
    private boolean checkEnterpriseComplete(EntEnterprise ent, Long enterpriseId) {
        if (ent == null || isBlank(ent.getEnterpriseName()) || isBlank(ent.getCreditCode())) {
            return false;
        }
        EntEnterpriseSetting setting = enterpriseSettingMapper.selectByEnterpriseId(enterpriseId);
        if (setting == null) return false;
        return !isBlank(setting.getRegion())
                && !isBlank(setting.getIndustryField())
                && !isBlank(setting.getUnitNature())
                && !isBlank(setting.getEnergyUsageType())
                && !isBlank(setting.getIndustryCode())
                && setting.getRegisteredDate() != null
                && setting.getRegisteredCapital() != null
                && !isBlank(setting.getLegalRepresentative())
                && !isBlank(setting.getLegalPhone())
                && !isBlank(setting.getSuperiorDepartment())
                && !isBlank(setting.getEnterpriseAddress())
                && !isBlank(setting.getPostalCode())
                && !isBlank(setting.getEnterpriseEmail())
                && !isBlank(setting.getEnergyMgmtOrg())
                && !isBlank(setting.getEnergyLeaderName())
                && !isBlank(setting.getEnergyLeaderPhone())
                && !isBlank(setting.getEnergyLeaderTitle())
                && !isBlank(setting.getEnergyDeptName())
                && !isBlank(setting.getEnergyManagerName())
                && !isBlank(setting.getEnergyManagerMobile())
                && !isBlank(setting.getEnergyAuditContactName())
                && !isBlank(setting.getEnergyAuditContactPhone())
                && !isBlank(setting.getCompilerContact())
                && !isBlank(setting.getCompilerName())
                && !isBlank(setting.getCompilerMobile())
                && !isBlank(setting.getCompilerEmail())
                && setting.getEnergyCert() != null
                && setting.getHasEnergyCenter() != null
                && checkCertFields(setting);
    }

    private static boolean checkCertFields(EntEnterpriseSetting s) {
        if (s.getEnergyCert() != null && s.getEnergyCert() == 1) {
            return s.getCertPassDate() != null && !isBlank(s.getCertAuthority());
        }
        return true;
    }

    private List<String> listMissingEnterpriseFields(EntEnterprise ent, Long enterpriseId) {
        List<String> missing = new ArrayList<>();
        if (ent == null || isBlank(ent.getEnterpriseName())) missing.add("企业名称");
        if (ent == null || isBlank(ent.getCreditCode())) missing.add("统一社会信用代码");
        EntEnterpriseSetting s = enterpriseSettingMapper.selectByEnterpriseId(enterpriseId);
        if (s == null) {
            missing.add("企业概况详细信息（未填写）");
            return missing;
        }
        if (isBlank(s.getRegion())) missing.add("所属地区");
        if (isBlank(s.getIndustryField())) missing.add("所属领域");
        if (isBlank(s.getUnitNature())) missing.add("单位类型");
        if (isBlank(s.getEnergyUsageType())) missing.add("用能企业类型");
        if (isBlank(s.getIndustryCode())) missing.add("行业分类");
        if (s.getRegisteredDate() == null) missing.add("单位注册日期");
        if (s.getRegisteredCapital() == null) missing.add("注册资本");
        if (isBlank(s.getLegalRepresentative())) missing.add("法定代表人");
        if (isBlank(s.getLegalPhone())) missing.add("联系电话");
        if (isBlank(s.getSuperiorDepartment())) missing.add("上级主管部门");
        if (isBlank(s.getEnterpriseAddress())) missing.add("单位地址");
        if (isBlank(s.getPostalCode())) missing.add("邮政编码");
        if (isBlank(s.getEnterpriseEmail())) missing.add("电子邮箱");
        if (isBlank(s.getEnergyMgmtOrg())) missing.add("能源管理机构");
        if (isBlank(s.getEnergyLeaderName())) missing.add("节能领导姓名");
        if (isBlank(s.getEnergyLeaderPhone())) missing.add("节能领导电话");
        if (isBlank(s.getEnergyLeaderTitle())) missing.add("节能领导职务");
        if (isBlank(s.getEnergyDeptName())) missing.add("节能主管部门");
        if (isBlank(s.getEnergyManagerName())) missing.add("能源管理负责人");
        if (isBlank(s.getEnergyManagerMobile())) missing.add("能源管理负责人电话");
        if (isBlank(s.getEnergyAuditContactName())) missing.add("审计联系人");
        if (isBlank(s.getEnergyAuditContactPhone())) missing.add("审计联系人电话");
        if (isBlank(s.getCompilerContact())) missing.add("编制单位");
        if (isBlank(s.getCompilerName())) missing.add("编制联系人");
        if (isBlank(s.getCompilerMobile())) missing.add("编制联系人电话");
        if (isBlank(s.getCompilerEmail())) missing.add("编制联系人邮箱");
        if (s.getEnergyCert() == null) missing.add("能源认证");
        if (s.getHasEnergyCenter() == null) missing.add("能源管理中心");
        if (s.getEnergyCert() != null && s.getEnergyCert() == 1) {
            if (s.getCertPassDate() == null) missing.add("认证通过日期");
            if (isBlank(s.getCertAuthority())) missing.add("认证机构");
        }
        return missing;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Server-side validation for fill records.
     * Legacy rows without itemType/itemId are allowed but flagged.
     */
    private static final Set<String> VALID_SOURCE_TYPES = Set.of("external_energy", "unit", "system");
    private static final Set<String> VALID_TARGET_TYPES = Set.of("unit", "product_output", "production_system");
    private static final Set<String> VALID_ITEM_TYPES = Set.of("energy", "product");

    private void validateFlowRecord(DeEnergyFlow flow, int index, Long enterpriseId) {
        // Legacy rows: if no sourceType and has energyProduct, treat as legacy — allow but flag
        if (isBlank(flow.getSourceType()) && !isBlank(flow.getEnergyProduct())) {
            return; // legacy row, skip strict validation
        }
        List<String> errors = new ArrayList<>();
        if (isBlank(flow.getSourceType())) {
            errors.add("来源类型(sourceType)不能为空");
        } else if (!VALID_SOURCE_TYPES.contains(flow.getSourceType())) {
            errors.add("来源类型(sourceType=" + flow.getSourceType() + ")不合法，允许值: " + VALID_SOURCE_TYPES);
        } else if ("unit".equals(flow.getSourceType())) {
            if (flow.getSourceRefId() == null) {
                errors.add("来源类型为unit时，sourceRefId不能为空");
            } else if (unitMapper.selectByIdAndEnterprise(flow.getSourceRefId(), enterpriseId) == null) {
                errors.add("来源单元(sourceRefId=" + flow.getSourceRefId() + ")在本企业中不存在");
            }
        } else if ("system".equals(flow.getSourceType()) && isBlank(flow.getSourceUnit())) {
            errors.add("来源系统名称不能为空");
        }
        if (isBlank(flow.getTargetType())) {
            errors.add("目的类型(targetType)不能为空");
        } else if (!VALID_TARGET_TYPES.contains(flow.getTargetType())) {
            errors.add("目的类型(targetType=" + flow.getTargetType() + ")不合法，允许值: " + VALID_TARGET_TYPES);
        } else if ("unit".equals(flow.getTargetType())) {
            if (flow.getTargetRefId() == null) {
                errors.add("目的类型为unit时，targetRefId不能为空");
            } else if (unitMapper.selectByIdAndEnterprise(flow.getTargetRefId(), enterpriseId) == null) {
                errors.add("目的单元(targetRefId=" + flow.getTargetRefId() + ")在本企业中不存在");
            }
        } else if ("production_system".equals(flow.getTargetType()) && isBlank(flow.getTargetUnit())) {
            errors.add("目的生产系统名称不能为空");
        }
        if (isBlank(flow.getItemType())) {
            errors.add("品目类型(itemType)不能为空");
        } else if (!VALID_ITEM_TYPES.contains(flow.getItemType())) {
            errors.add("品目类型(itemType=" + flow.getItemType() + ")不合法，只允许energy或product");
        }
        if (flow.getItemId() == null && !isBlank(flow.getItemType())) {
            errors.add("品目(itemId)不能为空");
        } else if (flow.getItemId() != null && !isBlank(flow.getItemType())) {
            if ("energy".equals(flow.getItemType())
                    && energyMapper.selectByIdAndEnterprise(flow.getItemId(), enterpriseId) == null) {
                errors.add("能源品种(itemId=" + flow.getItemId() + ")在本企业中不存在");
            } else if ("product".equals(flow.getItemType())
                    && productMapper.selectByIdAndEnterprise(flow.getItemId(), enterpriseId) == null) {
                errors.add("产品(itemId=" + flow.getItemId() + ")在本企业中不存在");
            }
        }
        if (flow.getPhysicalQuantity() == null) {
            errors.add("实物量(physicalQuantity)不能为空");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("填报记录[%d]校验失败: %s", index + 1, String.join("; ", errors)));
        }
    }

    private void calculateFlowValue(DeEnergyFlow flow, Long enterpriseId) {
        if (flow.getPhysicalQuantity() == null) return;
        if ("energy".equals(flow.getItemType()) && flow.getItemId() != null) {
            BsEnergy energy = energyMapper.selectByIdAndEnterprise(flow.getItemId(), enterpriseId);
            if (energy != null && energy.getEquivalentValue() != null) {
                flow.setCalculatedValue(flow.getPhysicalQuantity().multiply(energy.getEquivalentValue()));
            }
        } else if ("product".equals(flow.getItemType()) && flow.getItemId() != null) {
            BsProduct product = productMapper.selectByIdAndEnterprise(flow.getItemId(), enterpriseId);
            if (product != null && product.getUnitPrice() != null) {
                flow.setCalculatedValue(flow.getPhysicalQuantity().multiply(product.getUnitPrice()));
            }
        } else if (flow.getEnergyProduct() != null && !flow.getEnergyProduct().isBlank()) {
            // Backward compat: try matching by name
            BsEnergy energy = energyMapper.selectByEnterpriseAndName(enterpriseId, flow.getEnergyProduct());
            if (energy != null && energy.getEquivalentValue() != null) {
                flow.setCalculatedValue(flow.getPhysicalQuantity().multiply(energy.getEquivalentValue()));
            }
        }
    }
}
