package my.gov.perkeso.assist.registration.constant;

/**
 * Mirrors ASSIST {@code SectionEnum} branches used by {@code AbstractRegCounter#takeRefService}.
 */
public enum RegistrationSection {
    REG_NEW_REG(200L),
    REG_UPDATE_ADD_NEW_EMPLOYEE(201L),
    REG_UPDATE_ADD_EMPLOYEE_RESIGN_DATE(202L),
    REG_UPDATE_EMPLOYEE_INFO(203L),
    REG_UPDATE_EMPLOYER_INFO(204L),
    REG_UPDATE_EMPLOYER_STATUS_REASON(205L),
    REG_UPDATE_DIRECTOR_OWNER_INFO(206L),
    REG_UPDATE_ADD_SUPPORTING_DOC(207L),
    REG_DISCONTINUE_BIZ(208L),
    REG_SSN(209L),
    REG_UPDATE_ADD_EMPLOYEE_RESIGNED_DATE_SIP(213L),
    REG_UPDATE_EMPLOYER_STATUS_REASON_SIP(214L),
    REG_DISCONTINUE_BIZ_SIP(215L),
    REG_UPDATE_EMPLOYEE_START_DATE_SIP(216L),
    REG_UPDATE_NEW_REG_SIP(217L),
    REG_UPDATE_EMPLOYEE_RESIGNED_DATE(222L),
    REG_UPDATE_ADD_NEW_EMPLOYEE_BY_UPLOAD_FILE(223L),
    REG_UPDATE_EMPLOYEE_RESIGNED_DATE_SIP(224L),
    REG_UPDATE_ADD_REACTIVATE_RESIGNED_EMPLOYEE(225L),
    REG_UPDATE_ADD_REACTIVATE_RESIGNED_EMPLOYEE_SIP(226L),
    REG_NEW_REG_SST_SALES_TAX(1100L),
    REG_SST_TOURISM_TAX(1101L),
    REG_SST_DIGITAL_TAX(1102L),
    REG_SST_DISCONTINUE_TAX(1103L),
    REG_SST_DPSP_TAX(1104L),
    REG_SST_SERVICE_TAX(1105L),
    REG_UPDATE_TAX_PAYER_SERVICE_TAX(1200L),
    REG_UPDATE_TAX_PAYER_SALES_TAX(1201L),
    REG_UPDATE_TAX_PAYER_TOURISM_TAX(1202L),
    REG_UPDATE_TAX_PAYER_DIGITAL_TAX(1203L),
    REG_UPDATE_TAX_PAYER_DPSP_TAX(1204L),
    REG_INCOMPLETE_TAX_PAYER_SERVICE_TAX(1205L),
    REG_INCOMPLETE_TAX_PAYER_SALES_TAX(1206L),
    REG_INCOMPLETE_TAX_PAYER_TOURISM_TAX(1207L),
    REG_INCOMPLETE_TAX_PAYER_DIGITAL_TAX(1208L),
    REG_INCOMPLETE_TAX_PAYER_DPSP_TAX(1209L);

    private final long assistSectionId;

    RegistrationSection(final long assistSectionId) {
        this.assistSectionId = assistSectionId;
    }

    public long getAssistSectionId() {
        return assistSectionId;
    }

    public static RegistrationSection fromAssistSectionId(final long assistSectionId) {
        for (final RegistrationSection section : values()) {
            if (section.assistSectionId == assistSectionId) {
                return section;
            }
        }
        throw new IllegalArgumentException("Registration section not found for id: " + assistSectionId);
    }
}
