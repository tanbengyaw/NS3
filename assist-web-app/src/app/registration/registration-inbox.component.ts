import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { RegistrationCaseSummary } from '../core/models/registration.model';
import { RegistrationService } from './registration.service';

const SECTION_SALES_TAX = 1100;

@Component({
  selector: 'assist-registration-inbox',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './registration-inbox.component.html',
  styleUrl: './registration-inbox.component.scss',
})
export class RegistrationInboxComponent {
  private readonly registration = inject(RegistrationService);
  private readonly auth = inject(AuthService);

  readonly cases = signal<RegistrationCaseSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
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
}
