import { Component, EventEmitter, HostBinding, Input, Output, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../core/auth/auth.service';
import { FormModalComponent } from '../shared/form-modal.component';
import {
  RegistrationWorkflowAction,
  RegistrationWorkflowResult,
  buildRegistrationWorkflowResult,
} from './registration-workflow.model';
import { RegistrationService } from './registration.service';

@Component({
  selector: 'assist-registration-officer-actions',
  standalone: true,
  imports: [ReactiveFormsModule, FormModalComponent],
  templateUrl: './registration-officer-actions.component.html',
  styleUrl: './registration-officer-actions.component.scss',
})
export class RegistrationOfficerActionsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly registration = inject(RegistrationService);
  private readonly auth = inject(AuthService);

  @Input({ required: true }) caseId: number | null = null;
  @Input({ required: true }) appStatus: string | null = null;
  @Input() busy = false;
  @Input() layout: 'bar' | 'inline' = 'bar';

  @Output() workflowStarted = new EventEmitter<void>();
  @Output() workflowCompleted = new EventEmitter<RegistrationWorkflowResult>();
  @Output() workflowFailed = new EventEmitter<unknown>();

  readonly queryModalOpen = signal(false);
  readonly rejectModalOpen = signal(false);

  readonly queryForm = this.fb.nonNullable.group({
    remark: ['', Validators.required],
  });

  readonly rejectForm = this.fb.nonNullable.group({
    reason: ['', Validators.required],
  });

  @HostBinding('class.inline')
  get inlineLayout(): boolean {
    return this.layout === 'inline';
  }

  get canReview(): boolean {
    return this.isOfficer && this.appStatus === 'SUBMITTED';
  }

  get isOfficer(): boolean {
    return this.auth.hasRole('OFFICER') || this.auth.hasRole('ADMIN');
  }

  get approveLabel(): string {
    return this.layout === 'inline' ? 'Approve' : 'Approve case';
  }

  get queryLabel(): string {
    return this.layout === 'inline' ? 'Query' : 'Send to query';
  }

  get rejectLabel(): string {
    return this.layout === 'inline' ? 'Reject' : 'Reject case';
  }

  approveCase(): void {
    this.runAction('approve', () => this.registration.approveCase(this.caseId!));
  }

  openQueryModal(): void {
    this.queryForm.reset({ remark: '' });
    this.queryModalOpen.set(true);
  }

  closeQueryModal(): void {
    this.queryModalOpen.set(false);
  }

  submitQuery(): void {
    if (this.queryForm.invalid) {
      this.queryForm.markAllAsTouched();
      return;
    }
    const remark = this.queryForm.controls.remark.value.trim();
    this.runAction('query', () => this.registration.queryCase(this.caseId!, remark), { queryRemark: remark });
  }

  openRejectModal(): void {
    this.rejectForm.reset({ reason: '' });
    this.rejectModalOpen.set(true);
  }

  closeRejectModal(): void {
    this.rejectModalOpen.set(false);
  }

  submitReject(): void {
    if (this.rejectForm.invalid) {
      this.rejectForm.markAllAsTouched();
      return;
    }
    const reason = this.rejectForm.controls.reason.value.trim();
    this.runAction('reject', () => this.registration.rejectCase(this.caseId!, reason), { appStatusReason: reason });
  }

  showFieldError(control: AbstractControl | null | undefined): boolean {
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  private runAction(
    action: RegistrationWorkflowAction,
    call: () => ReturnType<RegistrationService['approveCase']>,
    extras?: { queryRemark?: string; appStatusReason?: string },
  ): void {
    if (!this.caseId || this.busy) {
      return;
    }
    this.workflowStarted.emit();
    call().subscribe({
      next: (result) => {
        if (action === 'query') {
          this.queryModalOpen.set(false);
        }
        if (action === 'reject') {
          this.rejectModalOpen.set(false);
        }
        this.workflowCompleted.emit(buildRegistrationWorkflowResult(action, result, extras));
      },
      error: (err) => this.workflowFailed.emit(err),
    });
  }
}
