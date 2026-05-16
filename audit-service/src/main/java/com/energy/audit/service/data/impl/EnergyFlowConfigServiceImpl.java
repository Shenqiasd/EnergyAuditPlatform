package com.energy.audit.service.data.impl;

import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.data.DeEnergyFlowDiagramMapper;
import com.energy.audit.dao.mapper.data.DeEnergyFlowEdgeMapper;
import com.energy.audit.dao.mapper.data.DeEnergyFlowMapper;
import com.energy.audit.dao.mapper.data.DeEnergyFlowNodeMapper;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseMapper;
import com.energy.audit.dao.mapper.setting.BsEnergyMapper;
import com.energy.audit.dao.mapper.setting.BsProductMapper;
import com.energy.audit.dao.mapper.setting.BsUnitMapper;
import com.energy.audit.model.dto.DiagramConfigDTO;
import com.energy.audit.model.dto.EnergyFlowConfigDTO;
import com.energy.audit.model.dto.SaveEnergyFlowConfigDTO;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
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
    private final BsUnitMapper unitMapper;
    private final BsEnergyMapper energyMapper;
    private final BsProductMapper productMapper;

    public EnergyFlowConfigServiceImpl(DeEnergyFlowMapper flowMapper,
                                        DeEnergyFlowDiagramMapper diagramMapper,
                                        DeEnergyFlowNodeMapper nodeMapper,
                                        DeEnergyFlowEdgeMapper edgeMapper,
                                        EntEnterpriseMapper enterpriseMapper,
                                        BsUnitMapper unitMapper,
                                        BsEnergyMapper energyMapper,
                                        BsProductMapper productMapper) {
        this.flowMapper = flowMapper;
        this.diagramMapper = diagramMapper;
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.enterpriseMapper = enterpriseMapper;
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
        boolean entComplete = ent != null
                && ent.getEnterpriseName() != null && !ent.getEnterpriseName().isBlank()
                && ent.getCreditCode() != null && !ent.getCreditCode().isBlank()
                && ent.getContactPerson() != null && !ent.getContactPerson().isBlank();
        validation.setEnterpriseComplete(entComplete);
        validation.setHasUnits(!unitList.isEmpty());
        validation.setHasEnergies(!energyList.isEmpty());
        validation.setHasProducts(!productList.isEmpty());
        validation.setValid(entComplete && !unitList.isEmpty() && !energyList.isEmpty() && !productList.isEmpty());

        List<String> warnings = new ArrayList<>();
        List<String> exportErrors = new ArrayList<>();
        if (!entComplete) {
            exportErrors.add("企业信息不完整（需填写企业名称、统一社会信用代码、联系人）");
        }
        if (unitList.isEmpty()) exportErrors.add("至少需要一个用能单元");
        if (energyList.isEmpty()) exportErrors.add("至少需要一个能源品种");
        if (productList.isEmpty()) exportErrors.add("至少需要一个产品");

        for (DeEnergyFlow f : flows) {
            if ("energy".equals(f.getItemType()) && f.getItemId() != null) {
                BsEnergy energy = energyMapper.selectByIdAndEnterprise(f.getItemId(), enterpriseId);
                if (energy != null && energy.getEquivalentValue() == null) {
                    String msg = "能源 [" + energy.getName() + "] 缺少折标系数";
                    warnings.add(msg);
                    exportErrors.add(msg);
                }
            } else if ("product".equals(f.getItemType()) && f.getItemId() != null) {
                BsProduct product = productMapper.selectByIdAndEnterprise(f.getItemId(), enterpriseId);
                if (product != null && product.getUnitPrice() == null) {
                    String msg = "产品 [" + product.getName() + "] 缺少单价";
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

        // 1. Upsert flow records (preserve IDs so edges keep stable flowRecordId)
        List<DeEnergyFlow> savedRecords = new ArrayList<>();
        if (dto.getFlowRecords() != null) {
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

            // Replace edges (resolve flowRecordId from flowRecordIndex if needed)
            edgeMapper.deleteByDiagramId(diagramId, operator);
            if (dc.getEdges() != null) {
                for (DiagramConfigDTO.FlowEdgeDTO ed : dc.getEdges()) {
                    DeEnergyFlowEdge edge = new DeEnergyFlowEdge();
                    edge.setDiagramId(diagramId);
                    edge.setEdgeId(ed.getEdgeId());
                    edge.setSourceNodeId(ed.getSourceNodeId());
                    edge.setTargetNodeId(ed.getTargetNodeId());

                    // Resolve flowRecordId: prefer flowRecordIndex → saved record ID
                    Long resolvedRecordId = ed.getFlowRecordId();
                    if (ed.getFlowRecordIndex() != null
                            && ed.getFlowRecordIndex() >= 0
                            && ed.getFlowRecordIndex() < savedRecords.size()) {
                        resolvedRecordId = savedRecords.get(ed.getFlowRecordIndex()).getId();
                    }
                    edge.setFlowRecordId(resolvedRecordId);

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
