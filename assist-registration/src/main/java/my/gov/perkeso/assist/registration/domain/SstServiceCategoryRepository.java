package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SstServiceCategoryRepository extends JpaRepository<SstServiceCategory, Long> {

    List<SstServiceCategory> findByEmployerIdAndDeletedFalseOrderByIdAsc(Long employerId);
}
