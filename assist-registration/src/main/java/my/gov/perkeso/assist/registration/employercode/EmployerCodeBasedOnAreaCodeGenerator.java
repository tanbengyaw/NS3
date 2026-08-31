package my.gov.perkeso.assist.registration.employercode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.exception.EmployerCodeDuplicateException;
import my.gov.perkeso.assist.registration.exception.EmployerCodeFormatInvalidException;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;
import my.gov.perkeso.assist.registration.service.AreaCodeLookupService;
import org.springframework.stereotype.Service;

/**
 * Mirrors ASSIST {@code EmployerCodeBasedOnAreaCode}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerCodeBasedOnAreaCodeGenerator {

    private static final int MAX_EMPLOYER_CODE_LENGTH = 12;
    private static final int MAX_RETRY_EMPLOYER_CODE_GENERATION = 2;

    private final SequenceNoService sequenceNoService;
    private final AreaCodeLookupService areaCodeLookupService;
    private final EmployerRepository employerRepository;

    public String generateCode(final EmployerCodeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Component cant be null!");
        }
        if (context.getBranchId() == null || context.getBranchId() == 0) {
            throw new IllegalArgumentException("branchId cant be null or 0");
        }
        if (context.getPostCode() == null || context.getPostCode().isBlank()) {
            throw new IllegalArgumentException("postCode cant be blank");
        }

        log.info("generate employer code based on area code");
        String employerCode = null;
        for (int i = 1; i <= MAX_RETRY_EMPLOYER_CODE_GENERATION; i++) {
            employerCode = prepareCodeGeneration(context);

            if (employerCode.length() != MAX_EMPLOYER_CODE_LENGTH) {
                if (i < MAX_RETRY_EMPLOYER_CODE_GENERATION) {
                    log.error("employer code format is invalid, employer code: {}", employerCode);
                    continue;
                }
                throw new EmployerCodeFormatInvalidException("employer code format invalid:" + employerCode);
            }

            if (employerRepository.existsByEmployerCodeAndDeletedFalse(employerCode)) {
                if (i < MAX_RETRY_EMPLOYER_CODE_GENERATION) {
                    log.error("employer code is duplicate, employer code: {}", employerCode);
                    continue;
                }
                throw new EmployerCodeDuplicateException("employer code is duplicate:" + employerCode);
            }
            break;
        }

        log.info("completed generated employer code based on area code :{}", employerCode);
        return employerCode;
    }

    private String prepareCodeGeneration(final EmployerCodeContext context) {
        final String areaCode = resolveAreaCode(context);
        if (areaCode == null || areaCode.isBlank()) {
            throw new IllegalArgumentException("areaCode is null");
        }

        final String eightDigit = sequenceNoService.getEmployerCode8DigitSequenceNoByAreaCode(areaCode);
        final StringBuilder sb = new StringBuilder(areaCode);
        sb.append(eightDigit);
        sb.append(EmployerCheckDigitCalculator.calculateCheckDigit(areaCode, eightDigit));
        return sb.toString();
    }

    private String resolveAreaCode(final EmployerCodeContext context) {
        if (context.getAreaCode() != null && !context.getAreaCode().isBlank()) {
            return context.getAreaCode();
        }
        return areaCodeLookupService.findAreaCodeByPostCodeAndBranchId(context.getPostCode(), context.getBranchId());
    }
}
