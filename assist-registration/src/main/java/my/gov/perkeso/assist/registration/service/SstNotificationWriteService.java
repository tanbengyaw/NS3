package my.gov.perkeso.assist.registration.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import my.gov.perkeso.assist.registration.domain.SstNotification;
import my.gov.perkeso.assist.registration.domain.SstNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SstNotificationWriteService {

    public static final String REG_SUBMITTED = "Your registration has been successfully submitted for approval.";
    public static final String REG_APPROVED = "Your registration has been successfully approved.";
    public static final String PORTAL_ENROLLMENT_SUBMITTED =
            "Your portal ID registration has been submitted for approval.";
    public static final String PORTAL_ENROLLMENT_APPROVED = "Your portal ID registration has been approved.";

    private final SstNotificationRepository sstNotificationRepository;
    private final PortalUserRepository portalUserRepository;

    @Transactional
    public void notifyPortalUser(final Long portalUserId, final String details) {
        if (portalUserId == null || details == null || details.isBlank()) {
            return;
        }
        final SstNotification notification = new SstNotification();
        notification.setPortalUserId(portalUserId);
        notification.setDetails(details.trim());
        notification.setCreatedDate(LocalDateTime.now());
        notification.setDeleted(false);
        sstNotificationRepository.save(notification);
    }

    @Transactional
    public void notifyPortalUserByUsername(final String username, final String details) {
        portalUserRepository.findByUsernameIgnoreCase(username)
                .ifPresent(user -> notifyPortalUser(user.getId(), details));
    }

    @Transactional
    public void notifyEmployerPortalUsers(final Long employerId, final String details) {
        if (employerId == null) {
            return;
        }
        for (final PortalUser portalUser : portalUserRepository.findByEmployerId(employerId)) {
            notifyPortalUser(portalUser.getId(), details);
        }
    }
}
