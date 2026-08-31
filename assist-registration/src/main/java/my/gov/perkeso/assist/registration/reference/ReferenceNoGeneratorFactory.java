package my.gov.perkeso.assist.registration.reference;

import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.UpdateCaseCategoryEnum;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;
import org.springframework.stereotype.Component;

/**
 * Mirrors ASSIST {@code AbstractRegCounter#takeRefService}.
 */
@Component
@RequiredArgsConstructor
public class ReferenceNoGeneratorFactory {

    private final SequenceNoService sequenceNoService;
    private final NewRegReferenceNoGenerator newRegReferenceNoGenerator;
    private final UpdateAddNewEmployeeReferenceNoGenerator updateAddNewEmployeeReferenceNoGenerator;
    private final SsnReferenceNoGenerator ssnReferenceNoGenerator;

    public ReferenceNoGenerator forSection(final RegistrationSection section) {
        return switch (section) {
            case REG_NEW_REG, REG_NEW_REG_SST_SALES_TAX, REG_SST_TOURISM_TAX, REG_SST_DIGITAL_TAX, REG_SST_DPSP_TAX,
                    REG_INCOMPLETE_TAX_PAYER_SERVICE_TAX, REG_INCOMPLETE_TAX_PAYER_SALES_TAX,
                    REG_INCOMPLETE_TAX_PAYER_TOURISM_TAX, REG_INCOMPLETE_TAX_PAYER_DIGITAL_TAX,
                    REG_INCOMPLETE_TAX_PAYER_DPSP_TAX -> newRegReferenceNoGenerator;
            case REG_UPDATE_ADD_NEW_EMPLOYEE_BY_UPLOAD_FILE, REG_UPDATE_ADD_NEW_EMPLOYEE ->
                updateAddNewEmployeeReferenceNoGenerator;
            case REG_UPDATE_EMPLOYEE_INFO, REG_UPDATE_EMPLOYER_INFO, REG_UPDATE_ADD_EMPLOYEE_RESIGN_DATE,
                    REG_UPDATE_ADD_SUPPORTING_DOC, REG_UPDATE_DIRECTOR_OWNER_INFO, REG_UPDATE_EMPLOYER_STATUS_REASON,
                    REG_UPDATE_EMPLOYEE_RESIGNED_DATE, REG_UPDATE_ADD_REACTIVATE_RESIGNED_EMPLOYEE,
                    REG_UPDATE_TAX_PAYER_SERVICE_TAX, REG_UPDATE_TAX_PAYER_SALES_TAX, REG_UPDATE_TAX_PAYER_TOURISM_TAX,
                    REG_UPDATE_TAX_PAYER_DIGITAL_TAX, REG_UPDATE_TAX_PAYER_DPSP_TAX ->
                UpdateReferenceNoGenerator.forCategory(sequenceNoService, UpdateCaseCategoryEnum.C1);
            case REG_DISCONTINUE_BIZ, REG_SST_DISCONTINUE_TAX ->
                UpdateReferenceNoGenerator.forCategory(sequenceNoService, UpdateCaseCategoryEnum.C2);
            case REG_SSN -> ssnReferenceNoGenerator;
            case REG_UPDATE_EMPLOYER_STATUS_REASON_SIP, REG_UPDATE_ADD_EMPLOYEE_RESIGNED_DATE_SIP, REG_UPDATE_NEW_REG_SIP,
                    REG_UPDATE_EMPLOYEE_START_DATE_SIP, REG_UPDATE_EMPLOYEE_RESIGNED_DATE_SIP,
                    REG_UPDATE_ADD_REACTIVATE_RESIGNED_EMPLOYEE_SIP ->
                UpdateSipReferenceNoGenerator.forCategory(sequenceNoService, UpdateCaseCategoryEnum.C1);
            case REG_DISCONTINUE_BIZ_SIP ->
                UpdateSipReferenceNoGenerator.forCategory(sequenceNoService, UpdateCaseCategoryEnum.C2);
        };
    }

    public String generateForSection(final RegistrationSection section) {
        return forSection(section).generateNo();
    }
}
