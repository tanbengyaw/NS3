import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { IncompleteAutoRegSearchResult } from '../core/models/registration.model';
import { ReferenceDataService } from '../core/services/reference-data.service';
import { registrationCaseWizardLink } from './registration-case-route.util';
import { RegistrationService } from './registration.service';

@Component({
  selector: 'assist-incomplete-auto-reg-search',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './incomplete-auto-reg-search.component.html',
  styleUrl: './incomplete-auto-reg-search.component.scss',
})
export class IncompleteAutoRegSearchComponent {
  private readonly fb = inject(FormBuilder);
  private readonly referenceData = inject(ReferenceDataService);
  private readonly registration = inject(RegistrationService);
  private readonly router = inject(Router);

  readonly taxTypes: { value: string; label: string }[] = [
    { value: 'SALES_TAX', label: 'Sales tax' },
    { value: 'SERVICE_TAX', label: 'Service tax' },
    { value: 'TOURISM_TAX', label: 'Tourism tax' },
    { value: 'DIGITAL_TAX', label: 'Digital tax' },
    { value: 'DPSP_TAX', label: 'DPSP tax' },
  ];

  readonly form = this.fb.nonNullable.group({
    taxType: ['SALES_TAX'],
    search: [''],
  });

  readonly ingestForm = this.fb.nonNullable.group({
    taxType: ['SALES_TAX'],
    employerName: ['Audit Incomplete Co'],
    registrationNo: ['201901234567'],
    postCode: ['46000'],
    pksBranchId: [3],
    addressLine1: ['Lot 1 Audit Park'],
    businessComDate: ['2024-01-01'],
    finYrEndMon: [12],
    anTotalTaxSalesVal: [250000],
    cusAudRefNo: ['AUD-001'],
  });

  readonly results = signal<IncompleteAutoRegSearchResult[]>([]);
  readonly loading = signal(false);
  readonly starting = signal<number | null>(null);
  readonly ingesting = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly searched = signal(false);

  search(): void {
    const { taxType, search } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
    this.searched.set(true);
    this.referenceData.searchIncompleteAutoRegs(taxType, search).subscribe({
      next: (rows) => {
        this.loading.set(false);
        this.results.set(rows);
      },
      error: (err) => {
        this.loading.set(false);
        this.results.set([]);
        this.error.set(this.errorMessage(err, 'Search failed. Check the API is running and restart the backend for migration 0044.'));
      },
    });
  }

  complete(row: IncompleteAutoRegSearchResult): void {
    if (row.openCaseId != null) {
      const link = registrationCaseWizardLink(row.sectionId, row.openCaseId);
      if (!link) {
        this.error.set(`Open case ${row.openCaseRefNo ?? row.openCaseId} has no wizard route for section ${row.sectionId}.`);
        return;
      }
      void this.router.navigate(link);
      return;
    }
    this.starting.set(row.sstInfoId);
    this.error.set(null);
    this.message.set(null);
    this.registration.startIncompleteAutoReg(row.sstInfoId).subscribe({
      next: (result) => {
        this.starting.set(null);
        const caseId = result.resourceId;
        if (caseId == null) {
          this.error.set('Completion case was created but no case id was returned.');
          return;
        }
        const link = registrationCaseWizardLink(row.sectionId, caseId);
        if (!link) {
          this.message.set(
            `Case ${result.resourceIdentifier ?? caseId} created, but no wizard route is mapped for section ${row.sectionId}.`,
          );
          return;
        }
        void this.router.navigate(link);
      },
      error: (err) => {
        this.starting.set(null);
        this.error.set(this.errorMessage(err, 'Failed to start completion.'));
      },
    });
  }

  simulateIngest(): void {
    const v = this.ingestForm.getRawValue();
    if (!v.employerName.trim() || !v.registrationNo.trim()) {
      this.error.set('Employer name and BRN are required to simulate audit ingest.');
      return;
    }
    this.ingesting.set(true);
    this.error.set(null);
    this.message.set(null);
    this.registration
      .ingestSstAutoRegistration({
        taxType: v.taxType,
        employerName: v.employerName.trim(),
        registrationNo: v.registrationNo.trim(),
        postCode: v.postCode.trim(),
        pksBranchId: Number(v.pksBranchId),
        addressLine1: v.addressLine1.trim() || null,
        businessComDate: v.businessComDate || null,
        finYrEndMon: Number(v.finYrEndMon) || null,
        anTotalTaxSalesVal: Number(v.anTotalTaxSalesVal) || null,
        cusAudRefNo: v.cusAudRefNo.trim() || null,
        source: 'audit',
      })
      .subscribe({
        next: (result) => {
          this.ingesting.set(false);
          this.form.patchValue({ taxType: v.taxType, search: v.registrationNo.trim() });
          this.message.set(
            `Ingested SST row ${result.resourceId}${result.changes?.['reused'] ? ' (reused existing incomplete row)' : ''}. Search to complete.`,
          );
          this.search();
        },
        error: (err) => {
          this.ingesting.set(false);
          this.error.set(this.errorMessage(err, 'Ingest failed.'));
        },
      });
  }

  private errorMessage(err: unknown, fallback: string): string {
    const body = (err as { error?: { message?: string; defaultUserMessage?: string } })?.error;
    return body?.defaultUserMessage ?? body?.message ?? fallback;
  }
}
