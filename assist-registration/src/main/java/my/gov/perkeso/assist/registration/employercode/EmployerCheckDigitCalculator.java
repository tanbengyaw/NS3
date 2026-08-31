package my.gov.perkeso.assist.registration.employercode;

import my.gov.perkeso.assist.registration.constant.CheckDigitTableEnum;

public final class EmployerCheckDigitCalculator {

    private static final String LEADING_TRIPLE_ZERO = "000";

    private EmployerCheckDigitCalculator() {}

    public static String calculateCheckDigit(final String areaCode, final String eightDigit) {
        final String areaCodeWithoutChar = areaCode.replaceAll("[^0-9]+", "");
        final String normalizedEightDigit = stripLeadingTripleZero(eightDigit);
        final String employerCodeNo = areaCodeWithoutChar + normalizedEightDigit;

        double remainderFactorResult = 0D;
        for (int i = 0; i < employerCodeNo.length(); i++) {
            final double extractNumber = Double.parseDouble(String.valueOf(employerCodeNo.charAt(i)));
            final double powerOf2 = Math.pow(2, (employerCodeNo.length() - i));
            remainderFactorResult += extractNumber * powerOf2;
        }
        final double mod = remainderFactorResult % 11;
        final double modulus11 = Math.abs(mod - 11);
        return CheckDigitTableEnum.fromValue(modulus11).name();
    }

    private static String stripLeadingTripleZero(final String runningNo) {
        if (runningNo.startsWith(LEADING_TRIPLE_ZERO)) {
            return runningNo.substring(3);
        }
        return runningNo;
    }
}
