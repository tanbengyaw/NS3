package my.gov.perkeso.assist.registration.domain.base;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseDocMappingRepository extends JpaRepository<BaseDocMapping, Long> {

    List<BaseDocMapping> findByTableNameAndTablePkIdAndDeletedFalse(String tableName, Long tablePkId);
}
