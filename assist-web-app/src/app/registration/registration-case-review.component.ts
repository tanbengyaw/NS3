import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { RegistrationCase } from '../core/models/registration.model';
import { ReferenceDataService } from '../core/services/reference-data.service';
import { RegistrationCaseStatusComponent } from './registration-case-status.component';
import {
  registrationCaseWizardLabel,
  registrationCaseWizardLink,
} from './registration-case-route.util';
import { RegistrationOfficerActionsComponent } from './registration-officer-actions.component';
import { registrationSectionLabel } from './registration-section.util';
import { RegistrationService } from './registration.service';
import { isUoWorkflowSection } from './submit-routing.util';
import { RegistrationWorkflowResult } from './registration-workflow.model';

@Component({
  selector: 'assist-registration-case-review',
  standalone: true,
  imports: [RouterLink, RegistrationCaseStatusComponent, RegistrationOfficerActionsComponent],
  templateUrl: './registration-case-review.component.html',
  styleUrl: './registration-case-review.component.scss',
})
export class RegistrationCaseReviewComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly registration = inject(RegistrationService);
  private readonly referenceData = inject(ReferenceDataService);

  readonly caseData = signal<RegistrationCase | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly branchLabels = signal<Map<number, string>>(new Map());

  readonly fromInbox = this.route.snapshot.queryParamMap.get('from') === 'inbox';

  constructor() {
    this.referenceData.listBranches().subscribe({
      next: (branches) => {
        const map = new Map<number, string>();
        for (const branch of branches) {
          map.set(branch.id, branch.label);
        }
        this.branchLabels.set(map);
      },
    });

    const caseId = Number(this.route.snapshot.paramMap.get('caseId'));
    if (!Number.isFinite(caseId) || caseId <= 0) {
      this.loading.set(false);
      this.error.set('Invalid case id.');
      return;
    }
    this.loadCase(caseId);
  }

  sectionLabel(item: RegistrationCase): string {
    return registrationSectionLabel(item.sectionId, item.sectionCode);
  }

  wizardLink(item: RegistrationCase): string[] | null {
    return registrationCaseWizardLink(item.sectionId, item.id);
  }

  wizardLabel(item: RegistrationCase): string | null {
    return registrationCaseWizardLabel(item.sectionId);
  }

  branchLabel(branchId: number | null | undefined): string {
    if (branchId == null) {
      return '—';
    }
    return this.branchLabels().get(branchId) ?? `Branch ${branchId}`;
  }

  formatAddress(item: RegistrationCase): string {
    const parts = [item.addressLine1, item.addressLine2, item.addressLine3, item.postCode].filter(Boolean);
    return parts.length ? parts.join(', ') : '—';
  }

  statusClass(status: string): string {
    return `status-${status.toLowerCase().replace(/_/g, '-')}`;
  }

  onWorkflowStarted(): void {
    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
  }

  onWorkflowCompleted(result: RegistrationWorkflowResult): void {
    this.message.set(result.message);
    const id = this.caseData()?.id;
    if (id) {
      this.loadCase(id);
    } else {
      this.loading.set(false);
    }
  }

  onWorkflowFailed(err: unknown): void {
    this.loading.set(false);
    this.error.set(
      (err as { error?: { defaultUserMessage?: string }; message?: string })?.error?.defaultUserMessage ??
        (err as { message?: string })?.message ??
        'Workflow action failed',
    );
  }

  private loadCase(caseId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.registration.getCase(caseId).subscribe({
      next: (item) => {
        this.caseData.set(item);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(
          (err as { error?: { defaultUserMessage?: string }; message?: string })?.error?.defaultUserMessage ??
            (err as { message?: string })?.message ??
            'Case not found or failed to load.',
        );
      },
    });
  }
}
