package com.energy.audit.service.report.impl;

import com.energy.audit.service.report.ReportFileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link ReportFileStore} that writes bytes to the local filesystem.
 * The DB BLOB redundancy ({@code ar_report.uploaded_file_data}) is intentionally
 * kept in the service layer rather than this class so this implementation stays
 * a pure file-bytes store and a future COS implementation can drop the BLOB
 * column without code shape changes here.
 *
 * <p>Path layout: {@code <upload-dir>/<uploaded-subdir>/<enterpriseId>/<auditYear>/<uuid>.docx}
 */
@Component
public class LocalFsAndBlobReportFileStore implements ReportFileStore {

    private static final Logger log = LoggerFactory.getLogger(LocalFsAndBlobReportFileStore.class);

    @Value("${app.report.upload-dir:upload/report}")
    private String uploadDir;

    @Value("${app.report.uploaded-report-subdir:uploaded}")
    private String uploadedSubdir;

    @Override
    public String save(long enterpriseId, int auditYear, String origFilename, byte[] bytes) {
        try {
            Path baseDir = baseDir().resolve(String.valueOf(enterpriseId)).resolve(String.valueOf(auditYear));
            Files.createDirectories(baseDir);

            String ext = ".docx";
            if (origFilename != null) {
                int dot = origFilename.lastIndexOf('.');
                if (dot > 0 && dot < origFilename.length() - 1) {
                    String candidate = origFilename.substring(dot).toLowerCase();
                    if (candidate.matches("\\.[a-z0-9]{1,8}")) {
                        ext = candidate;
                    }
                }
            }
            Path target = baseDir.resolve(UUID.randomUUID() + ext).normalize();
            if (!target.startsWith(baseDir)) {
                throw new IllegalStateException("computed path escapes base directory: " + target);
            }
            Files.write(target, bytes);
            log.info("[ReportFileStore] saved {} bytes to {}", bytes.length, target);
            return target.toString();
        } catch (IOException e) {
            // We don't fail the whole upload on filesystem write — the BLOB will still be persisted
            // by the service layer and {@link #load} will fall back to it. We do log loudly though.
            log.warn("[ReportFileStore] failed to write filesystem cache for enterprise={} year={}: {}",
                enterpriseId, auditYear, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Optional<byte[]> load(String storeKey) {
        if (storeKey == null || storeKey.isEmpty()) {
            return Optional.empty();
        }
        Path path = Paths.get(storeKey);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException e) {
            log.warn("[ReportFileStore] failed to read {}: {}", storeKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void delete(String storeKey) {
        if (storeKey == null || storeKey.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storeKey));
        } catch (IOException e) {
            log.warn("[ReportFileStore] failed to delete {}: {}", storeKey, e.getMessage());
        }
    }

    private Path baseDir() {
        return Paths.get(uploadDir, uploadedSubdir).toAbsolutePath().normalize();
    }
}
