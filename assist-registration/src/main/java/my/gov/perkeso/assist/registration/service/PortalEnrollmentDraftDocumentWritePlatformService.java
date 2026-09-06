package my.gov.perkeso.assist.registration.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.PortalEnrollmentDraftDocumentData;
import my.gov.perkeso.assist.registration.domain.base.BaseDocFile;
import my.gov.perkeso.assist.registration.domain.base.BaseDocFileRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseDocMapping;
import my.gov.perkeso.assist.registration.domain.base.BaseDocMappingRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseTableNames;
import my.gov.perkeso.assist.registration.domain.base.TempDocFile;
import my.gov.perkeso.assist.registration.domain.base.TempDocFileRepository;
import my.gov.perkeso.assist.registration.domain.base.TempDocMapping;
import my.gov.perkeso.assist.registration.domain.base.TempDocMappingRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortalEnrollmentDraftDocumentWritePlatformService {

    private static final long SELF_SERVICE_ACTOR_ID = 1L;

    private final TempDocFileRepository tempDocFileRepository;
    private final TempDocMappingRepository tempDocMappingRepository;
    private final BaseDocumentStorageService baseDocumentStorageService;
    private final BaseReferenceReadPlatformService baseReferenceReadPlatformService;

    @Transactional(readOnly = true)
    public List<PortalEnrollmentDraftDocumentData> listDraftDocuments(final String draftToken) {
        validateDraftToken(draftToken);
        final Map<Long, String> docTypeLabels = portalDocTypeLabels();
        return tempDocMappingRepository
                .findByTableNameAndDescriptionAndDeletedFalse(BaseTableNames.PORTAL_ENROLLMENT_DRAFT, draftToken)
                .stream()
                .map(mapping -> toData(tempDocFileRepository.findById(mapping.getTempDocFileId()).orElse(null),
                        docTypeLabels))
                .filter(data -> data != null)
                .toList();
    }

    @Transactional
    public PortalEnrollmentDraftDocumentData uploadDraftDocument(final String draftToken, final Long documentTypeId,
            final String originalFileName, final String contentType, final InputStream content) throws IOException {
        validateDraftToken(draftToken);
        baseReferenceReadPlatformService.requirePortalDocType(documentTypeId);

        final byte[] bytes = content.readAllBytes();
        final BaseDocumentStorageService.StoredBaseDocument stored = baseDocumentStorageService.storeDraftDocument(
                draftToken, originalFileName, contentType, bytes.length, new java.io.ByteArrayInputStream(bytes));

        final LocalDateTime now = LocalDateTime.now();
        final TempDocFile tempDocFile = new TempDocFile();
        tempDocFile.setRefDocTypeId(documentTypeId);
        tempDocFile.setUploaded(true);
        tempDocFile.setDocName(originalFileName);
        tempDocFile.setDocUri(stored.storedFileName());
        tempDocFile.setContentType(stored.contentType());
        tempDocFile.setFileSize(stored.fileSize());
        applyAudit(tempDocFile, SELF_SERVICE_ACTOR_ID, now);
        final TempDocFile savedFile = tempDocFileRepository.save(tempDocFile);

        final TempDocMapping mapping = new TempDocMapping();
        mapping.setTempDocFileId(savedFile.getId());
        mapping.setTableName(BaseTableNames.PORTAL_ENROLLMENT_DRAFT);
        mapping.setDescription(draftToken);
        applyAudit(mapping, SELF_SERVICE_ACTOR_ID, now);
        tempDocMappingRepository.save(mapping);

        return toData(savedFile, portalDocTypeLabels());
    }

    @Transactional(readOnly = true)
    public DownloadedBaseDocument downloadDraftDocument(final String draftToken, final Long documentId) {
        validateDraftToken(draftToken);
        final TempDocFile document = requireDraftDocument(draftToken, documentId);
        final Resource resource = new FileSystemResource(
                baseDocumentStorageService.resolveDraftDocumentPath(draftToken, document.getDocUri()));
        return new DownloadedBaseDocument(document.getDocName(), document.getContentType(), resource);
    }

    @Transactional
    public void deleteDraftDocument(final String draftToken, final Long documentId) {
        validateDraftToken(draftToken);
        final TempDocFile document = requireDraftDocument(draftToken, documentId);
        baseDocumentStorageService.deleteDraftDocument(draftToken, document.getDocUri());
        document.setDeleted(true);
        document.setUpdateById(SELF_SERVICE_ACTOR_ID);
        document.setUpdateDate(LocalDateTime.now());
        tempDocFileRepository.save(document);

        tempDocMappingRepository
                .findByTableNameAndDescriptionAndDeletedFalse(BaseTableNames.PORTAL_ENROLLMENT_DRAFT, draftToken)
                .stream()
                .filter(mapping -> documentId.equals(mapping.getTempDocFileId()))
                .forEach(mapping -> {
                    mapping.setDeleted(true);
                    mapping.setUpdateById(SELF_SERVICE_ACTOR_ID);
                    mapping.setUpdateDate(LocalDateTime.now());
                    tempDocMappingRepository.save(mapping);
                });
    }

    private TempDocFile requireDraftDocument(final String draftToken, final Long documentId) {
        final TempDocFile document = tempDocFileRepository.findById(documentId)
                .filter(file -> !file.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Draft document not found: " + documentId));

        final boolean linked = tempDocMappingRepository
                .findByTableNameAndDescriptionAndDeletedFalse(BaseTableNames.PORTAL_ENROLLMENT_DRAFT, draftToken)
                .stream()
                .anyMatch(mapping -> documentId.equals(mapping.getTempDocFileId()));
        if (!linked) {
            throw new IllegalArgumentException("Draft document not found: " + documentId);
        }
        return document;
    }

    private PortalEnrollmentDraftDocumentData toData(final TempDocFile document,
            final Map<Long, String> docTypeLabels) {
        if (document == null || document.isDeleted()) {
            return null;
        }
        return PortalEnrollmentDraftDocumentData.builder()
                .id(document.getId())
                .documentTypeId(document.getRefDocTypeId())
                .documentTypeLabel(docTypeLabels.get(document.getRefDocTypeId()))
                .fileName(document.getDocName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .build();
    }

    private Map<Long, String> portalDocTypeLabels() {
        return baseReferenceReadPlatformService.retrievePortalDocTypes().stream()
                .collect(Collectors.toMap(type -> type.getId(), type -> type.getLabel()));
    }

    private static void validateDraftToken(final String draftToken) {
        if (draftToken == null || draftToken.isBlank()) {
            throw new IllegalArgumentException("draftToken is required");
        }
    }

    private static void applyAudit(final my.gov.perkeso.assist.registration.domain.base.BaseAuditEntity entity,
            final Long actorId, final LocalDateTime now) {
        entity.setDeleted(false);
        entity.setCreateById(actorId);
        entity.setCreateDate(now);
    }

    public record DownloadedBaseDocument(String fileName, String contentType, Resource resource) {
    }
}
