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
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.RegistrationSpecialCaseType;
import my.gov.perkeso.assist.registration.data.RegistrationSpecialCase;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import org.junit.jupiter.api.Test;

class RegistrationCaseSubmitRouterTest {

    private static final Long NEW_REG = RegistrationSection.REG_NEW_REG.getAssistSectionId();
    private static final Long UPDATE_SALES_TAX = RegistrationSection.REG_UPDATE_TAX_PAYER_SALES_TAX.getAssistSectionId();
    private static final Long DISCONTINUE_TAX = RegistrationSection.REG_SST_DISCONTINUE_TAX.getAssistSectionId();
    private static final Long INCOMPLETE_SALES_TAX =
            RegistrationSection.REG_INCOMPLETE_TAX_PAYER_SALES_TAX.getAssistSectionId();

    private final RegistrationCaseSubmitRouter router = new RegistrationCaseSubmitRouter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void employerSubmitGoesToSubmitted() throws Exception {
        final PlatformUser employer = new PlatformUser("employer", "hr@acme.example",
                EnumSet.of(PlatformUserRole.EMPLOYER), null);

        assertThat(router.resolveSubmitStatus(employer, jsonCommand(Map.of()), List.of(), NEW_REG))
                .isEqualTo(AppStatus.SUBMITTED);
    }

    @Test
    void employerSubmitWithSpecialCaseGoesToInProgress() throws Exception {
        final PlatformUser employer = new PlatformUser("employer", "hr@acme.example",
                EnumSet.of(PlatformUserRole.EMPLOYER), null);

        assertThat(router.resolveSubmitStatus(employer, jsonCommand(Map.of()), duplicateBrnCases(), NEW_REG))
                .isEqualTo(AppStatus.IN_PROGRESS);
    }

    @Test
    void roSubmitAutoApprovesNewReg() throws Exception {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.resolveSubmitStatus(ro, jsonCommand(Map.of()), List.of(), NEW_REG))
                .isEqualTo(AppStatus.APPROVED);
    }

    @Test
    void roSubmitUpdateTaxGoesToSubmitted() throws Exception {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.resolveSubmitStatus(ro, jsonCommand(Map.of()), List.of(), UPDATE_SALES_TAX))
                .isEqualTo(AppStatus.SUBMITTED);
    }

    @Test
    void roSubmitDiscontinueTaxGoesToSubmitted() throws Exception {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.resolveSubmitStatus(ro, jsonCommand(Map.of()), List.of(), DISCONTINUE_TAX))
                .isEqualTo(AppStatus.SUBMITTED);
    }

    @Test
    void roSubmitIgnoresIncompleteFlagOnNewReg() throws Exception {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.resolveSubmitStatus(ro, jsonCommand(Map.of("incomplete", true)), List.of(), NEW_REG))
                .isEqualTo(AppStatus.APPROVED);
    }

    @Test
    void roDuplicateBrnShouldRejectOnNewReg() {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.shouldRejectRoDuplicateBrn(ro, duplicateBrnCases(), NEW_REG)).isTrue();
    }

    @Test
    void roDuplicateBrnIgnoredOnUpdateTax() {
        final PlatformUser ro = new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);

        assertThat(router.shouldRejectRoDuplicateBrn(ro, duplicateBrnCases(), UPDATE_SALES_TAX)).isFalse();
    }

    @Test
    void officerIncompleteSubmitGoesToInProgress() throws Exception {
        final PlatformUser officer = new PlatformUser("admin", "officer@perkeso.example",
                EnumSet.of(PlatformUserRole.OFFICER), 2L);

        assertThat(router.resolveSubmitStatus(officer, jsonCommand(Map.of("incomplete", true)), List.of(), NEW_REG))
                .isEqualTo(AppStatus.IN_PROGRESS);
    }

    @Test
    void officerIncompleteTaxSectionAutoApproves() throws Exception {
        final PlatformUser officer = new PlatformUser("officer_pj", "officer@perkeso.example",
                EnumSet.of(PlatformUserRole.OFFICER), 3L);

        assertThat(router.resolveSubmitStatus(officer, jsonCommand(Map.of()), List.of(), INCOMPLETE_SALES_TAX))
                .isEqualTo(AppStatus.APPROVED);
    }

    @Test
    void officerCompleteSubmitGoesToSubmitted() throws Exception {
        final PlatformUser officer = new PlatformUser("admin", "officer@perkeso.example",
                EnumSet.of(PlatformUserRole.OFFICER), 2L);

        assertThat(router.resolveSubmitStatus(officer, jsonCommand(Map.of("incomplete", false)), List.of(), NEW_REG))
                .isEqualTo(AppStatus.SUBMITTED);
    }

    @Test
    void officerSubmitWithSpecialCaseGoesToInProgress() throws Exception {
        final PlatformUser officer = new PlatformUser("admin", "officer@perkeso.example",
                EnumSet.of(PlatformUserRole.OFFICER), 2L);

        assertThat(router.resolveSubmitStatus(officer, jsonCommand(Map.of()), duplicateBrnCases(), NEW_REG))
                .isEqualTo(AppStatus.IN_PROGRESS);
    }

    @Test
    void uoIncompleteSubmitGoesToInProgress() throws Exception {
        final PlatformUser uo = new PlatformUser("uo_jb", "uo.jb@perkeso.example", EnumSet.of(PlatformUserRole.UO), 4L);

        assertThat(router.resolveSubmitStatus(uo, jsonCommand(Map.of("incomplete", true)), List.of(), NEW_REG))
                .isEqualTo(AppStatus.IN_PROGRESS);
    }

    @Test
    void uoUpdateTaxSubmitGoesToSubmitted() throws Exception {
        final PlatformUser uo = new PlatformUser("uo_jb", "uo.jb@perkeso.example", EnumSet.of(PlatformUserRole.UO), 4L);

        assertThat(router.resolveSubmitStatus(uo, jsonCommand(Map.of()), List.of(), UPDATE_SALES_TAX))
                .isEqualTo(AppStatus.SUBMITTED);
    }

    @Test
    void uoDiscontinueTaxSubmitGoesToSubmitted() throws Exception {
        final PlatformUser uo = new PlatformUser("uo_jb", "uo.jb@perkeso.example", EnumSet.of(PlatformUserRole.UO), 4L);

        assertThat(router.resolveSubmitStatus(uo, jsonCommand(Map.of()), List.of(), DISCONTINUE_TAX))
                .isEqualTo(AppStatus.SUBMITTED);
    }

    @Test
    void pkrBoIncompleteSubmitAutoApproves() throws Exception {
        final PlatformUser pkrBo = new PlatformUser("pkrbo_kl", "pkrbo.kl@perkeso.example",
                EnumSet.of(PlatformUserRole.PKR_BO), 1L);

        assertThat(router.resolveSubmitStatus(pkrBo, jsonCommand(Map.of("incomplete", true)), List.of(), NEW_REG))
                .isEqualTo(AppStatus.APPROVED);
    }

    @Test
    void pkrBoIncompleteTaxSectionAutoApproves() throws Exception {
        final PlatformUser pkrBo = new PlatformUser("pkrbo_kl", "pkrbo.kl@perkeso.example",
                EnumSet.of(PlatformUserRole.PKR_BO), 1L);

        assertThat(router.resolveSubmitStatus(pkrBo, jsonCommand(Map.of()), List.of(), INCOMPLETE_SALES_TAX))
                .isEqualTo(AppStatus.APPROVED);
    }

    @Test
    void pkrBoCompleteSubmitGoesToSubmitted() throws Exception {
        final PlatformUser pkrBo = new PlatformUser("pkrbo_kl", "pkrbo.kl@perkeso.example",
                EnumSet.of(PlatformUserRole.PKR_BO), 1L);

        assertThat(router.resolveSubmitStatus(pkrBo, jsonCommand(Map.of("incomplete", false)), List.of(), NEW_REG))
                .isEqualTo(AppStatus.SUBMITTED);
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
