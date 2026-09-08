package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SstNotificationRepository extends JpaRepository<SstNotification, Long> {

    List<SstNotification> findByPortalUserIdAndDeletedFalseOrderByCreatedDateDesc(Long portalUserId);
}
