package my.gov.perkeso.assist.registration.service;

import java.util.ArrayList;
import java.util.List;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.constant.RegistrationSpecialCaseType;
import my.gov.perkeso.assist.registration.data.RegistrationSpecialCase;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.springframework.stereotype.Service;

/**
 * Simplified ASSIST {@code SpecialCase#checkIsEmployerProblemCase} for submit routing.
 */
@Service
public class RegistrationSpecialCaseService {

    private final EmployerCodeGenerator employerCodeGenerator;

    public RegistrationSpecialCaseService(final EmployerCodeGenerator employerCodeGenerator) {
        this.employerCodeGenerator = employerCodeGenerator;
    }

    public List<RegistrationSpecialCase> checkOnSubmit(final RegGeneralInfo regCase) {
        final List<RegistrationSpecialCase> specialCases = new ArrayList<>();
        final TempEmployer tempEmployer = regCase.getTempEmployer();
        if (tempEmployer == null || tempEmployer.getBusinessInfo() == null) {
            return specialCases;
        }

        if (RegistrationSectionRouting.isSstNewRegSection(regCase.getSectionId())) {
            return specialCases;
        }

        if (RegistrationSectionRouting.isUpdateTaxSection(regCase.getSectionId())
                || RegistrationSectionRouting.isDiscontinueTaxSection(regCase.getSectionId())) {
            // The temp employer's BRN legitimately matches the very employer being updated/
            // discontinued — duplicate-BRN detection is meaningless (and always a false positive)
            // for these cases.
            return specialCases;
        }

        if (!tempEmployer.isBranch()) {
            final String registrationNo = tempEmployer.getBusinessInfo().getRegistrationNo();
            if (registrationNo != null && !registrationNo.isBlank()
                    && employerCodeGenerator.isRegistrationNoRegistered(registrationNo.trim())) {
                specialCases.add(RegistrationSpecialCase.builder()
                        .type(RegistrationSpecialCaseType.DUPLICATE_REGISTRATION_NUMBER)
                        .message("Business registration number already registered: " + registrationNo.trim())
                        .build());
            }
        }

        return specialCases;
    }

    public boolean hasDuplicateRegistrationNumber(final List<RegistrationSpecialCase> specialCases) {
        return specialCases.stream()
                .anyMatch(item -> item.getType() == RegistrationSpecialCaseType.DUPLICATE_REGISTRATION_NUMBER);
    }
}
