import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { RegistrationService } from './registration.service';

@Component({
  selector: 'assist-registration-wizard',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registration-wizard.component.html',
  styleUrl: './registration-wizard.component.scss',
})
export class RegistrationWizardComponent {
  private readonly fb = inject(FormBuilder);
  private readonly registration = inject(RegistrationService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly step = signal(1);
  readonly caseId = signal<number | null>(null);
  readonly caseRefNo = signal<string | null>(null);
  readonly appStatus = signal<string | null>(null);
  readonly employerCode = signal<string | null>(null);
  readonly employees = signal<
    { id: number; employeeName: string; identificationNo: string; employmentStartDate: string }[]
  >([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);

  readonly form1 = this.fb.nonNullable.group({
    employerName: ['', Validators.required],
    registrationNo: ['', Validators.required],
    serviceTypeId: [1, Validators.required],
    pksBranchId: [2, Validators.required],
    postCode: ['50812', Validators.required],
    businessEntityTypeId: [1, Validators.required],
    email: ['', Validators.required],
    phone: [''],
    addressLine1: ['', Validators.required],
    addressLine2: [''],
  });

  readonly employeeForm = this.fb.nonNullable.group({
    employeeName: ['', Validators.required],
    identificationNo: ['', Validators.required],
    employmentStartDate: ['2024-01-01', Validators.required],
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('caseId');
    if (idParam) {
      this.loadCase(Number(idParam));
    } else {
      this.form1.patchValue({ registrationNo: this.newBrn() });
    }
  }

  get isOfficer(): boolean {
    return this.auth.hasRole('OFFICER') || this.auth.hasRole('ADMIN');
  }

  get isEditable(): boolean {
    const status = this.appStatus();
    return !status || status === 'NEW' || status === 'IN_QUERY' || status === 'IN_PROGRESS';
  }

  createDraft(): void {
    if (this.form1.invalid) {
      this.form1.markAllAsTouched();
      return;
    }
    const { businessEntityTypeId, email, phone, addressLine1, addressLine2, ...createBody } =
      this.form1.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.registration.createCase(createBody).subscribe({
      next: (result) => {
        const id = result.resourceId!;
        this.caseId.set(id);
        this.caseRefNo.set(result.resourceIdentifier);
        this.appStatus.set('NEW');
        this.registration.updateCase(id, { businessEntityTypeId, email, phone, addressLine1, addressLine2 }).subscribe({
          next: () => {
            this.loading.set(false);
            this.step.set(2);
            this.message.set(`Draft created: ${result.resourceIdentifier}`);
            void this.router.navigate(['/registration', id]);
          },
          error: (err) => this.onError(err),
        });
      },
      error: (err) => this.onError(err),
    });
  }

  saveForm1(): void {
    const id = this.caseId();
    if (!id || this.form1.invalid) {
      this.form1.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.updateCase(id, this.form1.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.message.set('Business details saved.');
      },
      error: (err) => this.onError(err),
    });
  }

  addEmployee(): void {
    const id = this.caseId();
    if (!id || this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.addEmployee(id, this.employeeForm.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.employeeForm.reset({
          employeeName: '',
          identificationNo: '',
          employmentStartDate: '2024-01-01',
        });
        this.loadEmployees(id);
        this.message.set('Employee added.');
      },
      error: (err) => this.onError(err),
    });
  }

  submitCase(): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.submitCase(id).subscribe({
      next: (result) => {
        this.loading.set(false);
        const status = String(result.changes?.['appStatus'] ?? 'SUBMITTED');
        this.appStatus.set(status);
        if (status === 'APPROVED') {
          this.employerCode.set(result.resourceIdentifier);
          this.message.set(`Approved. Employer code: ${result.resourceIdentifier}`);
        } else {
          this.message.set(`Submitted. Status: ${status}`);
        }
        this.loadCase(id);
      },
      error: (err) => this.onError(err),
    });
  }

  approveCase(): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.approveCase(id).subscribe({
      next: (result) => {
        this.loading.set(false);
        this.appStatus.set('APPROVED');
        this.employerCode.set(result.resourceIdentifier);
        this.message.set(`Approved. Employer code: ${result.resourceIdentifier}`);
        this.loadCase(id);
      },
      error: (err) => this.onError(err),
    });
  }

  goToStep(next: number): void {
    this.step.set(next);
    this.message.set(null);
  }

  private loadCase(caseId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.registration.getCase(caseId).subscribe({
      next: (c) => {
        this.loading.set(false);
        this.caseId.set(c.id);
        this.caseRefNo.set(c.caseRefNo);
        this.appStatus.set(c.appStatus);
        this.form1.patchValue({
          employerName: c.employerName ?? '',
          registrationNo: c.registrationNo ?? '',
          serviceTypeId: c.serviceTypeId ?? 1,
          pksBranchId: c.pksBranchId ?? 2,
          postCode: c.postCode ?? '',
          businessEntityTypeId: c.businessEntityTypeId ?? 1,
          email: c.email ?? '',
          phone: c.phone ?? '',
          addressLine1: c.addressLine1 ?? '',
          addressLine2: c.addressLine2 ?? '',
        });
        if (this.caseId()) {
          this.loadEmployees(caseId);
        }
      },
      error: (err) => this.onError(err),
    });
  }

  private loadEmployees(caseId: number): void {
    this.registration.listEmployees(caseId).subscribe({
      next: (list) => this.employees.set(list),
    });
  }

  private onError(err: unknown): void {
    this.loading.set(false);
    const body = (err as { error?: { message?: string; defaultUserMessage?: string } })?.error;
    this.error.set(body?.defaultUserMessage ?? body?.message ?? 'Request failed. Check API is running.');
  }

  private newBrn(): string {
    return `BRN${Date.now().toString().slice(-10)}`;
  }
}
