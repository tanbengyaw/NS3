package my.gov.perkeso.assist.registration.reference;

import java.time.LocalDate;
import my.gov.perkeso.assist.registration.constant.UpdateCaseCategoryEnum;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;

public class UpdateReferenceNoGenerator implements ReferenceNoGenerator {

    private final SequenceNoService sequenceNoService;
    private final UpdateCaseCategoryEnum caseCategory;

    public UpdateReferenceNoGenerator(final SequenceNoService sequenceNoService,
            final UpdateCaseCategoryEnum caseCategory) {
        this.sequenceNoService = sequenceNoService;
        this.caseCategory = caseCategory;
    }

    @Override
    public String generateNo() {
        final LocalDate today = LocalDate.now();
        final String case8Digit;
        final StringBuilder sb = new StringBuilder("CUN");
        sb.append(String.format("%02d", today.getMonthValue()));
        sb.append(today.getYear());
        if (UpdateCaseCategoryEnum.C1.equals(caseCategory)) {
            case8Digit = sequenceNoService.getUpdateC18DigitSequenceNo();
            sb.append("C1");
        } else if (UpdateCaseCategoryEnum.C2.equals(caseCategory)) {
            case8Digit = sequenceNoService.getUpdateC28DigitSequenceNo();
            sb.append("C2");
        } else {
            throw new IllegalArgumentException("Update Case Category Enum not found!");
        }
        sb.append(case8Digit);
        return sb.toString();
    }

    public static UpdateReferenceNoGenerator forCategory(final SequenceNoService sequenceNoService,
            final UpdateCaseCategoryEnum caseCategory) {
        return new UpdateReferenceNoGenerator(sequenceNoService, caseCategory);
    }
}
