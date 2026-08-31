package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectorOwnerRepository extends JpaRepository<DirectorOwner, Long> {

    List<DirectorOwner> findByEmployerIdAndDeletedFalseOrderByIdAsc(Long employerId);
}
