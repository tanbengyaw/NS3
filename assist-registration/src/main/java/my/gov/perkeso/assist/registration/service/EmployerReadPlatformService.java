package my.gov.perkeso.assist.registration.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.infrastructure.service.Page;
import my.gov.perkeso.assist.core.infrastructure.service.SearchParameters;
import my.gov.perkeso.assist.registration.data.EmployerData;
import my.gov.perkeso.assist.registration.data.RegistrationCaseData;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployerReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final RegGeneralInfoRepository regGeneralInfoRepository;

    public Page<EmployerData> retrieveAll(final SearchParameters params) {
        final StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.employer_code, e.employer_name, bi.registration_no,
                       e.service_type_id, e.pks_branch_id, e.is_branch, e.msic_id,
                       e.is_contribution_active, esi.status, e.created_date
                FROM registration.employer e
                JOIN registration.business_info bi ON bi.id = e.business_info_id
                LEFT JOIN registration.employer_status_info esi ON esi.employer_id = e.id AND esi.is_current = true
                WHERE e.is_deleted = false
                """);
        final List<Object> args = new ArrayList<>();
        appendSearchFilter(sql, args, params);
        sql.append(" ORDER BY e.id DESC LIMIT ? OFFSET ?");
        args.add(params.getLimit());
        args.add(params.getOffset());

        final List<EmployerData> items = jdbcTemplate.query(sql.toString(), new EmployerRowMapper(), args.toArray());
        final int total = countEmployers(params);
        return new Page<>(total, items);
    }

    public EmployerData retrieveOne(final Long employerId) {
        final List<EmployerData> results = jdbcTemplate.query("""
                SELECT e.id, e.employer_code, e.employer_name, bi.registration_no,
                       e.service_type_id, e.pks_branch_id, e.is_branch, e.msic_id,
                       e.is_contribution_active, esi.status, e.created_date
                FROM registration.employer e
                JOIN registration.business_info bi ON bi.id = e.business_info_id
                LEFT JOIN registration.employer_status_info esi ON esi.employer_id = e.id AND esi.is_current = true
                WHERE e.is_deleted = false AND e.id = ?
                """, new EmployerRowMapper(), employerId);
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Employer not found: " + employerId);
        }
        return results.get(0);
    }

    public EmployerData retrieveByEmployerCode(final String employerCode) {
        final List<EmployerData> results = jdbcTemplate.query("""
                SELECT e.id, e.employer_code, e.employer_name, bi.registration_no,
                       e.service_type_id, e.pks_branch_id, e.is_branch, e.msic_id,
                       e.is_contribution_active, esi.status, e.created_date
                FROM registration.employer e
                JOIN registration.business_info bi ON bi.id = e.business_info_id
                LEFT JOIN registration.employer_status_info esi ON esi.employer_id = e.id AND esi.is_current = true
                WHERE e.is_deleted = false AND e.employer_code = ?
                """, new EmployerRowMapper(), employerCode);
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Employer not found: " + employerCode);
        }
        return results.get(0);
    }

    public RegistrationCaseData retrieveCaseByRefNo(final String caseRefNo) {
        final RegGeneralInfo regCase = regGeneralInfoRepository.findByCaseRefNo(caseRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Registration case not found: " + caseRefNo));
        return toCaseData(regCase);
    }

    public RegistrationCaseData retrieveCaseById(final Long caseId) {
        final RegGeneralInfo regCase = regGeneralInfoRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration case not found: " + caseId));
        return toCaseData(regCase);
    }

    private int countEmployers(final SearchParameters params) {
        final StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM registration.employer e
                JOIN registration.business_info bi ON bi.id = e.business_info_id
                WHERE e.is_deleted = false
                """);
        final List<Object> args = new ArrayList<>();
        appendSearchFilter(sql, args, params);
        final Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return count != null ? count : 0;
    }

    private void appendSearchFilter(final StringBuilder sql, final List<Object> args, final SearchParameters params) {
        if (params.getOfficeId() != null) {
            sql.append(" AND e.pks_branch_id = ?");
            args.add(params.getOfficeId());
        }
        if (params.getSearchValue() != null && !params.getSearchValue().isBlank()) {
            final String type = params.getSearchType() != null ? params.getSearchType().toUpperCase() : "NAME";
            switch (type) {
                case "CODE" -> {
                    sql.append(" AND e.employer_code ILIKE ?");
                    args.add("%" + params.getSearchValue() + "%");
                }
                case "BRN" -> {
                    sql.append(" AND bi.registration_no ILIKE ?");
                    args.add("%" + params.getSearchValue() + "%");
                }
                default -> {
                    sql.append(" AND e.employer_name ILIKE ?");
                    args.add("%" + params.getSearchValue() + "%");
                }
            }
        }
    }

    private RegistrationCaseData toCaseData(final RegGeneralInfo regCase) {
        final TempEmployer temp = regCase.getTempEmployer();
        final RegistrationSection section = RegistrationSection.fromAssistSectionId(regCase.getSectionId());
        final DataSource dataSource = DataSource.fromAssistId(regCase.getDataSourceId());
        return RegistrationCaseData.builder().id(regCase.getId()).caseRefNo(regCase.getCaseRefNo())
                .appStatus(regCase.getAppStatus().name()).appStatusReason(regCase.getAppStatusReason())
                .sectionId(regCase.getSectionId()).sectionCode(section.name())
                .dataSourceId(regCase.getDataSourceId()).dataSourceCode(dataSource.name())
                .casePksBranchId(regCase.getPksBranchId())
                .processingPksBranchId(regCase.getProcessingPksBranchId())
                .receivingPksBranchId(regCase.getReceivingPksBranchId())
                .employerName(temp.getEmployerName()).registrationNo(temp.getBusinessInfo().getRegistrationNo())
                .businessEntityTypeId(temp.getBusinessInfo().getBusinessEntityTypeId())
                .email(temp.getEmail()).phone(temp.getPhone())
                .contactPhones(temp.getContactPhones()).contactFaxes(temp.getContactFaxes())
                .addressLine1(temp.getAddressLine1()).addressLine2(temp.getAddressLine2())
                .addressLine3(temp.getAddressLine3()).stateId(temp.getStateId()).cityId(temp.getCityId())
                .cityName(temp.getCityName()).postCode(temp.getPostCode())
                .corrAddressLine1(temp.getCorrAddressLine1()).corrAddressLine2(temp.getCorrAddressLine2())
                .corrAddressLine3(temp.getCorrAddressLine3()).corrPostCode(temp.getCorrPostCode())
                .corrStateId(temp.getCorrStateId()).corrCityId(temp.getCorrCityId())
                .corrCityName(temp.getCorrCityName()).serviceTypeId(temp.getServiceTypeId())
                .pksBranchId(temp.getPksBranchId()).branch(temp.isBranch()).msicId(temp.getMsicId())
                .methodContributionPaymentId(temp.getMethodContributionPaymentId())
                .employerId(regCase.getEmployerId()).createdByUsername(regCase.getCreatedByUsername())
                .submittedByUsername(regCase.getSubmittedByUsername())
                .documentReceivedDate(regCase.getDocumentReceivedDate())
                .submissionDate(regCase.getSubmissionDate()).createdDate(regCase.getCreatedDate())
                .inqueryByUsername(regCase.getInqueryByUsername()).inqueryDate(regCase.getInqueryDate())
                .queryRemark(regCase.getQueryRemark()).build();
    }

    private static final class EmployerRowMapper implements RowMapper<EmployerData> {

        @Override
        public EmployerData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return EmployerData.builder().id(rs.getLong("id")).employerCode(rs.getString("employer_code"))
                    .employerName(rs.getString("employer_name")).registrationNo(rs.getString("registration_no"))
                    .serviceTypeId(rs.getObject("service_type_id") != null ? rs.getLong("service_type_id") : null)
                    .pksBranchId(rs.getLong("pks_branch_id")).branch(rs.getBoolean("is_branch"))
                    .msicId(rs.getObject("msic_id") != null ? rs.getLong("msic_id") : null)
                    .contributionActive(rs.getBoolean("is_contribution_active"))
                    .operationalStatus(rs.getString("status"))
                    .createdDate(rs.getTimestamp("created_date") != null
                            ? rs.getTimestamp("created_date").toLocalDateTime()
                            : null)
                    .build();
        }
    }
}
