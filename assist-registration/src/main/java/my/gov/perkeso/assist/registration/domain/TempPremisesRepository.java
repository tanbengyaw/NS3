package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempPremisesRepository extends JpaRepository<TempPremises, Long> {

    List<TempPremises> findByRegGeneralInfoIdAndDeletedFalseOrderByIdAsc(Long regGeneralInfoId);
}
