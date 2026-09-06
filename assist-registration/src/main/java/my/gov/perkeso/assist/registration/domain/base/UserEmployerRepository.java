package my.gov.perkeso.assist.registration.domain.base;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEmployerRepository extends JpaRepository<UserEmployer, Long> {

    Optional<UserEmployer> findByUserIdAndDeletedFalse(Long userId);
}
