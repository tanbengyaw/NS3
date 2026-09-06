package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class PostcodeBranchRoutingServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PostcodeBranchRoutingService routingService;

    @Test
    void resolvesBranchFromPostcodeOfficeLookup() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("50812"))).thenReturn(List.of(2L));

        assertThat(routingService.resolvePrimaryBranchIdForPostcode("50812")).contains(2L);
    }

    @Test
    void appliesProcessingBranchForPortalCaseOnSubmit() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("50812"))).thenReturn(List.of(2L));

        final RegGeneralInfo regCase = portalCase("50812", 1L);
        routingService.applyPortalSubmitBranchRouting(regCase);

        assertThat(regCase.getProcessingPksBranchId()).isEqualTo(2L);
        assertThat(regCase.getReceivingPksBranchId()).isEqualTo(2L);
    }

    @Test
    void leavesOtcCaseUnchanged() {
        final RegGeneralInfo regCase = portalCase("50812", 3L);
        regCase.setDataSourceId(DataSource.OTC.getAssistId());

        routingService.applyPortalSubmitBranchRouting(regCase);

        assertThat(regCase.getProcessingPksBranchId()).isEqualTo(3L);
    }

    @Test
    void keepsHqWhenPostcodeHasNoOfficeMapping() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("99999"))).thenReturn(List.of());

        final RegGeneralInfo regCase = portalCase("99999", 1L);
        routingService.applyPortalSubmitBranchRouting(regCase);

        assertThat(regCase.getProcessingPksBranchId()).isEqualTo(1L);
    }

    private static RegGeneralInfo portalCase(final String postCode, final long processingBranchId) {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo("BRN123");

        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setEmployerName("Portal Co");
        tempEmployer.setBusinessInfo(businessInfo);
        tempEmployer.setPostCode(postCode);

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setDataSourceId(DataSource.PORTAL.getAssistId());
        regCase.setProcessingPksBranchId(processingBranchId);
        regCase.setReceivingPksBranchId(processingBranchId);
        regCase.setTempEmployer(tempEmployer);
        return regCase;
    }
}
