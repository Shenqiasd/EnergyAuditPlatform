package com.energy.audit.web.job;

import com.energy.audit.service.audit.RectificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RectificationOverdueJob {

    private static final Logger log = LoggerFactory.getLogger(RectificationOverdueJob.class);

    private final RectificationService rectificationService;

    public RectificationOverdueJob(RectificationService rectificationService) {
        this.rectificationService = rectificationService;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void checkOverdue() {
        log.info("Starting daily rectification overdue check...");
        try {
            rectificationService.markOverdueItems();
            log.info("Rectification overdue check completed.");
        } catch (Exception e) {
            log.error("Rectification overdue check failed", e);
        }
    }
}
