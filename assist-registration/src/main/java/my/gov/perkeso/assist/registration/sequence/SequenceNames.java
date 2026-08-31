package my.gov.perkeso.assist.registration.sequence;

public final class SequenceNames {

    public static final String EMPLOYER_CODE = "EMPLOYER_CODE_SEQ";
    public static final String NEW_REG_CASE = "NEW_REG_CASE_SEQ";
    public static final String UPDATE_C1_CASE = "UPDATE_C1_CASE_SEQ";
    public static final String UPDATE_C2_CASE = "UPDATE_C2_CASE_SEQ";
    public static final String UPDATE_ADD_NEW_EMPLOYEE_CASE = "UPDATE_ADD_NEW_EMPLOYEE_CASE_SEQ";
    public static final String UPDATE_SSN_CASE = "UPDATE_SSN_CASE_SEQ";
    public static final String SSN = "SSN_SEQ";
    public static final String SIP_UPDATE_C1_CASE = "SIP_UPDATE_C1_CASE_SEQ";
    public static final String SIP_UPDATE_C2_CASE = "SIP_UPDATE_C2_CASE_SEQ";
    public static final String SSN_FOREIGN_EMPLOYEE = "SSN_FOREIGN_EMPLOYEE_SEQ";
    public static final String SMK_NO = "SMK_NO_SEQ";

    private SequenceNames() {}

    public static String employerCodeAreaCode(final String areaCode) {
        return "EMPLOYER_CODE_AREA_CODE_" + areaCode + "_SEQ";
    }
}
