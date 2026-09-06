package my.gov.perkeso.assist.registration.domain.base;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseAddressRepository extends JpaRepository<BaseAddress, Long> {

    Optional<BaseAddress> findFirstByTableNameAndTablePkIdAndDeletedFalse(String tableName, Long tablePkId);
}
