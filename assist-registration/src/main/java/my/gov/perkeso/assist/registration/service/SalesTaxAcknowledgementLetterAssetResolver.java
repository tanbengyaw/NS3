package my.gov.perkeso.assist.registration.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SalesTaxAcknowledgementLetterAssetResolver {

    /** 1x1 transparent PNG used when legacy logo files are not on the classpath. */
    static final String TRANSPARENT_IMAGE_DATA_URI =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAD0lEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    private static final List<String> DEFAULT_EXTERNAL_DIRS = List.of(
            "C:/development/pdf_template/images",
            "C:/development/pdf_template/version2/images");

    private final String configuredAssetsPath;

    public SalesTaxAcknowledgementLetterAssetResolver(
            @Value("${registration.letter.assets-path:}") final String configuredAssetsPath) {
        this.configuredAssetsPath = configuredAssetsPath;
    }

    public String resolveImageUri(final String fileName) {
        final String classpathUri = resolveClasspathUri(fileName);
        if (classpathUri != null) {
            return classpathUri;
        }
        final String externalUri = resolveExternalUri(fileName);
        if (externalUri != null) {
            return externalUri;
        }
        log.warn("Letter asset not found for {} — using transparent placeholder", fileName);
        return TRANSPARENT_IMAGE_DATA_URI;
    }

    private String resolveClasspathUri(final String fileName) {
        final ClassPathResource resource = new ClassPathResource("letter-assets/" + fileName);
        if (!resource.exists()) {
            return null;
        }
        try {
            return resource.getURL().toExternalForm();
        } catch (IOException ex) {
            log.warn("Failed to resolve classpath letter asset {}", fileName, ex);
            return null;
        }
    }

    private String resolveExternalUri(final String fileName) {
        if (configuredAssetsPath != null && !configuredAssetsPath.isBlank()) {
            final String uri = toFileUri(Path.of(configuredAssetsPath.trim(), fileName));
            if (uri != null) {
                return uri;
            }
        }
        for (final String directory : DEFAULT_EXTERNAL_DIRS) {
            final String uri = toFileUri(Path.of(directory, fileName));
            if (uri != null) {
                log.info("Using legacy letter asset from {}", directory);
                return uri;
            }
        }
        return null;
    }

    private static String toFileUri(final Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        return path.toUri().toString();
    }
}
