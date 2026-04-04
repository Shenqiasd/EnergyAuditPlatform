package com.energy.audit.service.audit;

import com.energy.audit.model.entity.audit.AwRectificationTrack;

import java.util.List;

public interface RectificationService {

    void createItems(Long taskId, List<AwRectificationTrack> items, Long operatorId, String username);

    List<AwRectificationTrack> listByTaskId(Long taskId);

    List<AwRectificationTrack> listByEnterpriseId(Long enterpriseId);

    int countOverdueByTaskId(Long taskId);

    void updateProgress(Long id, Integer status, String result, Long operatorId, String username);

    void acceptItem(Long id, Long operatorId, String username);

    AwRectificationTrack getById(Long id);

    void markOverdueItems();
}
