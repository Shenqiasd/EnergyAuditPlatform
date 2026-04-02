package com.energy.audit.model.dto;

import java.util.List;

/**
 * Request body for importing energy types from the global catalog.
 */
public class ImportCatalogRequest {

    private List<Long> catalogIds;

    public List<Long> getCatalogIds() {
        return catalogIds;
    }

    public void setCatalogIds(List<Long> catalogIds) {
        this.catalogIds = catalogIds;
    }
}
