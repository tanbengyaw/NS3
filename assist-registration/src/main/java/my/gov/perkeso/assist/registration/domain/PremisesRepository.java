package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremisesRepository extends JpaRepository<Premises, Long> {

    List<Premises> findByEmployerIdAndDeletedFalseOrderByIdAsc(Long employerId);
}
