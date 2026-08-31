package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.registration.constant.RegistrationSpecialCaseType;
import my.gov.perkeso.assist.registration.data.RegistrationSpecialCase;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import org.junit.jupiter.api.Test;

class RegistrationCaseSubmitRouterTest {

    private final RegistrationCaseSubmitRouter router = new RegistrationCaseSubmitRouter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void employerSubmitGoesToSubmitted() throws Exception {
        final PlatformUser employer = new PlatformUser("employer", "hr@acme.example",
                EnumSet.of(PlatformUserRole.EMPLOYER), null);

        assertThat(router.resolveSubmitStatus(employer, jsonCommand(Map.of()), List.of()))
                .isEqualTo(AppStatus.SUBMITTED);
    }

    @Test
    void employerSubmitWithSpecialCaseGoesToInProgress() throws Exception {
        final PlatformUser employer = new PlatformUser("employer", "hr@acme.example",
                EnumSet.of(PlatformUserRole.EMPLOYER), null);

        assertThat(router.resolveSubmitStatus(employer, jsonCommand(Map.of()), duplicateBrnCases()))
                .isEqualTo(AppStatus.IN_PROGRESS);
    }

    @Test
    void roSubmitAutoApproves() throws Exception {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.resolveSubmitStatus(ro, jsonCommand(Map.of()), List.of())).isEqualTo(AppStatus.APPROVED);
    }

    @Test
    void roSubmitIgnoresIncompleteFlag() throws Exception {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.resolveSubmitStatus(ro, jsonCommand(Map.of("incomplete", true)), List.of()))
                .isEqualTo(AppStatus.APPROVED);
    }

    @Test
    void roDuplicateBrnShouldReject() {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.shouldRejectRoDuplicateBrn(ro, duplicateBrnCases())).isTrue();
    }

    @Test
    void officerIncompleteSubmitGoesToInProgress() throws Exception {
        final PlatformUser officer = new PlatformUser("admin", "officer@perkeso.example",
                EnumSet.of(PlatformUserRole.OFFICER), 2L);

        assertThat(router.resolveSubmitStatus(officer, jsonCommand(Map.of("incomplete", true)), List.of()))
                .isEqualTo(AppStatus.IN_PROGRESS);
    }

    @Test
    void officerCompleteSubmitGoesToSubmitted() throws Exception {
        final PlatformUser officer = new PlatformUser("admin", "officer@perkeso.example",
                EnumSet.of(PlatformUserRole.OFFICER), 2L);

        assertThat(router.resolveSubmitStatus(officer, jsonCommand(Map.of("incomplete", false)), List.of()))
                .isEqualTo(AppStatus.SUBMITTED);
    }

    @Test
    void officerSubmitWithSpecialCaseGoesToInProgress() throws Exception {
        final PlatformUser officer = new PlatformUser("admin", "officer@perkeso.example",
                EnumSet.of(PlatformUserRole.OFFICER), 2L);

        assertThat(router.resolveSubmitStatus(officer, jsonCommand(Map.of()), duplicateBrnCases()))
                .isEqualTo(AppStatus.IN_PROGRESS);
    }

    private static List<RegistrationSpecialCase> duplicateBrnCases() {
        return List.of(RegistrationSpecialCase.builder()
                .type(RegistrationSpecialCaseType.DUPLICATE_REGISTRATION_NUMBER)
                .message("Business registration number already registered: 201901234567")
                .build());
    }

    private JsonCommand jsonCommand(final Map<String, Object> fields) throws Exception {
        final JsonNode parsed = objectMapper.valueToTree(fields);
        return new JsonCommand(parsed.toString(), parsed, 1L, fields);
    }
}
