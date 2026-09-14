package my.gov.perkeso.assist.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.audit.constant.AuditTaskStatus;
import my.gov.perkeso.assist.audit.domain.AuditCase;
import my.gov.perkeso.assist.audit.domain.AuditCaseRepository;
import my.gov.perkeso.assist.audit.domain.AuditCaseTaxPayerRepository;
import my.gov.perkeso.assist.audit.domain.AuditPlanningInfo;
import my.gov.perkeso.assist.audit.domain.AuditPlanningInfoRepository;
import my.gov.perkeso.assist.audit.domain.FieldWork;
import my.gov.perkeso.assist.audit.domain.FieldWorkRepository;
import my.gov.perkeso.assist.audit.domain.FindingsAnalysis;
import my.gov.perkeso.assist.audit.domain.FindingsAnalysisRepository;
import my.gov.perkeso.assist.audit.domain.TaxpaperResponse;
import my.gov.perkeso.assist.audit.domain.TaxpaperResponseRepository;
import my.gov.perkeso.assist.audit.domain.WorkingPaper;
import my.gov.perkeso.assist.audit.domain.WorkingPaperRepository;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditPostCreateWritePlatformService {

    public static final long WORKING_PAPER_DRAFT = 1L;
    public static final int SUPERVISOR_APPROVE = 1;
    public static final int SUPERVISOR_INSUFFICIENT = 2;
    public static final int RESPONSE_TYPE_ACCEPT = 1;
    public static final int RESPONSE_TYPE_DISPUTE = 2;
    public static final int RESPONSE_TYPE_APPEAL = 3;
    public static final int OUTCOME_BOD_CONFIRMED = 1;
    public static final int OUTCOME_AMENDED = 2;
    public static final int OUTCOME_CANCELLED = 3;
    public static final int OUTCOME_NO_CHANGE = 4;
    public static final int OUTCOME_ESCALATED_APPEAL = 5;

    private final AuditCaseRepository auditCaseRepository;
    private final AuditCaseTaxPayerRepository taxPayerRepository;
    private final AuditPlanningInfoRepository planningRepository;
    private final FieldWorkRepository fieldWorkRepository;
    private final WorkingPaperRepository workingPaperRepository;
    private final FindingsAnalysisRepository findingsRepository;
    private final TaxpaperResponseRepository taxpayerResponseRepository;
    private final PlatformUserContext platformUserContext;

    @Transactional
    public void ensurePlanning(final AuditCase auditCase) {
        planningRepository.findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(auditCase.getId()).orElseGet(() -> {
            final LocalDateTime now = LocalDateTime.now();
            final AuditPlanningInfo created = new AuditPlanningInfo();
            created.setAuditCaseId(auditCase.getId());
            created.setDeleted(false);
            created.setCreateDate(now);
            created.setUpdateDate(now);
            return planningRepository.save(created);
        });
    }

    @Transactional
    public CommandProcessingResult savePlanning(final Long caseId, final JsonNode node) {
        assertStaff();
        final AuditCase auditCase = requireFieldworkCase(caseId);
        ensurePlanning(auditCase);
        final AuditPlanningInfo planning = planningRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(caseId).orElseThrow();
        if (node.has("refProposedCaseTypeId")) {
            auditCase.setRefProposedCaseTypeId(longValue(node, "refProposedCaseTypeId"));
        }
        if (auditCase.getRefProposedCaseTypeId() == null) {
            auditCase.setRefProposedCaseTypeId(AuditTaskStatus.FIELD_CASE_TYPE);
        }
        planning.setTimelineFrom(dateValue(node, "timelineFrom"));
        planning.setTimelineTo(dateValue(node, "timelineTo"));
        planning.setActivitiesDetails(text(node, "activitiesDetails"));
        planning.setExclusions(text(node, "exclusions"));
        planning.setLimitationDisclosure(text(node, "limitationDisclosure"));
        planning.setUpdateDate(LocalDateTime.now());
        auditCase.setUpdateDate(LocalDateTime.now());
        planningRepository.save(planning);
        auditCaseRepository.save(auditCase);
        return CommandProcessingResult.withChanges(auditCase.getId(), auditCase.getCaseRefNo(),
                Map.of("planningId", planning.getId(), "refProposedCaseTypeId",
                        auditCase.getRefProposedCaseTypeId() == null ? 0L : auditCase.getRefProposedCaseTypeId()));
    }

    @Transactional
    public CommandProcessingResult saveFieldWork(final Long caseId, final JsonNode node) {
        assertStaff();
        final AuditCase auditCase = requireFieldworkCase(caseId);
        if (auditCase.getRefProposedCaseTypeId() != null
                && !Long.valueOf(AuditTaskStatus.FIELD_CASE_TYPE).equals(auditCase.getRefProposedCaseTypeId())) {
            throw new IllegalArgumentException("Fieldwork is only editable for Field case type.");
        }
        final FieldWork fieldWork = fieldWorkRepository.findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(caseId)
                .orElseGet(() -> {
                    final FieldWork created = new FieldWork();
                    created.setAuditCaseId(caseId);
                    created.setDeleted(false);
                    created.setCreateDate(LocalDateTime.now());
                    return created;
                });
        fieldWork.setRefVisitTypeId(longValue(node, "refVisitTypeId"));
        fieldWork.setOther(text(node, "other"));
        fieldWork.setObservation(text(node, "observation"));
        fieldWork.setRefSiteVisitOutcomeId(longValue(node, "refSiteVisitOutcomeId"));
        fieldWork.setOfficerRemark(text(node, "officerRemark"));
        fieldWork.setUpdateDate(LocalDateTime.now());
        fieldWorkRepository.save(fieldWork);
        return CommandProcessingResult.withChanges(auditCase.getId(), auditCase.getCaseRefNo(),
                Map.of("fieldWorkId", fieldWork.getId()));
    }

    @Transactional
    public CommandProcessingResult createWorkingPaper(final Long caseId) {
        assertStaff();
        final AuditCase auditCase = requireFieldworkCase(caseId);
        final LocalDateTime now = LocalDateTime.now();
        final WorkingPaper paper = new WorkingPaper();
        paper.setAuditCaseId(caseId);
        paper.setRefWorkingPaperStatusId(WORKING_PAPER_DRAFT);
        paper.setFindingUsage(false);
        paper.setDeleted(false);
        paper.setCreateDate(now);
        paper.setUpdateDate(now);
        final WorkingPaper saved = workingPaperRepository.save(paper);
        final String suffix = auditCase.getCaseRefNo() == null ? String.format("%06d", saved.getId())
                : auditCase.getCaseRefNo().replaceFirst("^AUD", "");
        saved.setCaseRefNo("WP" + suffix + "-" + saved.getId());
        workingPaperRepository.save(saved);
        return CommandProcessingResult.withChanges(saved.getId(), saved.getCaseRefNo(),
                Map.of("auditCaseId", caseId, "workingPaperId", saved.getId()));
    }

    @Transactional
    public CommandProcessingResult saveWorkingPaper(final Long caseId, final Long workingPaperId, final JsonNode node) {
        assertStaff();
        requireFieldworkCase(caseId);
        final WorkingPaper paper = workingPaperRepository.findById(workingPaperId)
                .filter(row -> !row.isDeleted() && caseId.equals(row.getAuditCaseId()))
                .orElseThrow(() -> new IllegalArgumentException("Working paper not found: " + workingPaperId));
        if (node.has("refWorkingPaperStatusId")) {
            paper.setRefWorkingPaperStatusId(longValue(node, "refWorkingPaperStatusId"));
        }
        if (node.has("findingUsage")) {
            paper.setFindingUsage(node.path("findingUsage").asBoolean(false));
        }
        paper.setRefFocusAreaId(longValue(node, "refFocusAreaId"));
        paper.setProceduresDetail(text(node, "proceduresDetail"));
        paper.setRefTestPerformedId(longValue(node, "refTestPerformedId"));
        paper.setTestPerformedOther(text(node, "testPerformedOther"));
        paper.setTestDescription(text(node, "testDescription"));
        paper.setPopulationSize(intValue(node, "populationSize"));
        paper.setPopulationTotalVal(decimalValue(node, "populationTotalVal"));
        paper.setPopulationPeriodFrom(timestampValue(node, "populationPeriodFrom"));
        paper.setPopulationPeriodTo(timestampValue(node, "populationPeriodTo"));
        paper.setPopulationDescription(text(node, "populationDescription"));
        paper.setRefSamplingMethodId(longValue(node, "refSamplingMethodId"));
        paper.setSamplingMethodOther(text(node, "samplingMethodOther"));
        paper.setSampleCount(intValue(node, "sampleCount"));
        paper.setSampleValue(decimalValue(node, "sampleValue"));
        paper.setSamplePeriodFrom(timestampValue(node, "samplePeriodFrom"));
        paper.setSamplePeriodTo(timestampValue(node, "samplePeriodTo"));
        paper.setSamplingDetail(text(node, "samplingDetail"));
        paper.setDeclaredAmount(decimalValue(node, "declaredAmount"));
        paper.setCorrectAmount(decimalValue(node, "correctAmount"));
        paper.setTotalErrorInSample(decimalValue(node, "totalErrorInSample"));
        paper.setErrorRate(decimalValue(node, "errorRate"));
        paper.setErrorSampleCompDetail(text(node, "errorSampleCompDetail"));
        paper.setRefProjectionMethodId(longValue(node, "refProjectionMethodId"));
        paper.setProjectionJustification(text(node, "projectionJustification"));
        if (node.has("businessStabilityConfirmed")) {
            paper.setBusinessStabilityConfirmed(
                    node.get("businessStabilityConfirmed").isNull() ? null
                            : node.get("businessStabilityConfirmed").asBoolean());
        }
        if (node.has("structuralChargeDetected")) {
            paper.setStructuralChargeDetected(
                    node.get("structuralChargeDetected").isNull() ? null
                            : node.get("structuralChargeDetected").asBoolean());
        }
        paper.setRefTaxTypeId(longValue(node, "refTaxTypeId"));
        paper.setProjectedAdjustment(decimalValue(node, "projectedAdjustment"));
        paper.setApplicableTaxRate(decimalValue(node, "applicableTaxRate"));
        paper.setAdditionalTax(decimalValue(node, "additionalTax"));
        paper.setProjectedTaxImpact(decimalValue(node, "projectedTaxImpact"));
        paper.setRefFindingTypeId(longValue(node, "refFindingTypeId"));
        paper.setFindingTypeOther(text(node, "findingTypeOther"));
        paper.setConclution(text(node, "conclution"));
        paper.setAssumptions(text(node, "assumptions"));
        paper.setLimitations(text(node, "limitations"));
        paper.setFindingAnalysisRefNo(text(node, "findingAnalysisRefNo"));
        paper.setFindingDescription(text(node, "findingDescription"));
        paper.setUpdateDate(LocalDateTime.now());
        workingPaperRepository.save(paper);
        return CommandProcessingResult.withChanges(paper.getId(), paper.getCaseRefNo(),
                Map.of("workingPaperId", paper.getId()));
    }

    @Transactional
    public CommandProcessingResult saveFindings(final Long caseId, final JsonNode node) {
        assertStaff();
        final AuditCase auditCase = requireCase(caseId);
        final boolean supervisor = node.path("supervisor").asBoolean(false);
        final FindingsAnalysis findings = findingsRepository.findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(caseId)
                .orElseGet(() -> {
                    final FindingsAnalysis created = new FindingsAnalysis();
                    created.setAuditCaseId(caseId);
                    created.setDeleted(false);
                    created.setCreateDate(LocalDateTime.now());
                    return created;
                });
        if (supervisor) {
            if (!Long.valueOf(AuditTaskStatus.PENDING_AUDIT_APPROVAL).equals(auditCase.getTaskStatusId())) {
                throw new IllegalArgumentException("Findings are not pending supervisor approval.");
            }
            final Integer status = intValue(node, "supervisorStatus");
            findings.setSupervisorStatus(status);
            findings.setSupervisorRemark(text(node, "supervisorRemark"));
            if (status != null && status == SUPERVISOR_APPROVE) {
                auditCase.setTaskStatusId(AuditTaskStatus.AUDIT_APPROVED_PENDING_TAXPAYER);
                ensureTaxpayerResponse(auditCase);
            } else if (status != null && status == SUPERVISOR_INSUFFICIENT) {
                auditCase.setTaskStatusId(AuditTaskStatus.FIELDWORK);
            } else {
                throw new IllegalArgumentException("supervisorStatus must be 1 (approve) or 2 (insufficient info).");
            }
        } else {
            if (!Long.valueOf(AuditTaskStatus.FIELDWORK).equals(auditCase.getTaskStatusId())
                    && !Long.valueOf(AuditTaskStatus.PENDING_AUDIT_APPROVAL).equals(auditCase.getTaskStatusId())) {
                throw new IllegalArgumentException("Findings can only be submitted from Fieldwork.");
            }
            findings.setSummaryDetail(text(node, "summaryDetail"));
            if (node.path("submit").asBoolean(false)) {
                if (findings.getSummaryDetail() == null || findings.getSummaryDetail().isBlank()) {
                    throw new IllegalArgumentException("summaryDetail is required to submit findings.");
                }
                auditCase.setTaskStatusId(AuditTaskStatus.PENDING_AUDIT_APPROVAL);
            }
        }
        findings.setUpdateDate(LocalDateTime.now());
        auditCase.setUpdateDate(LocalDateTime.now());
        findingsRepository.save(findings);
        auditCaseRepository.save(auditCase);
        return CommandProcessingResult.withChanges(auditCase.getId(), auditCase.getCaseRefNo(),
                Map.of("findingsId", findings.getId(), "taskStatusId", auditCase.getTaskStatusId()));
    }

    @Transactional
    public void ensureTaxpayerResponseIfPending(final Long caseId) {
        assertStaff();
        final AuditCase auditCase = requireCase(caseId);
        if (Long.valueOf(AuditTaskStatus.AUDIT_APPROVED_PENDING_TAXPAYER).equals(auditCase.getTaskStatusId())
                || Long.valueOf(AuditTaskStatus.UNDER_REVIEW).equals(auditCase.getTaskStatusId())) {
            ensureTaxpayerResponse(auditCase);
        }
    }

    @Transactional
    public CommandProcessingResult saveTaxpayerResponse(final Long caseId, final JsonNode node) {
        assertStaff();
        final AuditCase auditCase = requireCase(caseId);
        final TaxpaperResponse response = ensureTaxpayerResponse(auditCase);
        final boolean officer = node.path("officer").asBoolean(false);
        if (officer) {
            applyOfficerOutcome(auditCase, response, node);
        } else {
            applyTaxpayerReply(auditCase, response, node);
        }
        response.setUpdateDate(LocalDateTime.now());
        auditCase.setUpdateDate(LocalDateTime.now());
        taxpayerResponseRepository.save(response);
        auditCaseRepository.save(auditCase);
        return CommandProcessingResult.withChanges(auditCase.getId(), auditCase.getCaseRefNo(),
                Map.of("taxpayerResponseId", response.getId(), "taskStatusId", auditCase.getTaskStatusId(),
                        "responseId", response.getResponseId() == null ? "" : response.getResponseId()));
    }

    private TaxpaperResponse ensureTaxpayerResponse(final AuditCase auditCase) {
        return taxpayerResponseRepository
                .findFirstByAuditCaseIdAndActivatedTrueAndDeletedFalseOrderByIdDesc(auditCase.getId())
                .orElseGet(() -> createTaxpayerResponse(auditCase));
    }

    private TaxpaperResponse createTaxpayerResponse(final AuditCase auditCase) {
        final LocalDateTime now = LocalDateTime.now();
        final Long employerId = taxPayerRepository
                .findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(auditCase.getId())
                .map(row -> row.getEmployerId()).orElse(null);
        final TaxpaperResponse created = new TaxpaperResponse();
        created.setAuditCaseId(auditCase.getId());
        created.setEmployerId(employerId);
        created.setActivated(true);
        created.setDeleted(false);
        created.setCreateDate(now);
        created.setUpdateDate(now);
        created.setResponseId("TMP");
        final TaxpaperResponse saved = taxpayerResponseRepository.save(created);
        final String suffix = auditCase.getCaseRefNo() == null ? String.format("%06d", saved.getId())
                : auditCase.getCaseRefNo().replaceFirst("^AUD", "");
        saved.setResponseId("TXR" + suffix + "-" + saved.getId());
        return taxpayerResponseRepository.save(saved);
    }

    private void applyTaxpayerReply(final AuditCase auditCase, final TaxpaperResponse response, final JsonNode node) {
        if (!Long.valueOf(AuditTaskStatus.AUDIT_APPROVED_PENDING_TAXPAYER).equals(auditCase.getTaskStatusId())) {
            throw new IllegalArgumentException("Taxpayer response can only be recorded while pending taxpayer.");
        }
        response.setResponseChannel(intValue(node, "responseChannel"));
        response.setResponseType(intValue(node, "responseType"));
        response.setTaxpayerComments(text(node, "taxpayerComments"));
        response.setRevisionAmount(decimalValue(node, "revisionAmount"));
        response.setRevisionReason(text(node, "revisionReason"));
        final Integer type = response.getResponseType();
        if (type != null) {
            response.setOfficersStatus(type);
        }
        if (node.path("submit").asBoolean(false)) {
            if (response.getResponseChannel() == null) {
                throw new IllegalArgumentException("responseChannel is required.");
            }
            if (type == null) {
                throw new IllegalArgumentException("responseType is required.");
            }
            if (type == RESPONSE_TYPE_DISPUTE) {
                if (response.getRevisionAmount() == null) {
                    throw new IllegalArgumentException("revisionAmount is required for a dispute.");
                }
                if (response.getRevisionReason() == null || response.getRevisionReason().isBlank()) {
                    throw new IllegalArgumentException("revisionReason is required for a dispute.");
                }
            }
            response.setResponseDate(LocalDate.now());
            auditCase.setTaskStatusId(AuditTaskStatus.UNDER_REVIEW);
        }
    }

    private void applyOfficerOutcome(final AuditCase auditCase, final TaxpaperResponse response, final JsonNode node) {
        if (!Long.valueOf(AuditTaskStatus.UNDER_REVIEW).equals(auditCase.getTaskStatusId())) {
            throw new IllegalArgumentException("Officer outcome can only be recorded while Under Review.");
        }
        final Integer outcome = intValue(node, "officersFinalOutcome");
        final String remarks = text(node, "officersRemarks");
        if (outcome == null) {
            throw new IllegalArgumentException("officersFinalOutcome is required.");
        }
        if (remarks == null) {
            throw new IllegalArgumentException("officersRemarks is required.");
        }
        final Integer type = response.getResponseType();
        validateOutcomeForType(type, outcome);
        response.setOfficersFinalOutcome(outcome);
        response.setOfficersRemarks(remarks);
        if (outcome == OUTCOME_BOD_CONFIRMED) {
            auditCase.setTaskStatusId(AuditTaskStatus.CLOSED_CASE);
        } else if (outcome == OUTCOME_AMENDED) {
            response.setActivated(false);
            auditCase.setTaskStatusId(AuditTaskStatus.FIELDWORK);
        } else if (outcome == OUTCOME_CANCELLED) {
            response.setCancellationId("CAN" + (auditCase.getCaseRefNo() == null ? response.getId()
                    : auditCase.getCaseRefNo().replaceFirst("^AUD", "")) + "-" + response.getId());
            auditCase.setTaskStatusId(AuditTaskStatus.CLOSED_CASE);
        } else if (outcome == OUTCOME_NO_CHANGE) {
            response.setActivated(false);
            createTaxpayerResponse(auditCase);
            auditCase.setTaskStatusId(AuditTaskStatus.AUDIT_APPROVED_PENDING_TAXPAYER);
        } else if (outcome == OUTCOME_ESCALATED_APPEAL) {
            auditCase.setTaskStatusId(AuditTaskStatus.CLOSED_FOR_APPEAL);
        } else {
            throw new IllegalArgumentException("Unsupported officersFinalOutcome: " + outcome);
        }
    }

    private static void validateOutcomeForType(final Integer type, final int outcome) {
        if (type == null) {
            throw new IllegalArgumentException("Taxpayer response type is missing.");
        }
        final boolean allowed = switch (type) {
            case RESPONSE_TYPE_ACCEPT -> outcome == OUTCOME_BOD_CONFIRMED || outcome == OUTCOME_CANCELLED;
            case RESPONSE_TYPE_DISPUTE -> outcome == OUTCOME_AMENDED || outcome == OUTCOME_NO_CHANGE;
            case RESPONSE_TYPE_APPEAL -> outcome == OUTCOME_ESCALATED_APPEAL;
            default -> false;
        };
        if (!allowed) {
            throw new IllegalArgumentException("officersFinalOutcome is not valid for this response type.");
        }
    }

    private AuditCase requireFieldworkCase(final Long caseId) {
        final AuditCase auditCase = requireCase(caseId);
        if (!Long.valueOf(AuditTaskStatus.FIELDWORK).equals(auditCase.getTaskStatusId())) {
            throw new IllegalArgumentException("Audit case is not in Fieldwork.");
        }
        return auditCase;
    }

    private AuditCase requireCase(final Long caseId) {
        return auditCaseRepository.findById(caseId)
                .filter(row -> !row.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Audit case not found: " + caseId));
    }

    private void assertStaff() {
        if (platformUserContext.getCurrentUser().isEmployer()) {
            throw new IllegalArgumentException("Audit cases are staff-only.");
        }
    }

    private static String text(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        final String value = node.get(field).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Long longValue(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asLong();
    }

    private static Integer intValue(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asInt();
    }

    private static LocalDate dateValue(final JsonNode node, final String field) {
        final String value = text(node, field);
        return value == null ? null : LocalDate.parse(value);
    }

    private static LocalDateTime timestampValue(final JsonNode node, final String field) {
        final String value = text(node, field);
        if (value == null) {
            return null;
        }
        if (value.length() <= 10) {
            return LocalDate.parse(value).atTime(LocalTime.MIN);
        }
        return LocalDateTime.parse(value);
    }

    private static BigDecimal decimalValue(final JsonNode node, final String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).decimalValue();
    }
}
