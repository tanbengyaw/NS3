package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SstInfoRepository extends JpaRepository<SstInfo, Long> {

    List<SstInfo> findByEmployerIdAndDeletedFalseOrderByIdAsc(Long employerId);

    Optional<SstInfo> findFirstByEmployerIdAndDeletedFalseOrderByIdDesc(Long employerId);

    List<SstInfo> findByRegGeneralInfoIdAndDeletedFalse(Long regGeneralInfoId);
}
