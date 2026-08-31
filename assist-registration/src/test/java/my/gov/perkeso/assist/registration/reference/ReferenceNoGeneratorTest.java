package my.gov.perkeso.assist.registration.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import my.gov.perkeso.assist.registration.constant.UpdateCaseCategoryEnum;
import my.gov.perkeso.assist.registration.sequence.SequenceNoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceNoGeneratorTest {

    @Mock
    private SequenceNoService sequenceNoService;

    @InjectMocks
    private NewRegReferenceNoGenerator newRegReferenceNoGenerator;

    @InjectMocks
    private UpdateAddNewEmployeeReferenceNoGenerator updateAddNewEmployeeReferenceNoGenerator;

    @InjectMocks
    private SsnReferenceNoGenerator ssnReferenceNoGenerator;

    @Test
    void newRegReferenceNoUsesAssistFormat() {
        when(sequenceNoService.getNewRegCase8DigitSequenceNo()).thenReturn("00000001");

        final String refNo = newRegReferenceNoGenerator.generateNo();
        final LocalDate today = LocalDate.now();

        assertThat(refNo).isEqualTo(
                "CRN" + String.format("%02d", today.getMonthValue()) + today.getYear() + "00000001");
    }

    @Test
    void updateReferenceNoC1UsesAssistFormat() {
        when(sequenceNoService.getUpdateC18DigitSequenceNo()).thenReturn("00000016");

        final UpdateReferenceNoGenerator generator = UpdateReferenceNoGenerator.forCategory(sequenceNoService,
                UpdateCaseCategoryEnum.C1);
        final LocalDate today = LocalDate.now();

        assertThat(generator.generateNo()).isEqualTo(
                "CUN" + String.format("%02d", today.getMonthValue()) + today.getYear() + "C100000016");
    }

    @Test
    void updateAddEmployeeReferenceNoUsesAssistFormat() {
        when(sequenceNoService.getUpdateAddEmployee8DigitSequenceNo()).thenReturn("00000002");

        final LocalDate today = LocalDate.now();

        assertThat(updateAddNewEmployeeReferenceNoGenerator.generateNo()).isEqualTo(
                "CAN" + String.format("%02d", today.getMonthValue()) + today.getYear() + "00000002");
    }

    @Test
    void ssnReferenceNoUsesAssistFormat() {
        when(sequenceNoService.getSsnCase8DigitSequenceNo()).thenReturn("00000003");

        final LocalDate today = LocalDate.now();

        assertThat(ssnReferenceNoGenerator.generateNo()).isEqualTo(
                "SSN" + String.format("%02d", today.getMonthValue()) + today.getYear() + "00000003");
    }
}
