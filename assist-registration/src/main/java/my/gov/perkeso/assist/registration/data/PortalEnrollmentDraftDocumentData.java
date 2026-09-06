package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortalEnrollmentDraftDocumentData {

    private final Long id;
    private final Long documentTypeId;
    private final String documentTypeLabel;
    private final String fileName;
    private final String contentType;
    private final Long fileSize;
}
