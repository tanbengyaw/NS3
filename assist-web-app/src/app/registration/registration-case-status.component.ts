import { Component, Input, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

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

  get isOfficer(): boolean {
    return this.auth.hasRole('OFFICER') || this.auth.hasRole('ADMIN');
  }

  get canOfficerReview(): boolean {
    return this.isOfficer && this.appStatus === 'SUBMITTED';
  }

  get showReviewQueue(): boolean {
    return this.appStatus === 'SUBMITTED' || this.appStatus === 'IN_PROGRESS';
  }
}
