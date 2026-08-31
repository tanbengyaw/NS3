package my.gov.perkeso.assist.registration.reference;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SsnReferenceNoGenerator implements ReferenceNoGenerator {

    private final SequenceNoService sequenceNoService;

    @Override
    public String generateNo() {
        final LocalDate today = LocalDate.now();
        final String case8Digit = sequenceNoService.getSsnCase8DigitSequenceNo();
        return "SSN" + String.format("%02d", today.getMonthValue()) + today.getYear() + case8Digit;
    }

    public String generateSsnNo() {
        final LocalDate today = LocalDate.now();
        final String case5Digit = sequenceNoService.getSsnCase5DigitSequenceNo();
        return "SSN" + String.format("%02d", today.getMonthValue()) + today.getYear() + case5Digit;
    }
}
