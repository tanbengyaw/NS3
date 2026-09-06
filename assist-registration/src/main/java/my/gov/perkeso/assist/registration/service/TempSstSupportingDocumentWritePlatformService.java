package my.gov.perkeso.assist.registration.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.data.SupportingDocumentTypeData;
import my.gov.perkeso.assist.registration.data.TempSstSupportingDocumentData;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstSupportingDocument;
import my.gov.perkeso.assist.registration.domain.TempSstSupportingDocumentRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TempSstSupportingDocumentWritePlatformService {

    private final TempSstInfoWritePlatformService tempSstInfoWritePlatformService;
    private final TempSstSupportingDocumentRepository tempSstSupportingDocumentRepository;
    private final RegistrationReferenceReadPlatformService registrationReferenceReadPlatformService;
    private final RegistrationDocumentStorageService registrationDocumentStorageService;

    @Transactional(readOnly = true)
    public List<TempSstSupportingDocumentData> listSupportingDocuments(final Long caseId) {
        final RegGeneralInfo regCase = tempSstInfoWritePlatformService.loadCaseForDocuments(caseId);
        assertSstNewRegSection(regCase);
        try {
            final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
            return listSupportingDocuments(tempSstInfo);
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    List<TempSstSupportingDocumentData> listSupportingDocuments(final TempSstInfo tempSstInfo) {
        if (tempSstInfo.getId() == null) {
            return List.of();
        }
        final Map<Long, SupportingDocumentTypeData> typeMap = registrationReferenceReadPlatformService
                .retrieveSupportingDocumentTypeMap();
        return tempSstSupportingDocumentRepository.findByTempSstInfoIdAndDeletedFalseOrderByIdAsc(tempSstInfo.getId())
                .stream()
                .map(doc -> toData(doc, typeMap.get(doc.getDocumentTypeId())))
                .toList();
    }

    @Transactional
    public TempSstSupportingDocumentData uploadSupportingDocument(final Long caseId, final Long documentTypeId,
            final String originalFileName, final String contentType, final InputStream content) throws IOException {
        final RegGeneralInfo regCase = tempSstInfoWritePlatformService.loadEditableCaseForDocuments(caseId);
        assertSstNewRegSection(regCase);
        final SupportingDocumentTypeData documentType = registrationReferenceReadPlatformService
                .requireSupportingDocumentType(documentTypeId);

        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireOrCreateTempSstInfo(regCase);
        final byte[] bytes = content.readAllBytes();
        final RegistrationDocumentStorageService.StoredRegistrationDocument stored = registrationDocumentStorageService
                .storeDraftDocument(caseId, originalFileName, contentType, bytes.length,
                        new java.io.ByteArrayInputStream(bytes));

        final TempSstSupportingDocument document = new TempSstSupportingDocument();
        document.setTempSstInfoId(tempSstInfo.getId());
        document.setDocumentTypeId(documentType.getId());
        document.setFileName(originalFileName);
        document.setStoredFileName(stored.storedFileName());
        document.setContentType(stored.contentType());
        document.setFileSize(stored.fileSize());
        document.setDeleted(false);
        document.setCreatedDate(LocalDateTime.now());

        return toData(tempSstSupportingDocumentRepository.save(document), documentType);
    }

    @Transactional
    public void deleteSupportingDocument(final Long caseId, final Long documentId) {
        final RegGeneralInfo regCase = tempSstInfoWritePlatformService.loadEditableCaseForDocuments(caseId);
        assertSstNewRegSection(regCase);
        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
        final TempSstSupportingDocument document = tempSstSupportingDocumentRepository
                .findByIdAndTempSstInfoIdAndDeletedFalse(documentId, tempSstInfo.getId())
                .orElseThrow(() -> new my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException(
                        "Supporting document not found: " + documentId));

        document.setDeleted(true);
        tempSstSupportingDocumentRepository.save(document);
        registrationDocumentStorageService.deleteDraftDocument(caseId, document.getStoredFileName());
    }

    @Transactional(readOnly = true)
    public DownloadedRegistrationDocument downloadSupportingDocument(final Long caseId, final Long documentId) {
        final RegGeneralInfo regCase = tempSstInfoWritePlatformService.loadCaseForDocuments(caseId);
        assertSstNewRegSection(regCase);
        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
        final TempSstSupportingDocument document = tempSstSupportingDocumentRepository
                .findByIdAndTempSstInfoIdAndDeletedFalse(documentId, tempSstInfo.getId())
                .orElseThrow(() -> new my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException(
                        "Supporting document not found: " + documentId));

        final var path = registrationDocumentStorageService.resolveDraftDocumentPath(caseId,
                document.getStoredFileName());
        if (!Files.exists(path)) {
            throw new IllegalStateException("Document file is missing on disk");
        }
        return new DownloadedRegistrationDocument(new FileSystemResource(path), document.getFileName(),
                document.getContentType());
    }

    private static TempSstSupportingDocumentData toData(final TempSstSupportingDocument document,
            final SupportingDocumentTypeData documentType) {
        return TempSstSupportingDocumentData.builder()
                .id(document.getId())
                .documentTypeId(document.getDocumentTypeId())
                .documentTypeLabel(documentType != null ? documentType.getLabel() : null)
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .uploadedDate(document.getCreatedDate())
                .build();
    }

    private static void assertSstNewRegSection(final RegGeneralInfo regCase) {
        if (!RegistrationSectionRouting.isSstCaseSection(regCase.getSectionId())) {
            throw new IllegalArgumentException(
                    "SST info API is only available for SST new registration (1100-1105), "
                            + "Update Tax Payer (1200-1204), or Discontinue Tax (1103)");
        }
    }

    public record DownloadedRegistrationDocument(Resource resource, String fileName, String contentType) {
    }
}
