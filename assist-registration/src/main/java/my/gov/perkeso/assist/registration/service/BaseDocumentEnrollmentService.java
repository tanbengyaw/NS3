package my.gov.perkeso.assist.registration.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.domain.base.BaseDocFile;
import my.gov.perkeso.assist.registration.domain.base.BaseDocFileRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseDocMapping;
import my.gov.perkeso.assist.registration.domain.base.BaseDocMappingRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseTableNames;
import my.gov.perkeso.assist.registration.domain.base.TempDocFile;
import my.gov.perkeso.assist.registration.domain.base.TempDocFileRepository;
import my.gov.perkeso.assist.registration.domain.base.TempDocMapping;
import my.gov.perkeso.assist.registration.domain.base.TempDocMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BaseDocumentEnrollmentService {

    private static final long SELF_SERVICE_ACTOR_ID = 1L;

    private final TempDocMappingRepository tempDocMappingRepository;
    private final TempDocFileRepository tempDocFileRepository;
    private final BaseDocFileRepository baseDocFileRepository;
    private final BaseDocMappingRepository baseDocMappingRepository;
    private final BaseDocumentStorageService baseDocumentStorageService;
    private final BaseReferenceReadPlatformService baseReferenceReadPlatformService;

    @Transactional
    public void promoteDraftDocuments(final String draftToken, final Long userEmployerId, final Long employerId,
            final Long actorId) {
        if (draftToken == null || draftToken.isBlank()) {
            validateRequiredPortalDocuments(Set.of());
            return;
        }

        promoteDraftDocumentsInternal(draftToken, userEmployerId, employerId, actorId);
        validateRequiredPortalDocuments(collectUploadedTypeIds(userEmployerId, Set.of()));
    }

    @Transactional
    public void promoteDraftDocumentsForResubmit(final String draftToken, final Long userEmployerId,
            final Long employerId, final Long actorId) {
        if (draftToken != null && !draftToken.isBlank()) {
            promoteDraftDocumentsInternal(draftToken, userEmployerId, employerId, actorId);
        }
        validateRequiredPortalDocuments(collectUploadedTypeIds(userEmployerId, Set.of()));
    }

    private void promoteDraftDocumentsInternal(final String draftToken, final Long userEmployerId,
            final Long employerId, final Long actorId) {
        final Long effectiveActorId = actorId != null ? actorId : SELF_SERVICE_ACTOR_ID;
        final LocalDateTime now = LocalDateTime.now();
        final List<TempDocMapping> draftMappings = tempDocMappingRepository
                .findByTableNameAndDescriptionAndDeletedFalse(BaseTableNames.PORTAL_ENROLLMENT_DRAFT, draftToken);

        for (final TempDocMapping draftMapping : draftMappings) {
            final TempDocFile tempDocFile = tempDocFileRepository.findById(draftMapping.getTempDocFileId())
                    .filter(file -> !file.isDeleted())
                    .orElse(null);
            if (tempDocFile == null) {
                continue;
            }

            baseReferenceReadPlatformService.requirePortalDocType(tempDocFile.getRefDocTypeId());

            try {
                final BaseDocumentStorageService.StoredBaseDocument promoted = baseDocumentStorageService
                        .promoteDraftDocument(draftToken, userEmployerId, tempDocFile.getDocUri(),
                                tempDocFile.getDocName(), tempDocFile.getContentType(), tempDocFile.getFileSize());

                final BaseDocFile docFile = new BaseDocFile();
                docFile.setRefDocTypeId(tempDocFile.getRefDocTypeId());
                docFile.setUploaded(true);
                docFile.setDocName(tempDocFile.getDocName());
                docFile.setDocUri(promoted.storedFileName());
                docFile.setContentType(promoted.contentType());
                docFile.setFileSize(promoted.fileSize());
                applyAudit(docFile, effectiveActorId, now);
                final BaseDocFile savedDocFile = baseDocFileRepository.save(docFile);

                final BaseDocMapping docMapping = new BaseDocMapping();
                docMapping.setDocFileId(savedDocFile.getId());
                docMapping.setTableName(BaseTableNames.USER_EMPLOYER);
                docMapping.setTablePkId(userEmployerId);
                docMapping.setEmployerId(employerId);
                applyAudit(docMapping, effectiveActorId, now);
                baseDocMappingRepository.save(docMapping);

                markTempDeleted(tempDocFile, draftMapping, effectiveActorId, now);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to promote draft document " + tempDocFile.getDocName(), ex);
            }
        }
    }

    private Set<Long> collectUploadedTypeIds(final Long userEmployerId, final Set<Long> additionalTypeIds) {
        final Set<Long> uploadedTypeIds = new HashSet<>(additionalTypeIds);
        final List<BaseDocMapping> mappings = baseDocMappingRepository
                .findByTableNameAndTablePkIdAndDeletedFalse(BaseTableNames.USER_EMPLOYER, userEmployerId);
        for (final BaseDocMapping mapping : mappings) {
            baseDocFileRepository.findById(mapping.getDocFileId())
                    .filter(file -> !file.isDeleted())
                    .ifPresent(file -> uploadedTypeIds.add(file.getRefDocTypeId()));
        }
        return uploadedTypeIds;
    }

    private void validateRequiredPortalDocuments(final Set<Long> uploadedTypeIds) {
        final List<Long> missingRequired = baseReferenceReadPlatformService.retrievePortalDocTypes().stream()
                .filter(type -> type.isRequiredForPortalId())
                .map(type -> type.getId())
                .filter(requiredId -> !uploadedTypeIds.contains(requiredId))
                .toList();
        if (!missingRequired.isEmpty()) {
            throw new IllegalArgumentException("Required portal documents are missing");
        }
    }

    private void markTempDeleted(final TempDocFile tempDocFile, final TempDocMapping draftMapping,
            final Long actorId, final LocalDateTime now) {
        tempDocFile.setDeleted(true);
        tempDocFile.setUpdateById(actorId);
        tempDocFile.setUpdateDate(now);
        tempDocFileRepository.save(tempDocFile);

        draftMapping.setDeleted(true);
        draftMapping.setUpdateById(actorId);
        draftMapping.setUpdateDate(now);
        tempDocMappingRepository.save(draftMapping);
    }

    private static void applyAudit(final my.gov.perkeso.assist.registration.domain.base.BaseAuditEntity entity,
            final Long actorId, final LocalDateTime now) {
        entity.setDeleted(false);
        entity.setCreateById(actorId);
        entity.setCreateDate(now);
    }
}
