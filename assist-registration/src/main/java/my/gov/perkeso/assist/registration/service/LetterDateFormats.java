package my.gov.perkeso.assist.registration.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.experimental.UtilityClass;

/**
 * Legacy ASSIST letter dates use {@code dd/MM/yyyy} ({@code PKSUtilityDateBean.DATE_FORMAT}).
 */
@UtilityClass
public class LetterDateFormats {

    public static final DateTimeFormatter LETTER_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);

    public static String formatLetterDate(final LocalDate date) {
        return date != null ? LETTER_DATE.format(date) : "—";
    }
}
