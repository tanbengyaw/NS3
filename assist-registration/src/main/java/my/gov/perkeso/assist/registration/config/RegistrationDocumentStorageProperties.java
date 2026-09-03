package my.gov.perkeso.assist.registration.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "assist.registration.document-storage")
@Getter
@Setter
public class RegistrationDocumentStorageProperties {

    /** Base directory for uploaded registration documents. */
    private String path = "./data/registration-documents";
}
