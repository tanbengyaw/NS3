package my.gov.perkeso.assist.registration.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mirrors ASSIST {@code UserEmployerWs#getUserEmployerByEmailToUpdateEmployerId} on portal approve.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalUserLinkService {

    private final PortalUserRepository portalUserRepository;

    @Transactional
    public void linkEmployerOnApprove(final RegGeneralInfo regCase, final Employer employer) {
        if (regCase.getDataSourceId() == null
                || regCase.getDataSourceId() != DataSource.PORTAL.getAssistId()) {
            return;
        }
        final String email = regCase.getTempEmployer().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Portal case {} approved without email; skipping portal user link", regCase.getCaseRefNo());
            return;
        }

        portalUserRepository.findByEmailIgnoreCase(email.trim()).ifPresentOrElse(portalUser -> {
            portalUser.setEmployerId(employer.getId());
            portalUser.setEmployerCode(employer.getEmployerCode());
            portalUser.setLinkedDate(LocalDateTime.now());
            portalUserRepository.save(portalUser);
            log.info("Linked portal user {} to employer {}", portalUser.getUsername(), employer.getEmployerCode());
        }, () -> log.warn("No portal user found for email {} on case {}", email, regCase.getCaseRefNo()));
    }
}
