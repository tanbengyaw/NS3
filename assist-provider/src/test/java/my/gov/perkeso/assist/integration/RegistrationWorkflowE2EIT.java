package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.domain.EmployeeRepository;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseDocMappingRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseTableNames;
import my.gov.perkeso.assist.registration.domain.base.UserEmployerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegistrationWorkflowE2EIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private UserEmployerRepository userEmployerRepository;

    @Autowired
    private BaseDocMappingRepository baseDocMappingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/assist-provider/api/v1";
    }

    @Test
    @Order(1)
    void portalEmployerSubmitThenOfficerApprove() {
        final String brn = uniqueBrn();
        final RegistrationApiClient employer = client("employer", "password");
        final RegistrationApiClient admin = client("admin", "password");

        final JsonNode created = employer.createCase("""
                {
                  "employerName": "Acme Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812"
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();
        final String caseRefNo = created.get("resourceIdentifier").asText();

        employer.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "email": "hr@acme.example",
                  "addressLine1": "No 1 Jalan Example"
                }
                """);

        employer.createEmployee(caseId, """
                {
                  "employeeName": "Ali Ahmad",
                  "identificationNo": "900101015432",
                  "employmentStartDate": "2024-01-01"
                }
                """);

        final JsonNode submitted = employer.submitCase(caseId, "{}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("SUBMITTED");

        final JsonNode approved = admin.approveCase(caseId);
        final String employerCode = approved.get("resourceIdentifier").asText();
        assertThat(employerCode).isNotBlank();
        assertThat(approved.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");

        final JsonNode caseView = admin.getCaseByRefNo(caseRefNo);
        assertThat(caseView.get("appStatus").asText()).isEqualTo("APPROVED");
        assertThat(caseView.get("dataSourceId").asLong()).isEqualTo(2L);
        assertThat(caseView.get("employerId").asLong()).isPositive();

        final JsonNode employerView = admin.getEmployerByCode(employerCode);
        assertThat(employerView.get("employerName").asText()).isEqualTo("Acme Sdn Bhd");
        assertThat(employerView.get("registrationNo").asText()).isEqualTo(brn);

        final var portalUser = portalUserRepository.findByEmailIgnoreCase("hr@acme.example");
        assertThat(portalUser).isPresent();
        assertThat(portalUser.get().getEmployerCode()).isEqualTo(employerCode);
        assertThat(portalUser.get().getEmployerId()).isEqualTo(caseView.get("employerId").asLong());
        assertThat(portalUser.get().getLinkedDate()).isNotNull();

        assertThat(employeeRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(caseView.get("employerId").asLong()))
                .hasSize(1)
                .first()
                .satisfies(employee -> {
                    assertThat(employee.getEmployeeName()).isEqualTo("Ali Ahmad");
                    assertThat(employee.getIdentificationNo()).isEqualTo("900101015432");
                });
    }

    @Test
    @Order(2)
    void roSubmitAutoApproves() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");

        final JsonNode created = ro.createCase("""
                {
                  "employerName": "Beta Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        ro.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 2 Jalan Example"
                }
                """);

        final JsonNode submitted = ro.submitCase(caseId, "{}");
        final String employerCode = submitted.get("resourceIdentifier").asText();
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");
        assertThat(employerCode).isNotBlank();

        final JsonNode employerView = ro.getEmployerByCode(employerCode);
        assertThat(employerView.get("employerName").asText()).isEqualTo("Beta Sdn Bhd");
        assertThat(employerView.get("registrationNo").asText()).isEqualTo(brn);
    }

    @Test
    @Order(3)
    void otcOfficerIncompleteThenApprove() {
        final String brn = uniqueBrn();
        final RegistrationApiClient admin = client("admin", "password");

        final JsonNode created = admin.createCase("""
                {
                  "employerName": "Gamma Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();
        final String caseRefNo = created.get("resourceIdentifier").asText();

        admin.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 3 Jalan Example"
                }
                """);

        final JsonNode incompleteSubmit = admin.submitCase(caseId, "{\"incomplete\": true}");
        assertThat(incompleteSubmit.get("changes").get("appStatus").asText()).isEqualTo("IN_PROGRESS");

        final JsonNode inProgressCase = admin.getCaseByRefNo(caseRefNo);
        assertThat(inProgressCase.get("appStatus").asText()).isEqualTo("IN_PROGRESS");

        admin.createEmployee(caseId, """
                {
                  "employeeName": "Siti Rahman",
                  "identificationNo": "880202015432",
                  "employmentStartDate": "2024-06-01"
                }
                """);

        final JsonNode finalSubmit = admin.submitCase(caseId, "{}");
        assertThat(finalSubmit.get("changes").get("appStatus").asText()).isEqualTo("SUBMITTED");

        final JsonNode approved = admin.approveCase(caseId);
        final String employerCode = approved.get("resourceIdentifier").asText();
        assertThat(employerCode).isNotBlank();

        final JsonNode caseView = admin.getCaseByRefNo(caseRefNo);
        assertThat(caseView.get("appStatus").asText()).isEqualTo("APPROVED");
        assertThat(caseView.get("dataSourceId").asLong()).isEqualTo(1L);

        assertThat(employeeRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(caseView.get("employerId").asLong()))
                .hasSize(1);
    }

    @Test
    @Order(4)
    void portalEmployerInQueryThenResubmitAndApprove() {
        final String brn = uniqueBrn();
        final RegistrationApiClient employer = client("employer", "password");
        final RegistrationApiClient admin = client("admin", "password");

        final JsonNode created = employer.createCase("""
                {
                  "employerName": "Delta Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812"
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();
        final String caseRefNo = created.get("resourceIdentifier").asText();

        employer.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "email": "hr@acme.example",
                  "addressLine1": "No 4 Jalan Example"
                }
                """);
        employer.submitCase(caseId, "{}");

        final JsonNode queried = admin.queryCase(caseId, """
                {"remark": "Please provide complete business address"}
                """);
        assertThat(queried.get("changes").get("appStatus").asText()).isEqualTo("IN_QUERY");
        assertThat(queried.get("changes").get("queryRemark").asText())
                .isEqualTo("Please provide complete business address");

        final JsonNode inQueryCase = employer.getCaseByRefNo(caseRefNo);
        assertThat(inQueryCase.get("appStatus").asText()).isEqualTo("IN_QUERY");
        assertThat(inQueryCase.get("queryRemark").asText()).isEqualTo("Please provide complete business address");

        employer.updateCase(caseId, """
                {
                  "addressLine2": "Level 5",
                  "phone": "03-12345678"
                }
                """);

        final JsonNode resubmitted = employer.submitCase(caseId, "{}");
        assertThat(resubmitted.get("changes").get("appStatus").asText()).isEqualTo("SUBMITTED");

        final JsonNode approved = admin.approveCase(caseId);
        assertThat(approved.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");
        assertThat(admin.getCaseByRefNo(caseRefNo).get("appStatus").asText()).isEqualTo("APPROVED");
    }

    @Test
    @Order(5)
    void officerSubmitWithDuplicateBrnGoesToInProgress() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");
        final RegistrationApiClient admin = client("admin", "password");

        final JsonNode approved = ro.createCase("""
                {
                  "employerName": "Existing Co Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long firstCaseId = approved.get("resourceId").asLong();
        ro.updateCase(firstCaseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 1 Jalan Existing"
                }
                """);
        ro.submitCase(firstCaseId, "{}");

        final JsonNode duplicateCase = admin.createCase("""
                {
                  "employerName": "Duplicate Co Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long duplicateCaseId = duplicateCase.get("resourceId").asLong();
        admin.updateCase(duplicateCaseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 2 Jalan Duplicate"
                }
                """);

        final JsonNode submitted = admin.submitCase(duplicateCaseId, "{}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(submitted.get("changes").get("specialCases").get(0).asText())
                .isEqualTo("DUPLICATE_REGISTRATION_NUMBER");
    }

    @Test
    @Order(6)
    void portalEnrollmentNewAndExistingEmployer() {
        final RegistrationApiClient admin = client("admin", "password");
        final String draftToken = UUID.randomUUID().toString();
        uploadRequiredPortalDocuments(admin, draftToken);

        final JsonNode newEnrollment = admin.enrollPortalUser("""
                {
                  "username": "portal_new",
                  "email": "portal_new@example.com",
                  "applicationType": "NEW_EMPLOYER",
                  "employerName": "Portal New Co Sdn Bhd",
                  "registrationTypeId": 1,
                  "registrationNo": "201901019999",
                  "addressLine1": "No 1 Jalan Portal",
                  "stateId": 14,
                  "cityId": 1401,
                  "cityName": "Kuala Lumpur",
                  "postCode": "50450",
                  "fullName": "Portal New User",
                  "identificationTypeId": 2,
                  "identificationNo": "900101011234",
                  "phoneCallingCode": "+60",
                  "phoneNumber": "123456789",
                  "securityPhrase": "My secret phrase",
                  "draftToken": "%s"
                }
                """.formatted(draftToken));
        assertThat(newEnrollment.get("applicationType").asText()).isEqualTo("NEW_EMPLOYER");
        assertThat(newEnrollment.get("employerId").isNull()).isTrue();

        final var portalUser = portalUserRepository.findByUsernameIgnoreCase("portal_new");
        assertThat(portalUser).isPresent();
        assertThat(portalUser.get().getUserEmployerId()).isNotNull();
        assertThat(userEmployerRepository.findByUserIdAndDeletedFalse(portalUser.get().getId())).isPresent();
        assertThat(baseDocMappingRepository.findByTableNameAndTablePkIdAndDeletedFalse(
                BaseTableNames.USER_EMPLOYER, portalUser.get().getUserEmployerId())).hasSize(2);

        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");
        final JsonNode created = ro.createCase("""
                {
                  "employerName": "Linked Co Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();
        ro.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 5 Jalan Linked"
                }
                """);
        final JsonNode approved = ro.submitCase(caseId, "{}");
        final String employerCode = approved.get("resourceIdentifier").asText();

        final String existingDraftToken = UUID.randomUUID().toString();
        uploadRequiredPortalDocuments(admin, existingDraftToken);

        final JsonNode existingEnrollment = admin.enrollPortalUser("""
                {
                  "username": "portal_existing",
                  "email": "portal_existing@example.com",
                  "applicationType": "EXISTING_EMPLOYER",
                  "employerCode": "%s",
                  "employerName": "Linked Co Sdn Bhd",
                  "registrationTypeId": 1,
                  "registrationNo": "%s",
                  "addressLine1": "No 5 Jalan Linked",
                  "stateId": 14,
                  "cityId": 1401,
                  "cityName": "Kuala Lumpur",
                  "postCode": "50812",
                  "fullName": "Portal Existing User",
                  "identificationTypeId": 2,
                  "identificationNo": "900101011235",
                  "phoneCallingCode": "+60",
                  "phoneNumber": "123456780",
                  "securityPhrase": "Linked secret phrase",
                  "draftToken": "%s"
                }
                """.formatted(employerCode, brn, existingDraftToken));
        assertThat(existingEnrollment.get("applicationType").asText()).isEqualTo("EXISTING_EMPLOYER");
        assertThat(existingEnrollment.get("employerCode").asText()).isEqualTo(employerCode);
        assertThat(existingEnrollment.get("employerId").asLong()).isPositive();

        final var linkedPortalUser = portalUserRepository.findByUsernameIgnoreCase("portal_existing");
        assertThat(linkedPortalUser).isPresent();
        assertThat(linkedPortalUser.get().getUserEmployerId()).isNotNull();
        assertThat(userEmployerRepository.findByUserIdAndDeletedFalse(linkedPortalUser.get().getId()))
                .isPresent()
                .get()
                .satisfies(userEmployer -> assertThat(userEmployer.getEmployerId()).isPositive());
    }

    @Test
    @Order(7)
    void portalEnrollmentInQueryThenResubmit() {
        final RegistrationApiClient admin = client("admin", "password");
        final String username = "portal_inquery_" + UUID.randomUUID().toString().substring(0, 8);
        final String draftToken = UUID.randomUUID().toString();
        uploadRequiredPortalDocuments(admin, draftToken);

        admin.enrollPortalUser("""
                {
                  "username": "%s",
                  "email": "%s@example.com",
                  "applicationType": "NEW_EMPLOYER",
                  "employerName": "In Query Co Sdn Bhd",
                  "registrationTypeId": 1,
                  "registrationNo": "201901018888",
                  "addressLine1": "No 1 Jalan Query",
                  "stateId": 14,
                  "cityId": 1401,
                  "cityName": "Kuala Lumpur",
                  "postCode": "50450",
                  "fullName": "In Query User",
                  "identificationTypeId": 2,
                  "identificationNo": "900101011236",
                  "phoneCallingCode": "+60",
                  "phoneNumber": "123456781",
                  "securityPhrase": "Query secret phrase",
                  "draftToken": "%s"
                }
                """.formatted(username, username, draftToken));

        final JsonNode queried = admin.queryPortalEnrollment(username, """
                {"remark": "Please update address line 2"}
                """);
        assertThat(queried.get("enrollmentStatus").asText()).isEqualTo("IN_QUERY");
        assertThat(queried.get("queryRemark").asText()).isEqualTo("Please update address line 2");

        final JsonNode resubmitted = admin.resubmitPortalEnrollment(username, """
                {
                  "email": "%s@example.com",
                  "applicationType": "NEW_EMPLOYER",
                  "employerName": "In Query Co Sdn Bhd",
                  "registrationTypeId": 1,
                  "registrationNo": "201901018888",
                  "addressLine1": "No 1 Jalan Query",
                  "addressLine2": "Suite 10",
                  "stateId": 14,
                  "cityId": 1401,
                  "cityName": "Kuala Lumpur",
                  "postCode": "50450",
                  "fullName": "In Query User",
                  "identificationTypeId": 2,
                  "identificationNo": "900101011236",
                  "phoneCallingCode": "+60",
                  "phoneNumber": "123456781",
                  "securityPhrase": "Query secret phrase"
                }
                """.formatted(username));
        assertThat(resubmitted.get("enrollmentStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(resubmitted.get("queryRemark").isNull()).isTrue();
        assertThat(resubmitted.get("addressLine2").asText()).isEqualTo("Suite 10");
    }

    private void uploadRequiredPortalDocuments(final RegistrationApiClient client, final String draftToken) {
        client.uploadPortalDraftDocument(draftToken, 1L, minimalPdf(), "applicant-id.pdf", "application/pdf");
        client.uploadPortalDraftDocument(draftToken, 2L, minimalPdf(), "ssm-cert.pdf", "application/pdf");
    }

    private static byte[] minimalPdf() {
        return ("%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        final String suffix = UUID.randomUUID().toString().replaceAll("\\D", "");
        return "2019" + suffix.substring(0, 8);
    }
}
