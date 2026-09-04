package my.gov.perkeso.assist.registration.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegGeneralInfoRepository extends JpaRepository<RegGeneralInfo, Long> {

    Optional<RegGeneralInfo> findByCaseRefNo(String caseRefNo);

    Optional<RegGeneralInfo> findFirstByEmployerIdOrderByUpdatedDateDesc(Long employerId);
}
