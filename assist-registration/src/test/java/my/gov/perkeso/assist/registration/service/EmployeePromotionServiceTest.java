package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import my.gov.perkeso.assist.registration.domain.Employee;
import my.gov.perkeso.assist.registration.domain.EmployeeRepository;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.TempEmployee;
import my.gov.perkeso.assist.registration.domain.TempEmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePromotionServiceTest {

    @Mock
    private TempEmployeeRepository tempEmployeeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeePromotionService employeePromotionService;

    @Test
    void promotesTempEmployeesOnApprove() {
        final TempEmployee tempEmployee = new TempEmployee();
        tempEmployee.setId(10L);
        tempEmployee.setTempEmployerId(5L);
        tempEmployee.setEmployeeName("Ali Ahmad");
        tempEmployee.setIdentificationNo("900101015432");
        tempEmployee.setEmploymentStartDate(LocalDate.of(2024, 1, 1));
        tempEmployee.setNationalityId(458L);
        tempEmployee.setDeleted(false);

        when(tempEmployeeRepository.findByTempEmployerIdAndDeletedFalseOrderByIdAsc(5L))
                .thenReturn(List.of(tempEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            final Employee employee = invocation.getArgument(0);
            employee.setId(100L);
            return employee;
        });

        final Employer employer = new Employer();
        employer.setId(99L);
        employer.setEmployerCode("A3700000001F");

        final List<Employee> promoted = employeePromotionService.promoteOnApprove(5L, employer);

        assertThat(promoted).hasSize(1);
        final ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployerId()).isEqualTo(99L);
        assertThat(captor.getValue().getEmployeeName()).isEqualTo("Ali Ahmad");
        assertThat(captor.getValue().getTempEmployeeId()).isEqualTo(10L);
    }
}
