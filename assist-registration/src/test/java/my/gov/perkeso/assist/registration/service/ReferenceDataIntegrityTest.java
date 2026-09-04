package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ReferenceDataIntegrityTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ReferenceDataIntegrityService service;

    @BeforeEach
    void setUp() {
        service = new ReferenceDataIntegrityService(jdbcTemplate);
    }

    @Test
    void countOfficePostcodesMissingAreaCode_returnsZeroWhenHealthy() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class))).thenReturn(0);

        assertThat(service.countOfficePostcodesMissingAreaCode()).isZero();
        assertThat(service.isReferenceDataHealthy()).isTrue();
    }

    @Test
    void countOfficePostcodesMissingAreaCode_reportsGaps() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class))).thenReturn(3);

        assertThat(service.countOfficePostcodesMissingAreaCode()).isEqualTo(3);
        assertThat(service.isReferenceDataHealthy()).isFalse();
    }
}
