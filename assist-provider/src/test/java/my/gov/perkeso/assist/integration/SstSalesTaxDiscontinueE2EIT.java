package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.constant.SstContractType;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstStatusInfo;
import my.gov.perkeso.assist.registration.domain.SstStatusInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end coverage for the "Discontinue Tax" workflow (section 1103): search a currently-active
 * taxpayer, start a discontinue case, set the new status + cessation-effective date, submit (must
 * go to the UO queue, never RO auto-approve), approve as UO, and confirm the target
 * {@code SstStatusInfo} row was flipped to CANCEL in place (old row closed, new row current) — no
 * new Employer/SstInfo/SMK created.
 */
@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SstSalesTaxDiscontinueE2EIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SstInfoRepository sstInfoRepository;

    @Autowired
    private SstStatusInfoRepository sstStatusInfoRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/assist-provider/api/v1";
    }

    @Test
    void discontinueTaxFlowFlipsStatusInPlaceAndRoutesToUo() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");

        // 1. Create + fully fill out + submit a brand-new Sales Tax registration case so it
        // auto-approves (RO auto-approves new-reg).
        final JsonNode created = ro.createCase("""
                {
                  "sectionId": 1100,
                  "employerName": "Discontinue Flow Trading Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long newRegCaseId = created.get("resourceId").asLong();

        ro.updateCase(newRegCaseId, """
                {
                  "businessEntityTypeId": 1,
                  "email": "discontinueflow@example.com",
                  "addressLine1": "Lot 2 Industrial Park"
                }
                """);

        ro.upsertSstInfo(newRegCaseId, """
                {
                  "tradeName": "Discontinue Flow Trading",
                  "anTotalTaxSalesVal": 800000.00,
                  "dateSaleValTaxGoods": "2024-06-01",
                  "manComDate": "2024-01-15",
                  "finYrEndMon": 12,
                  "businessComDate": "2024-01-01",
                  "localSales": 600000.00,
                  "exportSales": 100000.00,
                  "salesToDesignArea": 50000.00,
                  "othersSales": 50000.00,
                  "subContractWork": false,
                  "declareTrue": true,
                  "declareDate": "2024-06-15",
                  "applicantName": "Lim Ah Meng"
                }
                """);

        ro.createDirector(newRegCaseId, """
                {
                  "name": "Lim Ah Meng",
                  "identificationTypeId": 2,
                  "identificationNo": "800202025432",
                  "email": "director2@example.com",
                  "designation": "Managing Director"
                }
                """);

        ro.createTariffCode(newRegCaseId, """
                {
                  "tariffCodeSalesTypeId": 101,
                  "contractTypeId": %d,
                  "finishedGoods": "Assorted trading goods"
                }
                """.formatted(SstContractType.MAIN_CONTRACT.getAssistId()));

        final byte[] pdfSample = "%PDF-1.4 test".getBytes();
        ro.uploadSupportingDocument(newRegCaseId, 2L, pdfSample, "ssm-cert.pdf", "application/pdf");
        ro.uploadSupportingDocument(newRegCaseId, 1L, pdfSample, "applicant-id.pdf", "application/pdf");

        final JsonNode approved = ro.submitCase(newRegCaseId, "{}");
        assertThat(approved.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");

        final long employerId = approved.get("resourceId").asLong();
        final String salesTaxSmkRegNo = approved.get("changes").get("salesTaxSmkRegNo").asText();

        final List<SstInfo> sstInfoRows = sstInfoRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId);
        assertThat(sstInfoRows).hasSize(1);
        final long sstInfoId = sstInfoRows.get(0).getId();

        final SstStatusInfo statusBefore = sstStatusInfoRepository.findBySstInfoIdAndDeletedFalse(sstInfoId).stream()
                .filter(SstStatusInfo::isCurrent)
                .findFirst()
                .orElseThrow();
        assertThat(statusBefore.getSstStatusId()).isEqualTo(1L); // SstStatus.ACTIVE
        final long statusBeforeId = statusBefore.getId();

        // 2. Search the discontinue-tax-payers endpoint — must find this ACTIVE sales-tax
        // registration.
        final List<JsonNode> searchResults = ro.searchDiscontinueTaxPayers("SALES_TAX", brn);
        final JsonNode match = searchResults.stream()
                .filter(row -> row.get("employerId").asLong() == employerId)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected employer " + employerId + " in discontinue search results: " + searchResults));
        assertThat(match.get("sstInfoId").asLong()).isEqualTo(sstInfoId);
        assertThat(match.get("taxTypeId").asLong()).isEqualTo(TaxType.SALES_TAX.getAssistId());
        assertThat(match.get("smkRegNo").asText()).isEqualTo(salesTaxSmkRegNo);

        // 3. Start a discontinue case for that (employer, sstInfo) pair.
        final JsonNode startDiscontinue = ro.startDiscontinueTax(employerId, sstInfoId);
        final long discontinueCaseId = startDiscontinue.get("resourceId").asLong();
        assertThat(startDiscontinue.get("resourceIdentifier").asText()).isNotBlank();

        // 3b. Both the case-detail lookup (used by the discontinue-tax-case page's employer-info
        // banner) and the officer-inbox summary listing must resolve the SMK reg no by employer id
        // — the discontinue case's own SstInfo lookup by reg_general_info_id would find nothing,
        // since the promoted SstInfo still belongs to the ORIGINAL new-reg case.
        final JsonNode discontinueCaseDetail = ro.getCaseByRefNo(startDiscontinue.get("resourceIdentifier").asText());
        assertThat(discontinueCaseDetail.get("salesTaxSmkRegNo").asText()).isEqualTo(salesTaxSmkRegNo);

        final List<JsonNode> allSummaries = ro.listCases(null, null, "1103,1200,1201,1202,1203,1204");
        final JsonNode summaryRow = allSummaries.stream()
                .filter(row -> row.get("id").asLong() == discontinueCaseId)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected discontinue case " + discontinueCaseId + " in inbox summaries: " + allSummaries));
        assertThat(summaryRow.get("salesTaxSmkRegNo").asText()).isEqualTo(salesTaxSmkRegNo);

        // 4. The draft must reflect the current (Active) status and no requested new status yet.
        final JsonNode draftBeforeEdit = ro.getDiscontinueInfo(discontinueCaseId);
        assertThat(draftBeforeEdit.get("sstInfoId").asLong()).isEqualTo(sstInfoId);
        assertThat(draftBeforeEdit.get("currentSstStatusLabel").asText()).isEqualTo("Active");
        assertThat(draftBeforeEdit.get("newSstStatusId").isNull()).isTrue();

        // 5. Set the new status to CANCEL with a cessation-effective date.
        final JsonNode draftAfterEdit = ro.upsertDiscontinueInfo(discontinueCaseId, """
                {
                  "newSstStatusId": 2,
                  "cessationTaxEffectiveFrom": "2026-12-01"
                }
                """);
        assertThat(draftAfterEdit.get("newSstStatusId").asLong()).isEqualTo(2L);
        assertThat(draftAfterEdit.get("cessationTaxEffectiveFrom").asText()).isEqualTo("2026-12-01");

        // 5b. Supporting documents must be uploadable/listable on the discontinue case itself —
        // TempSstSupportingDocumentWritePlatformService had its own separate section guard that
        // was never widened past new-reg-only sections when Update Tax Payer / Discontinue Tax
        // were added, so this previously failed with "SST info API is only available for SST
        // new registration (1100, 1101, 1102, 1104)".
        ro.uploadSupportingDocument(discontinueCaseId, 2L, pdfSample, "discontinue-support.pdf", "application/pdf");
        final JsonNode discontinueSstInfo = ro.getSstInfo(discontinueCaseId);
        assertThat(discontinueSstInfo.get("supportingDocuments")).hasSize(1);
        assertThat(discontinueSstInfo.get("supportingDocuments").get(0).get("fileName").asText())
                .isEqualTo("discontinue-support.pdf");

        // 6. Submitting as RO must route to the UO queue (SUBMITTED), never auto-approve.
        final JsonNode submitted = ro.submitCase(discontinueCaseId, "{}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("SUBMITTED");

        // 6b. A non-UO staff user (RO here) must be forbidden from approving a UO-workflow-section
        // case, even though it's SUBMITTED and ready — mirrors legacy ASSIST's UO-only BPM routing.
        final ResponseEntity<String> forbidden = restTemplate.exchange(
                baseUrl + "/registration-cases/" + discontinueCaseId + "?command=approve",
                HttpMethod.POST,
                new HttpEntity<>("{}", roAuthHeaders()),
                String.class);
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);

        // 7. Approve as a UO user.
        final RegistrationApiClient uo = client("uo_jb", "password");
        final JsonNode approvedDiscontinue = uo.approveCase(discontinueCaseId);
        assertThat(approvedDiscontinue.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");
        assertThat(approvedDiscontinue.get("changes").get("newStatus").asText()).isEqualTo("Cancelled");

        // 8. The OLD status row must be closed (not current, end date set); a NEW status row must
        // be current with the requested status + start date. No duplicate Employer/SstInfo.
        final List<SstStatusInfo> statusesAfter = sstStatusInfoRepository
                .findBySstInfoIdAndDeletedFalse(sstInfoId);
        final SstStatusInfo oldStatus = statusesAfter.stream()
                .filter(status -> status.getId() == statusBeforeId)
                .findFirst()
                .orElseThrow();
        assertThat(oldStatus.isCurrent()).isFalse();
        assertThat(oldStatus.getEndDate()).isNotNull();

        final SstStatusInfo newStatus = statusesAfter.stream()
                .filter(SstStatusInfo::isCurrent)
                .findFirst()
                .orElseThrow();
        assertThat(newStatus.getSstStatusId()).isEqualTo(2L); // SstStatus.CANCEL
        assertThat(newStatus.getStartDate().toString()).isEqualTo("2026-12-01");
        assertThat(newStatus.getTaxTypeId()).isEqualTo(TaxType.SALES_TAX.getAssistId());

        assertThat(sstInfoRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId)).hasSize(1);

        // 9. The now-cancelled registration must drop out of the discontinue search results.
        final List<JsonNode> searchAfter = ro.searchDiscontinueTaxPayers("SALES_TAX", brn);
        assertThat(searchAfter.stream().anyMatch(row -> row.get("employerId").asLong() == employerId)).isFalse();
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static HttpHeaders roAuthHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("ro", "password");
        return headers;
    }

    private static String uniqueBrn() {
        return "2019" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
