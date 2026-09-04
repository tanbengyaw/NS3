package my.gov.perkeso.assist.registration.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesTaxRejectionLetterAttributeBuilder {

    private final RegistrationLetterCommonAttributeBuilder commonAttributeBuilder;

    Map<String, String> buildAttributes(final RegGeneralInfo regCase) {
        final Map<String, String> attributes = commonAttributeBuilder.buildCommonAttributes(regCase);
        attributes.put("employerList", buildEmployerListHtml(regCase));
        return attributes;
    }

    private static String buildEmployerListHtml(final RegGeneralInfo regCase) {
        final TempEmployer tempEmployer = regCase.getTempEmployer();
        final String employerName = tempEmployer != null ? tempEmployer.getEmployerName() : "-";
        final String brn = tempEmployer != null && tempEmployer.getBusinessInfo() != null
                ? tempEmployer.getBusinessInfo().getRegistrationNo()
                : "-";
        final String reason = regCase.getAppStatusReason();
        final String rejectReason = reason != null && !reason.isBlank() ? reason.trim() : "-";

        return """
                <tr class="titleFontArial11NoBold">
                    <td><span>1</span></td>
                    <td><span>%s</span></td>
                    <td><span>%s</span></td>
                    <td><span>%s</span></td>
                </tr>
                """.formatted(
                RegistrationLetterCommonAttributeBuilder.escapeHtml(employerName),
                RegistrationLetterCommonAttributeBuilder.escapeHtml(brn),
                RegistrationLetterCommonAttributeBuilder.escapeHtml(rejectReason));
    }
}
