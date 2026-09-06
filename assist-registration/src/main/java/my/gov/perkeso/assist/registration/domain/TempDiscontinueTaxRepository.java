package my.gov.perkeso.assist.registration.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempDiscontinueTaxRepository extends JpaRepository<TempDiscontinueTax, Long> {

    Optional<TempDiscontinueTax> findByRegGeneralInfoId(Long regGeneralInfoId);
}
