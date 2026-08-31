package my.gov.perkeso.assist.registration.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployerRepository extends JpaRepository<Employer, Long> {

    Optional<Employer> findByEmployerCodeAndDeletedFalse(String employerCode);

    Optional<Employer> findByEmployerCodeIgnoreCaseAndDeletedFalse(String employerCode);

    Optional<Employer> findByBusinessInfo_RegistrationNoIgnoreCaseAndDeletedFalse(String registrationNo);

    boolean existsByEmployerCodeAndDeletedFalse(String employerCode);
}
