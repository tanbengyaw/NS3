package my.gov.perkeso.assist.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.audit.constant.AuditTaskStatus;
import my.gov.perkeso.assist.audit.domain.AuditCase;
import my.gov.perkeso.assist.audit.domain.AuditCaseRepository;
import my.gov.perkeso.assist.audit.domain.AuditCaseTaxPayer;
import my.gov.perkeso.assist.audit.domain.AuditCaseTaxPayerRepository;
import my.gov.perkeso.assist.audit.domain.AuditFocusAreaDetails;
import my.gov.perkeso.assist.audit.domain.AuditFocusAreaDetailsRepository;
import my.gov.perkeso.assist.audit.domain.AuditLimitationDisclosureDetails;
import my.gov.perkeso.assist.audit.domain.AuditLimitationDisclosureDetailsRepository;
import my.gov.perkeso.assist.audit.domain.AuditRiskIndicatorsDetails;
import my.gov.perkeso.assist.audit.domain.AuditRiskIndicatorsDetailsRepository;
import my.gov.perkeso.assist.audit.domain.TaxpayerTaxTypeInfo;
import my.gov.perkeso.assist.audit.domain.TaxpayerTaxTypeInfoRepository;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.service.AutoRegTaxPayerIngestService;
import my.gov.perkeso.assist.registration.service.IncompleteAutoRegCompleteness;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditCaseWritePlatformService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AuditCaseRepository auditCaseRepository;
    private final AuditCaseTaxPayerRepository taxPayerRepository;
    private final TaxpayerTaxTypeInfoRepository taxTypeInfoRepository;
    private final AuditRiskIndicatorsDetailsRepository riskRepository;
    private final AuditFocusAreaDetailsRepository focusRepository;
    private final AuditLimitationDisclosureDetailsRepository limitationRepository;
    private final AutoRegTaxPayerIngestService ingestService;
    private final AuditPostCreateWritePlatformService postCreateWriteService;
    private final PlatformUserContext platformUserContext;
    private final ObjectMapper objectMapper;

    @Transactional
    public CommandProcessingResult createDraft() {
        assertStaff();
        final LocalDateTime now = LocalDateTime.now();
        final AuditCase auditCase = new AuditCase();
        auditCase.setTaskStatusId(AuditTaskStatus.PENDING_CASE_CREATION);
        auditCase.setPreAuditSkip(true);
        auditCase.setRefCaseSourceId(3L);
        auditCase.setBranchId(platformUserContext.getCurrentUser().officeId());
        auditCase.setDeleted(false);
        auditCase.setCreateDate(now);
        auditCase.setUpdateDate(now);
        final AuditCase saved = auditCaseRepository.save(auditCase);
        saved.setCaseRefNo(formatCaseRef(saved.getId()));
        return result(saved, Map.of("taskStatusId", saved.getTaskStatusId()));
    }

    @Transactional
    public CommandProcessingResult save(final Long caseId, final JsonNode node) {
        assertStaff();
        final AuditCase auditCase = requireCase(caseId);
        assertDraft(auditCase);
        applyCaseFields(auditCase, node);
        auditCase.setUpdateDate(LocalDateTime.now());
        auditCaseRepository.save(auditCase);
        saveTaxpayer(auditCase, node.get("taxpayer"));
        saveTaxTypes(auditCase, node.get("taxTypes"));
        saveRisk(auditCase, node.get("risk"));
        saveFocus(auditCase, node.get("focus"));
        saveLimitation(auditCase, node.get("limitation"));
        return result(auditCase, Map.of("saved", true));
    }

    @Transactional
    public CommandProcessingResult submit(final Long caseId, final JsonNode node) {
        assertStaff();
        if (node != null && !node.isNull() && !node.isEmpty()) {
            save(caseId, node);
        }
        final AuditCase auditCase = requireCase(caseId);
        assertDraft(auditCase);
        final AuditCaseTaxPayer taxpayer = taxPayerRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Add a taxpayer before creating the audit case."));
        requireText(taxpayer.getTaxPayerName(), "taxpayer.taxPayerName");
        requireText(taxpayer.getBusinessRegNo(), "taxpayer.businessRegNo");
        requireText(taxpayer.getPostcode(), "taxpayer.postcode");
        if (taxpayer.getNs3BranchId() == null) {
            throw new IllegalArgumentException("taxpayer.ns3BranchId is required");
        }

        final List<TaxpayerTaxTypeInfo> selected = taxTypeInfoRepository
                .findByAuditCaseTaxPayerIdAndDeletedFalseOrderByIdAsc(taxpayer.getId()).stream()
                .filter(TaxpayerTaxTypeInfo::isSelectedAudit).toList();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Select at least one tax type for audit.");
        }

        final List<Map<String, Object>> ingested = new ArrayList<>();
        if (taxpayer.getEmployerId() == null) {
            Long employerId = null;
            for (final TaxpayerTaxTypeInfo taxTypeInfo : selected) {
                validateTaxTypeForIngest(taxTypeInfo);
                final CommandProcessingResult ingestResult = ingestService.ingest(ingestPayload(auditCase, taxpayer,
                        taxTypeInfo));
                employerId = ingestResult.getChanges() == null ? null
                        : toLong(ingestResult.getChanges().get("employerId"));
                final Long sstInfoId = ingestResult.getChanges() == null ? ingestResult.getResourceId()
                        : toLong(ingestResult.getChanges().get("sstInfoId"));
                taxTypeInfo.setSstInfoId(sstInfoId);
                taxTypeInfo.setUpdateDate(LocalDateTime.now());
                taxTypeInfoRepository.save(taxTypeInfo);
                ingested.add(Map.of("taxType", taxTypeName(taxTypeInfo.getRefTaxRegTypeId()), "sstInfoId",
                        sstInfoId == null ? 0L : sstInfoId));
            }
            taxpayer.setEmployerId(employerId);
            taxpayer.setUpdateDate(LocalDateTime.now());
            taxPayerRepository.save(taxpayer);
        }

        auditCase.setTaskStatusId(AuditTaskStatus.FIELDWORK);
        auditCase.setSubmitDate(LocalDate.now());
        if (auditCase.getRefProposedCaseTypeId() == null) {
            auditCase.setRefProposedCaseTypeId(AuditTaskStatus.FIELD_CASE_TYPE);
        }
        if (auditCase.getCaseRefNo() == null || auditCase.getCaseRefNo().isBlank()) {
            auditCase.setCaseRefNo(formatCaseRef(auditCase.getId()));
        }
        auditCase.setUpdateDate(LocalDateTime.now());
        auditCaseRepository.save(auditCase);
        postCreateWriteService.ensurePlanning(auditCase);

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("taskStatusId", auditCase.getTaskStatusId());
        changes.put("employerId", taxpayer.getEmployerId());
        changes.put("ingested", ingested);
        return result(auditCase, changes);
    }

    private void applyCaseFields(final AuditCase auditCase, final JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.hasNonNull("preAuditSkip")) {
            auditCase.setPreAuditSkip(node.get("preAuditSkip").asBoolean());
        }
        if (node.has("refCaseSourceId")) {
            auditCase.setRefCaseSourceId(longValue(node, "refCaseSourceId"));
        }
        if (node.has("refRiskLevelId")) {
            auditCase.setRefRiskLevelId(longValue(node, "refRiskLevelId"));
        }
        if (node.has("extimatedTaxExposureRm")) {
            auditCase.setExtimatedTaxExposureRm(decimalValue(node, "extimatedTaxExposureRm"));
        }
        if (node.has("periodFrom")) {
            auditCase.setPeriodFrom(dateValue(node, "periodFrom"));
        }
        if (node.has("periodTo")) {
            auditCase.setPeriodTo(dateValue(node, "periodTo"));
        }
        if (node.has("objective")) {
            auditCase.setObjective(text(node, "objective"));
        }
        if (node.has("exclusions")) {
            auditCase.setExclusions(text(node, "exclusions"));
        }
        if (node.has("justification")) {
            auditCase.setJustification(text(node, "justification"));
        }
        if (node.has("cusAudRefNo")) {
            auditCase.setCusAudRefNo(text(node, "cusAudRefNo"));
        }
        if (node.has("branchId") && node.get("branchId").isNumber()) {
            auditCase.setBranchId(node.get("branchId").asLong());
        }
    }

    private void saveTaxpayer(final AuditCase auditCase, final JsonNode node) {
        if (node == null || node.isNull() || node.isEmpty()) {
            return;
        }
        final AuditCaseTaxPayer taxpayer = taxPayerRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(auditCase.getId()).orElseGet(() -> {
                    final AuditCaseTaxPayer created = new AuditCaseTaxPayer();
                    created.setAuditCaseId(auditCase.getId());
                    created.setDeleted(false);
                    created.setCreateDate(LocalDateTime.now());
                    return created;
                });
        taxpayer.setTaxPayerName(text(node, "taxPayerName"));
        taxpayer.setBusinessRegNo(text(node, "businessRegNo"));
        taxpayer.setAddressLine1(text(node, "addressLine1"));
        taxpayer.setAddressLine2(text(node, "addressLine2"));
        taxpayer.setAddressLine3(text(node, "addressLine3"));
        taxpayer.setRefStateId(longValue(node, "refStateId"));
        taxpayer.setRefCityId(longValue(node, "refCityId"));
        taxpayer.setPostcode(text(node, "postcode"));
        taxpayer.setNs3BranchId(longValue(node, "ns3BranchId"));
        taxpayer.setUpdateDate(LocalDateTime.now());
        taxPayerRepository.save(taxpayer);
    }

    private void saveTaxTypes(final AuditCase auditCase, final JsonNode taxTypesNode) {
        if (taxTypesNode == null || !taxTypesNode.isArray()) {
            return;
        }
        final AuditCaseTaxPayer taxpayer = taxPayerRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(auditCase.getId())
                .orElseThrow(() -> new IllegalArgumentException("Add a taxpayer before saving tax types."));
        final List<TaxpayerTaxTypeInfo> existing = taxTypeInfoRepository
                .findByAuditCaseTaxPayerIdAndDeletedFalseOrderByIdAsc(taxpayer.getId());
        for (final TaxpayerTaxTypeInfo row : existing) {
            row.setDeleted(true);
            row.setUpdateDate(LocalDateTime.now());
            taxTypeInfoRepository.save(row);
        }
        for (final JsonNode item : taxTypesNode) {
            final TaxType taxType = parseTaxType(item);
            final TaxpayerTaxTypeInfo row = new TaxpayerTaxTypeInfo();
            row.setAuditCaseTaxPayerId(taxpayer.getId());
            row.setRefTaxRegTypeId(taxType.getAssistId());
            row.setSelectedAudit(item.path("selectedAudit").asBoolean(true));
            row.setBusinessComDate(dateValue(item, "businessComDate"));
            row.setManSerComDate(dateValue(item, "manSerComDate"));
            final Integer month = intValue(item, "finYrEndMon");
            row.setRefFinYrEndMonthId(month == null ? null : month.longValue());
            row.setAnnualTtlTaxSalSerVal(decimalValue(item, "annualTtlTaxSalSerVal"));
            row.setDeleted(false);
            row.setCreateDate(LocalDateTime.now());
            row.setUpdateDate(LocalDateTime.now());
            taxTypeInfoRepository.save(row);
        }
    }

    private void saveRisk(final AuditCase auditCase, final JsonNode node) {
        if (node == null || node.isNull() || node.isEmpty()) {
            return;
        }
        final AuditRiskIndicatorsDetails row = riskRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(auditCase.getId()).orElseGet(() -> {
                    final AuditRiskIndicatorsDetails created = new AuditRiskIndicatorsDetails();
                    created.setAuditCaseId(auditCase.getId());
                    created.setDeleted(false);
                    created.setCreateDate(LocalDateTime.now());
                    return created;
                });
        row.setCheckedRegAnomaly(node.path("checkedRegAnomaly").asBoolean(false));
        row.setCheckedFilingBehaviour(node.path("checkedFilingBehaviour").asBoolean(false));
        row.setCheckedPaymentBehaviour(node.path("checkedPaymentBehaviour").asBoolean(false));
        row.setCheckedReturnTax(node.path("checkedReturnTax").asBoolean(false));
        row.setCheckedFinancialAnomaly(node.path("checkedFinancialAnomaly").asBoolean(false));
        row.setCheckedThirdPartyMismatch(node.path("checkedThirdPartyMismatch").asBoolean(false));
        row.setCheckedHisComplianceIssue(node.path("checkedHisComplianceIssue").asBoolean(false));
        row.setCheckedIntelligenceBased(node.path("checkedIntelligenceBased").asBoolean(false));
        row.setRiskAssessmentDetails(text(node, "riskAssessmentDetails"));
        row.setRiskIndicatorsRemark(text(node, "riskIndicatorsRemark"));
        row.setUpdateDate(LocalDateTime.now());
        riskRepository.save(row);
    }

    private void saveFocus(final AuditCase auditCase, final JsonNode node) {
        if (node == null || node.isNull() || node.isEmpty()) {
            return;
        }
        final AuditFocusAreaDetails row = focusRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(auditCase.getId()).orElseGet(() -> {
                    final AuditFocusAreaDetails created = new AuditFocusAreaDetails();
                    created.setAuditCaseId(auditCase.getId());
                    created.setDeleted(false);
                    created.setCreateDate(LocalDateTime.now());
                    return created;
                });
        row.setCheckedOutputTax(node.path("checkedOutputTax").asBoolean(false));
        row.setCheckedInputTaxEligility(node.path("checkedInputTaxEligility").asBoolean(false));
        row.setCheckedRefund(node.path("checkedRefund").asBoolean(false));
        row.setCheckedClassification(node.path("checkedClassification").asBoolean(false));
        row.setCheckedExemption(node.path("checkedExemption").asBoolean(false));
        row.setCheckedImportReconciliation(node.path("checkedImportReconciliation").asBoolean(false));
        row.setCheckedRevenueReconciliation(node.path("checkedRevenueReconciliation").asBoolean(false));
        row.setCheckedOthers(node.path("checkedOthers").asBoolean(false));
        row.setOthersInput(text(node, "othersInput"));
        row.setUpdateDate(LocalDateTime.now());
        focusRepository.save(row);
    }

    private void saveLimitation(final AuditCase auditCase, final JsonNode node) {
        if (node == null || node.isNull() || node.isEmpty()) {
            return;
        }
        final AuditLimitationDisclosureDetails row = limitationRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(auditCase.getId()).orElseGet(() -> {
                    final AuditLimitationDisclosureDetails created = new AuditLimitationDisclosureDetails();
                    created.setAuditCaseId(auditCase.getId());
                    created.setDeleted(false);
                    created.setCreateDate(LocalDateTime.now());
                    return created;
                });
        row.setCheckedDataIncomplete(node.path("checkedDataIncomplete").asBoolean(false));
        row.setCheckedAccessLimitation(node.path("checkedAccessLimitation").asBoolean(false));
        row.setCheckedPreliminaryAssumptions(node.path("checkedPreliminaryAssumptions").asBoolean(false));
        row.setCheckedOthers(node.path("checkedOthers").asBoolean(false));
        row.setOthersInput(text(node, "othersInput"));
        row.setUpdateDate(LocalDateTime.now());
        limitationRepository.save(row);
    }

    private JsonNode ingestPayload(final AuditCase auditCase, final AuditCaseTaxPayer taxpayer,
            final TaxpayerTaxTypeInfo taxTypeInfo) {
        final ObjectNode payload = objectMapper.createObjectNode();
        payload.put("taxType", taxTypeName(taxTypeInfo.getRefTaxRegTypeId()));
        payload.put("employerName", taxpayer.getTaxPayerName());
        payload.put("registrationNo", taxpayer.getBusinessRegNo());
        payload.put("postCode", taxpayer.getPostcode());
        payload.put("pksBranchId", taxpayer.getNs3BranchId());
        if (notBlank(taxpayer.getAddressLine1())) {
            payload.put("addressLine1", taxpayer.getAddressLine1());
        }
        if (notBlank(taxpayer.getAddressLine2())) {
            payload.put("addressLine2", taxpayer.getAddressLine2());
        }
        if (notBlank(taxpayer.getAddressLine3())) {
            payload.put("addressLine3", taxpayer.getAddressLine3());
        }
        if (taxTypeInfo.getBusinessComDate() != null) {
            payload.put("businessComDate", taxTypeInfo.getBusinessComDate().format(ISO_DATE));
        }
        if (taxTypeInfo.getManSerComDate() != null) {
            payload.put("manComDate", taxTypeInfo.getManSerComDate().format(ISO_DATE));
        }
        if (taxTypeInfo.getRefFinYrEndMonthId() != null) {
            payload.put("finYrEndMon", taxTypeInfo.getRefFinYrEndMonthId().intValue());
        }
        if (taxTypeInfo.getAnnualTtlTaxSalSerVal() != null) {
            payload.put("anTotalTaxSalesVal", taxTypeInfo.getAnnualTtlTaxSalSerVal());
        }
        final String cusAudRefNo = notBlank(auditCase.getCusAudRefNo()) ? auditCase.getCusAudRefNo().trim()
                : auditCase.getCaseRefNo();
        if (notBlank(cusAudRefNo)) {
            payload.put("cusAudRefNo", cusAudRefNo);
        }
        payload.put("source", "audit");
        return payload;
    }

    private void validateTaxTypeForIngest(final TaxpayerTaxTypeInfo taxTypeInfo) {
        final TaxType taxType = taxTypeFromId(taxTypeInfo.getRefTaxRegTypeId());
        if (taxTypeInfo.getBusinessComDate() == null) {
            throw new IllegalArgumentException("businessComDate is required for " + taxType.name());
        }
        if (taxTypeInfo.getRefFinYrEndMonthId() == null) {
            throw new IllegalArgumentException("finYrEndMon is required for " + taxType.name());
        }
        if (taxType == TaxType.SALES_TAX || taxType == TaxType.SERVICE_TAX) {
            if (taxTypeInfo.getManSerComDate() == null) {
                throw new IllegalArgumentException("manSerComDate is required for " + taxType.name());
            }
            if (taxTypeInfo.getAnnualTtlTaxSalSerVal() == null) {
                throw new IllegalArgumentException("annualTtlTaxSalSerVal is required for " + taxType.name());
            }
        }
    }

    private AuditCase requireCase(final Long caseId) {
        return auditCaseRepository.findById(caseId)
                .filter(row -> !row.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Audit case not found: " + caseId));
    }

    private static void assertDraft(final AuditCase auditCase) {
        if (!Long.valueOf(AuditTaskStatus.PENDING_CASE_CREATION).equals(auditCase.getTaskStatusId())) {
            throw new IllegalArgumentException("Audit case " + auditCase.getCaseRefNo() + " is already created.");
        }
    }

    private void assertStaff() {
        if (platformUserContext.getCurrentUser().isEmployer()) {
            throw new IllegalArgumentException("Audit cases are staff-only.");
        }
    }

    private static CommandProcessingResult result(final AuditCase auditCase, final Map<String, Object> changes) {
        return CommandProcessingResult.withChanges(auditCase.getId(), auditCase.getCaseRefNo(), changes);
    }

    private static String formatCaseRef(final Long id) {
        return "AUD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"))
                + String.format("%06d", id == null ? 0 : id);
    }

    private static TaxType parseTaxType(final JsonNode node) {
        if (node.hasNonNull("taxType")) {
            return IncompleteAutoRegCompleteness.parseTaxType(node.get("taxType").asText());
        }
        return taxTypeFromId(longValue(node, "refTaxRegTypeId"));
    }

    private static TaxType taxTypeFromId(final Long assistId) {
        if (assistId == null) {
            throw new IllegalArgumentException("taxType is required");
        }
        for (final TaxType taxType : TaxType.values()) {
            if (taxType.getAssistId() == assistId) {
                return taxType;
            }
        }
        throw new IllegalArgumentException("Unsupported tax type id: " + assistId);
    }

    private static String taxTypeName(final Long assistId) {
        return taxTypeFromId(assistId).name();
    }

    private static Long toLong(final Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(value.toString());
    }

    private static String text(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        final String value = node.get(field).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void requireText(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static Long longValue(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asLong();
    }

    private static Integer intValue(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asInt();
    }

    private static LocalDate dateValue(final JsonNode node, final String field) {
        final String value = text(node, field);
        return value == null ? null : LocalDate.parse(value);
    }

    private static BigDecimal decimalValue(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).decimalValue();
    }

    private static boolean notBlank(final String value) {
        return value != null && !value.isBlank();
    }
}
