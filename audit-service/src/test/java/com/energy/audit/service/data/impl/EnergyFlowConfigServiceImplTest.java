package com.energy.audit.service.data.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnergyFlowConfigServiceImplTest {

    private DeEnergyFlowMapper flowMapper;
    private DeEnergyFlowDiagramMapper diagramMapper;
    private DeEnergyFlowNodeMapper nodeMapper;
    private DeEnergyFlowEdgeMapper edgeMapper;
    private EntEnterpriseMapper enterpriseMapper;
    private EntEnterpriseSettingMapper enterpriseSettingMapper;
    private BsUnitMapper unitMapper;
    private BsEnergyMapper energyMapper;
    private BsProductMapper productMapper;

    private EnergyFlowConfigServiceImpl service;

    private static final Long ENT_ID = 100L;
    private static final Integer YEAR = 2026;

    @BeforeEach
    void setUp() {
        flowMapper = mock(DeEnergyFlowMapper.class);
        diagramMapper = mock(DeEnergyFlowDiagramMapper.class);
        nodeMapper = mock(DeEnergyFlowNodeMapper.class);
        edgeMapper = mock(DeEnergyFlowEdgeMapper.class);
        enterpriseMapper = mock(EntEnterpriseMapper.class);
        enterpriseSettingMapper = mock(EntEnterpriseSettingMapper.class);
        unitMapper = mock(BsUnitMapper.class);
        energyMapper = mock(BsEnergyMapper.class);
        productMapper = mock(BsProductMapper.class);

        service = new EnergyFlowConfigServiceImpl(
                flowMapper, diagramMapper, nodeMapper, edgeMapper,
                enterpriseMapper, enterpriseSettingMapper, unitMapper, energyMapper, productMapper);
    }

    // ============================================================
    // getConfig tests
    // ============================================================

    @Test
    void getConfigReturnsEnterpriseInfo() {
        EntEnterprise ent = new EntEnterprise();
        ent.setId(ENT_ID);
        ent.setEnterpriseName("测试企业");
        when(enterpriseMapper.selectById(ENT_ID)).thenReturn(ent);
        stubEmpty();

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getEnterpriseInfo()).isNotNull();
        assertThat(result.getEnterpriseInfo().getName()).isEqualTo("测试企业");
    }

    @Test
    void getConfigValidationFailsWhenNoUnits() {
        stubEnterpriseComplete();
        when(unitMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isValid()).isFalse();
        assertThat(result.getValidation().isHasUnits()).isFalse();
        assertThat(result.getValidation().getExportErrors()).anyMatch(e -> e.contains("用能单元"));
    }

    @Test
    void getConfigValidationPassesWhenAllPrerequisitesMet() {
        stubEnterpriseComplete();
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isValid()).isTrue();
        assertThat(result.getValidation().isExportReady()).isTrue();
        assertThat(result.getValidation().getExportErrors()).isEmpty();
    }

    @Test
    void getConfigWarnsMissingEquivalentValue() {
        stubEnterpriseComplete();
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "天然气", null)));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));

        DeEnergyFlow flow = new DeEnergyFlow();
        flow.setItemType("energy");
        flow.setItemId(1L);
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(List.of(flow));

        BsEnergy e = energy(1L, "天然气", null);
        when(energyMapper.selectByIdAndEnterprise(1L, ENT_ID)).thenReturn(e);

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().getWarnings())
                .anyMatch(w -> w.contains("天然气") && w.contains("折标系数"));
        assertThat(result.getValidation().getExportErrors())
                .anyMatch(w -> w.contains("天然气") && w.contains("折标系数"));
        assertThat(result.getValidation().isExportReady()).isFalse();
    }

    @Test
    void getConfigWarnsMissingProductUnitPrice() {
        stubEnterpriseComplete();
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "车间", 3)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品B", null)));

        DeEnergyFlow flow = new DeEnergyFlow();
        flow.setItemType("product");
        flow.setItemId(1L);
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(List.of(flow));

        BsProduct p = product(1L, "产品B", null);
        when(productMapper.selectByIdAndEnterprise(1L, ENT_ID)).thenReturn(p);

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().getWarnings())
                .anyMatch(w -> w.contains("产品B") && w.contains("单价"));
        assertThat(result.getValidation().getExportErrors())
                .anyMatch(w -> w.contains("产品B") && w.contains("单价"));
        assertThat(result.getValidation().isExportReady()).isFalse();
    }

    @Test
    void getConfigReturnsDiagramNodesEdges() {
        stubEnterpriseComplete();
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        DeEnergyFlowDiagram diagram = new DeEnergyFlowDiagram();
        diagram.setId(10L);
        diagram.setName("测试图");
        diagram.setDiagramType(3);
        diagram.setCanvasWidth(1200);
        diagram.setCanvasHeight(800);
        when(diagramMapper.selectByEnterpriseYearType(ENT_ID, YEAR, 3)).thenReturn(diagram);

        DeEnergyFlowNode node = new DeEnergyFlowNode();
        node.setId(20L);
        node.setNodeId("node-1");
        node.setNodeType("unit");
        node.setLabel("锅炉房");
        node.setPositionX(100.0);
        node.setPositionY(200.0);
        when(nodeMapper.selectByDiagramId(10L)).thenReturn(List.of(node));

        DeEnergyFlowEdge edge = new DeEnergyFlowEdge();
        edge.setId(30L);
        edge.setEdgeId("edge-1");
        edge.setSourceNodeId("node-1");
        edge.setTargetNodeId("node-2");
        edge.setFlowRecordId(50L);
        edge.setItemType("energy");
        edge.setItemId(1L);
        edge.setPhysicalQuantity(new BigDecimal("100"));
        when(edgeMapper.selectByDiagramId(10L)).thenReturn(List.of(edge));

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getDiagram()).isNotNull();
        assertThat(result.getDiagram().getNodes()).hasSize(1);
        assertThat(result.getDiagram().getNodes().get(0).getLabel()).isEqualTo("锅炉房");
        assertThat(result.getDiagram().getEdges()).hasSize(1);
        assertThat(result.getDiagram().getEdges().get(0).getFlowRecordId()).isEqualTo(50L);
    }

    @Test
    void getConfigHandlesOldRecordsWithoutV2Fields() {
        stubEnterpriseComplete();
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));

        // Old record without itemType/itemId
        DeEnergyFlow oldFlow = new DeEnergyFlow();
        oldFlow.setId(99L);
        oldFlow.setSourceUnit("外购");
        oldFlow.setTargetUnit("锅炉房");
        oldFlow.setEnergyProduct("电力");
        oldFlow.setPhysicalQuantity(new BigDecimal("5000"));
        oldFlow.setStandardQuantity(new BigDecimal("614.5"));
        // v2 fields are null
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(List.of(oldFlow));

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getFlowRecords()).hasSize(1);
        assertThat(result.getFlowRecords().get(0).getEnergyProduct()).isEqualTo("电力");
        assertThat(result.getFlowRecords().get(0).getItemType()).isNull();
    }

    // ============================================================
    // Energy calculation tests
    // ============================================================

    @Test
    void saveConfigCalculatesEnergyFoldedValue() {
                com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
                try {
                    BsEnergy elec = energy(1L, "电力", new BigDecimal("0.1229"));
                    when(energyMapper.selectByIdAndEnterprise(1L, ENT_ID)).thenReturn(elec);

                    DeEnergyFlow flow = new DeEnergyFlow();
                    flow.setItemType("energy");
                    flow.setItemId(1L);
                    flow.setPhysicalQuantity(new BigDecimal("10000"));

                    SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
                    dto.setFlowRecords(List.of(flow));

                    service.saveConfig(ENT_ID, YEAR, dto);

                    verify(flowMapper).insert(any(DeEnergyFlow.class));
                    // calculatedValue = 10000 * 0.1229 = 1229.0000
                    assertThat(flow.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("1229.0000"));
                } finally {
                    com.energy.audit.common.util.SecurityUtils.clear();
                }
    }

    @Test
    void saveConfigCalculatesProductPriceValue() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            BsProduct prod = product(1L, "产品A", new BigDecimal("5.5"));
            when(productMapper.selectByIdAndEnterprise(1L, ENT_ID)).thenReturn(prod);

            DeEnergyFlow flow = new DeEnergyFlow();
            flow.setItemType("product");
            flow.setItemId(1L);
            flow.setPhysicalQuantity(new BigDecimal("2000"));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(flow));

            service.saveConfig(ENT_ID, YEAR, dto);

            verify(flowMapper).insert(any(DeEnergyFlow.class));
            // calculatedValue = 2000 * 5.5 = 11000.0
            assertThat(flow.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("11000.0"));
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigSkipsCalcWhenPhysicalQuantityNull() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            // Legacy row (has energyProduct, no sourceType) bypasses strict validation
            DeEnergyFlow flow = new DeEnergyFlow();
            flow.setEnergyProduct("电力");
            flow.setItemType("energy");
            flow.setItemId(1L);
            // physicalQuantity is null

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(flow));

            service.saveConfig(ENT_ID, YEAR, dto);

            verify(flowMapper).insert(any(DeEnergyFlow.class));
            assertThat(flow.getCalculatedValue()).isNull();
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigBackwardCompatByName() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            BsEnergy elec = energy(1L, "电力", new BigDecimal("0.1229"));
            when(energyMapper.selectByEnterpriseAndName(ENT_ID, "电力")).thenReturn(elec);

            DeEnergyFlow flow = new DeEnergyFlow();
            flow.setEnergyProduct("电力");
            flow.setPhysicalQuantity(new BigDecimal("5000"));
            // No itemType/itemId

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(flow));

            service.saveConfig(ENT_ID, YEAR, dto);

            // Should calculate via backward-compat name matching
            assertThat(flow.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("614.5000"));
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    // ============================================================
    // Diagram save tests
    // ============================================================

    @Test
    void saveConfigCreatesDiagramWhenNoneExists() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());
            when(diagramMapper.selectByEnterpriseYearType(ENT_ID, YEAR, 3)).thenReturn(null);

            // Create a record so the edge can bind to it
            DeEnergyFlow rec = new DeEnergyFlow();
            rec.setItemType("energy");
            rec.setItemId(1L);
            rec.setPhysicalQuantity(new BigDecimal("100"));
            doAnswer(inv -> {
                DeEnergyFlow f = inv.getArgument(0);
                f.setId(50L);
                return 1;
            }).when(flowMapper).insert(any(DeEnergyFlow.class));

            DiagramConfigDTO dc = new DiagramConfigDTO();
            dc.setName("测试图");
            dc.setCanvasWidth(1200);
            dc.setCanvasHeight(800);

            DiagramConfigDTO.FlowNodeDTO node = new DiagramConfigDTO.FlowNodeDTO();
            node.setNodeId("node-1");
            node.setNodeType("unit");
            node.setLabel("锅炉房");
            node.setPositionX(100.0);
            node.setPositionY(200.0);
            dc.setNodes(List.of(node));

            DiagramConfigDTO.FlowEdgeDTO edge = new DiagramConfigDTO.FlowEdgeDTO();
            edge.setEdgeId("edge-1");
            edge.setSourceNodeId("node-1");
            edge.setTargetNodeId("node-2");
            edge.setFlowRecordIndex(0); // resolve to first saved record (ID=50)
            dc.setEdges(List.of(edge));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(rec));
            dto.setDiagram(dc);

            service.saveConfig(ENT_ID, YEAR, dto);

            verify(diagramMapper).insert(any(DeEnergyFlowDiagram.class));
            verify(nodeMapper).insert(any(DeEnergyFlowNode.class));
            verify(edgeMapper).insert(any(DeEnergyFlowEdge.class));
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigUpdatesExistingDiagram() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            DeEnergyFlowDiagram existing = new DeEnergyFlowDiagram();
            existing.setId(10L);
            existing.setName("旧图");
            when(diagramMapper.selectByEnterpriseYearType(ENT_ID, YEAR, 3)).thenReturn(existing);

            DiagramConfigDTO dc = new DiagramConfigDTO();
            dc.setName("新图");
            dc.setCanvasWidth(1400);
            dc.setCanvasHeight(900);
            dc.setNodes(Collections.emptyList());
            dc.setEdges(Collections.emptyList());

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setDiagram(dc);

            service.saveConfig(ENT_ID, YEAR, dto);

            verify(diagramMapper).update(existing);
            verify(diagramMapper, never()).insert(any());
            assertThat(existing.getName()).isEqualTo("新图");
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigFlowRecordIdBindingPreserved() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());
            when(diagramMapper.selectByEnterpriseYearType(ENT_ID, YEAR, 3)).thenReturn(null);

            // Create a record so the edge can bind to it
            DeEnergyFlow rec = new DeEnergyFlow();
            rec.setItemType("energy");
            rec.setItemId(5L);
            rec.setPhysicalQuantity(new BigDecimal("100"));
            doAnswer(inv -> {
                DeEnergyFlow f = inv.getArgument(0);
                f.setId(77L);
                return 1;
            }).when(flowMapper).insert(any(DeEnergyFlow.class));

            DiagramConfigDTO dc = new DiagramConfigDTO();
            dc.setName("绑定测试图");
            dc.setCanvasWidth(1200);
            dc.setCanvasHeight(800);
            dc.setNodes(Collections.emptyList());

            DiagramConfigDTO.FlowEdgeDTO edge = new DiagramConfigDTO.FlowEdgeDTO();
            edge.setEdgeId("edge-binding");
            edge.setSourceNodeId("n1");
            edge.setTargetNodeId("n2");
            edge.setFlowRecordIndex(0); // resolve to first saved record (ID=77)
            edge.setItemType("energy");
            edge.setItemId(5L);
            dc.setEdges(List.of(edge));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(rec));
            dto.setDiagram(dc);

            List<DeEnergyFlowEdge> capturedEdges = new ArrayList<>();
            doAnswer(inv -> {
                capturedEdges.add(inv.getArgument(0));
                return 1;
            }).when(edgeMapper).insert(any(DeEnergyFlowEdge.class));

            service.saveConfig(ENT_ID, YEAR, dto);

            assertThat(capturedEdges).hasSize(1);
            assertThat(capturedEdges.get(0).getFlowRecordId()).isEqualTo(77L);
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    // ============================================================
    // Fix #1 regression: upsert preserves IDs, flowRecordIndex resolves
    // ============================================================

    @Test
    void saveConfigUpsertsExistingRecordsAndDeletesRemoved() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            // Simulate two existing records in DB
            DeEnergyFlow existing1 = new DeEnergyFlow();
            existing1.setId(10L);
            existing1.setEnterpriseId(ENT_ID);
            existing1.setAuditYear(YEAR);
            DeEnergyFlow existing2 = new DeEnergyFlow();
            existing2.setId(20L);
            existing2.setEnterpriseId(ENT_ID);
            existing2.setAuditYear(YEAR);
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR))
                    .thenReturn(List.of(existing1, existing2));

            // Incoming: keep record 10, drop record 20, add new record
            DeEnergyFlow keep = new DeEnergyFlow();
            keep.setId(10L);
            keep.setItemType("energy");
            keep.setItemId(1L);
            keep.setPhysicalQuantity(new BigDecimal("100"));

            DeEnergyFlow newRec = new DeEnergyFlow();
            newRec.setItemType("product");
            newRec.setItemId(2L);
            newRec.setPhysicalQuantity(new BigDecimal("200"));

            // Make insert set an ID to simulate auto-increment
            doAnswer(inv -> {
                DeEnergyFlow f = inv.getArgument(0);
                f.setId(30L);
                return 1;
            }).when(flowMapper).insert(any(DeEnergyFlow.class));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(keep, newRec));

            service.saveConfig(ENT_ID, YEAR, dto);

            // Existing record 10 → updateById, not insert
            verify(flowMapper).updateById(keep);
            // New record → insert
            verify(flowMapper).insert(newRec);
            // Record 20 removed → soft-delete
            verify(flowMapper).softDeleteByIdAndEnterprise(20L, ENT_ID, "test");
            // The old deleteByEnterpriseAndYear should NOT be called
            verify(flowMapper, never()).deleteByEnterpriseAndYear(anyLong(), anyInt(), anyString());
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigResolvesFlowRecordIdFromIndex() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());
            when(diagramMapper.selectByEnterpriseYearType(ENT_ID, YEAR, 3)).thenReturn(null);

            // One new record, insert simulates auto-increment ID = 42
            DeEnergyFlow rec = new DeEnergyFlow();
            rec.setItemType("energy");
            rec.setItemId(1L);
            rec.setPhysicalQuantity(new BigDecimal("500"));
            doAnswer(inv -> {
                DeEnergyFlow f = inv.getArgument(0);
                f.setId(42L);
                return 1;
            }).when(flowMapper).insert(any(DeEnergyFlow.class));

            // Edge references record by flowRecordIndex=0 (no flowRecordId yet)
            DiagramConfigDTO dc = new DiagramConfigDTO();
            dc.setName("idx resolve test");
            dc.setCanvasWidth(1200);
            dc.setCanvasHeight(800);
            dc.setNodes(Collections.emptyList());

            DiagramConfigDTO.FlowEdgeDTO edge = new DiagramConfigDTO.FlowEdgeDTO();
            edge.setEdgeId("edge-idx");
            edge.setSourceNodeId("n1");
            edge.setTargetNodeId("n2");
            edge.setFlowRecordId(null);
            edge.setFlowRecordIndex(0);
            dc.setEdges(List.of(edge));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(rec));
            dto.setDiagram(dc);

            // Capture the edge that gets inserted
            List<DeEnergyFlowEdge> capturedEdges = new ArrayList<>();
            doAnswer(inv -> {
                capturedEdges.add(inv.getArgument(0));
                return 1;
            }).when(edgeMapper).insert(any(DeEnergyFlowEdge.class));

            service.saveConfig(ENT_ID, YEAR, dto);

            // Edge's flowRecordId should be resolved to 42 (the newly inserted record's ID)
            assertThat(capturedEdges).hasSize(1);
            assertThat(capturedEdges.get(0).getFlowRecordId()).isEqualTo(42L);
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    // ============================================================
    // Fix #3 regression: export validation blocks on missing coefficients
    // ============================================================

    @Test
    void getConfigExportBlockedWhenEnterpriseIncomplete() {
        // Enterprise with name only (missing creditCode, contactPerson)
        stubEnterprise("测试企业");
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isExportReady()).isFalse();
        assertThat(result.getValidation().getExportErrors())
                .anyMatch(e -> e.contains("企业信息不完整"));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void stubEnterprise(String name) {
        EntEnterprise ent = new EntEnterprise();
        ent.setId(ENT_ID);
        ent.setEnterpriseName(name);
        when(enterpriseMapper.selectById(ENT_ID)).thenReturn(ent);
    }

    private void stubEnterpriseComplete() {
        EntEnterprise ent = new EntEnterprise();
        ent.setId(ENT_ID);
        ent.setEnterpriseName("测试企业");
        ent.setCreditCode("91000000MA0XXXXX");
        ent.setContactPerson("张三");
        when(enterpriseMapper.selectById(ENT_ID)).thenReturn(ent);
        // Stub a fully complete enterprise setting
        when(enterpriseSettingMapper.selectByEnterpriseId(ENT_ID)).thenReturn(buildCompleteSetting());
    }

    private static EntEnterpriseSetting buildCompleteSetting() {
        EntEnterpriseSetting s = new EntEnterpriseSetting();
        s.setEnterpriseId(ENT_ID);
        s.setRegion("浙江省");
        s.setIndustryField("制造业");
        s.setUnitNature("国有企业");
        s.setEnergyUsageType("重点用能");
        s.setIndustryCode("C26");
        s.setRegisteredDate(java.time.LocalDate.of(2020, 1, 1));
        s.setRegisteredCapital(new BigDecimal("5000"));
        s.setLegalRepresentative("李四");
        s.setLegalPhone("0571-88888888");
        s.setSuperiorDepartment("市发改委");
        s.setEnterpriseAddress("杭州市");
        s.setPostalCode("310000");
        s.setEnterpriseEmail("test@example.com");
        s.setEnergyMgmtOrg("能源管理部");
        s.setEnergyLeaderName("王五");
        s.setEnergyLeaderPhone("13800000001");
        s.setEnergyLeaderTitle("副总经理");
        s.setEnergyDeptName("节能办");
        s.setEnergyManagerName("赵六");
        s.setEnergyManagerMobile("13800000002");
        s.setEnergyAuditContactName("钱七");
        s.setEnergyAuditContactPhone("13800000003");
        s.setCompilerContact("审计公司");
        s.setCompilerName("孙八");
        s.setCompilerMobile("13800000004");
        s.setCompilerEmail("compiler@example.com");
        s.setEnergyCert(1);
        s.setHasEnergyCenter(0);
        s.setCertPassDate(java.time.LocalDate.of(2023, 6, 15));
        s.setCertAuthority("国家认证机构");
        return s;
    }

    private void stubEmpty() {
        when(unitMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(energyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(flowMapper.selectByEnterpriseAndYear(anyLong(), anyInt())).thenReturn(Collections.emptyList());
    }

    private static BsUnit unit(Long id, String name, Integer unitType) {
        BsUnit u = new BsUnit();
        u.setId(id);
        u.setName(name);
        u.setUnitType(unitType);
        return u;
    }

    private static BsEnergy energy(Long id, String name, BigDecimal equivalentValue) {
        BsEnergy e = new BsEnergy();
        e.setId(id);
        e.setName(name);
        e.setEquivalentValue(equivalentValue);
        return e;
    }

    private static BsProduct product(Long id, String name, BigDecimal unitPrice) {
        BsProduct p = new BsProduct();
        p.setId(id);
        p.setName(name);
        p.setUnitPrice(unitPrice);
        return p;
    }

    // ============================================================
    // Fix #5 regression: enterprise completeness via ent_enterprise_setting
    // ============================================================

    @Test
    void getConfigExportBlockedWhenSettingMissing() {
        // Enterprise basic fields OK, but no ent_enterprise_setting row
        EntEnterprise ent = new EntEnterprise();
        ent.setId(ENT_ID);
        ent.setEnterpriseName("测试企业");
        ent.setCreditCode("91000000MA0XXXXX");
        ent.setContactPerson("张三");
        when(enterpriseMapper.selectById(ENT_ID)).thenReturn(ent);
        when(enterpriseSettingMapper.selectByEnterpriseId(ENT_ID)).thenReturn(null);
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isEnterpriseComplete()).isFalse();
        assertThat(result.getValidation().isExportReady()).isFalse();
        assertThat(result.getValidation().getExportErrors())
                .anyMatch(e -> e.contains("企业信息不完整"));
    }

    @Test
    void getConfigExportBlockedWhenSettingFieldsMissing() {
        // Enterprise basic OK, setting exists but region is blank
        EntEnterprise ent = new EntEnterprise();
        ent.setId(ENT_ID);
        ent.setEnterpriseName("测试企业");
        ent.setCreditCode("91000000MA0XXXXX");
        ent.setContactPerson("张三");
        when(enterpriseMapper.selectById(ENT_ID)).thenReturn(ent);
        EntEnterpriseSetting partial = buildCompleteSetting();
        partial.setRegion(null); // missing
        partial.setEnergyLeaderName(""); // blank
        when(enterpriseSettingMapper.selectByEnterpriseId(ENT_ID)).thenReturn(partial);
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isEnterpriseComplete()).isFalse();
        assertThat(result.getValidation().isExportReady()).isFalse();
        assertThat(result.getValidation().getExportErrors())
                .anyMatch(e -> e.contains("所属地区"))
                .anyMatch(e -> e.contains("节能领导姓名"));
    }

    // ============================================================
    // Fix #1 (v3): visible edges without valid flowRecordId are NOT persisted
    // ============================================================

    @Test
    void saveConfigSkipsVisibleEdgeWithInvalidFlowRecordId() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());
            when(diagramMapper.selectByEnterpriseYearType(ENT_ID, YEAR, 3)).thenReturn(null);

            DeEnergyFlow rec = new DeEnergyFlow();
            rec.setItemType("energy");
            rec.setItemId(1L);
            rec.setPhysicalQuantity(new BigDecimal("100"));
            doAnswer(inv -> {
                DeEnergyFlow f = inv.getArgument(0);
                f.setId(50L);
                return 1;
            }).when(flowMapper).insert(any(DeEnergyFlow.class));

            DiagramConfigDTO dc = new DiagramConfigDTO();
            dc.setName("skip unbound test");
            dc.setCanvasWidth(1200);
            dc.setCanvasHeight(800);
            dc.setNodes(Collections.emptyList());

            // Visible edge with invalid flowRecordId (999 not in saved records)
            DiagramConfigDTO.FlowEdgeDTO badEdge = new DiagramConfigDTO.FlowEdgeDTO();
            badEdge.setEdgeId("edge-bad");
            badEdge.setSourceNodeId("n1");
            badEdge.setTargetNodeId("n2");
            badEdge.setFlowRecordId(999L);
            badEdge.setFlowRecordIndex(null);
            badEdge.setVisible(1);
            dc.setEdges(List.of(badEdge));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(rec));
            dto.setDiagram(dc);

            service.saveConfig(ENT_ID, YEAR, dto);

            // Visible edge with no valid binding must NOT be inserted
            verify(edgeMapper, never()).insert(any(DeEnergyFlowEdge.class));
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigSkipsVisibleEdgeWithNullFlowRecordId() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());
            when(diagramMapper.selectByEnterpriseYearType(ENT_ID, YEAR, 3)).thenReturn(null);

            DiagramConfigDTO dc = new DiagramConfigDTO();
            dc.setName("null ref test");
            dc.setCanvasWidth(1200);
            dc.setCanvasHeight(800);
            dc.setNodes(Collections.emptyList());

            DiagramConfigDTO.FlowEdgeDTO edge = new DiagramConfigDTO.FlowEdgeDTO();
            edge.setEdgeId("edge-null");
            edge.setSourceNodeId("n1");
            edge.setTargetNodeId("n2");
            edge.setFlowRecordId(null);
            edge.setFlowRecordIndex(null);
            edge.setVisible(1);
            dc.setEdges(List.of(edge));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setDiagram(dc);

            service.saveConfig(ENT_ID, YEAR, dto);

            verify(edgeMapper, never()).insert(any(DeEnergyFlowEdge.class));
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigSkipsVisibleEdgeWithOutOfRangeFlowRecordIndex() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());
            when(diagramMapper.selectByEnterpriseYearType(ENT_ID, YEAR, 3)).thenReturn(null);

            DeEnergyFlow rec = new DeEnergyFlow();
            rec.setItemType("energy");
            rec.setItemId(1L);
            rec.setPhysicalQuantity(new BigDecimal("100"));
            doAnswer(inv -> {
                DeEnergyFlow f = inv.getArgument(0);
                f.setId(50L);
                return 1;
            }).when(flowMapper).insert(any(DeEnergyFlow.class));

            DiagramConfigDTO dc = new DiagramConfigDTO();
            dc.setName("oob index test");
            dc.setCanvasWidth(1200);
            dc.setCanvasHeight(800);
            dc.setNodes(Collections.emptyList());

            DiagramConfigDTO.FlowEdgeDTO edge = new DiagramConfigDTO.FlowEdgeDTO();
            edge.setEdgeId("edge-oob");
            edge.setSourceNodeId("n1");
            edge.setTargetNodeId("n2");
            edge.setFlowRecordId(null);
            edge.setFlowRecordIndex(99); // out of range
            edge.setVisible(1);
            dc.setEdges(List.of(edge));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(rec));
            dto.setDiagram(dc);

            service.saveConfig(ENT_ID, YEAR, dto);

            verify(edgeMapper, never()).insert(any(DeEnergyFlowEdge.class));
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    // ============================================================
    // Fix #3 (v3): server-side fill record validation
    // ============================================================

    @Test
    void saveConfigRejectsMissingSourceType() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

            DeEnergyFlow flow = new DeEnergyFlow();
            // sourceType is null, not a legacy row (no energyProduct)
            flow.setTargetType("unit");
            flow.setTargetRefId(1L);
            flow.setItemType("energy");
            flow.setItemId(1L);
            flow.setPhysicalQuantity(new BigDecimal("100"));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(flow));

            assertThatThrownBy(() -> service.saveConfig(ENT_ID, YEAR, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceType");
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigRejectsMissingTargetType() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

            DeEnergyFlow flow = new DeEnergyFlow();
            flow.setSourceType("external_energy");
            // targetType is null
            flow.setItemType("energy");
            flow.setItemId(1L);
            flow.setPhysicalQuantity(new BigDecimal("100"));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(flow));

            assertThatThrownBy(() -> service.saveConfig(ENT_ID, YEAR, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("targetType");
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigRejectsMissingItemType() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

            DeEnergyFlow flow = new DeEnergyFlow();
            flow.setSourceType("external_energy");
            flow.setTargetType("unit");
            flow.setTargetRefId(1L);
            // itemType is null
            flow.setPhysicalQuantity(new BigDecimal("100"));

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(flow));

            assertThatThrownBy(() -> service.saveConfig(ENT_ID, YEAR, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("itemType");
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigRejectsMissingPhysicalQuantity() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

            DeEnergyFlow flow = new DeEnergyFlow();
            flow.setSourceType("external_energy");
            flow.setTargetType("unit");
            flow.setTargetRefId(1L);
            flow.setItemType("energy");
            flow.setItemId(1L);
            // physicalQuantity is null

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(flow));

            assertThatThrownBy(() -> service.saveConfig(ENT_ID, YEAR, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("physicalQuantity");
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void saveConfigAllowsLegacyRowWithoutStrictValidation() {
        com.energy.audit.common.util.SecurityUtils.setContext(1L, "test", 3, ENT_ID);
        try {
            when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

            // Legacy row: no sourceType but has energyProduct
            DeEnergyFlow flow = new DeEnergyFlow();
            flow.setEnergyProduct("电力");
            flow.setPhysicalQuantity(new BigDecimal("5000"));

            BsEnergy elec = energy(1L, "电力", new BigDecimal("0.1229"));
            when(energyMapper.selectByEnterpriseAndName(ENT_ID, "电力")).thenReturn(elec);

            SaveEnergyFlowConfigDTO dto = new SaveEnergyFlowConfigDTO();
            dto.setFlowRecords(List.of(flow));

            assertThatNoException().isThrownBy(() -> service.saveConfig(ENT_ID, YEAR, dto));
            verify(flowMapper).insert(any(DeEnergyFlow.class));
        } finally {
            com.energy.audit.common.util.SecurityUtils.clear();
        }
    }

    @Test
    void getConfigExportBlockedForLegacyUnresolvedRows() {
        stubEnterpriseComplete();
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));

        DeEnergyFlow legacy = new DeEnergyFlow();
        legacy.setEnergyProduct("煤炭");
        legacy.setPhysicalQuantity(new BigDecimal("3000"));
        // No itemType/itemId
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(List.of(legacy));

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isExportReady()).isFalse();
        assertThat(result.getValidation().getExportErrors())
                .anyMatch(e -> e.contains("待确认") && e.contains("煤炭"));
    }

    // ============================================================
    // Fix #4 (v3): conditional enterprise completeness (certPassDate/certAuthority)
    // ============================================================

    @Test
    void getConfigExportBlockedWhenEnergyCert1MissingCertDate() {
        EntEnterprise ent = new EntEnterprise();
        ent.setId(ENT_ID);
        ent.setEnterpriseName("测试企业");
        ent.setCreditCode("91000000MA0XXXXX");
        ent.setContactPerson("张三");
        when(enterpriseMapper.selectById(ENT_ID)).thenReturn(ent);
        EntEnterpriseSetting setting = buildCompleteSetting();
        setting.setCertPassDate(null); // Missing cert date when energyCert=1
        when(enterpriseSettingMapper.selectByEnterpriseId(ENT_ID)).thenReturn(setting);
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isEnterpriseComplete()).isFalse();
        assertThat(result.getValidation().isExportReady()).isFalse();
        assertThat(result.getValidation().getExportErrors())
                .anyMatch(e -> e.contains("认证通过日期"));
    }

    @Test
    void getConfigExportBlockedWhenEnergyCert1MissingCertAuthority() {
        EntEnterprise ent = new EntEnterprise();
        ent.setId(ENT_ID);
        ent.setEnterpriseName("测试企业");
        ent.setCreditCode("91000000MA0XXXXX");
        ent.setContactPerson("张三");
        when(enterpriseMapper.selectById(ENT_ID)).thenReturn(ent);
        EntEnterpriseSetting setting = buildCompleteSetting();
        setting.setCertAuthority(null); // Missing cert authority when energyCert=1
        when(enterpriseSettingMapper.selectByEnterpriseId(ENT_ID)).thenReturn(setting);
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isEnterpriseComplete()).isFalse();
        assertThat(result.getValidation().isExportReady()).isFalse();
        assertThat(result.getValidation().getExportErrors())
                .anyMatch(e -> e.contains("认证机构"));
    }

    @Test
    void getConfigExportPassesWhenEnergyCert0NoCertFields() {
        EntEnterprise ent = new EntEnterprise();
        ent.setId(ENT_ID);
        ent.setEnterpriseName("测试企业");
        ent.setCreditCode("91000000MA0XXXXX");
        ent.setContactPerson("张三");
        when(enterpriseMapper.selectById(ENT_ID)).thenReturn(ent);
        EntEnterpriseSetting setting = buildCompleteSetting();
        setting.setEnergyCert(0); // No certification
        setting.setCertPassDate(null);
        setting.setCertAuthority(null);
        when(enterpriseSettingMapper.selectByEnterpriseId(ENT_ID)).thenReturn(setting);
        when(unitMapper.selectList(any())).thenReturn(List.of(unit(1L, "锅炉房", 1)));
        when(energyMapper.selectList(any())).thenReturn(List.of(energy(1L, "电力", new BigDecimal("0.1229"))));
        when(productMapper.selectList(any())).thenReturn(List.of(product(1L, "产品A", new BigDecimal("1000"))));
        when(flowMapper.selectByEnterpriseAndYear(ENT_ID, YEAR)).thenReturn(Collections.emptyList());

        EnergyFlowConfigDTO result = service.getConfig(ENT_ID, YEAR);

        assertThat(result.getValidation().isEnterpriseComplete()).isTrue();
        assertThat(result.getValidation().isExportReady()).isTrue();
    }
}
