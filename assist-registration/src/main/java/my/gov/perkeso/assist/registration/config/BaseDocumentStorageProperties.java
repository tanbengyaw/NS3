package my.gov.perkeso.assist.registration.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "assist.base.document-storage")
@Getter
@Setter
public class BaseDocumentStorageProperties {

    private String path = "./data/base-documents";
}
