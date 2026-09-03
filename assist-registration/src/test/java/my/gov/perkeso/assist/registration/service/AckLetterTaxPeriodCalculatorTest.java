package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AckLetterTaxPeriodCalculatorTest {

    @Test
    void calculate_oddFinancialYearEndMonth_usesOneMonthPeriod() {
        final AckLetterTaxPeriodCalculator.TaxPeriodSchedule schedule =
                AckLetterTaxPeriodCalculator.calculate(LocalDate.of(2024, 1, 1), 9);

        assertThat(schedule.basicAcc()).isEqualTo("Asas Bayaran");
        assertThat(schedule.taxPeriod()).isEqualTo("Satu Bulan");
        assertThat(schedule.firstTaxPeriod()).isEqualTo("01/09/2024 sehingga 30/09/2024");
        assertThat(schedule.lastPaymentDate()).isEqualTo("31/10/2024");
        assertThat(schedule.nextTaxPeriod()).isEqualTo("Setiap Dua Bulan");
        assertThat(schedule.lastPaymentDate3()).isEqualTo("Hari terakhir bulan berikutnya");
    }
}
