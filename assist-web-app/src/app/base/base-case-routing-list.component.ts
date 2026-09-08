import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { RegistrationCase, RegistrationCaseSummary } from '../core/models/registration.model';
import { resolveRoutedToLabel } from '../registration/case-routing.util';
import { registrationCaseReviewLink } from '../registration/registration-case-route.util';
import { registrationSectionLabel } from '../registration/registration-section.util';
import { RegistrationService } from '../registration/registration.service';

type StatusFilter = 'ACTIVE' | 'SUBMITTED' | 'IN_PROGRESS' | 'IN_QUERY' | 'REJECTED' | 'APPROVED' | 'ALL';

@Component({
  selector: 'assist-base-case-routing-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './base-case-routing-list.component.html',
  styleUrl: './base-case-routing-list.component.scss',
})
export class BaseCaseRoutingListComponent {
  private readonly registration = inject(RegistrationService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly cases = signal<RegistrationCaseSummary[]>([]);
  readonly searchedCase = signal<RegistrationCase | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('ACTIVE');

  readonly searchForm = this.fb.nonNullable.group({
    caseRefNo: [''],
  });

  readonly displayedCases = computed(() => {
    const searched = this.searchedCase();
    if (searched) {
      return [this.toSummary(searched)];
    }
    return this.cases();
  });

  constructor() {
    this.load();
  }

  get isStaff(): boolean {
    return this.auth.hasStaffAccess();
  }

  load(): void {
    this.searchedCase.set(null);
    this.loading.set(true);
    this.error.set(null);

    const filter = this.statusFilter();
    if (filter === 'ACTIVE') {
      this.registration.listCases({ appStatus: 'SUBMITTED', limit: 100 }).subscribe({
        next: (submitted) => {
          this.registration.listCases({ appStatus: 'IN_PROGRESS', limit: 100 }).subscribe({
            next: (inProgress) => {
              this.registration.listCases({ appStatus: 'IN_QUERY', limit: 100 }).subscribe({
                next: (inQuery) => {
                  this.loading.set(false);
                  this.cases.set(this.mergeCases(submitted, inProgress, inQuery));
                },
                error: (err) => this.onLoadError(err),
              });
            },
            error: (err) => this.onLoadError(err),
          });
        },
        error: (err) => this.onLoadError(err),
      });
      return;
    }

    const appStatus = filter === 'ALL' ? undefined : filter;
    this.registration.listCases({ appStatus, limit: 100 }).subscribe({
      next: (items) => {
        this.loading.set(false);
        this.cases.set(items);
      },
      error: (err) => this.onLoadError(err),
    });
  }

  searchByRef(): void {
    const caseRefNo = this.searchForm.controls.caseRefNo.value.trim();
    if (!caseRefNo) {
      this.searchedCase.set(null);
      this.load();
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.cases.set([]);
    this.registration.getCaseByRefNo(caseRefNo).subscribe({
      next: (item) => {
        this.loading.set(false);
        this.searchedCase.set(item);
      },
      error: (err) => {
        this.loading.set(false);
        this.searchedCase.set(null);
        this.error.set(
          err?.error?.defaultUserMessage ?? err?.error?.message ?? err?.message ?? 'Case not found.',
        );
      },
    });
  }

  clearSearch(): void {
    this.searchForm.reset({ caseRefNo: '' });
    this.searchedCase.set(null);
    this.load();
  }

  setStatusFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    this.searchForm.reset({ caseRefNo: '' });
    this.load();
  }

  caseLink(item: RegistrationCaseSummary): string[] {
    return registrationCaseReviewLink(item.id);
  }

  caseLinkQueryParams(): { from: string } {
    return { from: 'base' };
  }

  formatWhen(value: string | null): string {
    if (!value) {
      return '—';
    }
    return value.replace('T', ' ').slice(0, 16);
  }

  sectionLabel(item: RegistrationCaseSummary): string {
    return registrationSectionLabel(item.sectionId, item.sectionCode);
  }

  routedTo(item: RegistrationCaseSummary): string {
    return resolveRoutedToLabel(item) ?? '—';
  }

  statusClass(status: string): string {
    return `status-${status.toLowerCase().replace(/_/g, '-')}`;
  }

  private mergeCases(...groups: RegistrationCaseSummary[][]): RegistrationCaseSummary[] {
    const byId = new Map<number, RegistrationCaseSummary>();
    for (const group of groups) {
      for (const item of group) {
        byId.set(item.id, item);
      }
    }
    return [...byId.values()].sort((a, b) => {
      const aDate = a.submissionDate ?? a.createdDate ?? '';
      const bDate = b.submissionDate ?? b.createdDate ?? '';
      return bDate.localeCompare(aDate);
    });
  }

  private toSummary(item: RegistrationCase): RegistrationCaseSummary {
    return {
      id: item.id,
      caseRefNo: item.caseRefNo,
      appStatus: item.appStatus,
      sectionId: item.sectionId,
      sectionCode: item.sectionCode,
      employerName: item.employerName,
      registrationNo: item.registrationNo,
      employerCode: item.employerCode,
      salesTaxSmkRegNo: item.salesTaxSmkRegNo,
      queryRemark: item.queryRemark,
      appStatusReason: item.appStatusReason,
      processingPksBranchId: null,
      processingBranchName: item.processingPksBranchName,
      submissionDate: null,
      createdDate: null,
      processingPksBranchName: item.processingPksBranchName,
      routedToRole: item.routedToRole,
      routedToUsername: item.routedToUsername,
      routedToLabel: item.routedToLabel,
    };
  }

  private onLoadError(err: unknown): void {
    this.loading.set(false);
    this.error.set(
      (err as { error?: { defaultUserMessage?: string }; message?: string })?.error?.defaultUserMessage ??
        (err as { message?: string })?.message ??
        'Failed to load cases',
    );
  }
}
