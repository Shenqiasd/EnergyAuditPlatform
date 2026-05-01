package com.energy.audit.service.report;

import java.util.Optional;

/**
 * Pluggable persistence for the bytes of an enterprise-uploaded report file.
 *
 * <p>The default implementation ({@code LocalFsAndBlobReportFileStore}) writes to
 * the local filesystem; the service layer additionally persists the same bytes
 * into {@code ar_report.uploaded_file_data} as a defensive copy that survives
 * Railway container restarts. Future implementations may target Tencent Cloud COS
 * or S3, in which case the BLOB column can be retired.
 *
 * <p>The {@code storeKey} returned by {@link #save} is opaque to callers and is
 * what gets persisted into {@code ar_report.uploaded_file_path}. Implementations
 * decide their own key format (file path, COS object URI, etc.).
 */
public interface ReportFileStore {

    /**
     * Persist file bytes and return an opaque key the caller should store and
     * later pass back to {@link #load} or {@link #delete}.
     *
     * @param enterpriseId the owning enterprise ID, used by some impls for path partitioning
     * @param auditYear    the audit year, used for path partitioning
     * @param origFilename the original filename (used to derive a sane extension)
     * @param bytes        file content
     * @return opaque store key (e.g. local absolute path or {@code cos://...} URI)
     */
    String save(long enterpriseId, int auditYear, String origFilename, byte[] bytes);

    /**
     * Read previously-saved bytes for a key. Returns empty if the key is null or
     * the underlying object is missing/unreadable. Implementations should NOT
     * throw on miss — the service layer falls back to the DB BLOB on empty.
     */
    Optional<byte[]> load(String storeKey);

    /**
     * Best-effort delete of a previously-saved object. Failures are logged but
     * never thrown — the caller has typically already updated the DB and a
     * stale file is preferable to an aborted overwrite.
     */
    void delete(String storeKey);
}
