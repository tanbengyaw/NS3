package my.gov.perkeso.assist.registration.employercode;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;
import my.gov.perkeso.assist.registration.service.AreaCodeLookupService;
import org.springframework.stereotype.Service;

/**
 * Mirrors ASSIST {@code SmkNo}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmkNoGenerator {

    private final SequenceNoService sequenceNoService;
    private final AreaCodeLookupService areaCodeLookupService;

    public String generateCode(final EmployerCodeContext context, final RegistrationSection section) {
        final String case8Digit = sequenceNoService.getSmkNo();
        String areaCode = areaCodeLookupService.findAreaCodeByPostCodeAndBranchId(context.getPostCode(),
                context.getBranchId());
        if (areaCode == null) {
            areaCode = "";
            log.error("AreaCode Not Found!");
        }

        final String regTypePrefix = resolvePrefix(section);
        final StringBuilder sb = new StringBuilder();
        sb.append(areaCode);
        sb.append(regTypePrefix);
        sb.append(case8Digit);
        sb.append("/");
        sb.append(LocalDate.now().getYear());
        return sb.toString();
    }

    private static String resolvePrefix(final RegistrationSection section) {
        if (section == null) {
            return "";
        }
        return switch (section) {
            case REG_NEW_REG, REG_SST_SERVICE_TAX, REG_INCOMPLETE_TAX_PAYER_SERVICE_TAX -> "-CP-";
            case REG_NEW_REG_SST_SALES_TAX, REG_INCOMPLETE_TAX_PAYER_SALES_TAX -> "-CJ-";
            case REG_SST_TOURISM_TAX, REG_INCOMPLETE_TAX_PAYER_TOURISM_TAX -> "-CO-";
            case REG_SST_DIGITAL_TAX, REG_INCOMPLETE_TAX_PAYER_DIGITAL_TAX -> "-CD-";
            case REG_SST_DPSP_TAX, REG_INCOMPLETE_TAX_PAYER_DPSP_TAX -> "-CT-";
            default -> {
                log.error("Prefix not available for {}", section.name());
                yield "";
            }
        };
    }
}
