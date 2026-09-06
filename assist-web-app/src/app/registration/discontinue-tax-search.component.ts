import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DiscontinueTaxSearchResult } from '../core/models/registration.model';
import { ReferenceDataService } from '../core/services/reference-data.service';
import { RegistrationService } from './registration.service';

@Component({
  selector: 'assist-discontinue-tax-search',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './discontinue-tax-search.component.html',
  styleUrl: './discontinue-tax-search.component.scss',
})
export class DiscontinueTaxSearchComponent {
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

  readonly results = signal<DiscontinueTaxSearchResult[]>([]);
  readonly loading = signal(false);
  readonly starting = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly searched = signal(false);

  search(): void {
    const { taxType, search } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
    this.searched.set(true);
    this.referenceData.searchDiscontinueTaxPayers(taxType, search).subscribe({
      next: (rows) => {
        this.loading.set(false);
        this.results.set(rows);
      },
      error: (err) => {
        this.loading.set(false);
        this.results.set([]);
        this.error.set(this.errorMessage(err, 'Search failed. Check the API is running.'));
      },
    });
  }

  startDiscontinue(row: DiscontinueTaxSearchResult): void {
    this.starting.set(row.sstInfoId);
    this.error.set(null);
    this.message.set(null);
    this.registration.startDiscontinueTax(row.employerId, row.sstInfoId).subscribe({
      next: (result) => {
        this.starting.set(null);
        const caseId = result.resourceId;
        if (caseId == null) {
          this.error.set('Discontinue case was created but no case id was returned.');
          return;
        }
        void this.router.navigate(['/registration/discontinue-tax', caseId]);
      },
      error: (err) => {
        this.starting.set(null);
        this.error.set(this.errorMessage(err, 'Failed to start discontinue case.'));
      },
    });
  }

  private errorMessage(err: unknown, fallback: string): string {
    const body = (err as { error?: { message?: string; defaultUserMessage?: string } })?.error;
    return body?.defaultUserMessage ?? body?.message ?? fallback;
  }
}
