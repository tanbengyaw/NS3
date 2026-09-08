import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { PostcodeOption, PortalDocTypeOption, RefOption } from '../core/models/reference.model';
import { PortalApplicationType, PortalDraftDocument, PortalPageMode, PortalUser } from '../core/models/portal.model';
import { ReferenceDataService } from '../core/services/reference-data.service';
import { RegistrationService } from '../registration/registration.service';
import { PortalEnrollmentService } from './portal-enrollment.service';

@Component({
  selector: 'assist-portal-id-registration',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './portal-id-registration.component.html',
  styleUrl: './portal-id-registration.component.scss',
})
export class PortalIdRegistrationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly referenceData = inject(ReferenceDataService);
  private readonly registrationService = inject(RegistrationService);
  private readonly portalEnrollment = inject(PortalEnrollmentService);

  readonly loading = signal(false);
  readonly searching = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly submittedUsername = signal<string | null>(null);

  readonly regNoTypes = signal<RefOption[]>([]);
  readonly idTypes = signal<RefOption[]>([]);
  readonly states = signal<RefOption[]>([]);
  readonly cities = signal<RefOption[]>([]);
  readonly postcodes = signal<PostcodeOption[]>([]);
  readonly docTypes = signal<PortalDocTypeOption[]>([]);
  readonly draftDocuments = signal<PortalDraftDocument[]>([]);
  readonly uploadingDocTypeId = signal<number | null>(null);
  readonly deletingDocumentId = signal<number | null>(null);
  readonly draftToken = signal(crypto.randomUUID());
  readonly enrollmentResult = signal<PortalUser | null>(null);
  readonly existingSearchType = signal<'CODE' | 'SST_REGISTRATION_NO'>('CODE');
  readonly pageMode = signal<PortalPageMode>('new');
  readonly loadedUser = signal<PortalUser | null>(null);
  readonly lookupUsername = signal('');
  readonly queryRemark = signal('');
  readonly approvePassword = signal('');

  readonly form = this.fb.nonNullable.group({
    applicationType: ['NEW_EMPLOYER' as PortalApplicationType, Validators.required],
    employerCodeSearch: [''],
    employerCode: [''],
    employerName: ['', Validators.required],
    registrationTypeId: [null as number | null, Validators.required],
    registrationNo: ['', Validators.required],
    addressLine1: ['', Validators.required],
    addressLine2: [''],
    addressLine3: [''],
    stateId: [null as number | null, Validators.required],
    cityId: [null as number | null],
    cityName: [''],
    postCode: ['', Validators.required],
    username: ['', Validators.required],
    fullName: ['', Validators.required],
    identificationTypeId: [null as number | null, Validators.required],
    identificationNo: ['', Validators.required],
    phoneCallingCode: ['+60'],
    phoneNumber: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    securityPhrase: ['', Validators.required],
  });

  ngOnInit(): void {
    forkJoin({
      regNoTypes: this.referenceData.listRegNoTypes(),
      idTypes: this.referenceData.listPortalIdentificationTypes(),
      states: this.referenceData.listStates(),
      docTypes: this.referenceData.listPortalDocTypes(),
    }).subscribe({
      next: ({ regNoTypes, idTypes, states, docTypes }) => {
        this.regNoTypes.set(regNoTypes);
        this.idTypes.set(idTypes);
        this.states.set(states);
        this.docTypes.set(docTypes);
      },
      error: () => this.error.set('Failed to load reference data.'),
    });

    this.form.controls.stateId.valueChanges.subscribe(stateId => {
      this.form.patchValue({ cityId: null, cityName: '', postCode: '' }, { emitEvent: false });
      this.cities.set([]);
      this.postcodes.set([]);
      if (stateId != null) {
        this.referenceData.listCities(stateId).subscribe(cities => this.cities.set(cities));
      }
    });

    this.form.controls.cityId.valueChanges.subscribe(cityId => {
      const stateId = this.form.controls.stateId.value;
      this.form.patchValue({ postCode: '' }, { emitEvent: false });
      this.postcodes.set([]);
      if (stateId != null) {
        this.referenceData.listPostcodes(stateId, cityId ?? undefined).subscribe(postcodes => {
          this.postcodes.set(postcodes);
        });
      }
    });

    this.form.controls.postCode.valueChanges.subscribe(postcode => {
      const match = this.postcodes().find(row => row.postcode === postcode);
      if (match) {
        this.form.patchValue({ cityName: match.cityName }, { emitEvent: false });
      }
    });

    this.updateEmployerCodeValidators();
    this.refreshDraftDocuments();
  }

  showFieldError(control: AbstractControl | null | undefined): boolean {
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  fieldErrorText(control: AbstractControl | null | undefined): string {
    if (!control?.errors) {
      return 'Required';
    }
    if (control.errors['required']) {
      return 'Required';
    }
    if (control.errors['email']) {
      return 'Enter a valid email address';
    }
    return 'Invalid value';
  }

  get isExistingTaxpayer(): boolean {
    return this.form.controls.applicationType.value === 'EXISTING_EMPLOYER';
  }

  get isResubmitMode(): boolean {
    return this.loadedUser()?.enrollmentStatus === 'IN_QUERY';
  }

  get isFormEditable(): boolean {
    return this.pageMode() === 'new' || this.isResubmitMode;
  }

  setPageMode(mode: PortalPageMode): void {
    this.pageMode.set(mode);
    this.error.set(null);
    this.message.set(null);
    this.enrollmentResult.set(null);
    this.loadedUser.set(null);
    this.lookupUsername.set('');
    this.queryRemark.set('');
    this.approvePassword.set('');
    if (mode === 'new') {
      this.resetEnrollmentForm();
    }
  }

  loadPortalUser(): void {
    const username = this.lookupUsername().trim();
    if (!username) {
      this.error.set('Enter a portal username to load.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
    this.enrollmentResult.set(null);
    this.portalEnrollment.getPortalUser(username).subscribe({
      next: user => {
        this.loading.set(false);
        this.loadedUser.set(user);
        this.populateFormFromUser(user);
        this.message.set(`Loaded enrollment for ${user.username} (${user.enrollmentStatus ?? '—'}).`);
      },
      error: err => {
        this.loading.set(false);
        this.loadedUser.set(null);
        this.error.set(this.httpErrorMessage(err, 'Portal user not found.'));
      },
    });
  }

  sendToQuery(): void {
    const user = this.loadedUser();
    const remark = this.queryRemark().trim();
    if (!user) {
      return;
    }
    if (!remark) {
      this.error.set('Enter a query remark for the applicant.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.portalEnrollment.queryEnrollment(user.username, remark).subscribe({
      next: updated => {
        this.loading.set(false);
        this.loadedUser.set(updated);
        this.queryRemark.set('');
        this.message.set(`Enrollment sent to query for ${updated.username}.`);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(this.httpErrorMessage(err, 'Failed to send enrollment to query.'));
      },
    });
  }

  approveEnrollment(): void {
    const user = this.loadedUser();
    const password = this.approvePassword().trim();
    if (!user) {
      return;
    }
    if (!password) {
      this.error.set('Enter an initial password for the portal user.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.portalEnrollment.approveEnrollment(user.username, password).subscribe({
      next: updated => {
        this.loading.set(false);
        this.loadedUser.set(updated);
        this.approvePassword.set('');
        this.message.set(
          `Portal ID ${updated.username} approved. They can sign in with that username and the initial password.`,
        );
      },
      error: err => {
        this.loading.set(false);
        this.error.set(this.httpErrorMessage(err, 'Failed to approve enrollment.'));
      },
    });
  }

  rejectEnrollment(): void {
    const user = this.loadedUser();
    if (!user) {
      return;
    }
    if (!window.confirm(`Reject portal enrollment for ${user.username}?`)) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.portalEnrollment.rejectEnrollment(user.username).subscribe({
      next: updated => {
        this.loading.set(false);
        this.loadedUser.set(updated);
        this.message.set(`Enrollment rejected for ${updated.username}.`);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(this.httpErrorMessage(err, 'Failed to reject enrollment.'));
      },
    });
  }

  onApplicationTypeChange(): void {
    this.form.patchValue({ employerCodeSearch: '', employerCode: '' });
    this.updateEmployerCodeValidators();
  }

  private updateEmployerCodeValidators(): void {
    const employerCode = this.form.controls.employerCode;
    if (this.isExistingTaxpayer) {
      employerCode.setValidators(Validators.required);
    } else {
      employerCode.clearValidators();
    }
    employerCode.updateValueAndValidity({ emitEvent: false });
  }

  refreshDraftDocuments(): void {
    this.portalEnrollment.listDraftDocuments(this.draftToken()).subscribe({
      next: docs => this.draftDocuments.set(docs),
      error: () => {
        // Empty draft is normal on first load.
        this.draftDocuments.set([]);
      },
    });
  }

  deleteDraftDocument(documentId: number): void {
    this.deletingDocumentId.set(documentId);
    this.error.set(null);
    this.portalEnrollment.deleteDraftDocument(this.draftToken(), documentId).subscribe({
      next: () => {
        this.deletingDocumentId.set(null);
        this.refreshDraftDocuments();
        this.message.set('Document removed.');
      },
      error: err => {
        this.deletingDocumentId.set(null);
        this.error.set(this.httpErrorMessage(err, 'Failed to remove document.'));
      },
    });
  }

  formatFileSize(bytes: number | null): string {
    if (bytes == null) {
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

  onDocumentSelected(documentTypeId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }

    this.uploadingDocTypeId.set(documentTypeId);
    this.error.set(null);
    this.portalEnrollment.uploadDraftDocument(this.draftToken(), documentTypeId, file).subscribe({
      next: () => {
        this.uploadingDocTypeId.set(null);
        this.refreshDraftDocuments();
        this.message.set(`Uploaded ${file.name}.`);
      },
      error: err => {
        this.uploadingDocTypeId.set(null);
        this.error.set(this.httpErrorMessage(err, 'Document upload failed.'));
      },
    });
  }

  documentsForType(documentTypeId: number): PortalDraftDocument[] {
    return this.draftDocuments().filter(doc => doc.documentTypeId === documentTypeId);
  }

  requiredDocumentsUploaded(): boolean {
    if (this.isResubmitMode && this.draftDocuments().length === 0) {
      return true;
    }
    const requiredTypeIds = this.docTypes()
      .filter(type => type.requiredForPortalId)
      .map(type => type.id);
    return requiredTypeIds.every(typeId => this.documentsForType(typeId).length > 0);
  }

  buildEnrollmentPayload() {
    const value = this.form.getRawValue();
    return {
      email: value.email.trim(),
      applicationType: value.applicationType,
      employerCode: value.employerCode?.trim() || null,
      employerName: value.employerName.trim(),
      registrationTypeId: value.registrationTypeId!,
      registrationNo: value.registrationNo.trim(),
      addressLine1: value.addressLine1.trim(),
      addressLine2: value.addressLine2.trim() || null,
      addressLine3: value.addressLine3.trim() || null,
      stateId: value.stateId!,
      cityId: value.cityId,
      cityName: value.cityName.trim() || null,
      postCode: value.postCode.trim(),
      fullName: value.fullName.trim(),
      identificationTypeId: value.identificationTypeId!,
      identificationNo: value.identificationNo.trim(),
      phoneCallingCode: value.phoneCallingCode.trim() || '+60',
      phoneNumber: value.phoneNumber.trim(),
      securityPhrase: value.securityPhrase.trim(),
      draftToken: this.draftDocuments().length > 0 ? this.draftToken() : null,
    };
  }

  searchExistingTaxpayer(): void {
    const searchValue = this.form.controls.employerCodeSearch.value.trim();
    if (!searchValue) {
      const label = this.existingSearchType() === 'CODE' ? 'employer code' : 'SST registration number';
      this.error.set(`Enter an ${label} to search.`);
      return;
    }

    this.searching.set(true);
    this.error.set(null);
    this.registrationService.searchTaxPayer(this.existingSearchType(), searchValue).subscribe({
      next: profile => {
        this.searching.set(false);
        this.form.patchValue({
          employerCode: profile.employerCode,
          employerName: profile.employerName ?? '',
          registrationNo: profile.registrationNo ?? '',
          addressLine1: profile.addressLine1 ?? profile.corrAddressLine1 ?? '',
          addressLine2: profile.addressLine2 ?? profile.corrAddressLine2 ?? '',
          addressLine3: profile.addressLine3 ?? profile.corrAddressLine3 ?? '',
          stateId: profile.stateId ?? profile.corrStateId ?? null,
          cityId: profile.cityId ?? profile.corrCityId ?? null,
          cityName: profile.cityName ?? profile.corrCityName ?? '',
          postCode: profile.postCode ?? profile.corrPostCode ?? '',
          email: profile.email ?? this.form.controls.email.value,
          phoneNumber: profile.phone ?? this.form.controls.phoneNumber.value,
        });
        const stateId = this.form.controls.stateId.value;
        if (stateId != null) {
          this.referenceData.listCities(stateId).subscribe(cities => this.cities.set(cities));
          const cityId = this.form.controls.cityId.value;
          this.referenceData.listPostcodes(stateId, cityId ?? undefined).subscribe(postcodes => {
            this.postcodes.set(postcodes);
          });
        }
        this.message.set(`Loaded profile for ${profile.employerCode}.`);
      },
      error: err => {
        this.searching.set(false);
        this.error.set(this.httpErrorMessage(err, 'Taxpayer not found.'));
      },
    });
  }

  submit(): void {
    this.error.set(null);
    this.message.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Please complete all required fields.');
      return;
    }

    const value = this.form.getRawValue();
    if (!this.requiredDocumentsUploaded()) {
      this.error.set('Upload all required supporting documents before submitting.');
      return;
    }

    this.loading.set(true);
    const payload = this.buildEnrollmentPayload();

    if (this.isResubmitMode && this.loadedUser()) {
      this.portalEnrollment.resubmitEnrollment(this.loadedUser()!.username, payload).subscribe({
        next: result => this.onSubmitSuccess(result, 'resubmitted'),
        error: err => this.onSubmitError(err, 'Resubmit failed.'),
      });
      return;
    }

    this.portalEnrollment.submitEnrollment({
      username: value.username.trim(),
      ...payload,
      draftToken: this.draftToken(),
    }).subscribe({
      next: result => this.onSubmitSuccess(result, 'submitted'),
      error: err => this.onSubmitError(err, 'Enrollment failed.'),
    });
  }

  private onSubmitSuccess(result: PortalUser, action: 'submitted' | 'resubmitted'): void {
    this.loading.set(false);
    this.enrollmentResult.set(result);
    this.submittedUsername.set(result.username);
    this.loadedUser.set(result);
    this.message.set(`Portal ID enrollment ${action} for ${result.username}.`);
  }

  private onSubmitError(err: unknown, fallback: string): void {
    this.loading.set(false);
    this.error.set(this.httpErrorMessage(err, fallback));
  }

  startNewEnrollment(): void {
    this.pageMode.set('new');
    this.loadedUser.set(null);
    this.lookupUsername.set('');
    this.queryRemark.set('');
    this.approvePassword.set('');
    this.enrollmentResult.set(null);
    this.submittedUsername.set(null);
    this.message.set(null);
    this.error.set(null);
    this.resetEnrollmentForm();
  }

  private resetEnrollmentForm(): void {
    this.draftToken.set(crypto.randomUUID());
    this.draftDocuments.set([]);
    this.form.reset({
      applicationType: 'NEW_EMPLOYER',
      employerCodeSearch: '',
      employerCode: '',
      employerName: '',
      registrationTypeId: null,
      registrationNo: '',
      addressLine1: '',
      addressLine2: '',
      addressLine3: '',
      stateId: null,
      cityId: null,
      cityName: '',
      postCode: '',
      username: '',
      fullName: '',
      identificationTypeId: null,
      identificationNo: '',
      phoneCallingCode: '+60',
      phoneNumber: '',
      email: '',
      securityPhrase: '',
    });
    this.cities.set([]);
    this.postcodes.set([]);
    this.updateEmployerCodeValidators();
    this.refreshDraftDocuments();
  }

  applicationTypeLabel(type: PortalApplicationType | null | undefined): string {
    return type === 'EXISTING_EMPLOYER' ? 'Existing taxpayer' : 'New taxpayer';
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    return value.replace('T', ' ').slice(0, 16);
  }

  employerCodeLabel(result: PortalUser): string {
    if (result.employerCode) {
      return result.employerCode;
    }
    return result.applicationType === 'NEW_EMPLOYER' ? 'Pending (new taxpayer)' : '—';
  }

  enrollmentStatusLabel(status: string | null | undefined): string {
    return status?.replace('_', ' ') ?? '—';
  }

  private populateFormFromUser(user: PortalUser): void {
    this.draftToken.set(crypto.randomUUID());
    this.draftDocuments.set([]);
    this.refreshDraftDocuments();
    this.form.reset({
      applicationType: user.applicationType,
      employerCodeSearch: '',
      employerCode: user.employerCode ?? '',
      employerName: user.employerName ?? '',
      registrationTypeId: user.registrationTypeId,
      registrationNo: user.registrationNo ?? '',
      addressLine1: user.addressLine1 ?? '',
      addressLine2: user.addressLine2 ?? '',
      addressLine3: user.addressLine3 ?? '',
      stateId: user.stateId,
      cityId: user.cityId,
      cityName: user.cityName ?? '',
      postCode: user.postCode ?? '',
      username: user.username,
      fullName: user.fullName ?? '',
      identificationTypeId: user.identificationTypeId,
      identificationNo: user.identificationNo ?? '',
      phoneCallingCode: user.phoneCallingCode ?? '+60',
      phoneNumber: user.phoneNumber ?? '',
      email: user.email,
      securityPhrase: user.securityPhrase ?? '',
    });
    this.updateEmployerCodeValidators();
    const stateId = user.stateId;
    if (stateId != null) {
      this.referenceData.listCities(stateId).subscribe(cities => this.cities.set(cities));
      this.referenceData.listPostcodes(stateId, user.cityId ?? undefined).subscribe(postcodes => {
        this.postcodes.set(postcodes);
      });
    } else {
      this.cities.set([]);
      this.postcodes.set([]);
    }
  }

  private httpErrorMessage(err: unknown, fallback: string): string {
    const httpErr = err as {
      status?: number;
      error?: { message?: string; defaultUserMessage?: string } | string;
    };
    const body = httpErr.error;
    if (typeof body === 'string' && body.trim()) {
      return body;
    }
    if (body && typeof body === 'object') {
      return body.defaultUserMessage ?? body.message ?? fallback;
    }
    return fallback;
  }
}
