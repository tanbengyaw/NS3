package my.gov.perkeso.assist.registration.employercode;

import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import org.springframework.stereotype.Component;

/**
 * Selects employer-code generator by registration section, mirroring ASSIST counters.
 */
@Component
@RequiredArgsConstructor
public class EmployerCodeGeneratorFactory {

    private final EmployerCodeBasedOnAreaCodeGenerator employerCodeBasedOnAreaCodeGenerator;
    private final SstRegistrationNoGenerator sstRegistrationNoGenerator;
    private final SmkNoGenerator smkNoGenerator;

    public String generateCode(final RegistrationSection section, final EmployerCodeContext context) {
        if (section == null || isStandardNewReg(section)) {
            return employerCodeBasedOnAreaCodeGenerator.generateCode(context);
        }
        if (isSstSection(section)) {
            return sstRegistrationNoGenerator.generateCode(context);
        }
        throw new IllegalArgumentException("Employer code generator not found for section: " + section);
    }

    public String generateSmkNo(final RegistrationSection section, final EmployerCodeContext context) {
        return smkNoGenerator.generateCode(context, section);
    }

    private static boolean isStandardNewReg(final RegistrationSection section) {
        return section == RegistrationSection.REG_NEW_REG;
    }

    private static boolean isSstSection(final RegistrationSection section) {
        return switch (section) {
            case REG_NEW_REG_SST_SALES_TAX, REG_SST_TOURISM_TAX, REG_SST_DIGITAL_TAX, REG_SST_DPSP_TAX,
                    REG_INCOMPLETE_TAX_PAYER_SERVICE_TAX, REG_INCOMPLETE_TAX_PAYER_SALES_TAX,
                    REG_INCOMPLETE_TAX_PAYER_TOURISM_TAX, REG_INCOMPLETE_TAX_PAYER_DIGITAL_TAX,
                    REG_INCOMPLETE_TAX_PAYER_DPSP_TAX -> true;
            default -> false;
        };
    }
}
