package com.energy.audit.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImportCatalogRequest {
    private List<Long> catalogIds;
}
