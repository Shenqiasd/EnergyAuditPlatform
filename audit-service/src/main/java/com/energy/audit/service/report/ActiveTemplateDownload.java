package com.energy.audit.service.report;

import com.energy.audit.model.entity.report.ArReportTemplate;

/**
 * Atomic snapshot of the currently-active report template + its file bytes,
 * resolved by a single {@code selectActive()} call. Used by the download
 * endpoint to guarantee the {@code Content-Disposition} filename and the body
 * bytes both come from the same template row even if an admin switches the
 * active template between requests.
 */
public final class ActiveTemplateDownload {

    private final ArReportTemplate template;
    private final byte[] bytes;

    public ActiveTemplateDownload(ArReportTemplate template, byte[] bytes) {
        this.template = template;
        this.bytes = bytes;
    }

    public ArReportTemplate getTemplate() {
        return template;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
