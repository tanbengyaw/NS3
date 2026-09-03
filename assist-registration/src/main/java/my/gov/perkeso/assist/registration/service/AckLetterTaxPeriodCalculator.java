package my.gov.perkeso.assist.registration.service;

import java.time.LocalDate;
import java.time.YearMonth;
import my.gov.perkeso.assist.registration.domain.SstInfo;

/**
 * Calculates SST acknowledgement letter taxable periods from financial year end month
 * and business commencement date (mirrors legacy ASSIST {@code AckLetterTaxPeriodCalculator}).
 */
public final class AckLetterTaxPeriodCalculator {

    private static final String BASIC_ACC = "Asas Bayaran";
    private static final String NEXT_PERIOD_LABEL = "Setiap Dua Bulan";
    private static final String LAST_PAYMENT_DATE_3 = "Hari terakhir bulan berikutnya";
    private static final String TAX_PERIOD_ONE_MONTH = "Satu Bulan";
    private static final String TAX_PERIOD_TWO_MONTHS = "Dua Bulan";
    private static final String PERIOD_SEPARATOR = " sehingga ";

    private AckLetterTaxPeriodCalculator() {
    }

    public static TaxPeriodSchedule calculateFromSstInfo(final SstInfo sstInfo) {
        if (sstInfo == null) {
            throw new IllegalArgumentException("SstInfo is required to calculate acknowledgement letter tax periods");
        }
        if (sstInfo.getBusinessComDate() == null) {
            throw new IllegalArgumentException(
                    "Business commencement date is required to calculate acknowledgement letter tax periods");
        }
        if (sstInfo.getFinYrEndMon() == null) {
            throw new IllegalArgumentException(
                    "Financial year end month is required to calculate acknowledgement letter tax periods");
        }
        return calculate(sstInfo.getBusinessComDate(), sstInfo.getFinYrEndMon());
    }

    public static TaxPeriodSchedule calculate(final LocalDate businessCommencementDate,
            final int financialYearEndMonth) {
        validateInputs(businessCommencementDate, financialYearEndMonth);

        final int commencementYear = businessCommencementDate.getYear();
        final boolean oddFinancialYearEndMonth = financialYearEndMonth % 2 == 1;

        final int firstStartYear;
        final int firstStartMonth;
        final int firstEndYear;
        final int firstEndMonth;

        if (oddFinancialYearEndMonth) {
            firstStartYear = commencementYear;
            firstStartMonth = financialYearEndMonth;
            firstEndYear = commencementYear;
            firstEndMonth = financialYearEndMonth;
        } else {
            final YearMonth firstStart = YearMonth.of(commencementYear, financialYearEndMonth).minusMonths(1);
            final YearMonth firstEnd = firstStart.plusMonths(1);
            firstStartYear = firstStart.getYear();
            firstStartMonth = firstStart.getMonthValue();
            firstEndYear = firstEnd.getYear();
            firstEndMonth = firstEnd.getMonthValue();
        }

        final YearMonth secondStart = YearMonth.of(firstEndYear, firstEndMonth).plusMonths(1);
        final YearMonth secondEnd = secondStart.plusMonths(1);

        final TaxPeriodSchedule schedule = new TaxPeriodSchedule();
        schedule.basicAcc = BASIC_ACC;
        schedule.taxPeriod = oddFinancialYearEndMonth ? TAX_PERIOD_ONE_MONTH : TAX_PERIOD_TWO_MONTHS;
        schedule.firstTaxPeriod = formatPeriod(firstStartYear, firstStartMonth, firstEndYear, firstEndMonth);
        schedule.lastPaymentDate = formatDate(paymentDueDate(firstEndYear, firstEndMonth));
        schedule.secondTaxPeriod = formatPeriod(secondStart.getYear(), secondStart.getMonthValue(),
                secondEnd.getYear(), secondEnd.getMonthValue());
        schedule.lastPaymentDate2 = formatDate(paymentDueDate(secondEnd.getYear(), secondEnd.getMonthValue()));
        schedule.nextTaxPeriod = NEXT_PERIOD_LABEL;
        schedule.lastPaymentDate3 = LAST_PAYMENT_DATE_3;
        return schedule;
    }

    private static void validateInputs(final LocalDate businessCommencementDate, final int financialYearEndMonth) {
        if (businessCommencementDate == null) {
            throw new IllegalArgumentException("Business commencement date is required");
        }
        if (financialYearEndMonth < 1 || financialYearEndMonth > 12) {
            throw new IllegalArgumentException("Financial year end month must be between 1 and 12");
        }
    }

    private static String formatPeriod(final int startYear, final int startMonth, final int endYear,
            final int endMonth) {
        return formatDate(firstDayOfMonth(startYear, startMonth)) + PERIOD_SEPARATOR
                + formatDate(lastDayOfMonth(endYear, endMonth));
    }

    private static LocalDate paymentDueDate(final int periodEndYear, final int periodEndMonth) {
        final YearMonth dueMonth = YearMonth.of(periodEndYear, periodEndMonth).plusMonths(1);
        return dueMonth.atEndOfMonth();
    }

    private static LocalDate firstDayOfMonth(final int year, final int month) {
        return LocalDate.of(year, month, 1);
    }

    private static LocalDate lastDayOfMonth(final int year, final int month) {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    private static String formatDate(final LocalDate date) {
        return LetterDateFormats.formatLetterDate(date);
    }

    public static final class TaxPeriodSchedule {
        private String basicAcc;
        private String taxPeriod;
        private String firstTaxPeriod;
        private String lastPaymentDate;
        private String secondTaxPeriod;
        private String lastPaymentDate2;
        private String nextTaxPeriod;
        private String lastPaymentDate3;

        public String basicAcc() {
            return basicAcc;
        }

        public String taxPeriod() {
            return taxPeriod;
        }

        public String firstTaxPeriod() {
            return firstTaxPeriod;
        }

        public String lastPaymentDate() {
            return lastPaymentDate;
        }

        public String secondTaxPeriod() {
            return secondTaxPeriod;
        }

        public String lastPaymentDate2() {
            return lastPaymentDate2;
        }

        public String nextTaxPeriod() {
            return nextTaxPeriod;
        }

        public String lastPaymentDate3() {
            return lastPaymentDate3;
        }
    }
}
