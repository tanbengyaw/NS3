package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempSstTariffCodeRepository extends JpaRepository<TempSstTariffCode, Long> {

    List<TempSstTariffCode> findByTempSstInfoIdAndDeletedFalseOrderByIdAsc(Long tempSstInfoId);
}
