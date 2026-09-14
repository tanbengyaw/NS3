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
}
