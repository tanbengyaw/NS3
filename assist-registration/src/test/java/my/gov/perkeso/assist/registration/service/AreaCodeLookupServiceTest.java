package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class AreaCodeLookupServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AreaCodeLookupService service;

    @BeforeEach
    void setUp() {
        service = new AreaCodeLookupService(jdbcTemplate);
    }

    @Test
    void findAreaCode_returnsDatabaseValue() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(2L), eq("50812")))
                .thenReturn("A37");

        assertThat(service.findAreaCodeByPostCodeAndBranchId("50812", 2L)).isEqualTo("A37");
    }

    @Test
    void findAreaCode_trimsPostcode() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(2L), eq("50812")))
                .thenReturn("A37");

        assertThat(service.findAreaCodeByPostCodeAndBranchId(" 50812 ", 2L)).isEqualTo("A37");
    }

    @Test
    void findAreaCode_fallsBackToPostcodeOnly() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(2L), eq("50013")))
                .thenReturn(null);
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("50013")))
                .thenReturn("W10");

        assertThat(service.findAreaCodeByPostCodeAndBranchId("50013", 2L)).isEqualTo("W10");
    }

    @Test
    void requireAreaCode_throwsWhenNotFound() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(2L), eq("99999")))
                .thenReturn(null);
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("99999")))
                .thenReturn(null);

        assertThatThrownBy(() -> service.requireAreaCodeByPostCodeAndBranchId("99999", 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No area code");
    }

    @Test
    void findAreaCode_throwsWhenNotFound() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(2L), eq("99999")))
                .thenReturn(null);
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("99999")))
                .thenReturn(null);

        assertThat(service.findAreaCodeByPostCodeAndBranchId("99999", 2L)).isNull();
    }

    @Test
    void findCustomsAreaNameByPostcode_returnsDatabaseValue() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("50812")))
                .thenReturn("Shah Alam (OPA)");

        assertThat(service.findCustomsAreaNameByPostcode("50812")).isEqualTo("Shah Alam (OPA)");
    }
}
