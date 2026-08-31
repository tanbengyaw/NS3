package my.gov.perkeso.assist.registration.reference;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateAddNewEmployeeReferenceNoGenerator implements ReferenceNoGenerator {

    private final SequenceNoService sequenceNoService;

    @Override
    public String generateNo() {
        final LocalDate today = LocalDate.now();
        final String case8Digit = sequenceNoService.getUpdateAddEmployee8DigitSequenceNo();
        return "CAN" + String.format("%02d", today.getMonthValue()) + today.getYear() + case8Digit;
    }
}
