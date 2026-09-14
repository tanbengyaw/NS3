package my.gov.perkeso.assist.audit.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.audit.constant.AuditTaskStatus;
import my.gov.perkeso.assist.audit.data.AuditCaseDetailData;
import my.gov.perkeso.assist.audit.data.AuditCaseListingData;
import my.gov.perkeso.assist.audit.data.AuditFocusData;
import my.gov.perkeso.assist.audit.data.AuditLimitationData;
import my.gov.perkeso.assist.audit.data.AuditRiskData;
import my.gov.perkeso.assist.audit.data.AuditTaxTypeData;
import my.gov.perkeso.assist.audit.data.AuditTaxpayerData;
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
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.registration.constant.TaxType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditCaseReadPlatformService {

    private final AuditCaseRepository auditCaseRepository;
    private final AuditCaseTaxPayerRepository taxPayerRepository;
    private final TaxpayerTaxTypeInfoRepository taxTypeInfoRepository;
    private final AuditRiskIndicatorsDetailsRepository riskRepository;
    private final AuditFocusAreaDetailsRepository focusRepository;
    private final AuditLimitationDisclosureDetailsRepository limitationRepository;
    private final PlatformUserContext platformUserContext;

    @Transactional(readOnly = true)
    public List<AuditCaseListingData> search(final String search) {
        assertStaff();
        return auditCaseRepository.search(search == null ? "" : search.trim()).stream().map(this::toListing).toList();
    }

    @Transactional(readOnly = true)
    public AuditCaseDetailData get(final Long caseId) {
        assertStaff();
        final AuditCase auditCase = auditCaseRepository.findById(caseId)
                .filter(row -> !row.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Audit case not found: " + caseId));
        final AuditCaseTaxPayer taxpayer = taxPayerRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(caseId).orElse(null);
        final List<AuditTaxTypeData> taxTypes = taxpayer == null ? List.of()
                : taxTypeInfoRepository.findByAuditCaseTaxPayerIdAndDeletedFalseOrderByIdAsc(taxpayer.getId()).stream()
                        .map(this::toTaxType).toList();
        return AuditCaseDetailData.builder()
                .id(auditCase.getId())
                .caseRefNo(auditCase.getCaseRefNo())
                .taskStatusId(auditCase.getTaskStatusId())
                .taskStatusLabel(AuditTaskStatus.label(auditCase.getTaskStatusId()))
                .submitted(!Long.valueOf(AuditTaskStatus.PENDING_CASE_CREATION).equals(auditCase.getTaskStatusId()))
                .preAuditSkip(auditCase.isPreAuditSkip())
                .refCaseSourceId(auditCase.getRefCaseSourceId())
                .refRiskLevelId(auditCase.getRefRiskLevelId())
                .extimatedTaxExposureRm(auditCase.getExtimatedTaxExposureRm())
                .periodFrom(auditCase.getPeriodFrom())
                .periodTo(auditCase.getPeriodTo())
                .objective(auditCase.getObjective())
                .exclusions(auditCase.getExclusions())
                .justification(auditCase.getJustification())
                .cusAudRefNo(auditCase.getCusAudRefNo())
                .taxpayer(toTaxpayer(taxpayer))
                .taxTypes(taxTypes)
                .risk(toRisk(riskRepository.findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(caseId).orElse(null)))
                .focus(toFocus(focusRepository.findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(caseId).orElse(null)))
                .limitation(toLimitation(
                        limitationRepository.findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(caseId).orElse(null)))
                .build();
    }

    private AuditCaseListingData toListing(final AuditCase auditCase) {
        final AuditCaseTaxPayer taxpayer = taxPayerRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(auditCase.getId()).orElse(null);
        return AuditCaseListingData.builder()
                .id(auditCase.getId())
                .caseRefNo(auditCase.getCaseRefNo())
                .taskStatusId(auditCase.getTaskStatusId())
                .taskStatusLabel(AuditTaskStatus.label(auditCase.getTaskStatusId()))
                .submitted(!Long.valueOf(AuditTaskStatus.PENDING_CASE_CREATION).equals(auditCase.getTaskStatusId()))
                .taxPayerName(taxpayer == null ? null : taxpayer.getTaxPayerName())
                .businessRegNo(taxpayer == null ? null : taxpayer.getBusinessRegNo())
                .employerId(taxpayer == null ? null : taxpayer.getEmployerId())
                .build();
    }

    private AuditTaxpayerData toTaxpayer(final AuditCaseTaxPayer taxpayer) {
        if (taxpayer == null) {
            return null;
        }
        return AuditTaxpayerData.builder()
                .id(taxpayer.getId())
                .employerId(taxpayer.getEmployerId())
                .taxPayerName(taxpayer.getTaxPayerName())
                .businessRegNo(taxpayer.getBusinessRegNo())
                .addressLine1(taxpayer.getAddressLine1())
                .addressLine2(taxpayer.getAddressLine2())
                .addressLine3(taxpayer.getAddressLine3())
                .refStateId(taxpayer.getRefStateId())
                .refCityId(taxpayer.getRefCityId())
                .postcode(taxpayer.getPostcode())
                .ns3BranchId(taxpayer.getNs3BranchId())
                .build();
    }

    private AuditTaxTypeData toTaxType(final TaxpayerTaxTypeInfo row) {
        String taxTypeName = null;
        if (row.getRefTaxRegTypeId() != null) {
            for (final TaxType taxType : TaxType.values()) {
                if (taxType.getAssistId() == row.getRefTaxRegTypeId()) {
                    taxTypeName = taxType.name();
                    break;
                }
            }
        }
        return AuditTaxTypeData.builder()
                .id(row.getId())
                .taxType(taxTypeName)
                .refTaxRegTypeId(row.getRefTaxRegTypeId())
                .selectedAudit(row.isSelectedAudit())
                .businessComDate(row.getBusinessComDate())
                .manSerComDate(row.getManSerComDate())
                .finYrEndMon(row.getRefFinYrEndMonthId())
                .annualTtlTaxSalSerVal(row.getAnnualTtlTaxSalSerVal())
                .sstInfoId(row.getSstInfoId())
                .build();
    }

    private static AuditRiskData toRisk(final AuditRiskIndicatorsDetails row) {
        if (row == null) {
            return AuditRiskData.builder().build();
        }
        return AuditRiskData.builder()
                .checkedRegAnomaly(row.isCheckedRegAnomaly())
                .checkedFilingBehaviour(row.isCheckedFilingBehaviour())
                .checkedPaymentBehaviour(row.isCheckedPaymentBehaviour())
                .checkedReturnTax(row.isCheckedReturnTax())
                .checkedFinancialAnomaly(row.isCheckedFinancialAnomaly())
                .checkedThirdPartyMismatch(row.isCheckedThirdPartyMismatch())
                .checkedHisComplianceIssue(row.isCheckedHisComplianceIssue())
                .checkedIntelligenceBased(row.isCheckedIntelligenceBased())
                .riskAssessmentDetails(row.getRiskAssessmentDetails())
                .riskIndicatorsRemark(row.getRiskIndicatorsRemark())
                .build();
    }

    private static AuditFocusData toFocus(final AuditFocusAreaDetails row) {
        if (row == null) {
            return AuditFocusData.builder().build();
        }
        return AuditFocusData.builder()
                .checkedOutputTax(row.isCheckedOutputTax())
                .checkedInputTaxEligility(row.isCheckedInputTaxEligility())
                .checkedRefund(row.isCheckedRefund())
                .checkedClassification(row.isCheckedClassification())
                .checkedExemption(row.isCheckedExemption())
                .checkedImportReconciliation(row.isCheckedImportReconciliation())
                .checkedRevenueReconciliation(row.isCheckedRevenueReconciliation())
                .checkedOthers(row.isCheckedOthers())
                .othersInput(row.getOthersInput())
                .build();
    }

    private static AuditLimitationData toLimitation(final AuditLimitationDisclosureDetails row) {
        if (row == null) {
            return AuditLimitationData.builder().build();
        }
        return AuditLimitationData.builder()
                .checkedDataIncomplete(row.isCheckedDataIncomplete())
                .checkedAccessLimitation(row.isCheckedAccessLimitation())
                .checkedPreliminaryAssumptions(row.isCheckedPreliminaryAssumptions())
                .checkedOthers(row.isCheckedOthers())
                .othersInput(row.getOthersInput())
                .build();
    }

    private void assertStaff() {
        if (platformUserContext.getCurrentUser().isEmployer()) {
            throw new IllegalArgumentException("Audit cases are staff-only.");
        }
    }
}
