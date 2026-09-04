package my.gov.perkeso.assist.registration.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.SupportingDocumentTypeData;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempSstSupportingDocument;
import my.gov.perkeso.assist.registration.domain.TempSstSupportingDocumentRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesTaxInquiryLetterAttributeBuilder {

    private final RegistrationLetterCommonAttributeBuilder commonAttributeBuilder;
    private final RegistrationReferenceReadPlatformService registrationReferenceReadPlatformService;
    private final TempSstInfoRepository tempSstInfoRepository;
    private final TempSstSupportingDocumentRepository tempSstSupportingDocumentRepository;

    Map<String, String> buildAttributes(final RegGeneralInfo regCase) {
        final Map<String, String> attributes = commonAttributeBuilder.buildCommonAttributes(regCase);
        attributes.put("employeeList", buildEmployeeListHtml());
        attributes.put("orderList", buildOrderListHtml(regCase));
        return attributes;
    }

    private static String buildEmployeeListHtml() {
        return """
                <tr class="titleFontArial11NoBold">
                    <td><span>—</span></td>
                    <td><span>—</span></td>
                    <td><span>Tiada maklumat pekerja</span></td>
                </tr>
                """;
    }

    private String buildOrderListHtml(final RegGeneralInfo regCase) {
        final List<String> items = new ArrayList<>(findMissingSupportingDocumentLabels(regCase));
        final String remark = regCase.getQueryRemark();
        items.add("CATATAN: " + (remark != null && !remark.isBlank() ? remark.trim() : "-"));
        return RegistrationLetterCommonAttributeBuilder.buildOrderListHtml(items);
    }

    private List<String> findMissingSupportingDocumentLabels(final RegGeneralInfo regCase) {
        final TempEmployer tempEmployer = regCase.getTempEmployer();
        if (tempEmployer == null) {
            return List.of();
        }
        final TempSstInfo tempSstInfo = tempSstInfoRepository.findByTempEmployerId(tempEmployer.getId())
                .orElse(null);
        if (tempSstInfo == null) {
            return registrationReferenceReadPlatformService.retrieveSupportingDocumentTypesForSalesTax().stream()
                    .map(SupportingDocumentTypeData::getLabel)
                    .toList();
        }

        final Set<Long> uploadedTypeIds = tempSstSupportingDocumentRepository
                .findByTempSstInfoIdAndDeletedFalseOrderByIdAsc(tempSstInfo.getId()).stream()
                .map(TempSstSupportingDocument::getDocumentTypeId)
                .collect(Collectors.toSet());

        return registrationReferenceReadPlatformService.retrieveSupportingDocumentTypesForSalesTax().stream()
                .filter(type -> !uploadedTypeIds.contains(type.getId()))
                .map(SupportingDocumentTypeData::getLabel)
                .toList();
    }
}
