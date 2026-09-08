package my.gov.perkeso.assist.registration.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.registration.data.SstNotificationData;
import my.gov.perkeso.assist.registration.data.TaxRegistrantCompanyInfoData;
import my.gov.perkeso.assist.registration.data.TaxRegistrantRegistrationInfoData;
import my.gov.perkeso.assist.registration.data.TaxRegistrantRegistrationTypeRowData;
import my.gov.perkeso.assist.registration.data.TaxRegistrantSummaryData;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstNotification;
import my.gov.perkeso.assist.registration.domain.SstNotificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxRegistrantReadPlatformService {

    private final PlatformUserContext platformUserContext;
    private final PortalUserRepository portalUserRepository;
    private final EmployerRepository employerRepository;
    private final SstInfoRepository sstInfoRepository;
    private final SstNotificationRepository sstNotificationRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public TaxRegistrantSummaryData retrieveSummaryForCurrentUser() {
        final PortalUser portalUser = requireCurrentPortalUser();
        final Optional<Employer> employer = findLinkedEmployer(portalUser);
        final String taxpayerName = employer.map(Employer::getEmployerName)
                .orElse(Optional.ofNullable(portalUser.getEmployerName()).orElse("-"));
        final String sstRegistrationNo = resolvePrimarySstRegistrationNo(portalUser, employer);
        return TaxRegistrantSummaryData.builder()
                .taxpayerName(taxpayerName)
                .sstRegistrationNo(sstRegistrationNo)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SstNotificationData> listNotificationsForCurrentUser() {
        final PortalUser portalUser = requireCurrentPortalUser();
        final List<SstNotificationData> stored = sstNotificationRepository
                .findByPortalUserIdAndDeletedFalseOrderByCreatedDateDesc(portalUser.getId())
                .stream()
                .map(TaxRegistrantReadPlatformService::toNotificationData)
                .toList();
        if (!stored.isEmpty()) {
            return stored;
        }
        return deriveNotificationsFromCases(portalUser);
    }

    @Transactional(readOnly = true)
    public TaxRegistrantRegistrationInfoData retrieveRegistrationInfoForCurrentUser() {
        final PortalUser portalUser = requireCurrentPortalUser();
        final Optional<Employer> employer = findLinkedEmployer(portalUser);
        return TaxRegistrantRegistrationInfoData.builder()
                .companyInfo(buildCompanyInfo(portalUser, employer))
                .registrationTypes(buildRegistrationTypes(portalUser, employer))
                .build();
    }

    private PortalUser requireCurrentPortalUser() {
        final PlatformUser currentUser = platformUserContext.getCurrentUser();
        if (!currentUser.isEmployer()) {
            throw new IllegalStateException("Tax Registrant 360 is available to portal employers only");
        }
        return portalUserRepository.findByUsernameIgnoreCase(currentUser.username())
                .orElseThrow(() -> new ResourceNotFoundException("Portal user not found: " + currentUser.username()));
    }

    private Optional<Employer> findLinkedEmployer(final PortalUser portalUser) {
        if (portalUser.getEmployerId() == null) {
            return Optional.empty();
        }
        return employerRepository.findById(portalUser.getEmployerId()).filter(employer -> !employer.isDeleted());
    }

    private String resolvePrimarySstRegistrationNo(final PortalUser portalUser, final Optional<Employer> employer) {
        if (employer.isPresent()) {
            final List<SstInfo> sstRows = sstInfoRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(
                    employer.get().getId());
            for (final SstInfo row : sstRows) {
                final String smk = row.taxSpecificSmkRegNo();
                if (smk != null && !smk.isBlank()) {
                    return smk;
                }
            }
        }
        if (portalUser.getRegistrationNo() != null && !portalUser.getRegistrationNo().isBlank()) {
            return portalUser.getRegistrationNo();
        }
        return "-";
    }

    private TaxRegistrantCompanyInfoData buildCompanyInfo(final PortalUser portalUser,
            final Optional<Employer> employer) {
        if (employer.isPresent()) {
            final Employer linked = employer.get();
            final Long entityTypeId = linked.getBusinessInfo() != null
                    ? linked.getBusinessInfo().getBusinessEntityTypeId()
                    : null;
            final String brn = linked.getBusinessInfo() != null
                    ? linked.getBusinessInfo().getRegistrationNo()
                    : null;
            return TaxRegistrantCompanyInfoData.builder()
                    .businessType(lookupBusinessEntityName(entityTypeId))
                    .businessRegistrationNo(defaultDash(brn))
                    .registeredBusinessName(defaultDash(linked.getEmployerName()))
                    .tradeName(defaultDash(linked.getEmployerName()))
                    .premiseAddressLine1(defaultDash(portalUser.getAddressLine1()))
                    .premiseAddressLine2(emptyToNull(portalUser.getAddressLine2()))
                    .premiseAddressLine3(emptyToNull(portalUser.getAddressLine3()))
                    .telNo(formatPhone(portalUser.getPhoneCallingCode(), portalUser.getPhoneNumber()))
                    .build();
        }
        return TaxRegistrantCompanyInfoData.builder()
                .businessType("-")
                .businessRegistrationNo(defaultDash(portalUser.getRegistrationNo()))
                .registeredBusinessName(defaultDash(portalUser.getEmployerName()))
                .tradeName("-")
                .premiseAddressLine1(defaultDash(portalUser.getAddressLine1()))
                .premiseAddressLine2(emptyToNull(portalUser.getAddressLine2()))
                .premiseAddressLine3(emptyToNull(portalUser.getAddressLine3()))
                .telNo(formatPhone(portalUser.getPhoneCallingCode(), portalUser.getPhoneNumber()))
                .build();
    }

    private List<TaxRegistrantRegistrationTypeRowData> buildRegistrationTypes(final PortalUser portalUser,
            final Optional<Employer> employer) {
        if (employer.isEmpty()) {
            return List.of();
        }
        final List<TaxRegistrantRegistrationTypeRowData> rows = new ArrayList<>();
        for (final SstInfo sstInfo : sstInfoRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(
                employer.get().getId())) {
            addRegistrationTypeRow(rows, "Sales tax", sstInfo.getSalesTaxSmkRegNo(), sstInfo.getCreatedDate());
            addRegistrationTypeRow(rows, "Service tax", sstInfo.getServiceTaxSmkRegNo(), sstInfo.getCreatedDate());
            addRegistrationTypeRow(rows, "Tourism tax", sstInfo.getTourismTaxSmkRegNo(), sstInfo.getCreatedDate());
            addRegistrationTypeRow(rows, "Digital tax", sstInfo.getDigitalTaxSmkRegNo(), sstInfo.getCreatedDate());
            addRegistrationTypeRow(rows, "DPSP tax", sstInfo.getDpspTaxSmkRegNo(), sstInfo.getCreatedDate());
        }
        return rows;
    }

    private static void addRegistrationTypeRow(final List<TaxRegistrantRegistrationTypeRowData> rows,
            final String taxType, final String smk, final LocalDateTime registeredDate) {
        if (smk == null || smk.isBlank()) {
            return;
        }
        rows.add(TaxRegistrantRegistrationTypeRowData.builder()
                .taxType(taxType)
                .sstRegistrationNo(smk)
                .registeredDate(registeredDate)
                .status("Active")
                .build());
    }

    private List<SstNotificationData> deriveNotificationsFromCases(final PortalUser portalUser) {
        final String sql = """
                SELECT submission_date, updated_date, app_status
                FROM registration.reg_general_info
                WHERE (
                    LOWER(submitted_by_username) = LOWER(?)
                    OR (employer_id IS NOT NULL AND employer_id = ?)
                  )
                ORDER BY COALESCE(submission_date, updated_date, created_date) DESC
                """;
        final Long employerId = portalUser.getEmployerId();
        final List<SstNotificationData> derived = new ArrayList<>();
        long seq = 1L;
        for (final CaseNotificationRow row : jdbcTemplate.query(sql, caseNotificationRowMapper(),
                portalUser.getUsername(), employerId)) {
            if (row.submissionDate() != null && row.appStatus() != AppStatus.NEW) {
                derived.add(SstNotificationData.builder()
                        .id(seq++)
                        .details(SstNotificationWriteService.REG_SUBMITTED)
                        .createdDate(row.submissionDate())
                        .build());
            }
            if (row.appStatus() == AppStatus.APPROVED && row.updatedDate() != null) {
                derived.add(SstNotificationData.builder()
                        .id(seq++)
                        .details(SstNotificationWriteService.REG_APPROVED)
                        .createdDate(row.updatedDate())
                        .build());
            }
        }
        derived.sort(Comparator.comparing(SstNotificationData::getCreatedDate).reversed());
        return derived;
    }

    private String lookupBusinessEntityName(final Long entityTypeId) {
        if (entityTypeId == null) {
            return "-";
        }
        final List<String> names = jdbcTemplate.query("""
                SELECT name FROM reference.ref_business_entity WHERE id = ?
                """, (rs, rowNum) -> rs.getString("name"), entityTypeId);
        return names.isEmpty() ? "-" : names.get(0);
    }

    private static SstNotificationData toNotificationData(final SstNotification notification) {
        return SstNotificationData.builder()
                .id(notification.getId())
                .details(notification.getDetails())
                .createdDate(notification.getCreatedDate())
                .build();
    }

    private static RowMapper<CaseNotificationRow> caseNotificationRowMapper() {
        return (ResultSet rs, int rowNum) -> new CaseNotificationRow(
                rs.getTimestamp("submission_date") != null
                        ? rs.getTimestamp("submission_date").toLocalDateTime()
                        : null,
                rs.getTimestamp("updated_date") != null
                        ? rs.getTimestamp("updated_date").toLocalDateTime()
                        : null,
                AppStatus.valueOf(rs.getString("app_status")));
    }

    private static String defaultDash(final String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String emptyToNull(final String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String formatPhone(final String callingCode, final String number) {
        if (number == null || number.isBlank()) {
            return "-";
        }
        if (callingCode != null && !callingCode.isBlank()) {
            return callingCode + number;
        }
        return number;
    }

    private record CaseNotificationRow(LocalDateTime submissionDate, LocalDateTime updatedDate, AppStatus appStatus) {
    }
}
