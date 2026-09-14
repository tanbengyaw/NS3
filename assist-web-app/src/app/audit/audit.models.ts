export interface AuditCaseListing {
  id: number;
  caseRefNo: string | null;
  taskStatusId: number | null;
  taskStatusLabel: string | null;
  submitted: boolean;
  taxPayerName: string | null;
  businessRegNo: string | null;
  employerId: number | null;
}

export interface AuditTaxpayer {
  id: number | null;
  employerId: number | null;
  taxPayerName: string | null;
  businessRegNo: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  addressLine3: string | null;
  refStateId: number | null;
  refCityId: number | null;
  postcode: string | null;
  ns3BranchId: number | null;
}

export interface AuditTaxType {
  id: number | null;
  taxType: string | null;
  refTaxRegTypeId: number | null;
  selectedAudit: boolean;
  businessComDate: string | null;
  manSerComDate: string | null;
  finYrEndMon: number | null;
  annualTtlTaxSalSerVal: number | null;
  sstInfoId: number | null;
}

export interface AuditPlanning {
  id: number | null;
  refProposedCaseTypeId: number | null;
  timelineFrom: string | null;
  timelineTo: string | null;
  activitiesDetails: string | null;
  exclusions: string | null;
  limitationDisclosure: string | null;
}

export interface AuditFieldWork {
  id: number | null;
  refVisitTypeId: number | null;
  other: string | null;
  observation: string | null;
  refSiteVisitOutcomeId: number | null;
  officerRemark: string | null;
}

export interface AuditWorkingPaper {
  id: number;
  caseRefNo: string | null;
  refWorkingPaperStatusId: number | null;
  findingUsage: boolean;
  refFocusAreaId: number | null;
  proceduresDetail: string | null;
  refTestPerformedId: number | null;
  testPerformedOther: string | null;
  testDescription: string | null;
  populationSize: number | null;
  populationTotalVal: number | null;
  populationPeriodFrom: string | null;
  populationPeriodTo: string | null;
  populationDescription: string | null;
  refSamplingMethodId: number | null;
  samplingMethodOther: string | null;
  sampleCount: number | null;
  sampleValue: number | null;
  samplePeriodFrom: string | null;
  samplePeriodTo: string | null;
  samplingDetail: string | null;
  declaredAmount: number | null;
  correctAmount: number | null;
  totalErrorInSample: number | null;
  errorRate: number | null;
  errorSampleCompDetail: string | null;
  refProjectionMethodId: number | null;
  projectionJustification: string | null;
  businessStabilityConfirmed: boolean | null;
  structuralChargeDetected: boolean | null;
  refTaxTypeId: number | null;
  projectedAdjustment: number | null;
  applicableTaxRate: number | null;
  additionalTax: number | null;
  projectedTaxImpact: number | null;
  refFindingTypeId: number | null;
  findingTypeOther: string | null;
  conclution: string | null;
  assumptions: string | null;
  limitations: string | null;
  findingAnalysisRefNo: string | null;
  findingDescription: string | null;
}

export interface AuditFindings {
  id: number | null;
  summaryDetail: string | null;
  supervisorStatus: number | null;
  supervisorRemark: string | null;
}

export interface AuditCaseDetail {
  id: number;
  caseRefNo: string | null;
  taskStatusId: number | null;
  taskStatusLabel: string | null;
  submitted: boolean;
  preAuditSkip: boolean;
  refCaseSourceId: number | null;
  refRiskLevelId: number | null;
  extimatedTaxExposureRm: number | null;
  periodFrom: string | null;
  periodTo: string | null;
  objective: string | null;
  exclusions: string | null;
  justification: string | null;
  cusAudRefNo: string | null;
  refProposedCaseTypeId: number | null;
  taxpayer: AuditTaxpayer | null;
  taxTypes: AuditTaxType[];
  risk: {
    checkedRegAnomaly: boolean;
    checkedFilingBehaviour: boolean;
    checkedPaymentBehaviour: boolean;
    checkedReturnTax: boolean;
    checkedFinancialAnomaly: boolean;
    checkedThirdPartyMismatch: boolean;
    checkedHisComplianceIssue: boolean;
    checkedIntelligenceBased: boolean;
    riskAssessmentDetails: string | null;
    riskIndicatorsRemark: string | null;
  };
  focus: {
    checkedOutputTax: boolean;
    checkedInputTaxEligility: boolean;
    checkedRefund: boolean;
    checkedClassification: boolean;
    checkedExemption: boolean;
    checkedImportReconciliation: boolean;
    checkedRevenueReconciliation: boolean;
    checkedOthers: boolean;
    othersInput: string | null;
  };
  limitation: {
    checkedDataIncomplete: boolean;
    checkedAccessLimitation: boolean;
    checkedPreliminaryAssumptions: boolean;
    checkedOthers: boolean;
    othersInput: string | null;
  };
  planning: AuditPlanning | null;
  fieldWork: AuditFieldWork | null;
  workingPapers: AuditWorkingPaper[];
  findings: AuditFindings | null;
  taxpayerResponse: AuditTaxpayerResponse | null;
  previousTaxpayerResponses: AuditTaxpayerResponse[];
}

export interface AuditTaxpayerResponse {
  id: number | null;
  responseId: string | null;
  responseDate: string | null;
  responseChannel: number | null;
  responseType: number | null;
  employerId: number | null;
  taxpayerComments: string | null;
  officersStatus: number | null;
  officersFinalOutcome: number | null;
  officersRemarks: string | null;
  activated: boolean;
  revisionAmount: number | null;
  revisionReason: string | null;
  cancellationId: string | null;
}

export const AUDIT_FIELDWORK = 1101;
export const AUDIT_PENDING_APPROVAL = 1102;
export const AUDIT_APPROVED_PENDING_TAXPAYER = 1103;
export const AUDIT_UNDER_REVIEW = 1104;
export const AUDIT_CLOSED_FOR_APPEAL = 1106;
export const AUDIT_CLOSED_CASE = 1107;
export const AUDIT_FIELD_CASE_TYPE = 2;
