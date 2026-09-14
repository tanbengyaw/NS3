import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuditCaseDetail, AuditTaxType } from './audit.models';
import { AuditService } from './audit.service';

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

  readonly caseId = signal<number | null>(null);
  readonly detail = signal<AuditCaseDetail | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);

  get submitted(): boolean {
    return this.detail()?.submitted === true;
  }

  get salesOrService(): boolean {
    const taxType = this.form.controls.taxType.value;
    return taxType === 'SALES_TAX' || taxType === 'SERVICE_TAX';
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
        this.detail.set(detail);
        this.patch(detail);
        this.loading.set(false);
        if (this.submitted) {
          this.form.disable();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.defaultUserMessage || err?.error?.developerMessage || err?.error?.message || 'Failed to load audit case.');
      },
    });
  }

  save(): void {
    const id = this.caseId();
    if (id == null) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.message.set(null);
    this.audit.save(id, this.toPayload()).subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set('Draft saved.');
        this.reload();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.defaultUserMessage || err?.error?.developerMessage || err?.error?.message || 'Save failed.');
      },
    });
  }

  createCase(): void {
    const id = this.caseId();
    if (id == null) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.message.set(null);
    this.audit.submit(id, this.toPayload()).subscribe({
      next: (result) => {
        this.submitting.set(false);
        const employerId = result.changes?.['employerId'];
        this.message.set(
          `Case created${employerId ? ` — employer ${employerId}` : ''}. Incomplete auto-reg should now list this taxpayer.`,
        );
        this.reload(true);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err?.error?.defaultUserMessage || err?.error?.developerMessage || err?.error?.message || 'Create case failed.');
      },
    });
  }

  private reload(disableAfter = false): void {
    const id = this.caseId();
    if (id == null) {
      return;
    }
    this.audit.get(id).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.patch(detail);
        if (disableAfter || detail.submitted) {
          this.form.disable();
        }
      },
    });
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
