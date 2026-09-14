package my.gov.perkeso.assist.registration.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.EmployerStatusInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.domain.TempEmployerRepository;
import my.gov.perkeso.assist.registration.employercode.EmployerCodeContext;
import my.gov.perkeso.assist.registration.employercode.EmployerCodeGeneratorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbound replacement for legacy {@code AutoRegTaxPayerWs}. Creates or reuses a live employer and
 * a partial {@code SstInfo} flagged as auto-registration. Does <em>not</em> create a 1205–1209
 * case — staff start that from the incomplete listing.
 */
@Service
@RequiredArgsConstructor
public class AutoRegTaxPayerIngestService {

    private static final String DEFAULT_SOURCE = "audit";

    private final EmployerRepository employerRepository;
    private final EmployerStatusInfoRepository employerStatusInfoRepository;
    private final EmployerCodeGenerator employerCodeGenerator;
    private final EmployerCodeGeneratorFactory employerCodeGeneratorFactory;
    private final SstInfoRepository sstInfoRepository;
    private final TempEmployerRepository tempEmployerRepository;

    @Transactional
    public CommandProcessingResult ingest(final JsonNode node) {
        final TaxType taxType = IncompleteAutoRegCompleteness.parseTaxType(text(node, "taxType"));
        final String employerName = requiredText(node, "employerName");
        final String registrationNo = requiredText(node, "registrationNo");
        final String postCode = requiredText(node, "postCode");
        final Long pksBranchId = longValue(node, "pksBranchId") != null ? longValue(node, "pksBranchId") : 1L;
        final Long serviceTypeId = longValue(node, "serviceTypeId") != null ? longValue(node, "serviceTypeId") : 1L;
        final Long businessEntityTypeId = longValue(node, "businessEntityTypeId") != null
                ? longValue(node, "businessEntityTypeId") : 1L;
        final String source = blankToDefault(text(node, "source"), DEFAULT_SOURCE);
        final String cusAudRefNo = blankToNull(text(node, "cusAudRefNo"));

        final Employer employer = findOrCreateEmployer(employerName, registrationNo, postCode, pksBranchId,
                serviceTypeId, businessEntityTypeId, text(node, "email"), text(node, "phone"), taxType);

        final SstInfo existing = findIdempotentRow(employer.getId(), taxType, cusAudRefNo);
        if (existing != null) {
            if (IncompleteAutoRegCompleteness.isComplete(taxType, existing) && taxType != TaxType.SALES_TAX) {
                throw new IllegalArgumentException(
                        "Employer already has a complete auto-registration for " + taxType.name());
            }
            if (!IncompleteAutoRegCompleteness.isComplete(taxType, existing)) {
                return result(employer, existing, true);
            }
        }

        final TempEmployer ingestTemp = saveIngestTempEmployer(employer, postCode, text(node, "addressLine1"),
                text(node, "addressLine2"), text(node, "addressLine3"));

        final SstInfo sstInfo = new SstInfo();
        sstInfo.setEmployerId(employer.getId());
        sstInfo.setTaxType(taxType.name());
        sstInfo.setAutoRegistration(true);
        sstInfo.setAutoRegistrationSource(source);
        sstInfo.setIngestTempEmployerId(ingestTemp.getId());
        sstInfo.setCusAudRefNo(cusAudRefNo);
        sstInfo.setTradeName(blankToNull(text(node, "tradeName")));
        sstInfo.setBusinessComDate(dateValue(node, "businessComDate"));
        sstInfo.setFinYrEndMon(intValue(node, "finYrEndMon"));
        sstInfo.setManComDate(dateValue(node, "manComDate"));
        sstInfo.setDateSaleValTaxGoods(dateValue(node, "dateSaleValTaxGoods"));
        sstInfo.setAnTotalTaxSalesVal(decimalValue(node, "anTotalTaxSalesVal"));
        sstInfo.setApplicantName(blankToNull(text(node, "applicantName")));
        sstInfo.setDeleted(false);
        sstInfo.setCreatedDate(LocalDateTime.now());
        final SstInfo saved = sstInfoRepository.save(sstInfo);

        return result(employer, saved, false);
    }

    private Employer findOrCreateEmployer(final String employerName, final String registrationNo,
            final String postCode, final Long pksBranchId, final Long serviceTypeId,
            final Long businessEntityTypeId, final String email, final String phone, final TaxType taxType) {
        return employerCodeGenerator.findExistingEmployerByRegistrationNo(registrationNo).orElseGet(() -> {
            final RegistrationSection section = RegistrationSection.fromAssistSectionId(
                    RegistrationSectionRouting.incompleteSectionIdFor(taxType));
            final String employerCode = employerCodeGeneratorFactory.generateCode(section,
                    EmployerCodeContext.builder().branchId(pksBranchId).postCode(postCode).build());

            final BusinessInfo businessInfo = new BusinessInfo();
            businessInfo.setRegistrationNo(registrationNo);
            businessInfo.setBusinessEntityTypeId(businessEntityTypeId);

            final Employer employer = new Employer();
            employer.setEmployerCode(employerCode);
            employer.setEmployerName(employerName);
            employer.setBusinessInfo(businessInfo);
            employer.setServiceTypeId(serviceTypeId);
            employer.setPksBranchId(pksBranchId);
            employer.setEmail(blankToNull(email));
            employer.setPhone(blankToNull(phone));
            employer.setContributionActive(true);
            employer.setDeleted(false);
            employer.setCreatedDate(LocalDateTime.now());
            final Employer saved = employerRepository.save(employer);
            employerStatusInfoRepository.save(employerCodeGenerator.createActiveStatus(saved.getId()));
            return saved;
        });
    }

    private SstInfo findIdempotentRow(final Long employerId, final TaxType taxType, final String cusAudRefNo) {
        final List<SstInfo> rows = sstInfoRepository
                .findByEmployerIdAndTaxTypeAndAutoRegistrationTrueAndDeletedFalseOrderByIdAsc(employerId,
                        taxType.name());
        if (rows.isEmpty()) {
            return null;
        }
        if (cusAudRefNo != null) {
            return rows.stream()
                    .filter(row -> Objects.equals(cusAudRefNo, row.getCusAudRefNo()))
                    .reduce((first, second) -> second)
                    .orElse(null);
        }
        return rows.stream()
                .filter(row -> !IncompleteAutoRegCompleteness.isComplete(taxType, row))
                .reduce((first, second) -> second)
                .orElse(rows.get(rows.size() - 1));
    }

    private TempEmployer saveIngestTempEmployer(final Employer employer, final String postCode,
            final String addressLine1, final String addressLine2, final String addressLine3) {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo(employer.getBusinessInfo().getRegistrationNo());
        businessInfo.setBusinessEntityTypeId(employer.getBusinessInfo().getBusinessEntityTypeId());

        final TempEmployer temp = new TempEmployer();
        temp.setEmployerName(employer.getEmployerName());
        temp.setBusinessInfo(businessInfo);
        temp.setServiceTypeId(employer.getServiceTypeId());
        temp.setPksBranchId(employer.getPksBranchId());
        temp.setEmail(employer.getEmail());
        temp.setPhone(employer.getPhone());
        temp.setPostCode(postCode);
        temp.setAddressLine1(addressLine1);
        temp.setAddressLine2(addressLine2);
        temp.setAddressLine3(addressLine3);
        return tempEmployerRepository.save(temp);
    }

    private static CommandProcessingResult result(final Employer employer, final SstInfo sstInfo,
            final boolean reused) {
        return CommandProcessingResult.withChanges(sstInfo.getId(), employer.getEmployerCode(),
                Map.of("employerId", employer.getId(), "sstInfoId", sstInfo.getId(), "taxType",
                        sstInfo.getTaxType() != null ? sstInfo.getTaxType() : "", "reused", reused,
                        "sectionId", RegistrationSectionRouting.incompleteSectionIdFor(
                                IncompleteAutoRegCompleteness.parseTaxType(sstInfo.getTaxType()))));
    }

    private static String requiredText(final JsonNode node, final String field) {
        final String value = text(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String text(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asText();
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
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private static BigDecimal decimalValue(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).decimalValue();
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
