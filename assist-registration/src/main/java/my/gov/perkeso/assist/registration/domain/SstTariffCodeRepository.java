package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SstTariffCodeRepository extends JpaRepository<SstTariffCode, Long> {

    List<SstTariffCode> findByEmployerIdAndDeletedFalseOrderByIdAsc(Long employerId);
}
