package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import org.junit.jupiter.api.Test;

class IncompleteAutoRegCompletenessTest {

    @Test
    void salesIncompleteUntilBothDatesPresent() {
        final SstInfo sstInfo = new SstInfo();
        assertThat(IncompleteAutoRegCompleteness.isComplete(TaxType.SALES_TAX, sstInfo)).isFalse();
        sstInfo.setManComDate(LocalDate.of(2024, 1, 15));
        assertThat(IncompleteAutoRegCompleteness.isComplete(TaxType.SALES_TAX, sstInfo)).isFalse();
        sstInfo.setDateSaleValTaxGoods(LocalDate.of(2024, 6, 1));
        assertThat(IncompleteAutoRegCompleteness.isComplete(TaxType.SALES_TAX, sstInfo)).isTrue();
    }

    @Test
    void tourismCompleteWhenApplicantNamePresent() {
        final SstInfo sstInfo = new SstInfo();
        assertThat(IncompleteAutoRegCompleteness.isComplete(TaxType.TOURISM_TAX, sstInfo)).isFalse();
        sstInfo.setApplicantName("Ahmad");
        assertThat(IncompleteAutoRegCompleteness.isComplete(TaxType.TOURISM_TAX, sstInfo)).isTrue();
    }
}
