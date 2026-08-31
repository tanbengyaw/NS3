package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SstInfoRepository extends JpaRepository<SstInfo, Long> {

    List<SstInfo> findByEmployerIdAndDeletedFalseOrderByIdAsc(Long employerId);

    List<SstInfo> findByRegGeneralInfoIdAndDeletedFalse(Long regGeneralInfoId);
}
