import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Employer, EmployerSearchType } from '../core/models/employer.model';
import { EmployersService } from './employers.service';

@Component({
  selector: 'assist-employers-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './employers-list.component.html',
  styleUrl: './employers-list.component.scss',
})
export class EmployersListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly employersService = inject(EmployersService);

  readonly searchTypes: { value: EmployerSearchType; label: string }[] = [
    { value: 'NAME', label: 'Name' },
    { value: 'CODE', label: 'Employer code' },
    { value: 'BRN', label: 'BRN' },
  ];

  readonly form = this.fb.nonNullable.group({
    searchType: ['NAME' as EmployerSearchType],
    searchValue: [''],
  });

  readonly employers = signal<Employer[]>([]);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.search();
  }

  search(): void {
    const { searchType, searchValue } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.employersService.search(searchType, searchValue).subscribe({
      next: (page) => {
        this.employers.set(page.pageItems);
        this.total.set(page.totalFilteredRecords);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load employers. Is the API running on port 8081?');
        this.loading.set(false);
      },
    });
  }
}
