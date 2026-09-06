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
import my.gov.perkeso.assist.registration.data.RegistrationCaseSummaryData;
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
    private final BranchReferenceReadPlatformService branchReferenceReadPlatformService;

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

    public List<RegistrationCaseSummaryData> retrieveCaseSummaries(final String appStatus, final Long sectionId,
            final int limit) {
        final StringBuilder sql = new StringBuilder("""
                SELECT r.id, r.case_ref_no, r.app_status, r.section_id, r.submission_date, r.created_date,
                       r.query_remark, r.app_status_reason,
                       r.submitted_by_username, r.created_by_username,
                       t.employer_name, bi.registration_no,
                       e.employer_code,
                       b.name AS processing_branch_name,
                       (SELECT s.sales_tax_smk_reg_no FROM registration.sst_info s
                        WHERE s.reg_general_info_id = r.id AND s.is_deleted = FALSE
                        ORDER BY s.id DESC LIMIT 1) AS sales_tax_smk_reg_no
                FROM registration.reg_general_info r
                JOIN registration.temp_employer t ON t.id = r.temp_employer_id
                JOIN registration.business_info bi ON bi.id = t.business_info_id
                LEFT JOIN registration.employer e ON e.id = r.employer_id AND e.is_deleted = FALSE
                LEFT JOIN reference.ref_branch b ON b.id = r.processing_pks_branch_id
                WHERE 1 = 1
                """);
        final List<Object> args = new ArrayList<>();
        if (appStatus != null && !appStatus.isBlank()) {
            sql.append(" AND r.app_status = ?");
            args.add(appStatus.trim().toUpperCase());
        }
        if (sectionId != null) {
            sql.append(" AND r.section_id = ?");
            args.add(sectionId);
        }
        sql.append(" ORDER BY COALESCE(r.submission_date, r.created_date) DESC LIMIT ?");
        args.add(Math.min(Math.max(limit, 1), 200));

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            final Long assistSectionId = rs.getObject("section_id") != null ? rs.getLong("section_id") : null;
            String sectionCode = null;
            if (assistSectionId != null) {
                try {
                    sectionCode = RegistrationSection.fromAssistSectionId(assistSectionId).name();
                } catch (IllegalArgumentException ignored) {
                    sectionCode = "SECTION_" + assistSectionId;
                }
            }
            final String rowStatus = rs.getString("app_status");
            final String processingBranchName = rs.getString("processing_branch_name");
            final RegistrationCaseRouting.Target routedTo = RegistrationCaseRouting.resolve(rowStatus, assistSectionId,
                    processingBranchName, rs.getString("submitted_by_username"), rs.getString("created_by_username"));
            return RegistrationCaseSummaryData.builder()
                    .id(rs.getLong("id"))
                    .caseRefNo(rs.getString("case_ref_no"))
                    .appStatus(rowStatus)
                    .sectionId(assistSectionId)
                    .sectionCode(sectionCode)
                    .employerName(rs.getString("employer_name"))
                    .registrationNo(rs.getString("registration_no"))
                    .employerCode(rs.getString("employer_code"))
                    .salesTaxSmkRegNo(rs.getString("sales_tax_smk_reg_no"))
                    .queryRemark(rs.getString("query_remark"))
                    .appStatusReason(rs.getString("app_status_reason"))
                    .submissionDate(rs.getTimestamp("submission_date") != null
                            ? rs.getTimestamp("submission_date").toLocalDateTime()
                            : null)
                    .createdDate(rs.getTimestamp("created_date") != null
                            ? rs.getTimestamp("created_date").toLocalDateTime()
                            : null)
                    .processingPksBranchName(processingBranchName)
                    .routedToRole(routedTo != null ? routedTo.role() : null)
                    .routedToUsername(routedTo != null ? routedTo.username() : null)
                    .routedToLabel(routedTo != null ? routedTo.label() : null)
                    .build();
        }, args.toArray());
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
        final String processingBranchName = lookupProcessingBranchName(regCase.getProcessingPksBranchId());
        final RegistrationCaseRouting.Target routedTo = RegistrationCaseRouting.resolve(regCase.getAppStatus().name(),
                regCase.getSectionId(), processingBranchName, regCase.getSubmittedByUsername(),
                regCase.getCreatedByUsername());
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
                .employerId(regCase.getEmployerId())
                .employerCode(lookupEmployerCode(regCase.getEmployerId()))
                .salesTaxSmkRegNo(lookupSalesTaxSmkRegNo(regCase.getId()))
                .createdByUsername(regCase.getCreatedByUsername())
                .submittedByUsername(regCase.getSubmittedByUsername())
                .documentReceivedDate(regCase.getDocumentReceivedDate())
                .submissionDate(regCase.getSubmissionDate()).createdDate(regCase.getCreatedDate())
                .inqueryByUsername(regCase.getInqueryByUsername()).inqueryDate(regCase.getInqueryDate())
                .queryRemark(regCase.getQueryRemark())
                .processingPksBranchName(processingBranchName)
                .routedToRole(routedTo != null ? routedTo.role() : null)
                .routedToUsername(routedTo != null ? routedTo.username() : null)
                .routedToLabel(routedTo != null ? routedTo.label() : null)
                .build();
    }

    private String lookupProcessingBranchName(final Long branchId) {
        final BranchReferenceReadPlatformService.BranchLetterData branch =
                branchReferenceReadPlatformService.retrieveBranchForLetter(branchId);
        return branch != null ? branch.getName() : null;
    }

    private String lookupEmployerCode(final Long employerId) {
        if (employerId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT employer_code FROM registration.employer
                WHERE id = ? AND is_deleted = FALSE
                """, rs -> rs.next() ? rs.getString(1) : null, employerId);
    }

    private String lookupSalesTaxSmkRegNo(final Long caseId) {
        if (caseId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT sales_tax_smk_reg_no FROM registration.sst_info
                WHERE reg_general_info_id = ? AND is_deleted = FALSE
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getString(1) : null, caseId);
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
