package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempSstServiceCategoryRepository extends JpaRepository<TempSstServiceCategory, Long> {

    List<TempSstServiceCategory> findByTempSstInfoIdAndDeletedFalseOrderByIdAsc(Long tempSstInfoId);
}
