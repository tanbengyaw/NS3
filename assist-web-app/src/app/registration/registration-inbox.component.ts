import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { RegistrationCaseSummary } from '../core/models/registration.model';
import { RegistrationOfficerActionsComponent } from './registration-officer-actions.component';
import { resolveRoutedToLabel } from './case-routing.util';
import { registrationSectionLabel } from './registration-section.util';
import { RegistrationService } from './registration.service';
import { RegistrationWorkflowResult } from './registration-workflow.model';

const SECTION_SALES_TAX = 1100;

@Component({
  selector: 'assist-registration-inbox',
  standalone: true,
  imports: [RouterLink, RegistrationOfficerActionsComponent],
  templateUrl: './registration-inbox.component.html',
  styleUrl: './registration-inbox.component.scss',
})
export class RegistrationInboxComponent {
  private readonly registration = inject(RegistrationService);
  private readonly auth = inject(AuthService);

  readonly cases = signal<RegistrationCaseSummary[]>([]);
  readonly loading = signal(false);
  readonly workflowCaseId = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly statusFilter = signal('SUBMITTED');

  constructor() {
    this.load();
  }

  get isOfficer(): boolean {
    return this.auth.hasRole('OFFICER') || this.auth.hasRole('ADMIN');
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.registration.listCases({ appStatus: this.statusFilter(), limit: 100 }).subscribe({
      next: (items) => {
        this.loading.set(false);
        this.cases.set(items);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.defaultUserMessage ?? err?.message ?? 'Failed to load inbox');
      },
    });
  }

  setStatusFilter(status: string): void {
    this.statusFilter.set(status);
    this.load();
  }

  caseLink(item: RegistrationCaseSummary): string[] {
    if (item.sectionId === SECTION_SALES_TAX) {
      return ['/registration/sales-tax', String(item.id)];
    }
    return ['/registration', String(item.id)];
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

  statusHint(item: RegistrationCaseSummary): string | null {
    if (item.appStatus === 'IN_QUERY' && item.queryRemark) {
      return item.queryRemark;
    }
    if (item.appStatus === 'REJECTED' && item.appStatusReason) {
      return item.appStatusReason;
    }
    return null;
  }

  isSalesTax(item: RegistrationCaseSummary): boolean {
    return item.sectionId === SECTION_SALES_TAX;
  }

  statusClass(status: string): string {
    return `status-${status.toLowerCase().replace(/_/g, '-')}`;
  }

  rowBusy(caseId: number): boolean {
    return this.loading() && this.workflowCaseId() === caseId;
  }

  onWorkflowStarted(caseId: number): void {
    this.workflowCaseId.set(caseId);
    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
  }

  onWorkflowCompleted(result: RegistrationWorkflowResult): void {
    this.loading.set(false);
    this.workflowCaseId.set(null);
    this.message.set(result.message);
    this.load();
  }

  onWorkflowFailed(err: unknown): void {
    this.loading.set(false);
    this.workflowCaseId.set(null);
    this.error.set(
      (err as { error?: { defaultUserMessage?: string }; message?: string })?.error?.defaultUserMessage ??
        (err as { message?: string })?.message ??
        'Workflow action failed',
    );
  }
}
