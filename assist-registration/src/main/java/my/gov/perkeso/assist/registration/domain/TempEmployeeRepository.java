package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempEmployeeRepository extends JpaRepository<TempEmployee, Long> {

    List<TempEmployee> findByTempEmployerIdAndDeletedFalseOrderByIdAsc(Long tempEmployerId);
}
