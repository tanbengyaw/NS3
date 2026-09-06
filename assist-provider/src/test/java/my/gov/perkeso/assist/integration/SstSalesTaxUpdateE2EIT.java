package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.constant.SstContractType;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end coverage for the "Update Tax Payer" workflow (sales tax, section 1201): search an
 * existing active taxpayer, start an update case (clone live -> temp draft), edit + diff, submit
 * (must go to the UO queue, never RO auto-approve), approve as UO, and confirm the promotion
 * updates the existing Employer/SstInfo rows in place (no duplicates).
 */
@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SstSalesTaxUpdateE2EIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SstInfoRepository sstInfoRepository;

    @Autowired
    private EmployerRepository employerRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/assist-provider/api/v1";
    }

    @Test
    void updateTaxPayerFlowClonesEditsDiffsAndPromotesInPlace() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");

        // 1. Create + fully fill out + submit a brand-new Sales Tax registration case so it
        // auto-approves (RO auto-approves new-reg).
        final JsonNode created = ro.createCase("""
                {
                  "sectionId": 1100,
                  "employerName": "Update Flow Manufacturing Sdn Bhd",
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
                  "email": "updateflow@example.com",
                  "addressLine1": "Lot 1 Industrial Park"
                }
                """);

        ro.upsertSstInfo(newRegCaseId, """
                {
                  "tradeName": "Update Flow Manufacturing",
                  "anTotalTaxSalesVal": 1500000.00,
                  "dateSaleValTaxGoods": "2024-06-01",
                  "manComDate": "2024-01-15",
                  "finYrEndMon": 12,
                  "businessComDate": "2024-01-01",
                  "localSales": 1000000.00,
                  "exportSales": 300000.00,
                  "salesToDesignArea": 100000.00,
                  "othersSales": 100000.00,
                  "subContractWork": false,
                  "declareTrue": true,
                  "declareDate": "2024-06-15",
                  "applicantName": "Tan Ah Kow"
                }
                """);

        ro.createDirector(newRegCaseId, """
                {
                  "name": "Tan Ah Kow",
                  "identificationTypeId": 2,
                  "identificationNo": "800101015432",
                  "email": "director@example.com",
                  "designation": "Managing Director"
                }
                """);

        ro.createTariffCode(newRegCaseId, """
                {
                  "tariffCodeSalesTypeId": 101,
                  "contractTypeId": %d,
                  "finishedGoods": "Finished plastic goods"
                }
                """.formatted(SstContractType.MAIN_CONTRACT.getAssistId()));

        final byte[] pdfSample = "%PDF-1.4 test".getBytes();
        ro.uploadSupportingDocument(newRegCaseId, 2L, pdfSample, "ssm-cert.pdf", "application/pdf");
        ro.uploadSupportingDocument(newRegCaseId, 1L, pdfSample, "applicant-id.pdf", "application/pdf");

        final JsonNode approved = ro.submitCase(newRegCaseId, "{}");
        assertThat(approved.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");

        final long employerId = approved.get("resourceId").asLong();
        final String employerCodeBeforeUpdate = approved.get("resourceIdentifier").asText();
        final String salesTaxSmkRegNo = approved.get("changes").get("salesTaxSmkRegNo").asText();

        final List<SstInfo> sstInfoRowsBeforeUpdate = sstInfoRepository
                .findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId);
        assertThat(sstInfoRowsBeforeUpdate).hasSize(1);
        final long originalSstInfoId = sstInfoRowsBeforeUpdate.get(0).getId();
        assertThat(sstInfoRowsBeforeUpdate.get(0).getSalesTaxSmkRegNo()).isEqualTo(salesTaxSmkRegNo);

        // 2. Search the new "update tax payer" endpoint and confirm the employer shows up with
        // the sales-tax update section id.
        final List<JsonNode> searchResults = ro.searchTaxPayerUpdates("SALES_TAX", brn);
        assertThat(searchResults).isNotEmpty();
        final JsonNode match = searchResults.stream()
                .filter(row -> row.get("employerId").asLong() == employerId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected employer " + employerId
                        + " to appear in tax-payer-update search results: " + searchResults));
        assertThat(match.get("sectionId").asLong()).isEqualTo(1201L);
        assertThat(match.get("smkRegNo").asText()).isEqualTo(salesTaxSmkRegNo);

        // 3. Start an update case for that employer.
        final JsonNode startUpdate = ro.startTaxPayerUpdate(employerId, 1201L);
        final long updateCaseId = startUpdate.get("resourceId").asLong();
        assertThat(startUpdate.get("resourceIdentifier").asText()).isNotBlank();

        // 4. The update case's sst-info must be pre-populated (cloned) from the live record.
        final JsonNode clonedSstInfo = ro.getSstInfo(updateCaseId);
        assertThat(clonedSstInfo.get("tradeName").asText()).isEqualTo("Update Flow Manufacturing");
        assertThat(clonedSstInfo.get("anTotalTaxSalesVal").decimalValue())
                .isEqualByComparingTo("1500000.00");
        assertThat(clonedSstInfo.get("directors")).hasSize(1);
        assertThat(clonedSstInfo.get("tariffCodes")).hasSize(1);

        // 5. Edit trade name + annual taxable sales value on the update draft, plus the fax
        // number (Employer/TempEmployer.contactFaxes — a Form1 field previously missing from
        // the diff entirely; see TaxPayerUpdateCaseService#getUpdateDiff). contactFaxes is
        // stored as a raw JSON array of {head, back} pairs (same shape the frontend's contact
        // line editor serializes), not a plain string.
        ro.upsertSstInfo(updateCaseId, """
                {
                  "tradeName": "Update Flow Manufacturing (Rebranded)",
                  "anTotalTaxSalesVal": 2750000.00
                }
                """);
        ro.updateCase(updateCaseId, """
                {
                  "contactFaxes": "[{\\"head\\":\\"+60\\",\\"back\\":\\"66666\\"}]",
                  "corrAddressLine1": "Level 5, Correspondence Tower"
                }
                """);

        final JsonNode editedSstInfo = ro.getSstInfo(updateCaseId);
        assertThat(editedSstInfo.get("tradeName").asText()).isEqualTo("Update Flow Manufacturing (Rebranded)");
        assertThat(editedSstInfo.get("anTotalTaxSalesVal").decimalValue())
                .isEqualByComparingTo("2750000.00");
        // Cloned children must still be present after editing an unrelated field.
        assertThat(editedSstInfo.get("directors")).hasSize(1);
        assertThat(editedSstInfo.get("directors").get(0).get("name").asText()).isEqualTo("Tan Ah Kow");
        assertThat(editedSstInfo.get("tariffCodes")).hasSize(1);

        // 6. The diff endpoint must report the changed fields with old/new values.
        final List<JsonNode> diff = ro.getTaxUpdateDiff(updateCaseId);
        final JsonNode tradeNameDiff = findDiff(diff, "Trade Name");
        assertThat(tradeNameDiff.get("oldValue").asText()).isEqualTo("Update Flow Manufacturing");
        assertThat(tradeNameDiff.get("newValue").asText()).isEqualTo("Update Flow Manufacturing (Rebranded)");

        final JsonNode salesValDiff = findDiff(diff, "Annual Taxable Sales Value");
        assertThat(salesValDiff.get("oldValue").asText()).isEqualTo("1500000.00");
        assertThat(salesValDiff.get("newValue").asText()).isEqualTo("2750000.00");

        // The raw "[{\"head\":\"+60\",\"back\":\"66666\"}]" JSON must be rendered as a readable
        // "+6066666" (head+back, no space, same style as the "Telephone" field), not dumped verbatim.
        final JsonNode faxDiff = findDiff(diff, "Fax");
        assertThat(faxDiff.get("oldValue").asText()).isEqualTo("");
        assertThat(faxDiff.get("newValue").asText()).isEqualTo("+6066666");

        // Correspondence address fields were previously missing from the diff entirely (the bug
        // this change fixes) — confirm a changed corrAddressLine1 now shows up.
        final JsonNode corrAddressDiff = findDiff(diff, "Correspondence Address Line 1");
        assertThat(corrAddressDiff.get("oldValue").asText()).isEqualTo("");
        assertThat(corrAddressDiff.get("newValue").asText()).isEqualTo("Level 5, Correspondence Tower");

        // 7. Submitting as RO must route to the UO queue (SUBMITTED), never auto-approve.
        final JsonNode submitted = ro.submitCase(updateCaseId, "{}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("SUBMITTED");

        // 8. Approve as a UO user.
        final RegistrationApiClient uo = client("uo_jb", "password");
        final JsonNode approvedUpdate = uo.approveCase(updateCaseId);
        assertThat(approvedUpdate.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");

        // 9. The ORIGINAL SstInfo row must be updated in place: same id, same SMK reg no, new
        // tradeName/anTotalTaxSalesVal, and no duplicate rows/employer created.
        final List<SstInfo> sstInfoRowsAfterUpdate = sstInfoRepository
                .findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId);
        assertThat(sstInfoRowsAfterUpdate).hasSize(1);
        final SstInfo promoted = sstInfoRowsAfterUpdate.get(0);
        assertThat(promoted.getId()).isEqualTo(originalSstInfoId);
        assertThat(promoted.getSalesTaxSmkRegNo()).isEqualTo(salesTaxSmkRegNo);
        assertThat(promoted.getSmkRegNo()).isEqualTo(salesTaxSmkRegNo);
        assertThat(promoted.getTradeName()).isEqualTo("Update Flow Manufacturing (Rebranded)");
        assertThat(promoted.getAnTotalTaxSalesVal()).isEqualByComparingTo("2750000.00");

        final Employer employerAfterUpdate = employerRepository.findById(employerId).orElseThrow();
        assertThat(employerAfterUpdate.getEmployerCode()).isEqualTo(employerCodeBeforeUpdate);
    }

    private static JsonNode findDiff(final List<JsonNode> diff, final String fieldLabel) {
        return diff.stream()
                .filter(row -> fieldLabel.equals(row.get("fieldLabel").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected diff to contain field '" + fieldLabel + "' but was: " + diff));
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        return "2019" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
