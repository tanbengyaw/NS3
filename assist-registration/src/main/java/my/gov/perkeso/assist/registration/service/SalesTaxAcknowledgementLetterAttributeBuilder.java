package my.gov.perkeso.assist.registration.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesTaxAcknowledgementLetterAttributeBuilder {

    private static final String SST_STESEN_MENGAWAL_PREFIX = "Jabatan Kastam Diraja Malaysia";

    private final JdbcTemplate jdbcTemplate;
    private final AreaCodeLookupService areaCodeLookupService;
    private final BranchReferenceReadPlatformService branchReferenceReadPlatformService;
    private final SalesTaxAcknowledgementLetterAssetResolver assetResolver;

    Map<String, String> buildAttributes(final RegGeneralInfo regCase, final SstInfo sstInfo) {
        final TempEmployer tempEmployer = regCase.getTempEmployer();
        final AckLetterTaxPeriodCalculator.TaxPeriodSchedule taxPeriodSchedule =
                AckLetterTaxPeriodCalculator.calculateFromSstInfo(sstInfo);

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("employerAddressWithName", formatEmployerAddressWithName(tempEmployer));
        attributes.put("refNoOur", escapeHtml(regCase.getCaseRefNo()));
        attributes.put("date", LetterDateFormats.formatLetterDate(approvalDate(regCase, sstInfo)));
        attributes.put("refNoYour", escapeHtml(Objects.requireNonNullElse(sstInfo.getSalesTaxSmkRegNo(), "—")));
        attributes.put("branchOffice", escapeHtml(formatStesenMengawal(tempEmployer)));
        attributes.put("requestDate", LetterDateFormats.formatLetterDate(requestDate(regCase)));
        attributes.put("approvalDate", LetterDateFormats.formatLetterDate(approvalDate(regCase, sstInfo)));
        attributes.put("basicAcc", escapeHtml(taxPeriodSchedule.basicAcc()));
        attributes.put("taxPeriod", escapeHtml(taxPeriodSchedule.taxPeriod()));
        attributes.put("firstTaxPeriod", escapeHtml(taxPeriodSchedule.firstTaxPeriod()));
        attributes.put("lastPaymentDate", escapeHtml(taxPeriodSchedule.lastPaymentDate()));
        attributes.put("secondTaxPeriod", escapeHtml(taxPeriodSchedule.secondTaxPeriod()));
        attributes.put("lastPaymentDate2", escapeHtml(taxPeriodSchedule.lastPaymentDate2()));
        attributes.put("nextTaxPeriod", escapeHtml(taxPeriodSchedule.nextTaxPeriod()));
        attributes.put("lastPaymentDate3", escapeHtml(taxPeriodSchedule.lastPaymentDate3()));
        attributes.put("branchOfficeAddress", formatBranchOfficeAddress(tempEmployer.getPksBranchId()));
        attributes.put("nationalEmblem", assetResolver.resolveImageUri("national_emblem.png"));
        attributes.put("customLogo", assetResolver.resolveImageUri("sst_kastam_logo.png"));
        return attributes;
    }

    private LocalDate requestDate(final RegGeneralInfo regCase) {
        if (regCase.getDocumentReceivedDate() != null) {
            return regCase.getDocumentReceivedDate().toLocalDate();
        }
        if (regCase.getSubmissionDate() != null) {
            return regCase.getSubmissionDate().toLocalDate();
        }
        if (regCase.getCreatedDate() != null) {
            return regCase.getCreatedDate().toLocalDate();
        }
        return LocalDate.now();
    }

    private LocalDate approvalDate(final RegGeneralInfo regCase, final SstInfo sstInfo) {
        if (regCase.getUpdatedDate() != null) {
            return regCase.getUpdatedDate().toLocalDate();
        }
        if (sstInfo.getCreatedDate() != null) {
            return sstInfo.getCreatedDate().toLocalDate();
        }
        return LocalDate.now();
    }

    private String formatStesenMengawal(final TempEmployer tempEmployer) {
        final String areaName = firstNonBlank(
                areaCodeLookupService.findCustomsAreaNameByPostcode(tempEmployer.getPostCode()),
                tempEmployer.getCityName(),
                lookupStateName(tempEmployer.getStateId()));
        if (areaName == null) {
            return SST_STESEN_MENGAWAL_PREFIX;
        }
        return SST_STESEN_MENGAWAL_PREFIX + " " + titleCaseWords(areaName.trim());
    }

    private String formatBranchOfficeAddress(final Long branchId) {
        final BranchReferenceReadPlatformService.BranchLetterData branch =
                branchReferenceReadPlatformService.retrieveBranchForLetter(branchId);
        if (branch == null) {
            return "—";
        }
        final StringBuilder address = new StringBuilder();
        appendHtmlLine(address, formatBranchDisplayName(branch.getName()));
        appendHtmlLine(address, branch.getAddressLine1());
        appendHtmlLine(address, branch.getAddressLine2());
        appendHtmlLine(address, branch.getAddressLine3());
        appendHtmlPostcodeCity(address, branch.getPostCode(), branch.getCityName());
        return address.isEmpty() ? "—" : address.toString();
    }

    private static String formatBranchDisplayName(final String branchName) {
        if (branchName == null) {
            return null;
        }
        return branchName.replaceAll("(?i)PERKESO", "").replaceAll("\\s{2,}", " ").trim();
    }

    private String lookupStateName(final Long stateId) {
        if (stateId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT name FROM reference.ref_state WHERE id = ?
                """, rs -> rs.next() ? rs.getString(1) : null, stateId);
    }

    private String formatEmployerAddressWithName(final TempEmployer tempEmployer) {
        final StringBuilder address = new StringBuilder();
        appendHtmlLine(address, tempEmployer.getEmployerName());
        appendHtmlLine(address, tempEmployer.getAddressLine1());
        appendHtmlLine(address, tempEmployer.getAddressLine2());
        appendHtmlLine(address, tempEmployer.getAddressLine3());
        appendHtmlPostcodeCity(address, tempEmployer.getPostCode(), tempEmployer.getCityName());
        appendHtmlLine(address, lookupStateName(tempEmployer.getStateId()));
        return address.toString();
    }

    private static void appendHtmlLine(final StringBuilder address, final String value) {
        if (!hasText(value)) {
            return;
        }
        if (!address.isEmpty()) {
            address.append("<br/>");
        }
        address.append("<span>").append(escapeHtml(value.trim())).append("</span>");
    }

    private static void appendHtmlPostcodeCity(final StringBuilder address, final String postCode,
            final String cityName) {
        if (!hasText(postCode) && !hasText(cityName)) {
            return;
        }
        if (!address.isEmpty()) {
            address.append("<br/>");
        }
        address.append("<span>");
        if (hasText(postCode)) {
            address.append(escapeHtml(postCode.trim())).append("&nbsp;");
        }
        if (hasText(cityName)) {
            address.append(escapeHtml(cityName.trim()));
        }
        address.append("</span>");
    }

    private static String titleCaseWords(final String value) {
        if (value.isBlank()) {
            return value;
        }
        final String[] parts = value.toLowerCase().split("\\s+");
        final StringBuilder builder = new StringBuilder();
        for (final String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private static String firstNonBlank(final String... values) {
        for (final String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }

    private static String escapeHtml(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
