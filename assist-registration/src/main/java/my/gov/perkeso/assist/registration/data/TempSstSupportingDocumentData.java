package my.gov.perkeso.assist.registration.data;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TempSstSupportingDocumentData {

    Long id;
    Long documentTypeId;
    String documentTypeLabel;
    String fileName;
    String contentType;
    Long fileSize;
    LocalDateTime uploadedDate;
}
