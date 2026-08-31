package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.Map;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.PksBranchConstants;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import org.junit.jupiter.api.Test;

class RegistrationCaseContextServiceTest {

    private final RegistrationCaseContextService service = new RegistrationCaseContextService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void employerPortalUserGetsPortalDataSourceAndHqBranches() throws Exception {
        final JsonCommand command = jsonCommand(Map.of(
                "employerName", "Acme",
                "registrationNo", "201901234567",
                "serviceTypeId", 1,
                "pksBranchId", 2,
                "postCode", "50812"));

        final PlatformUser employer = new PlatformUser("employer", "hr@acme.example",
                EnumSet.of(PlatformUserRole.EMPLOYER), null);
        final RegistrationCaseContext context = service.resolveForCreate(command, employer);

        assertThat(context.getSection()).isEqualTo(RegistrationSection.REG_NEW_REG);
        assertThat(context.getDataSource()).isEqualTo(DataSource.PORTAL);
        assertThat(context.getPksBranchId()).isEqualTo(PksBranchConstants.HQ);
        assertThat(context.getProcessingPksBranchId()).isEqualTo(PksBranchConstants.HQ);
        assertThat(context.getReceivingPksBranchId()).isEqualTo(PksBranchConstants.HQ);
    }

    @Test
    void officerUserGetsRequestedDataSourceAndOfficeBranch() throws Exception {
        final JsonCommand command = jsonCommand(Map.of(
                "employerName", "Acme",
                "registrationNo", "201901234567",
                "serviceTypeId", 1,
                "pksBranchId", 2,
                "postCode", "50812",
                "dataSourceId", 1,
                "sectionId", 200));

        final PlatformUser officer = new PlatformUser("admin", "officer@perkeso.example",
                EnumSet.of(PlatformUserRole.OFFICER), 2L);
        final RegistrationCaseContext context = service.resolveForCreate(command, officer);

        assertThat(context.getSection()).isEqualTo(RegistrationSection.REG_NEW_REG);
        assertThat(context.getDataSource()).isEqualTo(DataSource.OTC);
        assertThat(context.getPksBranchId()).isEqualTo(2L);
        assertThat(context.getProcessingPksBranchId()).isEqualTo(2L);
        assertThat(context.getReceivingPksBranchId()).isEqualTo(2L);
    }

    private JsonCommand jsonCommand(final Map<String, Object> fields) throws Exception {
        final JsonNode parsed = objectMapper.valueToTree(fields);
        return new JsonCommand(parsed.toString(), parsed, null, fields);
    }
}
