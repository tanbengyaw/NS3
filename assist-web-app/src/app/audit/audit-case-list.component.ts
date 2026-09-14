import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuditCaseListing } from './audit.models';
import { AuditService } from './audit.service';

@Component({
  selector: 'assist-audit-case-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './audit-case-list.component.html',
  styleUrl: './audit-case-list.component.scss',
})
export class AuditCaseListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly audit = inject(AuditService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    search: [''],
  });

  readonly results = signal<AuditCaseListing[]>([]);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly error = signal<string | null>(null);
  readonly searched = signal(false);

  constructor() {
    this.search();
  }

  search(): void {
    this.loading.set(true);
    this.error.set(null);
    this.searched.set(true);
    this.audit.search(this.form.controls.search.value).subscribe({
      next: (rows) => {
        this.loading.set(false);
        this.results.set(rows);
      },
      error: (err) => {
        this.loading.set(false);
        this.results.set([]);
        this.error.set(err?.error?.defaultUserMessage || err?.error?.developerMessage || err?.error?.message || 'Search failed.');
      },
    });
  }

  create(): void {
    this.creating.set(true);
    this.error.set(null);
    this.audit.createDraft().subscribe({
      next: (result) => {
        this.creating.set(false);
        if (result.resourceId == null) {
          this.error.set('Draft was created but no case id was returned.');
          return;
        }
        void this.router.navigate(['/audit', result.resourceId]);
      },
      error: (err) => {
        this.creating.set(false);
        this.error.set(err?.error?.defaultUserMessage || err?.error?.developerMessage || err?.error?.message || 'Could not create draft.');
      },
    });
  }
}
