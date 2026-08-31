package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempDirectorOwnerRepository extends JpaRepository<TempDirectorOwner, Long> {

    List<TempDirectorOwner> findByRegGeneralInfoIdAndDeletedFalseOrderByIdAsc(Long regGeneralInfoId);
}
