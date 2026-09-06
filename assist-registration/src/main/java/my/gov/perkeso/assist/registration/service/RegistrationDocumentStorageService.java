package my.gov.perkeso.assist.registration.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import my.gov.perkeso.assist.registration.config.RegistrationDocumentStorageProperties;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegistrationDocumentStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final RegistrationDocumentStorageProperties properties;

    public RegistrationDocumentStorageService(final RegistrationDocumentStorageProperties properties) {
        this.properties = properties;
    }

    public StoredRegistrationDocument storeDraftDocument(final Long caseId, final String originalFileName,
            final String contentType, final long fileSize, final InputStream content) throws IOException {
        final String effectiveContentType = resolveContentType(contentType, originalFileName);
        validateUpload(originalFileName, effectiveContentType, fileSize);

        final Path caseDir = resolveBasePath().resolve("case-" + caseId);
        Files.createDirectories(caseDir);

        final String storedFileName = UUID.randomUUID() + "-" + sanitizeFileName(originalFileName);
        final Path target = caseDir.resolve(storedFileName);
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);

        return new StoredRegistrationDocument(storedFileName, normalizeContentType(effectiveContentType), fileSize);
    }

    public Path resolveDraftDocumentPath(final Long caseId, final String storedFileName) {
        return resolveBasePath().resolve("case-" + caseId).resolve(storedFileName);
    }

    public void deleteDraftDocument(final Long caseId, final String storedFileName) {
        try {
            Files.deleteIfExists(resolveDraftDocumentPath(caseId, storedFileName));
        } catch (IOException ex) {
            log.warn("Failed to delete draft document {} for case {}", storedFileName, caseId, ex);
        }
    }

    public Path resolveApprovedDocumentPath(final Long employerId, final String storedFileName) {
        return resolveBasePath().resolve("employer-" + employerId).resolve(storedFileName);
    }

    public StoredRegistrationDocument promoteDraftDocument(final Long caseId, final Long employerId,
            final String storedFileName, final String originalFileName, final String contentType, final long fileSize)
            throws IOException {
        final Path source = resolveDraftDocumentPath(caseId, storedFileName);
        if (!Files.exists(source)) {
            throw new IllegalStateException("Draft document file not found: " + storedFileName);
        }

        final Path employerDir = resolveBasePath().resolve("employer-" + employerId);
        Files.createDirectories(employerDir);
        final String promotedName = UUID.randomUUID() + "-" + sanitizeFileName(originalFileName);
        Files.copy(source, employerDir.resolve(promotedName), StandardCopyOption.REPLACE_EXISTING);
        return new StoredRegistrationDocument(promotedName, normalizeContentType(contentType), fileSize);
    }

    private Path resolveBasePath() {
        return Path.of(properties.getPath()).toAbsolutePath().normalize();
    }

    private static void validateUpload(final String originalFileName, final String contentType, final long fileSize) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File is empty");
        }
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum size of 10 MB");
        }

        final String extension = extensionOf(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only PDF, JPG, JPEG, and PNG files are allowed");
        }

        final String normalizedType = normalizeContentType(contentType);
        if (normalizedType != null && !ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported file content type: " + contentType);
        }
    }

    private static String sanitizeFileName(final String originalFileName) {
        return originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String extensionOf(final String fileName) {
        final int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private static String normalizeContentType(final String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        return contentType.split(";")[0].trim().toLowerCase();
    }

    private static String resolveContentType(final String contentType, final String fileName) {
        final String normalized = normalizeContentType(contentType);
        if (normalized != null && !"form-data".equals(normalized) && ALLOWED_CONTENT_TYPES.contains(normalized)) {
            return normalized;
        }
        return inferContentTypeFromFileName(fileName);
    }

    private static String inferContentTypeFromFileName(final String fileName) {
        return switch (extensionOf(fileName)) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> null;
        };
    }

    public record StoredRegistrationDocument(String storedFileName, String contentType, long fileSize) {
    }
}
