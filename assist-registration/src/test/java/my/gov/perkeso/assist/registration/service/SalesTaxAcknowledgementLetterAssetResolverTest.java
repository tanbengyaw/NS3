package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SalesTaxAcknowledgementLetterAssetResolverTest {

    @Test
    void resolveImageUri_missingAsset_returnsTransparentPlaceholder() {
        final SalesTaxAcknowledgementLetterAssetResolver resolver =
                new SalesTaxAcknowledgementLetterAssetResolver("");

        assertThat(resolver.resolveImageUri("missing-file.png"))
                .isEqualTo(SalesTaxAcknowledgementLetterAssetResolver.TRANSPARENT_IMAGE_DATA_URI);
    }
}
