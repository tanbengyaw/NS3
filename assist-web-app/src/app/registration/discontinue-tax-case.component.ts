import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RegistrationCase, DiscontinueTaxInfo, TempSstSupportingDocument } from '../core/models/registration.model';
import { SupportingDocumentTypeOption } from '../core/models/reference.model';
import { ReferenceDataService } from '../core/services/reference-data.service';
import { ConfirmDialogComponent } from '../shared/confirm-dialog.component';
import { RegistrationCaseStatusComponent } from './registration-case-status.component';
import { RegistrationOfficerActionsComponent } from './registration-officer-actions.component';
import { RegistrationWorkflowResult } from './registration-workflow.model';
import { RegistrationService } from './registration.service';

/** SstStatus.ACTIVE / SstStatus.CANCEL */
const STATUS_ACTIVE = 1;
const STATUS_CANCEL = 2;

@Component({
  selector: 'assist-discontinue-tax-case',
  standalone: true,
  imports: [ReactiveFormsModule, RegistrationCaseStatusComponent, RegistrationOfficerActionsComponent, ConfirmDialogComponent],
  templateUrl: './discontinue-tax-case.component.html',
  styleUrl: './discontinue-tax-case.component.scss',
})
export class DiscontinueTaxCaseComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly registration = inject(RegistrationService);
  private readonly referenceData = inject(ReferenceDataService);

  readonly statusOptions = [
    { value: STATUS_ACTIVE, label: 'Active' },
    { value: STATUS_CANCEL, label: 'Cancelled' },
  ];

  readonly caseId = signal<number | null>(null);
  readonly caseRefNo = signal<string | null>(null);
  readonly appStatus = signal<string | null>(null);
  readonly queryRemark = signal<string | null>(null);
  readonly appStatusReason = signal<string | null>(null);

  readonly employerName = signal<string>('');
  readonly registrationNo = signal<string>('');
  readonly addressLine1 = signal<string>('');

  readonly discontinueInfo = signal<DiscontinueTaxInfo | null>(null);
  readonly supportingDocuments = signal<TempSstSupportingDocument[]>([]);
  readonly supportingDocumentTypes = signal<SupportingDocumentTypeOption[]>([]);
  readonly selectedDocumentTypeId = signal<number | null>(null);
  readonly pendingDeleteDocumentId = signal<number | null>(null);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    newSstStatusId: [STATUS_CANCEL, Validators.required],
    cessationTaxEffectiveFrom: [''],
  });

  readonly isEditable = computed(() => {
    const status = this.appStatus();
    return status === 'NEW' || status === 'IN_QUERY' || status === 'IN_PROGRESS';
  });

  // Plain methods, not computed() signals — this.form is a reactive-forms FormGroup, not itself a
  // signal, so a computed() reading form.getRawValue()/control.value would memoize its first
  // evaluation and never recompute on later form changes (no signal dependency to invalidate it).
  // Template method calls are re-evaluated every change-detection cycle, which is what we want here.
  showCessationDate(): boolean {
    return this.form.controls.newSstStatusId.value === STATUS_CANCEL;
  }

  previewIssues(): string[] {
    const issues: string[] = [];
    const raw = this.form.getRawValue();
    if (!raw.newSstStatusId) {
      issues.push('Select the new tax status.');
    }
    if (raw.newSstStatusId === STATUS_CANCEL && !raw.cessationTaxEffectiveFrom) {
      issues.push('Cessation-effective date is required when cancelling.');
    }
    return issues;
  }

  canSubmit(): boolean {
    return this.isEditable() && this.previewIssues().length === 0;
  }

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('caseId');
    const id = idParam ? Number(idParam) : null;
    if (id) {
      this.caseId.set(id);
      this.loadAll(id);
    }
    this.referenceData.listSupportingDocumentTypes().subscribe({
      next: (items) => {
        this.supportingDocumentTypes.set(items);
        if (items.length > 0 && this.selectedDocumentTypeId() == null) {
          this.selectedDocumentTypeId.set(items[0].id);
        }
      },
    });
  }

  save(): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    const raw = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.registration
      .upsertDiscontinueInfo(id, {
        newSstStatusId: raw.newSstStatusId,
        cessationTaxEffectiveFrom: raw.cessationTaxEffectiveFrom || null,
      })
      .subscribe({
        next: (info) => {
          this.loading.set(false);
          this.discontinueInfo.set(info);
          this.message.set('Saved.');
        },
        error: (err) => this.onError(err),
      });
  }

  submitCase(): void {
    const id = this.caseId();
    if (!id || !this.canSubmit()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.submitCase(id).subscribe({
      next: (result) => {
        this.loading.set(false);
        const status = (result.changes as { appStatus?: string } | null)?.appStatus;
        if (status) {
          this.appStatus.set(status);
        }
        this.message.set('Submitted.');
      },
      error: (err) => this.onError(err),
    });
  }

  onOfficerWorkflowStarted(): void {
    this.loading.set(true);
    this.error.set(null);
  }

  onOfficerWorkflowCompleted(result: RegistrationWorkflowResult): void {
    this.loading.set(false);
    this.appStatus.set(result.appStatus);
    if (result.queryRemark) {
      this.queryRemark.set(result.queryRemark);
    }
    if (result.appStatusReason) {
      this.appStatusReason.set(result.appStatusReason);
    }
    this.message.set(`Case ${result.appStatus.toLowerCase()}.`);
  }

  onOfficerWorkflowFailed(err: unknown): void {
    this.onError(err);
  }

  onSupportingDocumentFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    const typeId = this.selectedDocumentTypeId();
    const id = this.caseId();
    if (!id || typeId == null) {
      this.error.set('Select a document type before uploading.');
      input.value = '';
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.uploadSupportingDocument(id, typeId, file).subscribe({
      next: () => {
        this.loading.set(false);
        input.value = '';
        this.loadSupportingDocuments(id);
        this.message.set(`Uploaded ${file.name}.`);
      },
      error: (err) => {
        input.value = '';
        this.onError(err);
      },
    });
  }

  requestDeleteSupportingDocument(documentId: number): void {
    this.pendingDeleteDocumentId.set(documentId);
  }

  cancelDeleteSupportingDocument(): void {
    this.pendingDeleteDocumentId.set(null);
  }

  confirmDeleteSupportingDocument(): void {
    const id = this.caseId();
    const documentId = this.pendingDeleteDocumentId();
    if (!id || documentId == null) {
      return;
    }
    this.loading.set(true);
    this.registration.deleteSupportingDocument(id, documentId).subscribe({
      next: () => {
        this.loading.set(false);
        this.pendingDeleteDocumentId.set(null);
        this.loadSupportingDocuments(id);
        this.message.set('Document removed.');
      },
      error: (err) => this.onError(err),
    });
  }

  downloadSupportingDocument(doc: TempSstSupportingDocument): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    this.registration.downloadSupportingDocument(id, doc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = doc.fileName;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.onError(err),
    });
  }

  formatFileSize(bytes: number | null | undefined): string {
    if (bytes == null || bytes <= 0) {
      return '—';
    }
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private loadAll(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.registration.getCase(id).subscribe({
      next: (c: RegistrationCase) => {
        this.caseRefNo.set(c.caseRefNo);
        this.appStatus.set(c.appStatus);
        this.queryRemark.set(c.queryRemark);
        this.appStatusReason.set(c.appStatusReason);
        this.employerName.set(c.employerName ?? '');
        this.registrationNo.set(c.registrationNo ?? '');
        this.addressLine1.set(c.addressLine1 ?? '');
      },
      error: (err) => this.onError(err),
    });
    this.registration.getDiscontinueInfo(id).subscribe({
      next: (info) => {
        this.loading.set(false);
        this.discontinueInfo.set(info);
        this.form.patchValue({
          newSstStatusId: info.newSstStatusId ?? STATUS_CANCEL,
          cessationTaxEffectiveFrom: info.cessationTaxEffectiveFrom ?? '',
        });
      },
      error: (err) => this.onError(err),
    });
    this.loadSupportingDocuments(id);
  }

  private loadSupportingDocuments(id: number): void {
    this.registration.getSstInfo(id).subscribe({
      next: (sst) => this.supportingDocuments.set(sst.supportingDocuments ?? []),
    });
  }

  private onError(err: unknown): void {
    this.loading.set(false);
    const body = (err as { error?: { message?: string; defaultUserMessage?: string } })?.error;
    this.error.set(body?.defaultUserMessage ?? body?.message ?? 'Request failed. Check API is running.');
  }
}
