package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefSstServiceTypeRepository extends JpaRepository<RefSstServiceType, Long> {

    List<RefSstServiceType> findByDeletedFalseAndCodeContainingIgnoreCaseOrderByCode(String search);
}
