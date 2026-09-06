package my.gov.perkeso.assist.registration.domain.base;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseContactRepository extends JpaRepository<BaseContact, Long> {

    List<BaseContact> findByTableNameAndTablePkIdAndDeletedFalse(String tableName, Long tablePkId);
}
