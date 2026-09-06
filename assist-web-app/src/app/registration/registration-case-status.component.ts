import { Component, Input, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { isUoWorkflowSection } from './submit-routing.util';

@Component({
  selector: 'assist-registration-case-status',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './registration-case-status.component.html',
  styleUrl: './registration-case-status.component.scss',
})
export class RegistrationCaseStatusComponent {
  private readonly auth = inject(AuthService);

  @Input({ required: true }) appStatus: string | null = null;
  @Input() queryRemark: string | null = null;
  @Input() appStatusReason: string | null = null;
  @Input() routedToLabel: string | null = null;
  @Input() showInboxLink = true;
  @Input() sectionId: number | null = null;

  get isOfficer(): boolean {
    return this.auth.hasStaffAccess();
  }

  get isUoOnlySection(): boolean {
    return isUoWorkflowSection(this.sectionId);
  }

  get canOfficerReview(): boolean {
    if (this.appStatus !== 'SUBMITTED') {
      return false;
    }
    // Mirrors RegistrationOfficerActionsComponent.canReview — Update Tax Payer / Discontinue Tax
    // cases route to the UO queue specifically, so only a UO can act on them.
    if (this.isUoOnlySection) {
      return this.auth.hasRole('UO');
    }
    return this.isOfficer;
  }

  get showReviewQueue(): boolean {
    return this.appStatus === 'SUBMITTED' || this.appStatus === 'IN_PROGRESS';
  }
}
