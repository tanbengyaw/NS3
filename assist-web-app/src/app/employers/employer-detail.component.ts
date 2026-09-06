import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Employer } from '../core/models/employer.model';
import { ReferenceDataService } from '../core/services/reference-data.service';
import { EmployersService } from './employers.service';

@Component({
  selector: 'assist-employer-detail',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './employer-detail.component.html',
  styleUrl: './employer-detail.component.scss',
})
export class EmployerDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly employersService = inject(EmployersService);
  private readonly referenceData = inject(ReferenceDataService);

  readonly employer = signal<Employer | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly branchLabels = signal<Map<number, string>>(new Map());

  readonly pageTitle = computed(() => {
    const employer = this.employer();
    return employer?.employerName ?? 'Employer';
  });

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

    const employerId = Number(this.route.snapshot.paramMap.get('employerId'));
    if (!Number.isFinite(employerId) || employerId <= 0) {
      this.loading.set(false);
      this.error.set('Invalid employer id.');
      return;
    }

    this.employersService.getById(employerId).subscribe({
      next: (item) => {
        this.employer.set(item);
        this.loading.set(false);
      },
      error: (err: { error?: { defaultUserMessage?: string }; message?: string }) => {
        this.loading.set(false);
        this.error.set(
          err?.error?.defaultUserMessage ?? err?.message ?? 'Employer not found or failed to load.',
        );
      },
    });
  }

  branchLabel(branchId: number | null | undefined): string {
    if (branchId == null) {
      return '—';
    }
    return this.branchLabels().get(branchId) ?? `Branch ${branchId}`;
  }

  formatDate(value: string | null): string {
    if (!value) {
      return '—';
    }
    return value.replace('T', ' ').slice(0, 16);
  }
}
