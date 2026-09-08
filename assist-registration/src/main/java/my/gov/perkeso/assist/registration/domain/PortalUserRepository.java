package my.gov.perkeso.assist.registration.domain;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByEmailIgnoreCase(String email);

    Optional<PortalUser> findByUsernameIgnoreCase(String username);

    List<PortalUser> findByEmployerId(Long employerId);
}
