package my.gov.perkeso.assist.registration.employercode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmployerCheckDigitCalculatorTest {

    @Test
    void calculateCheckDigitMatchesAssistSample() {
        assertThat(EmployerCheckDigitCalculator.calculateCheckDigit("A37", "00000001")).isEqualTo("F");
    }
}
