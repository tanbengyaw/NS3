package my.gov.perkeso.assist.registration.domain.base;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempDocMappingRepository extends JpaRepository<TempDocMapping, Long> {

    List<TempDocMapping> findByTableNameAndDescriptionAndDeletedFalse(String tableName, String description);
}
