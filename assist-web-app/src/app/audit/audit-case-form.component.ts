import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  AUDIT_APPROVED_PENDING_TAXPAYER,
  AUDIT_CLOSED_CASE,
  AUDIT_CLOSED_FOR_APPEAL,
  AUDIT_FIELDWORK,
  AUDIT_FIELD_CASE_TYPE,
  AUDIT_PENDING_APPROVAL,
  AUDIT_UNDER_REVIEW,
  AuditCaseDetail,
  AuditTaxType,
  AuditTaxpayerResponse,
  AuditWorkingPaper,
} from './audit.models';
import { AuditService } from './audit.service';

type AuditWorkspaceTab = 'case' | 'planning' | 'fieldwork' | 'workingPapers' | 'findings' | 'taxpayer';

@Component({
  selector: 'assist-audit-case-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './audit-case-form.component.html',
  styleUrl: './audit-case-form.component.scss',
})
export class AuditCaseFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly audit = inject(AuditService);

  readonly taxTypeOptions = [
    { value: 'SALES_TAX', label: 'Sales tax' },
    { value: 'SERVICE_TAX', label: 'Service tax' },
    { value: 'TOURISM_TAX', label: 'Tourism tax' },
    { value: 'DIGITAL_TAX', label: 'Digital tax' },
    { value: 'DPSP_TAX', label: 'DPSP tax' },
  ];

  readonly caseSources = [
    { value: 1, label: 'Pre-Audit' },
    { value: 2, label: 'Refund' },
    { value: 3, label: 'Manual' },
    { value: 4, label: 'Directive' },
    { value: 5, label: 'Referral' },
  ];

  readonly riskLevels = [
    { value: 1, label: 'Low' },
    { value: 2, label: 'Medium' },
    { value: 3, label: 'High' },
  ];

  readonly months = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

  readonly caseTypes = [
    { value: 2, label: 'Field' },
    { value: 1, label: 'Desk' },
    { value: 3, label: 'Refund' },
    { value: 4, label: 'Special' },
  ];

  readonly visitTypes = [
    { value: 1, label: 'On-site' },
    { value: 2, label: 'Virtual' },
    { value: 3, label: 'Hybrid' },
    { value: 4, label: 'Correspondence' },
    { value: 5, label: 'Others' },
  ];

  readonly siteVisitOutcomes = [
    { value: 1, label: 'No issue' },
    { value: 2, label: 'Minor issues' },
    { value: 3, label: 'Major issues' },
    { value: 4, label: 'Escalate to enforcement' },
  ];

  readonly workingPaperStatuses = [
    { value: 1, label: 'Draft' },
    { value: 2, label: 'Completed' },
  ];

  readonly focusAreas = [
    { value: 1, label: 'Output tax' },
    { value: 2, label: 'Input tax eligibility' },
    { value: 3, label: 'Refund' },
    { value: 4, label: 'Classification' },
    { value: 5, label: 'Exemption' },
    { value: 6, label: 'Import reconciliation' },
    { value: 7, label: 'Revenue reconciliation' },
    { value: 8, label: 'Others' },
  ];

  readonly testsPerformed = [
    { value: 1, label: 'Analytical review' },
    { value: 2, label: 'Inspection' },
    { value: 3, label: 'Observation' },
    { value: 4, label: 'Inquiry' },
    { value: 5, label: 'Confirmation' },
    { value: 6, label: 'Recalculation' },
    { value: 7, label: 'Reperformance' },
    { value: 8, label: 'Scanning' },
    { value: 9, label: 'Others' },
  ];

  readonly samplingMethods = [
    { value: 1, label: 'Random' },
    { value: 2, label: 'Systematic' },
    { value: 3, label: 'Stratified' },
    { value: 4, label: 'Monetary unit' },
    { value: 5, label: 'Judgmental' },
    { value: 6, label: 'Haphazard' },
    { value: 7, label: 'Block' },
    { value: 8, label: 'Others' },
  ];

  readonly projectionMethods = [
    { value: 1, label: 'Ratio' },
    { value: 2, label: 'Difference' },
    { value: 3, label: 'Mean-per-unit' },
    { value: 4, label: 'None / not projected' },
  ];

  readonly findingTypes = [
    { value: 1, label: 'Under-declaration' },
    { value: 2, label: 'Over-claim' },
    { value: 3, label: 'Misclassification' },
    { value: 4, label: 'Exemption error' },
    { value: 5, label: 'Timing difference' },
    { value: 6, label: 'Documentation gap' },
    { value: 7, label: 'Others' },
  ];

  readonly wpTaxTypes = [
    { value: 2, label: 'Sales tax' },
    { value: 1, label: 'Service tax' },
    { value: 3, label: 'Tourism tax' },
    { value: 4, label: 'Digital tax' },
    { value: 5, label: 'DPSP tax' },
  ];

  readonly form = this.fb.nonNullable.group({
    preAuditSkip: [true],
    refCaseSourceId: [3 as number | null],
    refRiskLevelId: [2 as number | null],
    extimatedTaxExposureRm: [null as number | null],
    periodFrom: [''],
    periodTo: [''],
    objective: [''],
    exclusions: [''],
    justification: [''],
    cusAudRefNo: [''],
    taxPayerName: [''],
    businessRegNo: [''],
    addressLine1: [''],
    addressLine2: [''],
    addressLine3: [''],
    postcode: ['46000'],
    ns3BranchId: [3],
    taxType: ['SALES_TAX'],
    selectedAudit: [true],
    businessComDate: [''],
    manSerComDate: [''],
    finYrEndMon: [12 as number | null],
    annualTtlTaxSalSerVal: [null as number | null],
    checkedRegAnomaly: [false],
    checkedFilingBehaviour: [false],
    checkedPaymentBehaviour: [false],
    checkedReturnTax: [false],
    checkedFinancialAnomaly: [false],
    checkedThirdPartyMismatch: [false],
    checkedHisComplianceIssue: [false],
    checkedIntelligenceBased: [false],
    riskAssessmentDetails: [''],
    checkedOutputTax: [false],
    checkedInputTaxEligility: [false],
    checkedRefund: [false],
    checkedClassification: [false],
    checkedExemption: [false],
    checkedImportReconciliation: [false],
    checkedRevenueReconciliation: [false],
    focusOthers: [false],
    focusOthersInput: [''],
    checkedDataIncomplete: [false],
    checkedAccessLimitation: [false],
    checkedPreliminaryAssumptions: [false],
    limitationOthers: [false],
    limitationOthersInput: [''],
  });

  readonly planningForm = this.fb.nonNullable.group({
    refProposedCaseTypeId: [2 as number | null],
    timelineFrom: [''],
    timelineTo: [''],
    activitiesDetails: [''],
    exclusions: [''],
    limitationDisclosure: [''],
  });

  readonly fieldWorkForm = this.fb.nonNullable.group({
    refVisitTypeId: [null as number | null],
    other: [''],
    observation: [''],
    refSiteVisitOutcomeId: [null as number | null],
    officerRemark: [''],
  });

  readonly workingPaperForm = this.fb.nonNullable.group({
    refWorkingPaperStatusId: [1 as number | null],
    findingUsage: [false],
    refFocusAreaId: [null as number | null],
    proceduresDetail: [''],
    refTestPerformedId: [null as number | null],
    testPerformedOther: [''],
    testDescription: [''],
    populationSize: [null as number | null],
    populationTotalVal: [null as number | null],
    populationPeriodFrom: [''],
    populationPeriodTo: [''],
    populationDescription: [''],
    refSamplingMethodId: [null as number | null],
    samplingMethodOther: [''],
    sampleCount: [null as number | null],
    sampleValue: [null as number | null],
    samplePeriodFrom: [''],
    samplePeriodTo: [''],
    samplingDetail: [''],
    declaredAmount: [null as number | null],
    correctAmount: [null as number | null],
    totalErrorInSample: [null as number | null],
    errorRate: [null as number | null],
    errorSampleCompDetail: [''],
    refProjectionMethodId: [null as number | null],
    projectionJustification: [''],
    businessStabilityConfirmed: [null as boolean | null],
    structuralChargeDetected: [null as boolean | null],
    refTaxTypeId: [null as number | null],
    projectedAdjustment: [null as number | null],
    applicableTaxRate: [null as number | null],
    additionalTax: [null as number | null],
    projectedTaxImpact: [null as number | null],
    refFindingTypeId: [null as number | null],
    findingTypeOther: [''],
    conclution: [''],
    assumptions: [''],
    limitations: [''],
    findingAnalysisRefNo: [''],
    findingDescription: [''],
  });

  readonly findingsForm = this.fb.nonNullable.group({
    summaryDetail: [''],
  });

  readonly supervisorForm = this.fb.nonNullable.group({
    supervisorRemark: [''],
  });

  readonly taxpayerForm = this.fb.nonNullable.group({
    responseChannel: [2 as number | null],
    responseType: [null as number | null],
    taxpayerComments: [''],
    revisionAmount: [null as number | null],
    revisionReason: [''],
  });

  readonly officerOutcomeForm = this.fb.nonNullable.group({
    officersFinalOutcome: [null as number | null],
    officersRemarks: [''],
  });

  readonly responseChannels = [
    { value: 1, label: 'Portal' },
    { value: 2, label: 'OTC' },
    { value: 3, label: 'Email' },
  ];

  readonly responseTypes = [
    { value: 1, label: 'Accept' },
    { value: 2, label: 'Dispute' },
    { value: 3, label: 'Appeal' },
  ];

  readonly officerOutcomes = [
    { value: 1, label: 'BOD Confirmed', types: [1] },
    { value: 3, label: 'Cancelled', types: [1] },
    { value: 2, label: 'Amend', types: [2] },
    { value: 4, label: 'No Change', types: [2] },
    { value: 5, label: 'Escalated to Appeal', types: [3] },
  ];

  readonly caseId = signal<number | null>(null);
  readonly detail = signal<AuditCaseDetail | null>(null);
  readonly activeTab = signal<AuditWorkspaceTab>('case');
  readonly selectedWorkingPaperId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly showIncompleteAutoRegLink = signal(false);

  get submitted(): boolean {
    return this.detail()?.submitted === true;
  }

  get salesOrService(): boolean {
    const taxType = this.form.controls.taxType.value;
    return taxType === 'SALES_TAX' || taxType === 'SERVICE_TAX';
  }

  get inFieldwork(): boolean {
    return toId(this.detail()?.taskStatusId) === AUDIT_FIELDWORK;
  }

  get pendingApproval(): boolean {
    return toId(this.detail()?.taskStatusId) === AUDIT_PENDING_APPROVAL;
  }

  get approvedPendingTaxpayer(): boolean {
    return toId(this.detail()?.taskStatusId) === AUDIT_APPROVED_PENDING_TAXPAYER;
  }

  get underReview(): boolean {
    return toId(this.detail()?.taskStatusId) === AUDIT_UNDER_REVIEW;
  }

  get showTaxpayerTab(): boolean {
    const status = toId(this.detail()?.taskStatusId);
    return status != null && status >= AUDIT_APPROVED_PENDING_TAXPAYER;
  }

  get canEditTaxpayerReply(): boolean {
    return this.approvedPendingTaxpayer;
  }

  get canEditOfficerOutcome(): boolean {
    return this.underReview;
  }

  get disputeSelected(): boolean {
    return toId(this.taxpayerForm.controls.responseType.value) === 2;
  }

  get availableOfficerOutcomes(): { value: number; label: string }[] {
    const type = toId(this.detail()?.taxpayerResponse?.responseType) ?? toId(this.taxpayerForm.controls.responseType.value);
    return this.officerOutcomes.filter((row) => type != null && row.types.includes(type));
  }

  get previousTaxpayerResponses(): AuditTaxpayerResponse[] {
    return this.detail()?.previousTaxpayerResponses ?? [];
  }

  get proposedCaseTypeId(): number | null {
    return toId(this.detail()?.refProposedCaseTypeId)
      ?? toId(this.detail()?.planning?.refProposedCaseTypeId)
      ?? toId(this.planningForm.controls.refProposedCaseTypeId.value);
  }

  get proposedCaseTypeLabel(): string {
    const id = this.proposedCaseTypeId;
    return this.caseTypes.find((row) => row.value === id)?.label ?? 'not set';
  }

  get fieldCaseType(): boolean {
    const caseType = this.proposedCaseTypeId;
    return caseType == null || caseType === AUDIT_FIELD_CASE_TYPE;
  }

  get canEditPlanning(): boolean {
    return this.inFieldwork;
  }

  get canEditFieldWork(): boolean {
    return this.inFieldwork && this.fieldCaseType;
  }

  get canEditWorkingPapers(): boolean {
    return this.inFieldwork;
  }

  get canEditFindingsOfficer(): boolean {
    return this.inFieldwork;
  }

  get canEditSupervisor(): boolean {
    return this.pendingApproval;
  }

  get workingPapers(): AuditWorkingPaper[] {
    return this.detail()?.workingPapers ?? [];
  }

  get selectedWorkingPaper(): AuditWorkingPaper | null {
    const id = this.selectedWorkingPaperId();
    return this.workingPapers.find((paper) => paper.id === id) ?? null;
  }

  get visitTypeOther(): boolean {
    return this.fieldWorkForm.controls.refVisitTypeId.value === 5;
  }

  get testPerformedOther(): boolean {
    return this.workingPaperForm.controls.refTestPerformedId.value === 9;
  }

  get samplingMethodOther(): boolean {
    return this.workingPaperForm.controls.refSamplingMethodId.value === 8;
  }

  get findingTypeOther(): boolean {
    return this.workingPaperForm.controls.refFindingTypeId.value === 7;
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('caseId'));
    if (!id) {
      this.error.set('Missing audit case id.');
      this.loading.set(false);
      return;
    }
    this.caseId.set(id);
    this.audit.get(id).subscribe({
      next: (detail) => {
        this.applyDetail(detail);
        this.activeTab.set(this.initialTab(detail));
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(readError(err, 'Failed to load audit case.'));
      },
    });
  }

  selectTab(tab: AuditWorkspaceTab): void {
    this.activeTab.set(tab);
  }

  save(): void {
    this.runWrite((id) => this.audit.save(id, this.toPayload()), 'Draft saved.');
  }

  createCase(): void {
    const id = this.caseId();
    if (id == null) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.message.set(null);
    this.showIncompleteAutoRegLink.set(false);
    this.audit.submit(id, this.toPayload()).subscribe({
      next: (result) => {
        this.submitting.set(false);
        const employerId = result.changes?.['employerId'];
        this.message.set(
          `Case created${employerId ? ` — employer ${employerId}` : ''}. Continue on Planning — Registration can complete the planted SST row later.`,
        );
        this.showIncompleteAutoRegLink.set(true);
        this.reload('planning');
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(readError(err, 'Create case failed.'));
      },
    });
  }

  savePlanning(): void {
    const v = this.planningForm.getRawValue();
    this.persistPlanning({
      refProposedCaseTypeId: toId(v.refProposedCaseTypeId) ?? AUDIT_FIELD_CASE_TYPE,
      timelineFrom: emptyToNull(v.timelineFrom),
      timelineTo: emptyToNull(v.timelineTo),
      activitiesDetails: emptyToNull(v.activitiesDetails),
      exclusions: emptyToNull(v.exclusions),
      limitationDisclosure: emptyToNull(v.limitationDisclosure),
    }, 'Planning saved.');
  }

  useFieldCaseType(): void {
    const v = this.planningForm.getRawValue();
    this.persistPlanning({
      refProposedCaseTypeId: AUDIT_FIELD_CASE_TYPE,
      timelineFrom: emptyToNull(v.timelineFrom),
      timelineTo: emptyToNull(v.timelineTo),
      activitiesDetails: emptyToNull(v.activitiesDetails),
      exclusions: emptyToNull(v.exclusions),
      limitationDisclosure: emptyToNull(v.limitationDisclosure),
    }, 'Case type set to Field. Fieldwork is now editable.');
  }

  private persistPlanning(body: Record<string, unknown>, success: string): void {
    this.runWrite((id) => this.audit.savePlanning(id, body), success);
  }

  saveFieldWork(): void {
    const v = this.fieldWorkForm.getRawValue();
    this.runWrite(
      (id) =>
        this.audit.saveFieldWork(id, {
          refVisitTypeId: v.refVisitTypeId,
          other: emptyToNull(v.other),
          observation: emptyToNull(v.observation),
          refSiteVisitOutcomeId: v.refSiteVisitOutcomeId,
          officerRemark: emptyToNull(v.officerRemark),
        }),
      'Fieldwork saved.',
    );
  }

  addWorkingPaper(): void {
    const id = this.caseId();
    if (id == null) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.message.set(null);
    this.showIncompleteAutoRegLink.set(false);
    this.audit.createWorkingPaper(id).subscribe({
      next: (result) => {
        this.saving.set(false);
        this.message.set(`Working paper ${result.resourceIdentifier ?? ''} added.`);
        const paperId = result.resourceId ?? null;
        this.reload(undefined, paperId);
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(readError(err, 'Could not add working paper.'));
      },
    });
  }

  selectWorkingPaper(paperId: number): void {
    this.selectedWorkingPaperId.set(paperId);
    const paper = this.workingPapers.find((row) => row.id === paperId);
    if (paper) {
      this.patchWorkingPaper(paper);
      this.syncEditableState();
    }
  }

  saveWorkingPaper(): void {
    const caseId = this.caseId();
    const paperId = this.selectedWorkingPaperId();
    if (caseId == null || paperId == null) {
      return;
    }
    const v = this.workingPaperForm.getRawValue();
    this.runWrite(
      (id) =>
        this.audit.saveWorkingPaper(id, paperId, {
          refWorkingPaperStatusId: v.refWorkingPaperStatusId,
          findingUsage: v.findingUsage,
          refFocusAreaId: v.refFocusAreaId,
          proceduresDetail: emptyToNull(v.proceduresDetail),
          refTestPerformedId: v.refTestPerformedId,
          testPerformedOther: emptyToNull(v.testPerformedOther),
          testDescription: emptyToNull(v.testDescription),
          populationSize: v.populationSize,
          populationTotalVal: v.populationTotalVal,
          populationPeriodFrom: emptyToNull(v.populationPeriodFrom),
          populationPeriodTo: emptyToNull(v.populationPeriodTo),
          populationDescription: emptyToNull(v.populationDescription),
          refSamplingMethodId: v.refSamplingMethodId,
          samplingMethodOther: emptyToNull(v.samplingMethodOther),
          sampleCount: v.sampleCount,
          sampleValue: v.sampleValue,
          samplePeriodFrom: emptyToNull(v.samplePeriodFrom),
          samplePeriodTo: emptyToNull(v.samplePeriodTo),
          samplingDetail: emptyToNull(v.samplingDetail),
          declaredAmount: v.declaredAmount,
          correctAmount: v.correctAmount,
          totalErrorInSample: v.totalErrorInSample,
          errorRate: v.errorRate,
          errorSampleCompDetail: emptyToNull(v.errorSampleCompDetail),
          refProjectionMethodId: v.refProjectionMethodId,
          projectionJustification: emptyToNull(v.projectionJustification),
          businessStabilityConfirmed: v.businessStabilityConfirmed,
          structuralChargeDetected: v.structuralChargeDetected,
          refTaxTypeId: v.refTaxTypeId,
          projectedAdjustment: v.projectedAdjustment,
          applicableTaxRate: v.applicableTaxRate,
          additionalTax: v.additionalTax,
          projectedTaxImpact: v.projectedTaxImpact,
          refFindingTypeId: v.refFindingTypeId,
          findingTypeOther: emptyToNull(v.findingTypeOther),
          conclution: emptyToNull(v.conclution),
          assumptions: emptyToNull(v.assumptions),
          limitations: emptyToNull(v.limitations),
          findingAnalysisRefNo: emptyToNull(v.findingAnalysisRefNo),
          findingDescription: emptyToNull(v.findingDescription),
        }),
      'Working paper saved.',
      paperId,
    );
  }

  saveFindings(submit = false): void {
    const v = this.findingsForm.getRawValue();
    this.runWrite(
      (id) =>
        this.audit.saveFindings(id, {
          summaryDetail: emptyToNull(v.summaryDetail),
          submit,
        }),
      submit ? 'Findings submitted for approval.' : 'Findings saved.',
    );
  }

  decideFindings(supervisorStatus: 1 | 2): void {
    const v = this.supervisorForm.getRawValue();
    this.runWrite(
      (id) =>
        this.audit.saveFindings(id, {
          supervisor: true,
          supervisorStatus,
          supervisorRemark: emptyToNull(v.supervisorRemark),
        }),
      supervisorStatus === 1 ? 'Findings approved. Continue on Taxpayer response.' : 'Returned to Fieldwork — insufficient information.',
      undefined,
      supervisorStatus === 1 ? 'taxpayer' : undefined,
    );
  }

  saveTaxpayerReply(submit = false): void {
    const v = this.taxpayerForm.getRawValue();
    this.runWrite(
      (id) =>
        this.audit.saveTaxpayerResponse(id, {
          responseChannel: v.responseChannel,
          responseType: v.responseType,
          taxpayerComments: emptyToNull(v.taxpayerComments),
          revisionAmount: v.revisionAmount,
          revisionReason: emptyToNull(v.revisionReason),
          submit,
        }),
      submit ? 'Taxpayer response submitted. Officer can record the outcome.' : 'Taxpayer response saved.',
      undefined,
      'taxpayer',
    );
  }

  saveOfficerOutcome(): void {
    const v = this.officerOutcomeForm.getRawValue();
    const outcome = toId(v.officersFinalOutcome);
    const tab: AuditWorkspaceTab = outcome === 2 ? 'fieldwork' : 'taxpayer';
    this.runWrite(
      (id) =>
        this.audit.saveTaxpayerResponse(id, {
          officer: true,
          officersFinalOutcome: v.officersFinalOutcome,
          officersRemarks: emptyToNull(v.officersRemarks),
        }),
      this.officerOutcomeMessage(outcome),
      undefined,
      tab,
    );
  }

  responseTypeLabel(id: number | null | undefined): string {
    return this.responseTypes.find((row) => row.value === id)?.label ?? '—';
  }

  officerOutcomeLabel(id: number | null | undefined): string {
    return this.officerOutcomes.find((row) => row.value === id)?.label ?? '—';
  }

  officersStatusLabel(id: number | null | undefined): string {
    return this.responseTypeLabel(id);
  }

  private officerOutcomeMessage(outcome: number | null): string {
    if (outcome === 1) {
      return 'BOD confirmed. Case closed.';
    }
    if (outcome === 2) {
      return 'Outcome Amend — case returned to Fieldwork.';
    }
    if (outcome === 3) {
      return 'Findings cancelled. Case closed.';
    }
    if (outcome === 4) {
      return 'No change. Waiting for a new taxpayer response.';
    }
    if (outcome === 5) {
      return 'Escalated to appeal. Case closed for appeal.';
    }
    return 'Officer outcome saved.';
  }

  private initialTab(detail: AuditCaseDetail): AuditWorkspaceTab {
    const status = toId(detail.taskStatusId);
    if (status === AUDIT_APPROVED_PENDING_TAXPAYER || status === AUDIT_UNDER_REVIEW
        || status === AUDIT_CLOSED_CASE || status === AUDIT_CLOSED_FOR_APPEAL) {
      return 'taxpayer';
    }
    return detail.submitted ? 'planning' : 'case';
  }

  workingPaperStatusLabel(id: number | null): string {
    return this.workingPaperStatuses.find((row) => row.value === id)?.label ?? 'Draft';
  }

  compareId(a: number | null, b: number | null): boolean {
    return toId(a) === toId(b);
  }

  private runWrite(
    call: (id: number) => ReturnType<AuditService['save']>,
    success: string,
    paperId?: number | null,
    tab?: AuditWorkspaceTab,
  ): void {
    const id = this.caseId();
    if (id == null) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.message.set(null);
    this.showIncompleteAutoRegLink.set(false);
    call(id).subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set(success);
        this.reload(tab, paperId ?? this.selectedWorkingPaperId());
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(readError(err, 'Save failed.'));
      },
    });
  }

  private reload(tab?: AuditWorkspaceTab, paperId?: number | null): void {
    const id = this.caseId();
    if (id == null) {
      return;
    }
    this.audit.get(id).subscribe({
      next: (detail) => {
        if (tab) {
          this.activeTab.set(tab);
        }
        this.applyDetail(detail, paperId);
      },
    });
  }

  private applyDetail(detail: AuditCaseDetail, paperId?: number | null): void {
    this.detail.set(detail);
    this.patch(detail);
    this.patchPlanning(detail);
    this.patchFieldWork(detail);
    this.patchFindings(detail);
    const papers = detail.workingPapers ?? [];
    const selected = paperId ?? this.selectedWorkingPaperId() ?? papers[0]?.id ?? null;
    this.selectedWorkingPaperId.set(selected);
    const paper = papers.find((row) => row.id === selected);
    if (paper) {
      this.patchWorkingPaper(paper);
    } else {
      this.workingPaperForm.reset({
        refWorkingPaperStatusId: 1,
        findingUsage: false,
        businessStabilityConfirmed: null,
        structuralChargeDetected: null,
      });
    }
    this.syncEditableState();
    this.patchPlanning(detail);
    this.patchFieldWork(detail);
    this.patchFindings(detail);
    this.patchTaxpayerResponse(detail);
    if (paper) {
      this.patchWorkingPaper(paper);
    }
  }

  private syncEditableState(): void {
    if (this.submitted) {
      this.form.disable({ emitEvent: false });
    } else {
      this.form.enable({ emitEvent: false });
    }
    toggleForm(this.planningForm, this.canEditPlanning);
    toggleForm(this.fieldWorkForm, this.canEditFieldWork);
    toggleForm(this.workingPaperForm, this.canEditWorkingPapers && this.selectedWorkingPaperId() != null);
    toggleForm(this.findingsForm, this.canEditFindingsOfficer);
    toggleForm(this.supervisorForm, this.canEditSupervisor);
    toggleForm(this.taxpayerForm, this.canEditTaxpayerReply);
    toggleForm(this.officerOutcomeForm, this.canEditOfficerOutcome);
  }

  private patch(detail: AuditCaseDetail): void {
    const taxpayer = detail.taxpayer;
    const taxType: AuditTaxType | undefined = detail.taxTypes?.[0];
    this.form.patchValue({
      preAuditSkip: detail.preAuditSkip,
      refCaseSourceId: detail.refCaseSourceId,
      refRiskLevelId: detail.refRiskLevelId,
      extimatedTaxExposureRm: detail.extimatedTaxExposureRm,
      periodFrom: detail.periodFrom ?? '',
      periodTo: detail.periodTo ?? '',
      objective: detail.objective ?? '',
      exclusions: detail.exclusions ?? '',
      justification: detail.justification ?? '',
      cusAudRefNo: detail.cusAudRefNo ?? '',
      taxPayerName: taxpayer?.taxPayerName ?? '',
      businessRegNo: taxpayer?.businessRegNo ?? '',
      addressLine1: taxpayer?.addressLine1 ?? '',
      addressLine2: taxpayer?.addressLine2 ?? '',
      addressLine3: taxpayer?.addressLine3 ?? '',
      postcode: taxpayer?.postcode ?? '46000',
      ns3BranchId: taxpayer?.ns3BranchId ?? 3,
      taxType: taxType?.taxType ?? 'SALES_TAX',
      selectedAudit: taxType?.selectedAudit ?? true,
      businessComDate: taxType?.businessComDate ?? '',
      manSerComDate: taxType?.manSerComDate ?? '',
      finYrEndMon: taxType?.finYrEndMon ?? 12,
      annualTtlTaxSalSerVal: taxType?.annualTtlTaxSalSerVal ?? null,
      checkedRegAnomaly: detail.risk?.checkedRegAnomaly ?? false,
      checkedFilingBehaviour: detail.risk?.checkedFilingBehaviour ?? false,
      checkedPaymentBehaviour: detail.risk?.checkedPaymentBehaviour ?? false,
      checkedReturnTax: detail.risk?.checkedReturnTax ?? false,
      checkedFinancialAnomaly: detail.risk?.checkedFinancialAnomaly ?? false,
      checkedThirdPartyMismatch: detail.risk?.checkedThirdPartyMismatch ?? false,
      checkedHisComplianceIssue: detail.risk?.checkedHisComplianceIssue ?? false,
      checkedIntelligenceBased: detail.risk?.checkedIntelligenceBased ?? false,
      riskAssessmentDetails: detail.risk?.riskAssessmentDetails ?? '',
      checkedOutputTax: detail.focus?.checkedOutputTax ?? false,
      checkedInputTaxEligility: detail.focus?.checkedInputTaxEligility ?? false,
      checkedRefund: detail.focus?.checkedRefund ?? false,
      checkedClassification: detail.focus?.checkedClassification ?? false,
      checkedExemption: detail.focus?.checkedExemption ?? false,
      checkedImportReconciliation: detail.focus?.checkedImportReconciliation ?? false,
      checkedRevenueReconciliation: detail.focus?.checkedRevenueReconciliation ?? false,
      focusOthers: detail.focus?.checkedOthers ?? false,
      focusOthersInput: detail.focus?.othersInput ?? '',
      checkedDataIncomplete: detail.limitation?.checkedDataIncomplete ?? false,
      checkedAccessLimitation: detail.limitation?.checkedAccessLimitation ?? false,
      checkedPreliminaryAssumptions: detail.limitation?.checkedPreliminaryAssumptions ?? false,
      limitationOthers: detail.limitation?.checkedOthers ?? false,
      limitationOthersInput: detail.limitation?.othersInput ?? '',
    });
  }

  private patchPlanning(detail: AuditCaseDetail): void {
    this.planningForm.patchValue(
      {
        refProposedCaseTypeId: toId(detail.refProposedCaseTypeId) ?? toId(detail.planning?.refProposedCaseTypeId) ?? 2,
        timelineFrom: dateInput(detail.planning?.timelineFrom),
        timelineTo: dateInput(detail.planning?.timelineTo),
        activitiesDetails: detail.planning?.activitiesDetails ?? '',
        exclusions: detail.planning?.exclusions ?? '',
        limitationDisclosure: detail.planning?.limitationDisclosure ?? '',
      },
      { emitEvent: false },
    );
  }

  private patchFieldWork(detail: AuditCaseDetail): void {
    this.fieldWorkForm.patchValue({
      refVisitTypeId: detail.fieldWork?.refVisitTypeId ?? null,
      other: detail.fieldWork?.other ?? '',
      observation: detail.fieldWork?.observation ?? '',
      refSiteVisitOutcomeId: detail.fieldWork?.refSiteVisitOutcomeId ?? null,
      officerRemark: detail.fieldWork?.officerRemark ?? '',
    });
  }

  private patchWorkingPaper(paper: AuditWorkingPaper): void {
    this.workingPaperForm.patchValue({
      refWorkingPaperStatusId: paper.refWorkingPaperStatusId ?? 1,
      findingUsage: paper.findingUsage ?? false,
      refFocusAreaId: paper.refFocusAreaId,
      proceduresDetail: paper.proceduresDetail ?? '',
      refTestPerformedId: paper.refTestPerformedId,
      testPerformedOther: paper.testPerformedOther ?? '',
      testDescription: paper.testDescription ?? '',
      populationSize: paper.populationSize,
      populationTotalVal: paper.populationTotalVal,
      populationPeriodFrom: dateInput(paper.populationPeriodFrom),
      populationPeriodTo: dateInput(paper.populationPeriodTo),
      populationDescription: paper.populationDescription ?? '',
      refSamplingMethodId: paper.refSamplingMethodId,
      samplingMethodOther: paper.samplingMethodOther ?? '',
      sampleCount: paper.sampleCount,
      sampleValue: paper.sampleValue,
      samplePeriodFrom: dateInput(paper.samplePeriodFrom),
      samplePeriodTo: dateInput(paper.samplePeriodTo),
      samplingDetail: paper.samplingDetail ?? '',
      declaredAmount: paper.declaredAmount,
      correctAmount: paper.correctAmount,
      totalErrorInSample: paper.totalErrorInSample,
      errorRate: paper.errorRate,
      errorSampleCompDetail: paper.errorSampleCompDetail ?? '',
      refProjectionMethodId: paper.refProjectionMethodId,
      projectionJustification: paper.projectionJustification ?? '',
      businessStabilityConfirmed: paper.businessStabilityConfirmed,
      structuralChargeDetected: paper.structuralChargeDetected,
      refTaxTypeId: paper.refTaxTypeId,
      projectedAdjustment: paper.projectedAdjustment,
      applicableTaxRate: paper.applicableTaxRate,
      additionalTax: paper.additionalTax,
      projectedTaxImpact: paper.projectedTaxImpact,
      refFindingTypeId: paper.refFindingTypeId,
      findingTypeOther: paper.findingTypeOther ?? '',
      conclution: paper.conclution ?? '',
      assumptions: paper.assumptions ?? '',
      limitations: paper.limitations ?? '',
      findingAnalysisRefNo: paper.findingAnalysisRefNo ?? '',
      findingDescription: paper.findingDescription ?? '',
    });
  }

  private patchFindings(detail: AuditCaseDetail): void {
    this.findingsForm.patchValue({
      summaryDetail: detail.findings?.summaryDetail ?? '',
    });
    this.supervisorForm.patchValue({
      supervisorRemark: detail.findings?.supervisorRemark ?? '',
    });
  }

  private patchTaxpayerResponse(detail: AuditCaseDetail): void {
    const row = detail.taxpayerResponse;
    this.taxpayerForm.patchValue(
      {
        responseChannel: toId(row?.responseChannel) ?? 2,
        responseType: toId(row?.responseType),
        taxpayerComments: row?.taxpayerComments ?? '',
        revisionAmount: row?.revisionAmount ?? null,
        revisionReason: row?.revisionReason ?? '',
      },
      { emitEvent: false },
    );
    this.officerOutcomeForm.patchValue(
      {
        officersFinalOutcome: toId(row?.officersFinalOutcome),
        officersRemarks: row?.officersRemarks ?? '',
      },
      { emitEvent: false },
    );
  }

  private toPayload(): Record<string, unknown> {
    const v = this.form.getRawValue();
    return {
      preAuditSkip: v.preAuditSkip,
      refCaseSourceId: v.refCaseSourceId,
      refRiskLevelId: v.refRiskLevelId,
      extimatedTaxExposureRm: v.extimatedTaxExposureRm,
      periodFrom: emptyToNull(v.periodFrom),
      periodTo: emptyToNull(v.periodTo),
      objective: emptyToNull(v.objective),
      exclusions: emptyToNull(v.exclusions),
      justification: emptyToNull(v.justification),
      cusAudRefNo: emptyToNull(v.cusAudRefNo),
      taxpayer: {
        taxPayerName: emptyToNull(v.taxPayerName),
        businessRegNo: emptyToNull(v.businessRegNo),
        addressLine1: emptyToNull(v.addressLine1),
        addressLine2: emptyToNull(v.addressLine2),
        addressLine3: emptyToNull(v.addressLine3),
        postcode: emptyToNull(v.postcode),
        ns3BranchId: v.ns3BranchId,
      },
      taxTypes: [
        {
          taxType: v.taxType,
          selectedAudit: v.selectedAudit,
          businessComDate: emptyToNull(v.businessComDate),
          manSerComDate: emptyToNull(v.manSerComDate),
          finYrEndMon: v.finYrEndMon,
          annualTtlTaxSalSerVal: v.annualTtlTaxSalSerVal,
        },
      ],
      risk: {
        checkedRegAnomaly: v.checkedRegAnomaly,
        checkedFilingBehaviour: v.checkedFilingBehaviour,
        checkedPaymentBehaviour: v.checkedPaymentBehaviour,
        checkedReturnTax: v.checkedReturnTax,
        checkedFinancialAnomaly: v.checkedFinancialAnomaly,
        checkedThirdPartyMismatch: v.checkedThirdPartyMismatch,
        checkedHisComplianceIssue: v.checkedHisComplianceIssue,
        checkedIntelligenceBased: v.checkedIntelligenceBased,
        riskAssessmentDetails: emptyToNull(v.riskAssessmentDetails),
      },
      focus: {
        checkedOutputTax: v.checkedOutputTax,
        checkedInputTaxEligility: v.checkedInputTaxEligility,
        checkedRefund: v.checkedRefund,
        checkedClassification: v.checkedClassification,
        checkedExemption: v.checkedExemption,
        checkedImportReconciliation: v.checkedImportReconciliation,
        checkedRevenueReconciliation: v.checkedRevenueReconciliation,
        checkedOthers: v.focusOthers,
        othersInput: emptyToNull(v.focusOthersInput),
      },
      limitation: {
        checkedDataIncomplete: v.checkedDataIncomplete,
        checkedAccessLimitation: v.checkedAccessLimitation,
        checkedPreliminaryAssumptions: v.checkedPreliminaryAssumptions,
        checkedOthers: v.limitationOthers,
        othersInput: emptyToNull(v.limitationOthersInput),
      },
    };
  }
}

function emptyToNull(value: string | null | undefined): string | null {
  return value == null || value === '' ? null : value;
}

function toId(value: number | string | null | undefined): number | null {
  if (value == null || value === '') {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed !== 0 ? parsed : null;
}

function dateInput(value: string | null | undefined): string {
  return value ? value.substring(0, 10) : '';
}

function readError(err: { error?: { defaultUserMessage?: string; developerMessage?: string; message?: string } }, fallback: string): string {
  return err?.error?.defaultUserMessage || err?.error?.developerMessage || err?.error?.message || fallback;
}

function toggleForm(form: { enable: (opts: { emitEvent: boolean }) => void; disable: (opts: { emitEvent: boolean }) => void }, enabled: boolean): void {
  if (enabled) {
    form.enable({ emitEvent: false });
  } else {
    form.disable({ emitEvent: false });
  }
}
