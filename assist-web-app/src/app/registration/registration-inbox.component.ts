import { Component, inject, signal } from '@angular/core';

import { RouterLink } from '@angular/router';

import { AuthService } from '../core/auth/auth.service';

import { RegistrationCaseSummary } from '../core/models/registration.model';
import { resolveRoutedToLabel } from './case-routing.util';
import { registrationCaseReviewLink } from './registration-case-route.util';
import { registrationSectionLabel } from './registration-section.util';

import { isSstNewRegSection } from './sst-new-reg.config';

import { RegistrationService } from './registration.service';

import {

  InboxSectionFilter,

  UO_INBOX_SECTION_FILTERS,

  inboxSectionFilterParams,

} from './uo-inbox-section.util';



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

  readonly message = signal<string | null>(null);

  readonly statusFilter = signal('SUBMITTED');

  readonly sectionFilter = signal<InboxSectionFilter>(this.defaultSectionFilter());

  readonly uoSectionFilters = UO_INBOX_SECTION_FILTERS;



  constructor() {

    this.load();

  }



  get isOfficer(): boolean {

    return this.auth.hasStaffAccess();

  }



  get isUo(): boolean {

    return this.auth.hasRole('UO');

  }



  get showSectionFilter(): boolean {

    return this.isUo || this.auth.hasRole('ADMIN');

  }



  inboxDescription(): string {

    if (this.isUo) {

      return 'Update and discontinue tax cases for your branch (sections 1103, 1200–1204).';

    }

    return 'Cases for your branch after submit (filtered by processing branch).';

  }



  private defaultSectionFilter(): InboxSectionFilter {

    return this.auth.hasRole('UO') ? 'UO' : 'ALL';

  }



  load(): void {

    this.loading.set(true);

    this.error.set(null);

    this.registration

      .listCases({

        appStatus: this.statusFilter(),

        limit: 100,

        ...inboxSectionFilterParams(this.showSectionFilter ? this.sectionFilter() : 'ALL'),

      })

      .subscribe({

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



  setSectionFilter(filter: InboxSectionFilter): void {

    this.sectionFilter.set(filter);

    this.load();

  }



  caseLink(item: RegistrationCaseSummary): string[] {

    return registrationCaseReviewLink(item.id);

  }



  caseLinkQueryParams(): { from: string } {

    return { from: 'inbox' };

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



  isSstNewReg(item: RegistrationCaseSummary): boolean {

    return isSstNewRegSection(item.sectionId);

  }



  statusClass(status: string): string {

    return `status-${status.toLowerCase().replace(/_/g, '-')}`;

  }

}


