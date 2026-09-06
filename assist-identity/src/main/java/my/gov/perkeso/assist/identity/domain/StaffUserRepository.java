package my.gov.perkeso.assist.identity.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByUsernameIgnoreCase(String username);

    Optional<StaffUser> findByEmailIgnoreCase(String email);

    List<StaffUser> findAllByOrderByUsernameAsc();
}
