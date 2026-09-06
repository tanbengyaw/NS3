package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SstStatusInfoRepository extends JpaRepository<SstStatusInfo, Long> {

    List<SstStatusInfo> findBySstInfoIdAndDeletedFalse(Long sstInfoId);
}
