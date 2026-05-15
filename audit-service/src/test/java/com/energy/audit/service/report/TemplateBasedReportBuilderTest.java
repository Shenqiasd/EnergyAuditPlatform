package com.energy.audit.service.report;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateBasedReportBuilderTest {

    @Test
    @SuppressWarnings("unchecked")
    void annotationSheetMapUses0515Names() throws Exception {
        Field field = TemplateBasedReportBuilder.class.getDeclaredField("ANNOTATION_SHEET_MAP");
        field.setAccessible(true);
        Map<Integer, String> mapping = (Map<Integer, String>) field.get(null);

        assertThat(mapping).containsEntry(9, "6.重点设备能耗和效率");
        assertThat(mapping).containsEntry(17, "14.节能量计算");
        assertThat(mapping).doesNotContainKey(14);
        assertThat(mapping).containsEntry(20, "16.能碳管理改进建议");
        assertThat(mapping.values()).noneMatch(value ->
                value.contains("能效对标")
                        || value.contains("能源计量")
                        || value.contains("能源管理制度")
                        || value.contains("能源管理改进")
                        || value.contains("节能潜力")
                        || value.contains("节能技术改造"));
    }
}
