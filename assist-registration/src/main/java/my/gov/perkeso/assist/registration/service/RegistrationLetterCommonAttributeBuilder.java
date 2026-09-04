package my.gov.perkeso.assist.registration.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistrationLetterCommonAttributeBuilder {

    private static final String DEFAULT_MOTO =
            "KESSELAMATAN SOSIAL, KEBAJIKAN PEKERJA, KEMAJUAN NEGARA";
    private static final String DEFAULT_TAGLINE = "Memudahkan Perkhidmatan";
    private static final String DEFAULT_STAFF_TITLE = "Pegawai Pendaftaran";
    private static final String TRANSPARENT_IMAGE = SalesTaxAcknowledgementLetterAssetResolver.TRANSPARENT_IMAGE_DATA_URI;

    private final JdbcTemplate jdbcTemplate;
    private final AreaCodeLookupService areaCodeLookupService;
    private final BranchReferenceReadPlatformService branchReferenceReadPlatformService;
    private final SalesTaxAcknowledgementLetterTemplateRenderer templateRenderer;

    Map<String, String> buildCommonAttributes(final RegGeneralInfo regCase) {
        final TempEmployer tempEmployer = regCase.getTempEmployer();
        if (tempEmployer == null) {
            throw new IllegalStateException("Registration case has no temp employer: " + regCase.getId());
        }

        final BranchReferenceReadPlatformService.BranchLetterData branch =
                branchReferenceReadPlatformService.retrieveBranchForLetter(tempEmployer.getPksBranchId());
        final String stateName = lookupStateName(tempEmployer.getStateId());
        final String areaCode = resolveAreaCode(tempEmployer);
        final LocalDate receiveDate = receiveDate(regCase);

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("declareHeader", buildDeclareHeader(branch, stateName));
        attributes.put("refNoOur", escapeHtml(formatReferenceNoOur(areaCode, regCase.getCaseRefNo())));
        attributes.put("refNoYour", escapeHtml(regCase.getCaseRefNo()));
        attributes.put("receiveDate", LetterDateFormats.formatLetterDate(receiveDate));
        attributes.put("date", LetterDateFormats.formatLetterDate(LocalDate.now()));
        attributes.put("employerAddressWithName", buildEmployerAddressTableCell(tempEmployer, stateName));
        attributes.put("branchName", escapeHtml(branch != null ? branch.getName() : ""));
        attributes.put("branchTitleWithState", escapeHtml(formatBranchTitleWithState(stateName)));
        attributes.put("idStaff", escapeHtml(DEFAULT_STAFF_TITLE));
        attributes.put("designation", "");
        attributes.put("moto", escapeHtml(DEFAULT_MOTO));
        attributes.put("tagline", escapeHtml(DEFAULT_TAGLINE));
        attributes.put("letterPhrase", "");
        attributes.put("isCcNeeded", "");
        attributes.put("employerCode", "");
        return attributes;
    }

    String buildEmployerAddressBlock(final TempEmployer tempEmployer) {
        final String stateName = lookupStateName(tempEmployer.getStateId());
        final StringBuilder address = new StringBuilder();
        appendHtmlLine(address, tempEmployer.getEmployerName());
        appendHtmlLine(address, tempEmployer.getAddressLine1());
        appendHtmlLine(address, tempEmployer.getAddressLine2());
        appendHtmlLine(address, tempEmployer.getAddressLine3());
        appendHtmlPostcodeCity(address, tempEmployer.getPostCode(), tempEmployer.getCityName());
        appendHtmlLine(address, stateName);
        return address.toString();
    }

    private String buildDeclareHeader(final BranchReferenceReadPlatformService.BranchLetterData branch,
            final String stateName) {
        final Map<String, String> headerAttributes = new HashMap<>();
        headerAttributes.put("perkesoLeftHeader", TRANSPARENT_IMAGE);
        headerAttributes.put("perkesRightHeader", TRANSPARENT_IMAGE);
        headerAttributes.put("perkesoLogo", TRANSPARENT_IMAGE);
        headerAttributes.put("addr1", escapeHtml(branch != null ? branch.getAddressLine1() : ""));
        headerAttributes.put("addr2", escapeHtml(branch != null ? branch.getAddressLine2() : ""));
        headerAttributes.put("perkesoPost", escapeHtml(branch != null ? branch.getPostCode() : ""));
        headerAttributes.put("perkesoCity", escapeHtml(branch != null ? branch.getCityName() : ""));
        headerAttributes.put("perkesoState", escapeHtml(stateName));
        headerAttributes.put("NoFaks", "-");
        headerAttributes.put("Emel", "customercare@perkeso.gov.my");
        return templateRenderer.renderHeader(headerAttributes);
    }

    private String buildEmployerAddressTableCell(final TempEmployer tempEmployer, final String stateName) {
        final StringBuilder cell = new StringBuilder();
        cell.append("<td width=\"100%\" class=\"titleFontArial11NoBold\">");
        appendLegacyLine(cell, tempEmployer.getEmployerName());
        appendLegacyLine(cell, tempEmployer.getAddressLine1());
        appendLegacyLine(cell, tempEmployer.getAddressLine2());
        appendLegacyLine(cell, tempEmployer.getAddressLine3());
        appendLegacyPostcodeCity(cell, tempEmployer.getPostCode(), tempEmployer.getCityName());
        appendLegacyLine(cell, stateName);
        cell.append("</td>");
        return cell.toString();
    }

    private String formatReferenceNoOur(final String areaCode, final String caseRefNo) {
        if (areaCode == null || areaCode.isBlank()) {
            return caseRefNo;
        }
        return "208/" + areaCode + "/" + caseRefNo;
    }

    private String resolveAreaCode(final TempEmployer tempEmployer) {
        try {
            return areaCodeLookupService.findAreaCodeByPostCodeAndBranchId(
                    tempEmployer.getPostCode(), tempEmployer.getPksBranchId());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static LocalDate receiveDate(final RegGeneralInfo regCase) {
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

    private String lookupStateName(final Long stateId) {
        if (stateId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT name FROM reference.ref_state WHERE id = ?
                """, rs -> rs.next() ? rs.getString(1) : null, stateId);
    }

    private static String formatBranchTitleWithState(final String stateName) {
        if (stateName == null || stateName.isBlank()) {
            return "b.p : Pengarah Negeri";
        }
        return "b.p : Pengarah Negeri " + stateName;
    }

    static String buildOrderListHtml(final Iterable<String> items) {
        final StringBuilder html = new StringBuilder();
        int index = 1;
        for (final String item : items) {
            html.append("<tr class=\"titleFontArial11NoBold\">")
                    .append("<td width=\"20%\">")
                    .append(toLowerRoman(index)).append(")&nbsp;")
                    .append(escapeHtml(item))
                    .append("</td>")
                    .append("</tr>");
            index++;
        }
        return html.toString();
    }

    private static void appendLegacyLine(final StringBuilder cell, final String value) {
        if (!hasText(value)) {
            return;
        }
        cell.append("<span>").append(escapeHtml(value.trim())).append("</span><br/>");
    }

    private static void appendLegacyPostcodeCity(final StringBuilder cell, final String postCode,
            final String cityName) {
        if (!hasText(postCode) && !hasText(cityName)) {
            return;
        }
        cell.append("<span>");
        if (hasText(postCode)) {
            cell.append(escapeHtml(postCode.trim())).append("&nbsp;");
        }
        if (hasText(cityName)) {
            cell.append(escapeHtml(cityName.trim()));
        }
        cell.append("</span><br/>");
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

    private static String toLowerRoman(final int value) {
        return switch (value) {
            case 1 -> "i";
            case 2 -> "ii";
            case 3 -> "iii";
            case 4 -> "iv";
            case 5 -> "v";
            case 6 -> "vi";
            case 7 -> "vii";
            case 8 -> "viii";
            case 9 -> "ix";
            case 10 -> "x";
            default -> String.valueOf(value);
        };
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }

    static String escapeHtml(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
