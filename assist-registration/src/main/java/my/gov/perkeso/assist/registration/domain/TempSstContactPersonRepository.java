package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempSstContactPersonRepository extends JpaRepository<TempSstContactPerson, Long> {

    List<TempSstContactPerson> findByTempSstInfoIdAndDeletedFalseOrderByIdAsc(Long tempSstInfoId);
}
