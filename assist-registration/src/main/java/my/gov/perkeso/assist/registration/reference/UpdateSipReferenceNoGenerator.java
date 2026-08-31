package my.gov.perkeso.assist.registration.reference;

import java.time.LocalDate;
import my.gov.perkeso.assist.registration.constant.UpdateCaseCategoryEnum;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;

public class UpdateSipReferenceNoGenerator implements ReferenceNoGenerator {

    private final SequenceNoService sequenceNoService;
    private final UpdateCaseCategoryEnum caseCategory;

    public UpdateSipReferenceNoGenerator(final SequenceNoService sequenceNoService,
            final UpdateCaseCategoryEnum caseCategory) {
        this.sequenceNoService = sequenceNoService;
        this.caseCategory = caseCategory;
    }

    @Override
    public String generateNo() {
        final LocalDate today = LocalDate.now();
        final String case8Digit;
        final StringBuilder sb = new StringBuilder("ERN");
        sb.append(String.format("%02d", today.getMonthValue()));
        sb.append(today.getYear());
        if (UpdateCaseCategoryEnum.C1.equals(caseCategory)) {
            case8Digit = sequenceNoService.getSipUpdateC18DigitSequenceNo();
            sb.append("C1");
        } else if (UpdateCaseCategoryEnum.C2.equals(caseCategory)) {
            case8Digit = sequenceNoService.getSipUpdateC28DigitSequenceNo();
            sb.append("C2");
        } else {
            throw new IllegalArgumentException("Update Case Category Enum not found!");
        }
        sb.append(case8Digit);
        return sb.toString();
    }

    public static UpdateSipReferenceNoGenerator forCategory(final SequenceNoService sequenceNoService,
            final UpdateCaseCategoryEnum caseCategory) {
        return new UpdateSipReferenceNoGenerator(sequenceNoService, caseCategory);
    }
}
