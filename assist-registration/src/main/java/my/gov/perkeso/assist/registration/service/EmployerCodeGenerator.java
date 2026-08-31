package my.gov.perkeso.assist.registration.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerOperationalStatus;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.EmployerStatusInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.employercode.EmployerCodeContext;
import my.gov.perkeso.assist.registration.employercode.EmployerCodeGeneratorFactory;
import my.gov.perkeso.assist.registration.exception.RegistrationBrnDuplicateException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployerCodeGenerator {

    private final EmployerRepository employerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EmployerCodeGeneratorFactory employerCodeGeneratorFactory;

    public String generateEmployerCode(final TempEmployer tempEmployer, final RegGeneralInfo regCase) {
        final RegistrationSection section = RegistrationSection.fromAssistSectionId(regCase.getSectionId());
        final EmployerCodeContext context = EmployerCodeContext.builder()
                .branchId(tempEmployer.getPksBranchId())
                .postCode(tempEmployer.getPostCode())
                .build();
        return employerCodeGeneratorFactory.generateCode(section, context);
    }

    public void assertRegistrationNoUnique(final String registrationNo) {
        if (isRegistrationNoRegistered(registrationNo)) {
            throw new RegistrationBrnDuplicateException(registrationNo);
        }
    }

    public boolean isRegistrationNoRegistered(final String registrationNo) {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration.business_info bi "
                        + "JOIN registration.employer e ON e.business_info_id = bi.id "
                        + "WHERE bi.registration_no = ? AND e.is_deleted = false",
                Integer.class, registrationNo);
        return count != null && count > 0;
    }

    public Optional<Employer> findExistingEmployerByRegistrationNo(final String registrationNo) {
        final List<Long> ids = jdbcTemplate.queryForList(
                "SELECT e.id FROM registration.business_info bi "
                        + "JOIN registration.employer e ON e.business_info_id = bi.id "
                        + "WHERE bi.registration_no = ? AND e.is_deleted = false ORDER BY e.id ASC LIMIT 1",
                Long.class, registrationNo);
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return employerRepository.findById(ids.get(0));
    }

    /**
     * For SST sales tax (1100): reuse existing employer when BRN already registered,
     * otherwise create a new employer with SST registration number as employer code.
     */
    public Employer promoteOrReuseForSstSales(final RegGeneralInfo regCase) {
        final TempEmployer tempEmployer = regCase.getTempEmployer();
        final String registrationNo = tempEmployer.getBusinessInfo().getRegistrationNo();
        final Optional<Employer> existing = findExistingEmployerByRegistrationNo(registrationNo);
        if (existing.isPresent()) {
            return existing.get();
        }
        return buildNewEmployer(tempEmployer, regCase);
    }

    public Employer promoteFromTempEmployer(final TempEmployer tempEmployer, final RegGeneralInfo regCase) {
        assertRegistrationNoUnique(tempEmployer.getBusinessInfo().getRegistrationNo());
        return buildNewEmployer(tempEmployer, regCase);
    }

    private Employer buildNewEmployer(final TempEmployer tempEmployer, final RegGeneralInfo regCase) {
        final String employerCode = generateEmployerCode(tempEmployer, regCase);

        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo(tempEmployer.getBusinessInfo().getRegistrationNo());
        businessInfo.setBusinessEntityTypeId(tempEmployer.getBusinessInfo().getBusinessEntityTypeId());

        final Employer employer = new Employer();
        employer.setEmployerCode(employerCode);
        employer.setEmployerName(tempEmployer.getEmployerName());
        employer.setBusinessInfo(businessInfo);
        employer.setServiceTypeId(tempEmployer.getServiceTypeId());
        employer.setPksBranchId(tempEmployer.getPksBranchId());
        employer.setBranch(tempEmployer.isBranch());
        employer.setMsicId(tempEmployer.getMsicId());
        employer.setMethodContributionPaymentId(tempEmployer.getMethodContributionPaymentId());
        employer.setEmployerRegisterStatus(tempEmployer.getEmployerRegisterStatus());
        employer.setEmail(tempEmployer.getEmail());
        employer.setPhone(tempEmployer.getPhone());
        employer.setContributionActive(true);
        employer.setDeleted(false);
        employer.setCreatedDate(LocalDateTime.now());
        return employer;
    }

    public EmployerStatusInfo createActiveStatus(final Long employerId) {
        final EmployerStatusInfo statusInfo = new EmployerStatusInfo();
        statusInfo.setEmployerId(employerId);
        statusInfo.setStatus(EmployerOperationalStatus.ACTIVE);
        statusInfo.setStartDate(LocalDate.now());
        statusInfo.setCurrent(true);
        statusInfo.setCreatedDate(LocalDateTime.now());
        return statusInfo;
    }
}
