package com.energy.audit.model.dto;

import com.energy.audit.model.entity.extraction.DeEnergyFlow;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Request body for PUT /energy-flow/config — saves both flow records and diagram config.
 */
@Data
public class SaveEnergyFlowConfigDTO implements Serializable {

    private List<DeEnergyFlow> flowRecords;
    private DiagramConfigDTO diagram;
}
