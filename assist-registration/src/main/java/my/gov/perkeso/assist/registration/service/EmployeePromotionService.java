package my.gov.perkeso.assist.registration.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.gov.perkeso.assist.registration.domain.Employee;
import my.gov.perkeso.assist.registration.domain.EmployeeRepository;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.TempEmployee;
import my.gov.perkeso.assist.registration.domain.TempEmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simplified ASSIST {@code NewRegCounter#submitEmployee} — promotes Form 2 temp rows to live employees.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeePromotionService {

    private final TempEmployeeRepository tempEmployeeRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public List<Employee> promoteOnApprove(final Long tempEmployerId, final Employer employer) {
        final List<TempEmployee> tempEmployees = tempEmployeeRepository
                .findByTempEmployerIdAndDeletedFalseOrderByIdAsc(tempEmployerId);

        final List<Employee> promoted = tempEmployees.stream().map(tempEmployee -> toEmployee(tempEmployee, employer))
                .map(employeeRepository::save)
                .toList();

        log.info("Promoted {} temp employees for employer {}", promoted.size(), employer.getEmployerCode());
        return promoted;
    }

    private static Employee toEmployee(final TempEmployee tempEmployee, final Employer employer) {
        final Employee employee = new Employee();
        employee.setEmployerId(employer.getId());
        employee.setEmployeeName(tempEmployee.getEmployeeName());
        employee.setIdentificationNo(tempEmployee.getIdentificationNo());
        employee.setEmploymentStartDate(tempEmployee.getEmploymentStartDate());
        employee.setNationalityId(tempEmployee.getNationalityId());
        employee.setTempEmployeeId(tempEmployee.getId());
        employee.setCreatedDate(LocalDateTime.now());
        employee.setDeleted(false);
        return employee;
    }
}
