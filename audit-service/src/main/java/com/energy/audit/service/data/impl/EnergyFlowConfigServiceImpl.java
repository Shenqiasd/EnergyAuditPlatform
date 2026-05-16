package com.energy.audit.service.data.impl;

import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.data.DeEnergyConsumptionMapper;
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
import com.energy.audit.model.entity.extraction.DeEnergyConsumption;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final DeEnergyConsumptionMapper consumptionMapper;
    private final EntEnterpriseMapper enterpriseMapper;
    private final EntEnterpriseSettingMapper enterpriseSettingMapper;
    private final BsUnitMapper unitMapper;
    private final BsEnergyMapper energyMapper;
    private final BsProductMapper productMapper;

    public EnergyFlowConfigServiceImpl(DeEnergyFlowMapper flowMapper,
                                        DeEnergyFlowDiagramMapper diagramMapper,
                                        DeEnergyFlowNodeMapper nodeMapper,
                                        DeEnergyFlowEdgeMapper edgeMapper,
                                        DeEnergyConsumptionMapper consumptionMapper,
                                        EntEnterpriseMapper enterpriseMapper,
                                        EntEnterpriseSettingMapper enterpriseSettingMapper,
                                        BsUnitMapper unitMapper,
                                        BsEnergyMapper energyMapper,
                                        BsProductMapper productMapper) {
        this.flowMapper = flowMapper;
        this.diagramMapper = diagramMapper;
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.consumptionMapper = consumptionMapper;
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
            dto.setEqualValue(e.getEqualValue());
            dto.setColor(e.getColor());
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

        // Energy consumption data (for inventory 4-line indicators)
        List<DeEnergyConsumption> consumptions = consumptionMapper.selectByEnterpriseAndYear(enterpriseId, auditYear);
        Map<String, BsEnergy> energyByName = energyList.stream()
                .collect(Collectors.toMap(BsEnergy::getName, e -> e, (a, b) -> a));
        List<EnergyFlowConfigDTO.EnergyConsumptionDTO> consumptionDtos = new ArrayList<>();
        for (DeEnergyConsumption c : consumptions) {
            EnergyFlowConfigDTO.EnergyConsumptionDTO cdto = new EnergyFlowConfigDTO.EnergyConsumptionDTO();
            cdto.setId(c.getId());
            cdto.setEnergyName(c.getEnergyName());
            cdto.setMeasurementUnit(c.getMeasurementUnit());
            cdto.setOpeningStock(c.getOpeningStock());
            cdto.setPurchaseTotal(c.getPurchaseTotal());
            cdto.setPurchaseAmount(c.getPurchaseAmount());
            cdto.setClosingStock(c.getClosingStock());
            cdto.setExternalSupply(c.getExternalSupply());
            cdto.setIndustrialConsumption(c.getIndustrialConsumption());
            cdto.setMaterialConsumption(c.getMaterialConsumption());
            cdto.setTransportConsumption(c.getTransportConsumption());
            // Aggregate consumeAmount = industrial + material + transport
            java.math.BigDecimal consume = java.math.BigDecimal.ZERO;
            if (c.getIndustrialConsumption() != null) consume = consume.add(c.getIndustrialConsumption());
            if (c.getMaterialConsumption() != null) consume = consume.add(c.getMaterialConsumption());
            if (c.getTransportConsumption() != null) consume = consume.add(c.getTransportConsumption());
            cdto.setConsumeAmount(
                    (c.getIndustrialConsumption() != null || c.getMaterialConsumption() != null
                            || c.getTransportConsumption() != null) ? consume : null);
            cdto.setEquivFactor(c.getEquivFactor());
            cdto.setEqualFactor(c.getEqualFactor());
            cdto.setStandardCoal(c.getStandardCoal());
            BsEnergy matchedEnergy = energyByName.get(c.getEnergyName());
            if (matchedEnergy != null) {
                cdto.setEnergyId(matchedEnergy.getId());
            }
            consumptionDtos.add(cdto);
        }
        result.setEnergyConsumption(consumptionDtos);

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
                } else {
                    if (energy.getEquivalentValue() == null) {
                        String msg = "能源 [" + energy.getName() + "] 缺少当量值系数(equivalentValue)";
                        warnings.add(msg);
                        exportErrors.add(msg);
                    }
                    if (energy.getEqualValue() == null) {
                        String msg = "能源 [" + energy.getName() + "] 缺少等价值系数(equalValue)";
                        warnings.add(msg);
                        exportErrors.add(msg);
                    }
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
            // Product-output terminal-use validation for persisted records (mirrors saveConfig validation)
            if ("product_output".equals(f.getTargetType())) {
                if (!"product".equals(f.getItemType())) {
                    exportErrors.add("产品输出记录必须设置itemType=product，当前itemType=" + f.getItemType());
                } else if (f.getItemId() == null) {
                    exportErrors.add("产品输出记录必须关联有效的产品(itemId不能为空)");
                } else {
                    BsProduct prod = productMapper.selectByIdAndEnterprise(f.getItemId(), enterpriseId);
                    if (prod == null) {
                        exportErrors.add("产品输出记录关联的产品(itemId=" + f.getItemId() + ")不存在或已删除");
                    }
                }
                if (!"unit".equals(f.getSourceType())) {
                    exportErrors.add("产品输出记录的来源类型必须为unit(终端使用环节用能单元)，当前sourceType="
                            + f.getSourceType());
                } else if (f.getSourceRefId() == null) {
                    exportErrors.add("产品输出记录必须关联来源单元(sourceRefId不能为空)");
                } else {
                    BsUnit srcUnit = unitMapper.selectByIdAndEnterprise(f.getSourceRefId(), enterpriseId);
                    if (srcUnit == null) {
                        exportErrors.add("产品输出记录的来源单元(sourceRefId=" + f.getSourceRefId() + ")不存在");
                    } else if (srcUnit.getUnitType() == null) {
                        exportErrors.add("产品输出记录的来源单元 [" + srcUnit.getName()
                                + "] 缺少unitType，无法确认为终端使用环节");
                    } else if (srcUnit.getUnitType() != 3) {
                        exportErrors.add("产品输出记录的来源单元必须是终端使用环节(unitType=3)，当前来源单元类型为"
                                + srcUnit.getUnitType());
                    }
                }
            }
        }
        // Validate visible edges: record binding + endpoint node existence + endpoint semantics
        Set<Long> activeFlowIds = flows.stream()
                .filter(f -> f.getId() != null)
                .map(DeEnergyFlow::getId)
                .collect(Collectors.toSet());
        Map<Long, DeEnergyFlow> flowById = flows.stream()
                .filter(f -> f.getId() != null)
                .collect(Collectors.toMap(DeEnergyFlow::getId, f -> f, (a, b) -> a));
        Set<String> getNodeIds = new HashSet<>();
        Map<String, DiagramConfigDTO.FlowNodeDTO> getNodeMap = new LinkedHashMap<>();
        if (result.getDiagram() != null && result.getDiagram().getNodes() != null) {
            for (DiagramConfigDTO.FlowNodeDTO nd : result.getDiagram().getNodes()) {
                if (nd.getNodeId() != null) {
                    getNodeIds.add(nd.getNodeId());
                    getNodeMap.put(nd.getNodeId(), nd);
                }
            }
        }
        // Compute fixed-stage layout once for all edge validations (includes dynamic backflow lanes)
        List<DiagramConfigDTO.FlowEdgeDTO> allEdgeDtos = (result.getDiagram() != null && result.getDiagram().getEdges() != null)
                ? result.getDiagram().getEdges() : List.of();
        Map<String, double[]> fixedLayout = computeFixedStageLayout(getNodeMap, unitList,
                allEdgeDtos,
                diagram != null && diagram.getCanvasWidth() != null ? diagram.getCanvasWidth() : 1200);
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
                if (isBlank(ed.getSourceNodeId())) {
                    String msg = "连线 [" + ed.getEdgeId() + "] 的起点节点为空（待确认）";
                    warnings.add(msg);
                    exportErrors.add(msg);
                } else if (!getNodeIds.contains(ed.getSourceNodeId())) {
                    String msg = "连线 [" + ed.getEdgeId() + "] 的起点节点(" + ed.getSourceNodeId() + ")不存在（待确认）";
                    warnings.add(msg);
                    exportErrors.add(msg);
                }
                if (isBlank(ed.getTargetNodeId())) {
                    String msg = "连线 [" + ed.getEdgeId() + "] 的终点节点为空（待确认）";
                    warnings.add(msg);
                    exportErrors.add(msg);
                } else if (!getNodeIds.contains(ed.getTargetNodeId())) {
                    String msg = "连线 [" + ed.getEdgeId() + "] 的终点节点(" + ed.getTargetNodeId() + ")不存在（待确认）";
                    warnings.add(msg);
                    exportErrors.add(msg);
                }
                // Validate edge endpoint semantics against the bound fill record
                if (ed.getFlowRecordId() != null && activeFlowIds.contains(ed.getFlowRecordId())) {
                    DeEnergyFlow rec = flowById.get(ed.getFlowRecordId());
                    if (rec != null) {
                        List<String> semErrs = checkEdgeEndpointSemantics(ed, rec, getNodeMap);
                        for (String semErr : semErrs) {
                            warnings.add(semErr);
                            exportErrors.add(semErr);
                        }
                    }
                }
                // Validate routePoints against fixed-stage layout positions (same as final-effect renderer)
                List<String> rpErrs = validateEdgeRoutePoints(ed, fixedLayout);
                exportErrors.addAll(rpErrs);
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

            // Build active node ID set and node map for endpoint validation
            Set<String> activeNodeIds = new HashSet<>();
            Map<String, DiagramConfigDTO.FlowNodeDTO> nodeMap = new HashMap<>();
            if (dc.getNodes() != null) {
                for (DiagramConfigDTO.FlowNodeDTO nd : dc.getNodes()) {
                    if (nd.getNodeId() != null) {
                        activeNodeIds.add(nd.getNodeId());
                        nodeMap.put(nd.getNodeId(), nd);
                    }
                }
            }

            // Pre-validate edges: resolve flowRecordId, validate endpoints, reject invalid visible edges BEFORE deleting
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
                    // Validate source/target node endpoints exist in the incoming node set
                    if (edgeVisible == 1) {
                        if (isBlank(ed.getSourceNodeId())) {
                            throw new IllegalArgumentException(
                                    String.format("连线 [%s] 的起点节点为空，无法保存。可见连线必须指定起点节点。",
                                            ed.getEdgeId()));
                        }
                        if (isBlank(ed.getTargetNodeId())) {
                            throw new IllegalArgumentException(
                                    String.format("连线 [%s] 的终点节点为空，无法保存。可见连线必须指定终点节点。",
                                            ed.getEdgeId()));
                        }
                        if (!activeNodeIds.contains(ed.getSourceNodeId())) {
                            throw new IllegalArgumentException(
                                    String.format("连线 [%s] 的起点节点(%s)不存在于当前节点集合中，无法保存。",
                                            ed.getEdgeId(), ed.getSourceNodeId()));
                        }
                        if (!activeNodeIds.contains(ed.getTargetNodeId())) {
                            throw new IllegalArgumentException(
                                    String.format("连线 [%s] 的终点节点(%s)不存在于当前节点集合中，无法保存。",
                                            ed.getEdgeId(), ed.getTargetNodeId()));
                        }
                        // Validate edge endpoint semantics match the bound fill record
                        if (resolvedRecordId != null) {
                            final Long rid = resolvedRecordId;
                            DeEnergyFlow rec = savedRecords.stream()
                                    .filter(r -> r.getId().equals(rid)).findFirst().orElse(null);
                            if (rec != null) {
                                validateEdgeSourceSemantics(ed, rec, nodeMap);
                                validateEdgeTargetSemantics(ed, rec, nodeMap);
                            }
                        }
                    }
                    resolvedEdgeRecordIds.add(resolvedRecordId);
                }
            }

            // Now safe to replace edges (all validated) — auto-sync item fields from bound record
            edgeMapper.deleteByDiagramId(diagramId, operator);
            if (dc.getEdges() != null) {
                for (int i = 0; i < dc.getEdges().size(); i++) {
                    DiagramConfigDTO.FlowEdgeDTO ed = dc.getEdges().get(i);
                    Long rid = resolvedEdgeRecordIds.get(i);
                    // Auto-sync edge item fields from bound record to prevent drift
                    if (rid != null) {
                        DeEnergyFlow rec = savedRecords.stream()
                                .filter(r -> r.getId().equals(rid)).findFirst().orElse(null);
                        if (rec != null) {
                            ed.setItemType(rec.getItemType());
                            ed.setItemId(rec.getItemId());
                            ed.setPhysicalQuantity(rec.getPhysicalQuantity());
                            ed.setCalculatedValue(rec.getCalculatedValue());
                        }
                    }
                    DeEnergyFlowEdge edge = new DeEnergyFlowEdge();
                    edge.setDiagramId(diagramId);
                    edge.setEdgeId(ed.getEdgeId());
                    edge.setSourceNodeId(ed.getSourceNodeId());
                    edge.setTargetNodeId(ed.getTargetNodeId());
                    edge.setFlowRecordId(rid);
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
     * Non-throwing semantic check for GET/export: returns list of warning strings
     * when an edge's source/target node does not match the bound record's fields.
     */
    private List<String> checkEdgeEndpointSemantics(DiagramConfigDTO.FlowEdgeDTO edge,
                                                     DeEnergyFlow record,
                                                     Map<String, DiagramConfigDTO.FlowNodeDTO> nodeMap) {
        List<String> errors = new ArrayList<>();
        // Source semantics
        if (!isBlank(record.getSourceType()) && !isBlank(edge.getSourceNodeId())) {
            DiagramConfigDTO.FlowNodeDTO srcNode = nodeMap.get(edge.getSourceNodeId());
            if (srcNode != null) {
                String st = record.getSourceType();
                if ("external_energy".equals(st) && !"energy_input".equals(srcNode.getNodeType())) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的起点节点类型(" + srcNode.getNodeType() + ")与填报记录来源类型(external_energy→energy_input)不一致（待确认）");
                } else if ("external_energy".equals(st) && "energy".equals(record.getItemType())
                        && record.getItemId() != null && srcNode.getRefId() == null) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的起点节点未绑定具体能源(refId=null)，但填报记录指定了能源品种(itemId=" + record.getItemId() + ")（待确认）");
                } else if ("external_energy".equals(st) && "energy".equals(record.getItemType())
                        && record.getItemId() != null && srcNode.getRefId() != null
                        && !record.getItemId().equals(srcNode.getRefId())) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的起点节点引用能源(refId=" + srcNode.getRefId() + ")与填报记录能源品种(itemId=" + record.getItemId() + ")不一致（待确认）");
                } else if ("unit".equals(st) && !"unit".equals(srcNode.getNodeType())) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的起点节点类型(" + srcNode.getNodeType() + ")与填报记录来源类型(unit)不一致（待确认）");
                } else if ("unit".equals(st) && record.getSourceRefId() != null
                        && srcNode.getRefId() == null) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的起点节点未绑定具体单元(refId=null)，但填报记录指定了来源单元(sourceRefId=" + record.getSourceRefId() + ")（待确认）");
                } else if ("unit".equals(st) && record.getSourceRefId() != null
                        && srcNode.getRefId() != null && !record.getSourceRefId().equals(srcNode.getRefId())) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的起点节点引用单元(refId=" + srcNode.getRefId() + ")与填报记录来源单元(sourceRefId=" + record.getSourceRefId() + ")不一致（待确认）");
                }
            }
        }
        // Target semantics
        if (!isBlank(record.getTargetType()) && !isBlank(edge.getTargetNodeId())) {
            DiagramConfigDTO.FlowNodeDTO tgtNode = nodeMap.get(edge.getTargetNodeId());
            if (tgtNode != null) {
                String tt = record.getTargetType();
                if ("unit".equals(tt) && !"unit".equals(tgtNode.getNodeType())) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的终点节点类型(" + tgtNode.getNodeType() + ")与填报记录目的类型(unit)不一致（待确认）");
                } else if ("unit".equals(tt) && record.getTargetRefId() != null
                        && tgtNode.getRefId() == null) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的终点节点未绑定具体单元(refId=null)，但填报记录指定了目的单元(targetRefId=" + record.getTargetRefId() + ")（待确认）");
                } else if ("unit".equals(tt) && record.getTargetRefId() != null
                        && tgtNode.getRefId() != null && !record.getTargetRefId().equals(tgtNode.getRefId())) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的终点节点引用单元(refId=" + tgtNode.getRefId() + ")与填报记录目的单元(targetRefId=" + record.getTargetRefId() + ")不一致（待确认）");
                } else if ("product_output".equals(tt) && !"product_output".equals(tgtNode.getNodeType())) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的终点节点类型(" + tgtNode.getNodeType() + ")与填报记录目的类型(product_output)不一致（待确认）");
                } else if ("product_output".equals(tt) && "product".equals(record.getItemType())
                        && record.getItemId() != null && tgtNode.getRefId() == null) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的终点节点未绑定具体产品(refId=null)，但填报记录指定了产品(itemId=" + record.getItemId() + ")（待确认）");
                } else if ("product_output".equals(tt) && "product".equals(record.getItemType())
                        && record.getItemId() != null && tgtNode.getRefId() != null
                        && !record.getItemId().equals(tgtNode.getRefId())) {
                    errors.add("连线 [" + edge.getEdgeId() + "] 的终点节点引用产品(refId=" + tgtNode.getRefId() + ")与填报记录产品(itemId=" + record.getItemId() + ")不一致（待确认）");
                }
            }
        }
        return errors;
    }

    private void validateEdgeSourceSemantics(DiagramConfigDTO.FlowEdgeDTO edge,
                                              DeEnergyFlow record,
                                              Map<String, DiagramConfigDTO.FlowNodeDTO> nodeMap) {
        if (isBlank(record.getSourceType()) || isBlank(edge.getSourceNodeId())) return;
        DiagramConfigDTO.FlowNodeDTO srcNode = nodeMap.get(edge.getSourceNodeId());
        if (srcNode == null) return;
        String st = record.getSourceType();
        if ("external_energy".equals(st)) {
            if (!"energy_input".equals(srcNode.getNodeType())) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的起点节点类型(%s)与填报记录来源类型(external_energy→energy_input)不一致。",
                        edge.getEdgeId(), srcNode.getNodeType()));
            }
            if ("energy".equals(record.getItemType()) && record.getItemId() != null
                    && srcNode.getRefId() == null) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的起点节点未绑定具体能源(refId=null)，但填报记录指定了能源品种(itemId=%d)。",
                        edge.getEdgeId(), record.getItemId()));
            }
            if ("energy".equals(record.getItemType()) && record.getItemId() != null
                    && srcNode.getRefId() != null && !record.getItemId().equals(srcNode.getRefId())) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的起点节点引用能源(refId=%d)与填报记录能源品种(itemId=%d)不一致。",
                        edge.getEdgeId(), srcNode.getRefId(), record.getItemId()));
            }
        } else if ("unit".equals(st)) {
            if (!"unit".equals(srcNode.getNodeType())) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的起点节点类型(%s)与填报记录来源类型(unit)不一致。",
                        edge.getEdgeId(), srcNode.getNodeType()));
            }
            if (record.getSourceRefId() != null && srcNode.getRefId() == null) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的起点节点未绑定具体单元(refId=null)，但填报记录指定了来源单元(sourceRefId=%d)。",
                        edge.getEdgeId(), record.getSourceRefId()));
            }
            if (record.getSourceRefId() != null && srcNode.getRefId() != null
                    && !record.getSourceRefId().equals(srcNode.getRefId())) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的起点节点引用单元(refId=%d)与填报记录来源单元(sourceRefId=%d)不一致。",
                        edge.getEdgeId(), srcNode.getRefId(), record.getSourceRefId()));
            }
        }
    }

    private void validateEdgeTargetSemantics(DiagramConfigDTO.FlowEdgeDTO edge,
                                              DeEnergyFlow record,
                                              Map<String, DiagramConfigDTO.FlowNodeDTO> nodeMap) {
        if (isBlank(record.getTargetType()) || isBlank(edge.getTargetNodeId())) return;
        DiagramConfigDTO.FlowNodeDTO tgtNode = nodeMap.get(edge.getTargetNodeId());
        if (tgtNode == null) return;
        String tt = record.getTargetType();
        if ("unit".equals(tt)) {
            if (!"unit".equals(tgtNode.getNodeType())) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的终点节点类型(%s)与填报记录目的类型(unit)不一致。",
                        edge.getEdgeId(), tgtNode.getNodeType()));
            }
            if (record.getTargetRefId() != null && tgtNode.getRefId() == null) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的终点节点未绑定具体单元(refId=null)，但填报记录指定了目的单元(targetRefId=%d)。",
                        edge.getEdgeId(), record.getTargetRefId()));
            }
            if (record.getTargetRefId() != null && tgtNode.getRefId() != null
                    && !record.getTargetRefId().equals(tgtNode.getRefId())) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的终点节点引用单元(refId=%d)与填报记录目的单元(targetRefId=%d)不一致。",
                        edge.getEdgeId(), tgtNode.getRefId(), record.getTargetRefId()));
            }
        } else if ("product_output".equals(tt)) {
            if (!"product_output".equals(tgtNode.getNodeType())) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的终点节点类型(%s)与填报记录目的类型(product_output)不一致。",
                        edge.getEdgeId(), tgtNode.getNodeType()));
            }
            if ("product".equals(record.getItemType()) && record.getItemId() != null
                    && tgtNode.getRefId() == null) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的终点节点未绑定具体产品(refId=null)，但填报记录指定了产品(itemId=%d)。",
                        edge.getEdgeId(), record.getItemId()));
            }
            if ("product".equals(record.getItemType()) && record.getItemId() != null
                    && tgtNode.getRefId() != null && !record.getItemId().equals(tgtNode.getRefId())) {
                throw new IllegalArgumentException(String.format(
                        "连线 [%s] 的终点节点引用产品(refId=%d)与填报记录产品(itemId=%d)不一致。",
                        edge.getEdgeId(), tgtNode.getRefId(), record.getItemId()));
            }
        }
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
        // Terminal-use semantics: product output requires:
        //   sourceType=unit, active sourceRefId with unitType=3,
        //   itemType=product, active product itemId.
        // Block external_energy/system/custom/missing refs/missing unitType/non-terminal units.
        if ("product_output".equals(flow.getTargetType())) {
            // Must be itemType=product with active product itemId
            if (!"product".equals(flow.getItemType())) {
                errors.add("产品输出记录必须设置itemType=product，当前itemType=" + flow.getItemType());
            } else if (flow.getItemId() == null) {
                errors.add("产品输出记录必须关联有效的产品(itemId不能为空)");
            } else {
                BsProduct prod = productMapper.selectByIdAndEnterprise(flow.getItemId(), enterpriseId);
                if (prod == null) {
                    errors.add("产品输出记录关联的产品(itemId=" + flow.getItemId() + ")不存在");
                }
            }
            // Must be sourceType=unit with active terminal-use source
            if (!"unit".equals(flow.getSourceType())) {
                errors.add("产品输出记录的来源类型必须为unit(终端使用环节用能单元)，当前sourceType="
                        + flow.getSourceType());
            } else if (flow.getSourceRefId() == null) {
                errors.add("产品输出记录必须关联来源单元(sourceRefId不能为空)");
            } else {
                BsUnit srcUnit = unitMapper.selectByIdAndEnterprise(flow.getSourceRefId(), enterpriseId);
                if (srcUnit == null) {
                    errors.add("产品输出记录的来源单元(sourceRefId=" + flow.getSourceRefId() + ")不存在");
                } else if (srcUnit.getUnitType() == null) {
                    errors.add("产品输出记录的来源单元 [" + srcUnit.getName()
                            + "] 缺少unitType，无法确认为终端使用环节");
                } else if (srcUnit.getUnitType() != 3) {
                    errors.add("产品输出记录的来源单元必须是终端使用环节(unitType=3)，当前来源单元类型为"
                            + srcUnit.getUnitType());
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("填报记录[%d]校验失败: %s", index + 1, String.join("; ", errors)));
        }
    }

    /**
     * Compute fixed-stage layout positions for all visible non-product-output nodes.
     * Uses the same algorithm as the final-effect renderer (EnergyFlowConfigView).
     * Returns map of nodeId -> [cx, cy, w, h] in fixed-stage coordinates.
     */
    private Map<String, double[]> computeFixedStageLayout(
            Map<String, DiagramConfigDTO.FlowNodeDTO> nodeMap,
            List<BsUnit> unitList,
            List<DiagramConfigDTO.FlowEdgeDTO> edgeList,
            int canvasWidth) {
        final int STAGE_MARGIN = 80;
        final int BASE_HEADER_Y = 55;
        final int BF_LANE_SP = 12;
        final int ROW_H = 90;

        Map<Long, Integer> unitTypeMap = new HashMap<>();
        for (BsUnit u : unitList) {
            if (u.getId() != null && u.getUnitType() != null) {
                unitTypeMap.put(u.getId(), u.getUnitType());
            }
        }

        // Determine stage for each node
        Map<String, Integer> nodeStageMap = new HashMap<>();
        List<DiagramConfigDTO.FlowNodeDTO> visibleNodes = new ArrayList<>();
        for (DiagramConfigDTO.FlowNodeDTO nd : nodeMap.values()) {
            if (nd.getNodeId() == null) continue;
            Integer vis = nd.getVisible() != null ? nd.getVisible() : 1;
            if (vis == 0) continue;
            if ("product_output".equals(nd.getNodeType())) continue;
            int stage;
            if ("energy_input".equals(nd.getNodeType())) {
                stage = 0;
            } else if ("unit".equals(nd.getNodeType()) && nd.getRefId() != null) {
                Integer ut = unitTypeMap.get(nd.getRefId());
                if (ut != null) {
                    stage = (ut == 1) ? 1 : (ut == 2) ? 2 : (ut == 3) ? 3 : 2;
                } else {
                    stage = 2;
                }
            } else {
                stage = 2;
            }
            nodeStageMap.put(nd.getNodeId(), stage);
            visibleNodes.add(nd);
        }

        // Count backflow lanes (edges where source stage > target stage)
        // Same algorithm as EnergyFlowConfigView and config.vue fixedStageLayout
        Set<String> bfGroups = new HashSet<>();
        for (DiagramConfigDTO.FlowEdgeDTO ed : edgeList) {
            Integer eVis = ed.getVisible() != null ? ed.getVisible() : 1;
            if (eVis == 0) continue;
            Integer srcStage = nodeStageMap.get(ed.getSourceNodeId());
            Integer tgtStage = nodeStageMap.get(ed.getTargetNodeId());
            if (srcStage == null || tgtStage == null) continue;
            if (srcStage <= tgtStage) continue;
            Long iId = ed.getItemId();
            String key = iId != null
                    ? "bf-" + iId + "-" + ed.getSourceNodeId() + "-" + ed.getTargetNodeId()
                    : "bf-" + ed.getEdgeId();
            bfGroups.add(key);
        }
        int topChannelH = bfGroups.size() > 0 ? bfGroups.size() * BF_LANE_SP + 10 : 0;
        int BODY_TOP = BASE_HEADER_Y + topChannelH + 20;

        double sw = (canvasWidth - STAGE_MARGIN * 2) / 4.0;
        double[] stageXArr = new double[4];
        for (int i = 0; i < 4; i++) stageXArr[i] = STAGE_MARGIN + i * sw;

        // Bucket by stage (LinkedHashMap preserves insertion order for deterministic row assignment)
        Map<Integer, List<DiagramConfigDTO.FlowNodeDTO>> buckets = new LinkedHashMap<>();
        for (DiagramConfigDTO.FlowNodeDTO nd : visibleNodes) {
            int s = nodeStageMap.getOrDefault(nd.getNodeId(), 2);
            buckets.computeIfAbsent(s, k -> new ArrayList<>()).add(nd);
        }

        Map<String, double[]> result = new HashMap<>();
        for (Map.Entry<Integer, List<DiagramConfigDTO.FlowNodeDTO>> entry : buckets.entrySet()) {
            int stage = entry.getKey();
            List<DiagramConfigDTO.FlowNodeDTO> nodes = entry.getValue();
            for (int rowIdx = 0; rowIdx < nodes.size(); rowIdx++) {
                DiagramConfigDTO.FlowNodeDTO nd = nodes.get(rowIdx);
                boolean isCircle = "energy_input".equals(nd.getNodeType());
                double w = isCircle ? 60 : 100;
                double h = isCircle ? 60 : 50;
                double cx = stageXArr[stage] + sw / 2;
                double cy = BODY_TOP + rowIdx * ROW_H + ROW_H / 2.0;
                result.put(nd.getNodeId(), new double[]{cx, cy, w, h});
            }
        }
        return result;
    }

    /**
     * Validate route points on an edge using fixed-stage layout coordinates.
     * fixedLayout maps nodeId -> [cx, cy, w, h] from computeFixedStageLayout().
     */
    private List<String> validateEdgeRoutePoints(DiagramConfigDTO.FlowEdgeDTO ed,
                                                  Map<String, double[]> fixedLayout) {
        List<String> errors = new ArrayList<>();
        String rp = ed.getRoutePoints();
        if (rp == null || rp.isBlank()) return errors;

        // Parse JSON
        List<Map<String, Object>> points;
        try {
            ObjectMapper om = new ObjectMapper();
            points = om.readValue(rp, new TypeReference<>() {});
        } catch (Exception ex) {
            errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints为无效JSON: " + ex.getMessage());
            return errors;
        }
        if (points == null || points.isEmpty()) return errors;

        // Validate each point has numeric x, y
        List<double[]> coords = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Map<String, Object> pt = points.get(i);
            if (pt == null || !pt.containsKey("x") || !pt.containsKey("y")) {
                errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints[" + i + "]缺少x或y字段");
                return errors;
            }
            double x, y;
            try {
                x = ((Number) pt.get("x")).doubleValue();
                y = ((Number) pt.get("y")).doubleValue();
            } catch (Exception ex) {
                errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints[" + i + "]的x或y不是有效数值");
                return errors;
            }
            if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints[" + i + "]包含NaN或Infinity");
                return errors;
            }
            // Out of canvas
            if (x < 0 || y < 0 || x > 5000 || y > 5000) {
                errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints[" + i + "]超出画布范围(" + x + "," + y + ")");
            }
            coords.add(new double[]{x, y});
        }

        // Use fixed-stage layout positions for source/target endpoints
        double[] srcFixed = fixedLayout.get(ed.getSourceNodeId());
        double[] tgtFixed = fixedLayout.get(ed.getTargetNodeId());
        if (srcFixed != null && tgtFixed != null) {
            // srcFixed = [cx, cy, w, h]; exit = cx + w/2, cy
            double sx = srcFixed[0] + srcFixed[2] / 2;
            double sy = srcFixed[1];
            double tx = tgtFixed[0] - tgtFixed[2] / 2;
            double ty = tgtFixed[1];

            // Build full path: [source exit, ...routePoints, target entry]
            List<double[]> full = new ArrayList<>();
            full.add(new double[]{sx, sy});
            full.addAll(coords);
            full.add(new double[]{tx, ty});
            for (int i = 0; i < full.size() - 1; i++) {
                double[] a = full.get(i);
                double[] b = full.get(i + 1);
                if (Math.abs(a[0] - b[0]) > 1 && Math.abs(a[1] - b[1]) > 1) {
                    errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints第" + (i + 1)
                            + "段不符合90°正交规则");
                    break;
                }
            }

            // Check node crossing against fixed-stage positions
            for (Map.Entry<String, double[]> fEntry : fixedLayout.entrySet()) {
                String nid = fEntry.getKey();
                if (nid.equals(ed.getSourceNodeId()) || nid.equals(ed.getTargetNodeId())) continue;
                double[] fsn = fEntry.getValue();
                double nx = fsn[0] - fsn[2] / 2;
                double ny = fsn[1] - fsn[3] / 2;
                double nw = fsn[2];
                double nh = fsn[3];
                for (int i = 0; i < coords.size(); i++) {
                    double[] c = coords.get(i);
                    if (c[0] > nx && c[0] < nx + nw && c[1] > ny && c[1] < ny + nh) {
                        errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints["
                                + i + "]穿过节点[" + nid + "]");
                        break;
                    }
                }
            }

            // Check backflow top-channel bypass using fixed-stage positions
            if (sx > tx) {
                double topBound = Math.min(srcFixed[1] - srcFixed[3] / 2, tgtFixed[1] - tgtFixed[3] / 2);
                boolean hasTopPoint = false;
                for (double[] c : coords) {
                    if (c[1] < topBound) { hasTopPoint = true; break; }
                }
                if (!hasTopPoint) {
                    errors.add("回流连线 [" + ed.getEdgeId() + "] 的routePoints未经过顶部回流通道");
                }
            }

            // Check forward trunk compatibility against fixed-stage positions.
            // The final renderer only accepts forward X hints within ±30px of the canonical trunk X.
            // Trunk X = source exit X + 20 + slotOffset (computed per source+itemId group).
            // For simplicity, we validate that each route point X is either:
            //   (a) on a horizontal segment (same Y as source or target), OR
            //   (b) within reasonable range between source exit and target entry (±30px tolerance)
            // This prevents route points that the final renderer will silently ignore.
            if (sx < tx) {
                for (int i = 0; i < coords.size(); i++) {
                    double px = coords.get(i)[0];
                    double py = coords.get(i)[1];
                    // Route point must be within the source-to-target corridor
                    if (px < sx - 30 || px > tx + 30) {
                        errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints[" + i
                                + "]的X坐标(" + px + ")超出固定布局有效路由范围(±30px容差)，"
                                + "将被最终渲染器忽略");
                    }
                    // Vertical route points (not on source/target Y) should be near a trunk X
                    // Trunk X is typically source exit + 20~50px range
                    if (Math.abs(py - sy) > 1 && Math.abs(py - ty) > 1) {
                        // This is a vertical-segment waypoint — check it's in a valid trunk zone
                        double minTrunkX = sx + 5;
                        double maxTrunkX = tx - 5;
                        if (px < minTrunkX || px > maxTrunkX) {
                            errors.add("连线 [" + ed.getEdgeId() + "] 的routePoints[" + i
                                    + "]的X坐标(" + px + ")不在有效中继区域，最终渲染器可能忽略");
                        }
                    }
                }
            }
        }

        return errors;
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
