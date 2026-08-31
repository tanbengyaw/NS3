package my.gov.perkeso.assist.registration.employercode;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;
import my.gov.perkeso.assist.registration.service.AreaCodeLookupService;
import org.springframework.stereotype.Service;

/**
 * Mirrors ASSIST {@code SstRegistrationNo}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SstRegistrationNoGenerator {

    private final SequenceNoService sequenceNoService;
    private final AreaCodeLookupService areaCodeLookupService;

    public String generateCode(final EmployerCodeContext context) {
        final String eightDigit = sequenceNoService.getEmployerCode8DigitSequenceNo();
        String areaCode = areaCodeLookupService.findAreaCodeByPostCodeAndBranchId(context.getPostCode(),
                context.getBranchId());
        if (areaCode == null) {
            areaCode = "";
            log.error("AreaCode Not Found!");
        }

        final LocalDate today = LocalDate.now();
        return areaCode + "-" + today.getMonthValue() + today.getYear() + "-" + eightDigit;
    }
}
