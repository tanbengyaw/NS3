package my.gov.perkeso.assist.registration.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempSstInfoRepository extends JpaRepository<TempSstInfo, Long> {

    Optional<TempSstInfo> findByTempEmployerId(Long tempEmployerId);
}
