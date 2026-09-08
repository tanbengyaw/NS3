import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import {
  SstNotificationItem,
  TaxRegistrantRegistrationInfo,
  TaxRegistrantSummary,
  TaxRegistrantTab,
} from './tax-registrant.model';
import { TaxRegistrantService } from './tax-registrant.service';

@Component({
  selector: 'assist-tax-registrant-360',
  standalone: true,
  templateUrl: './tax-registrant-360.component.html',
  styleUrl: './tax-registrant-360.component.scss',
})
export class TaxRegistrant360Component implements OnInit {
  private readonly taxRegistrant = inject(TaxRegistrantService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<TaxRegistrantTab>('notifications');
  readonly summary = signal<TaxRegistrantSummary | null>(null);
  readonly notifications = signal<SstNotificationItem[]>([]);
  readonly registrationInfo = signal<TaxRegistrantRegistrationInfo | null>(null);
  readonly companyExpanded = signal(true);
  readonly registrationTypeExpanded = signal(false);

  ngOnInit(): void {
    const tab = this.route.snapshot.queryParamMap.get('tab');
    if (tab === 'registration-info') {
      this.activeTab.set('registration-info');
    }

    this.loading.set(true);
    forkJoin({
      summary: this.taxRegistrant.getSummary(),
      notifications: this.taxRegistrant.listNotifications(),
      registrationInfo: this.taxRegistrant.getRegistrationInfo(),
    }).subscribe({
      next: ({ summary, notifications, registrationInfo }) => {
        this.summary.set(summary);
        this.notifications.set(notifications);
        this.registrationInfo.set(registrationInfo);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Unable to load Tax Registrant 360 profile.');
      },
    });
  }

  setTab(tab: TaxRegistrantTab): void {
    this.activeTab.set(tab);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: tab === 'registration-info' ? 'registration-info' : null },
      queryParamsHandling: 'merge',
    });
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value.replace('T', ' ').slice(0, 10);
    }
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }

  toggleCompanyExpanded(): void {
    this.companyExpanded.update(v => !v);
  }

  toggleRegistrationTypeExpanded(): void {
    this.registrationTypeExpanded.update(v => !v);
  }
}
