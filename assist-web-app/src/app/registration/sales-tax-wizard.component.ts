import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { PostcodeOption, RefOption, SupportingDocumentTypeOption, TariffCodeSalesTypeOption } from '../core/models/reference.model';
import { ContactLine, TempDirectorOwner, TempPremises, TempSstContactPerson, TempSstSupportingDocument, TempSstTariffCode, TaxPayerRegistrationProfile } from '../core/models/registration.model';
import { ReferenceDataService } from '../core/services/reference-data.service';
import { ConfirmDialogComponent } from '../shared/confirm-dialog.component';
import { FormModalComponent } from '../shared/form-modal.component';
import { RegistrationService } from './registration.service';

/** ASSIST section REG_NEW_REG_SST_SALES_TAX */
const SECTION_SALES_TAX = 1100;
/** SstContractType.MAIN_CONTRACT */
const MAIN_CONTRACT = 1;
/** SstContractType.SUB_CONTRACT */
const SUB_CONTRACT = 2;

type PendingDelete = { kind: 'director' | 'premises' | 'tariff' | 'contactPerson' | 'supportingDocument'; id: number };

@Component({
  selector: 'assist-sales-tax-wizard',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FormModalComponent, ConfirmDialogComponent],
  templateUrl: './sales-tax-wizard.component.html',
  styleUrl: './sales-tax-wizard.component.scss',
})
export class SalesTaxWizardComponent {
  private readonly fb = inject(FormBuilder);
  private readonly registration = inject(RegistrationService);
  private readonly referenceData = inject(ReferenceDataService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly step = signal(1);
  readonly caseId = signal<number | null>(null);
  readonly caseRefNo = signal<string | null>(null);
  readonly appStatus = signal<string | null>(null);
  readonly employerCode = signal<string | null>(null);
  readonly salesTaxSmkRegNo = signal<string | null>(null);
  readonly directors = signal<TempDirectorOwner[]>([]);
  readonly premises = signal<TempPremises[]>([]);
  readonly tariffCodes = signal<TempSstTariffCode[]>([]);
  readonly contactPersons = signal<TempSstContactPerson[]>([]);
  readonly supportingDocuments = signal<TempSstSupportingDocument[]>([]);
  readonly supportingDocumentTypes = signal<SupportingDocumentTypeOption[]>([]);
  readonly selectedDocumentTypeId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly editingDirectorId = signal<number | null>(null);
  readonly editingPremisesId = signal<number | null>(null);
  readonly directorModalOpen = signal(false);
  readonly premisesModalOpen = signal(false);
  readonly tariffModalOpen = signal(false);
  readonly contactPersonModalOpen = signal(false);
  readonly pendingDelete = signal<PendingDelete | null>(null);
  readonly editingTariffId = signal<number | null>(null);
  readonly editingContactPersonId = signal<number | null>(null);
  readonly tariffSearchResults = signal<TariffCodeSalesTypeOption[]>([]);
  readonly tariffSearchAttempted = signal(false);
  readonly tariffSearching = signal(false);
  readonly tariffModalNotice = signal<string | null>(null);

  readonly mainContractType = MAIN_CONTRACT;
  readonly subContractType = SUB_CONTRACT;

  readonly showPreRegBlock = signal(false);
  readonly taxPayerSearchFound = signal<boolean | null>(null);
  readonly searchedEmployerCode = signal<string | null>(null);
  readonly states = signal<RefOption[]>([]);
  readonly cities = signal<RefOption[]>([]);
  readonly postcodes = signal<PostcodeOption[]>([]);
  readonly corrCities = signal<RefOption[]>([]);
  readonly corrPostcodes = signal<PostcodeOption[]>([]);
  readonly premisesCities = signal<RefOption[]>([]);
  readonly premisesPostcodes = signal<PostcodeOption[]>([]);
  readonly officeLocations = signal<RefOption[]>([]);
  readonly businessEntityTypes = signal<RefOption[]>([]);
  readonly identificationTypes = signal<RefOption[]>([]);

  readonly searchForm = this.fb.nonNullable.group({
    searchRegType: ['SST_REGISTRATION_NO'],
    searchRegValue: ['', Validators.required],
  });

  private pendingImportDirectors: TaxPayerRegistrationProfile['directors'] = [];
  private pendingImportPremises: TaxPayerRegistrationProfile['premises'] = [];

  readonly form1 = this.fb.nonNullable.group({
    employerName: ['', Validators.required],
    registrationNo: ['', Validators.required],
    businessEntityTypeId: [1, Validators.required],
    msicId: [null as number | null],
    serviceTypeId: [1, Validators.required],
    pksBranchId: [2, Validators.required],
    methodContributionPaymentId: [null as number | null],
    postCode: ['50812', Validators.required],
    email: ['', Validators.required],
    newRegisterTax: [false],
    addressLine1: ['', Validators.required],
    addressLine2: [''],
    addressLine3: [''],
    stateId: [12 as number | null],
    cityId: [1201 as number | null],
    cityName: ['Shah Alam'],
    sameAsBusinessAddr: [false],
    corrAddressLine1: [''],
    corrAddressLine2: [''],
    corrAddressLine3: [''],
    corrPostCode: [''],
    corrStateId: [null as number | null],
    corrCityId: [null as number | null],
    corrCityName: [''],
    sameAsTradeName: [false],
    tradeName: [''],
    tourTaxRegNo: [''],
    inTaxRefNo: [''],
    cusAudRefNo: [''],
    preRegNo: [''],
    preRegName: [''],
    dateOfReplacement: [''],
  });

  readonly form2 = this.fb.nonNullable.group({
    manComDate: ['2024-01-15', Validators.required],
    dateSaleValTaxGoods: ['2024-06-01', Validators.required],
    finYrEndMon: [12, [Validators.required, Validators.min(1), Validators.max(12)]],
    businessComDate: ['2024-01-01', Validators.required],
    anTotalTaxSalesVal: [0, [Validators.required, Validators.min(0.01)]],
    localSales: [0, [Validators.required, Validators.min(0)]],
    exportSales: [0, [Validators.required, Validators.min(0)]],
    salesToDesignArea: [0, [Validators.required, Validators.min(0)]],
    othersSales: [0, [Validators.required, Validators.min(0)]],
    subContractWork: [false],
    declareTrue: [false],
    declareDate: [''],
    applicantName: [''],
    identityCard: [''],
    designation: [''],
    applicantEmail: [''],
    applicantTelNo: [''],
  });

  readonly contactPersonForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  readonly contactForm = this.fb.group({
    phones: this.fb.array([this.newContactLine()]),
    faxes: this.fb.array([this.newContactLine()]),
  });

  readonly directorForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    identificationTypeId: [2, Validators.required],
    identificationNo: ['', Validators.required],
    email: [''],
    designation: ['Managing Director'],
  });

  readonly premisesForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    addressLine: ['', Validators.required],
    addressLine2: [''],
    addressLine3: [''],
    stateId: [null as number | null],
    cityId: [null as number | null],
    postCode: [''],
    cityName: [''],
    stateName: [''],
  });

  readonly tariffForm = this.fb.nonNullable.group({
    tariffCodeSearch: [''],
    tariffCodeSalesTypeId: [null as number | null, Validators.required],
    tariffCode: [''],
    tariffDescription: [''],
    contractTypeId: [MAIN_CONTRACT, Validators.required],
    finishedGoods: ['', Validators.required],
  });

  constructor() {
    this.loadReferenceData();
    this.form1.controls.newRegisterTax.valueChanges.subscribe((checked) => {
      if (checked) {
        this.taxPayerSearchFound.set(null);
        this.searchedEmployerCode.set(null);
        this.pendingImportDirectors = [];
        this.pendingImportPremises = [];
        this.searchForm.reset({ searchRegType: 'SST_REGISTRATION_NO', searchRegValue: '' });
      }
      this.syncForm1EntryState();
    });
    this.form2.controls.declareTrue.valueChanges.subscribe((checked) => {
      const nameCtrl = this.form2.controls.applicantName;
      if (checked) {
        nameCtrl.setValidators(Validators.required);
        if (!this.form2.controls.declareDate.value) {
          this.form2.patchValue({ declareDate: new Date().toISOString().slice(0, 10) }, { emitEvent: false });
        }
      } else {
        nameCtrl.clearValidators();
      }
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    });
    const idParam = this.route.snapshot.paramMap.get('caseId');
    if (idParam) {
      this.loadCase(Number(idParam));
    } else {
      this.form1.patchValue({ registrationNo: this.newBrn() });
      this.refreshBusinessAddressOptions();
      this.syncForm1EntryState();
    }
  }

  get isOfficer(): boolean {
    return this.auth.hasRole('OFFICER') || this.auth.hasRole('ADMIN');
  }

  get isEditable(): boolean {
    const status = this.appStatus();
    return !status || status === 'NEW' || status === 'IN_QUERY' || status === 'IN_PROGRESS';
  }

  get mainTariffCount(): number {
    return this.tariffCodes().filter((t) => t.contractTypeId === MAIN_CONTRACT).length;
  }

  get subTariffCount(): number {
    return this.tariffCodes().filter((t) => t.contractTypeId === SUB_CONTRACT).length;
  }

  get searchRegValueLabel(): string {
    return this.searchForm.controls.searchRegType.value === 'BRN'
      ? 'BRN (business registration no)'
      : 'SST Registration No.';
  }

  /** Search locks after draft is created or when registering a new tax payer. */
  get searchLocked(): boolean {
    return !!this.caseId() || this.form1.controls.newRegisterTax.value;
  }

  get brnReadonly(): boolean {
    return !!this.caseId() || (this.taxPayerSearchFound() === true && !this.form1.controls.newRegisterTax.value);
  }

  /** Form 1 fields unlock after a successful search, New Tax Payer tick, or when editing a draft. */
  get form1EntryAllowed(): boolean {
    if (this.caseId()) {
      return this.isEditable;
    }
    return this.form1.controls.newRegisterTax.value || this.taxPayerSearchFound() === true;
  }

  get phoneLines(): FormArray<FormGroup> {
    return this.contactForm.get('phones') as FormArray<FormGroup>;
  }

  get faxLines(): FormArray<FormGroup> {
    return this.contactForm.get('faxes') as FormArray<FormGroup>;
  }

  createDraft(): void {
    this.ensureDraft((id, refNo) => {
      this.message.set(`Draft created: ${refNo ?? id}`);
      this.syncRouteAfterDraft(id);
    });
  }

  /** Creates a draft when needed, then runs the callback with the case id. */
  private ensureDraft(onReady: (id: number, refNo: string | null) => void): void {
    const existing = this.caseId();
    if (existing) {
      onReady(existing, this.caseRefNo());
      return;
    }

    this.syncTradeNameFromBusiness();
    if (!this.form1.controls.newRegisterTax.value && this.taxPayerSearchFound() !== true) {
      this.showFormError('Please search for an existing tax payer or tick New Tax Payer Registration.');
      return;
    }
    if (this.form1.invalid) {
      this.form1.markAllAsTouched();
      this.showFormError(
        `${this.validationSummary(this.form1, 'Create draft')} Complete all Form 1 sections (including Contact and Premise address below) first.`,
      );
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.registration
      .createCase({ ...this.createCasePayload(), sectionId: SECTION_SALES_TAX, dataSourceId: 1 })
      .subscribe({
        next: (result) => {
          const id = result.resourceId!;
          this.caseId.set(id);
          this.caseRefNo.set(result.resourceIdentifier);
          this.appStatus.set('NEW');
          this.persistForm1(id, () => {
            this.importTaxPayerChildren(id, () => {
              this.loading.set(false);
              onReady(id, result.resourceIdentifier);
            });
          });
        },
        error: (err) => this.onError(err),
      });
  }

  private syncRouteAfterDraft(caseId: number): void {
    if (!this.route.snapshot.paramMap.get('caseId')) {
      void this.router.navigate(['/registration/sales-tax', caseId], { replaceUrl: true });
    }
  }

  saveForm1(): void {
    const id = this.caseId();
    this.syncTradeNameFromBusiness();
    if (!id || this.form1.invalid) {
      this.form1.markAllAsTouched();
      this.error.set(this.validationSummary(this.form1, 'Save'));
      this.message.set(null);
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.persistForm1(id, () => {
      this.loading.set(false);
      this.message.set('Business details saved.');
    });
  }

  searchTaxPayer(): void {
    if (this.searchLocked) {
      return;
    }
    if (this.searchForm.invalid) {
      this.searchForm.markAllAsTouched();
      this.error.set('Enter a value to search for an existing tax payer.');
      return;
    }

    const { searchRegType, searchRegValue } = this.searchForm.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
    this.registration.searchTaxPayer(searchRegType, searchRegValue.trim()).subscribe({
      next: (profile) => {
        this.loading.set(false);
        this.taxPayerSearchFound.set(true);
        this.searchedEmployerCode.set(profile.employerCode);
        this.applyTaxPayerProfile(profile);
        this.syncForm1EntryState();
        this.message.set(`Found tax payer: ${profile.employerName} (${profile.employerCode})`);
      },
      error: (err) => {
        this.loading.set(false);
        this.taxPayerSearchFound.set(false);
        this.searchedEmployerCode.set(null);
        this.pendingImportDirectors = [];
        this.pendingImportPremises = [];
        this.syncForm1EntryState();
        this.onError(err);
      },
    });
  }

  onSearchRegTypeChange(): void {
    this.taxPayerSearchFound.set(null);
    this.syncForm1EntryState();
  }

  addPhoneLine(): void {
    if (!this.form1EntryAllowed) {
      return;
    }
    this.phoneLines.push(this.newContactLine());
  }

  removePhoneLine(index: number): void {
    if (this.phoneLines.length > 1) {
      this.phoneLines.removeAt(index);
    }
  }

  addFaxLine(): void {
    if (!this.form1EntryAllowed) {
      return;
    }
    this.faxLines.push(this.newContactLine());
  }

  removeFaxLine(index: number): void {
    if (this.faxLines.length > 1) {
      this.faxLines.removeAt(index);
    }
  }

  toggleSameAsTradeName(): void {
    this.syncTradeNameFromBusiness();
  }

  toggleSameAsBusinessAddr(): void {
    if (this.form1.controls.sameAsBusinessAddr.value) {
      this.syncCorrespondenceFromBusiness();
    } else {
      this.clearCorrespondenceAddress();
    }
  }

  onBusinessAddressFieldChange(): void {
    this.syncCorrespondenceFromBusiness();
  }

  onBusinessStateChange(): void {
    const stateId = this.form1.controls.stateId.value;
    this.form1.patchValue({ cityId: null, cityName: '', postCode: '' }, { emitEvent: false });
    this.referenceData.listCities(stateId).subscribe((items) => this.cities.set(items));
    this.referenceData.listPostcodes(stateId).subscribe((items) => this.postcodes.set(items));
    this.refreshOfficeLocations('');
    this.syncCorrespondenceFromBusiness();
  }

  onBusinessCityChange(): void {
    const stateId = this.form1.controls.stateId.value;
    const cityId = this.form1.controls.cityId.value;
    const city = this.cities().find((c) => c.id === cityId);
    this.form1.patchValue({ cityName: city?.label ?? '', postCode: '' }, { emitEvent: false });
    this.referenceData.listPostcodes(stateId, cityId).subscribe((items) => this.postcodes.set(items));
    this.syncCorrespondenceFromBusiness();
  }

  onBusinessPostcodeChange(): void {
    const selected = this.postcodes().find((p) => p.postcode === this.form1.controls.postCode.value);
    if (!selected) {
      return;
    }
    this.form1.patchValue(
      {
        stateId: selected.stateId,
        cityId: selected.cityId,
        cityName: selected.cityName,
      },
      { emitEvent: false },
    );
    this.refreshBusinessAddressOptions();
    this.refreshOfficeLocations(this.form1.controls.postCode.value);
    this.syncCorrespondenceFromBusiness();
  }

  onCorrStateChange(): void {
    const stateId = this.form1.controls.corrStateId.value;
    this.form1.patchValue({ corrCityId: null, corrCityName: '', corrPostCode: '' }, { emitEvent: false });
    this.referenceData.listCities(stateId).subscribe((items) => this.corrCities.set(items));
    this.referenceData.listPostcodes(stateId).subscribe((items) => this.corrPostcodes.set(items));
  }

  onCorrCityChange(): void {
    const stateId = this.form1.controls.corrStateId.value;
    const cityId = this.form1.controls.corrCityId.value;
    const city = this.corrCities().find((c) => c.id === cityId);
    this.form1.patchValue({ corrCityName: city?.label ?? '', corrPostCode: '' }, { emitEvent: false });
    this.referenceData.listPostcodes(stateId, cityId).subscribe((items) => this.corrPostcodes.set(items));
  }

  onCorrPostcodeChange(): void {
    const selected = this.corrPostcodes().find((p) => p.postcode === this.form1.controls.corrPostCode.value);
    if (!selected) {
      return;
    }
    this.form1.patchValue(
      {
        corrStateId: selected.stateId,
        corrCityId: selected.cityId,
        corrCityName: selected.cityName,
      },
      { emitEvent: false },
    );
    this.refreshCorrespondenceAddressOptions();
  }

  onPremisesStateChange(): void {
    const stateId = this.premisesForm.controls.stateId.value;
    this.premisesForm.patchValue({ cityId: null, cityName: '', postCode: '', stateName: '' }, { emitEvent: false });
    const state = this.states().find((s) => s.id === stateId);
    this.premisesForm.patchValue({ stateName: state?.label ?? '' }, { emitEvent: false });
    this.referenceData.listCities(stateId).subscribe((items) => this.premisesCities.set(items));
    this.referenceData.listPostcodes(stateId).subscribe((items) => this.premisesPostcodes.set(items));
  }

  onPremisesCityChange(): void {
    const stateId = this.premisesForm.controls.stateId.value;
    const cityId = this.premisesForm.controls.cityId.value;
    const city = this.premisesCities().find((c) => c.id === cityId);
    this.premisesForm.patchValue({ cityName: city?.label ?? '', postCode: '' }, { emitEvent: false });
    this.referenceData.listPostcodes(stateId, cityId).subscribe((items) => this.premisesPostcodes.set(items));
  }

  onPremisesPostcodeChange(): void {
    const selected = this.premisesPostcodes().find((p) => p.postcode === this.premisesForm.controls.postCode.value);
    if (!selected) {
      return;
    }
    this.premisesForm.patchValue(
      {
        stateId: selected.stateId,
        cityId: selected.cityId,
        cityName: selected.cityName,
        stateName: selected.stateName,
      },
      { emitEvent: false },
    );
    this.refreshPremisesAddressOptions();
  }

  togglePreRegBlock(checked: boolean): void {
    if (!this.form1EntryAllowed) {
      return;
    }
    this.showPreRegBlock.set(checked);
    if (!checked) {
      this.form1.patchValue({ preRegNo: '', preRegName: '', dateOfReplacement: '' });
    }
  }

  saveForm2(): void {
    const id = this.caseId();
    if (!id || this.form2.invalid) {
      this.form2.markAllAsTouched();
      this.error.set(this.validationSummary(this.form2, 'Save Part B'));
      this.message.set(null);
      return;
    }
    this.persistForm2(id, 'Sales tax Part B saved.');
  }

  savePartC(): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    const partCControls = [
      this.form2.controls.declareTrue,
      this.form2.controls.declareDate,
      this.form2.controls.applicantName,
      this.form2.controls.identityCard,
      this.form2.controls.designation,
      this.form2.controls.applicantEmail,
      this.form2.controls.applicantTelNo,
    ];
    partCControls.forEach((c) => c.markAsTouched());
    if (!this.form2.controls.declareTrue.value) {
      this.error.set('Part C: accept the declaration before saving.');
      this.message.set(null);
      return;
    }
    if (this.form2.controls.applicantName.invalid) {
      this.error.set(this.validationSummary(this.form2, 'Save Part C'));
      this.message.set(null);
      return;
    }
    this.persistForm2(id, 'Part C declaration saved.');
  }

  private persistForm2(caseId: number, successMessage: string): void {
    this.loading.set(true);
    this.error.set(null);
    const raw = this.form2.getRawValue();
    this.registration
      .upsertSstInfo(caseId, {
        ...raw,
        declareDate: raw.declareDate || null,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.message.set(successMessage);
        },
        error: (err) => this.onError(err),
      });
  }

  addDirector(): void {
    if (this.directorForm.invalid) {
      this.directorForm.markAllAsTouched();
      this.error.set(this.validationSummary(this.directorForm, this.editingDirectorId() ? 'Update director' : 'Add director'));
      this.message.set(null);
      return;
    }
    const editingId = this.editingDirectorId();
    const persistDirector = (id: number, refNo: string | null): void => {
      this.loading.set(true);
      this.error.set(null);
      const body = this.directorForm.getRawValue();
      const request$ = editingId
        ? this.registration.updateDirector(id, editingId, body)
        : this.registration.addDirector(id, body);
      request$.subscribe({
        next: () => {
          this.loading.set(false);
          this.closeDirectorModal();
          this.loadSstInfo(id);
          const action = editingId ? 'updated' : 'added';
          this.message.set(refNo && !editingId ? `Draft ${refNo} saved. Director ${action}.` : `Director ${action}.`);
        },
        error: (err) => this.onError(err),
      });
    };
    if (editingId && this.caseId()) {
      persistDirector(this.caseId()!, this.caseRefNo());
      return;
    }
    this.ensureDraft((id, refNo) => {
      this.syncRouteAfterDraft(id);
      persistDirector(id, refNo);
    });
  }

  openDirectorModal(director?: TempDirectorOwner): void {
    if (director) {
      this.editingDirectorId.set(director.id);
      this.directorForm.patchValue({
        name: director.name,
        identificationTypeId: director.identificationTypeId ?? 2,
        identificationNo: director.identificationNo,
        email: director.email ?? '',
        designation: director.designation ?? 'Managing Director',
      });
    } else {
      this.resetDirectorForm();
    }
    this.error.set(null);
    this.directorModalOpen.set(true);
  }

  closeDirectorModal(): void {
    this.directorModalOpen.set(false);
    this.resetDirectorForm();
  }

  private resetDirectorForm(): void {
    this.editingDirectorId.set(null);
    this.directorForm.reset({
      name: '',
      identificationTypeId: 2,
      identificationNo: '',
      email: '',
      designation: 'Managing Director',
    });
  }

  requestDeleteDirector(directorId: number): void {
    this.pendingDelete.set({ kind: 'director', id: directorId });
  }

  requestDeletePremises(premisesId: number): void {
    this.pendingDelete.set({ kind: 'premises', id: premisesId });
  }

  requestDeleteContactPerson(contactPersonId: number): void {
    this.pendingDelete.set({ kind: 'contactPerson', id: contactPersonId });
  }

  cancelPendingDelete(): void {
    this.pendingDelete.set(null);
  }

  confirmPendingDelete(): void {
    const pending = this.pendingDelete();
    if (!pending) {
      return;
    }
    if (pending.kind === 'director') {
      this.executeRemoveDirector(pending.id);
    } else if (pending.kind === 'premises') {
      this.executeRemovePremises(pending.id);
    } else if (pending.kind === 'contactPerson') {
      this.executeRemoveContactPerson(pending.id);
    } else if (pending.kind === 'supportingDocument') {
      this.executeRemoveSupportingDocument(pending.id);
    } else {
      this.executeRemoveTariff(pending.id);
    }
    this.pendingDelete.set(null);
  }

  private executeRemoveDirector(directorId: number): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    if (this.editingDirectorId() === directorId) {
      this.closeDirectorModal();
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.deleteDirector(id, directorId).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadSstInfo(id);
        this.message.set('Director removed.');
      },
      error: (err) => this.onError(err),
    });
  }

  addPremises(): void {
    if (this.premisesForm.invalid) {
      this.premisesForm.markAllAsTouched();
      this.error.set(this.validationSummary(this.premisesForm, this.editingPremisesId() ? 'Update premises' : 'Add premises'));
      this.message.set(null);
      return;
    }
    const editingId = this.editingPremisesId();
    const v = this.premisesForm.getRawValue();
    const body = {
      name: v.name,
      addressLine: v.addressLine,
      addressLine2: v.addressLine2 || undefined,
      addressLine3: v.addressLine3 || undefined,
      postCode: v.postCode || undefined,
      cityName: v.cityName || undefined,
      stateName: v.stateName || undefined,
    };
    const persistPremises = (id: number, refNo: string | null): void => {
      this.loading.set(true);
      this.error.set(null);
      const request$ = editingId
        ? this.registration.updatePremises(id, editingId, body)
        : this.registration.addPremises(id, body);
      request$.subscribe({
        next: () => {
          this.loading.set(false);
          this.closePremisesModal();
          this.loadSstInfo(id);
          const action = editingId ? 'updated' : 'added';
          this.message.set(refNo && !editingId ? `Draft ${refNo} saved. Premises ${action}.` : `Premises ${action}.`);
        },
        error: (err) => this.onError(err),
      });
    };
    if (editingId && this.caseId()) {
      persistPremises(this.caseId()!, this.caseRefNo());
      return;
    }
    this.ensureDraft((id, refNo) => {
      this.syncRouteAfterDraft(id);
      persistPremises(id, refNo);
    });
  }

  openPremisesModal(premises?: TempPremises): void {
    if (premises) {
      this.editingPremisesId.set(premises.id);
      const state = this.states().find((s) => s.label === premises.stateName);
      this.premisesForm.patchValue({
        name: premises.name,
        addressLine: premises.addressLine,
        addressLine2: premises.addressLine2 ?? '',
        addressLine3: premises.addressLine3 ?? '',
        stateId: state?.id ?? null,
        cityId: null,
        postCode: premises.postCode ?? '',
        cityName: premises.cityName ?? '',
        stateName: premises.stateName ?? '',
      });
      if (state?.id) {
        this.referenceData.listCities(state.id).subscribe((cities) => {
          this.premisesCities.set(cities);
          const city = cities.find((c) => c.label === premises.cityName);
          this.premisesForm.patchValue({ cityId: city?.id ?? null }, { emitEvent: false });
          if (city?.id) {
            this.referenceData.listPostcodes(state.id, city.id).subscribe((items) => this.premisesPostcodes.set(items));
          } else {
            this.referenceData.listPostcodes(state.id).subscribe((items) => this.premisesPostcodes.set(items));
          }
        });
      } else {
        this.premisesCities.set([]);
        this.premisesPostcodes.set([]);
      }
    } else {
      this.resetPremisesForm();
    }
    this.error.set(null);
    this.premisesModalOpen.set(true);
  }

  closePremisesModal(): void {
    this.premisesModalOpen.set(false);
    this.resetPremisesForm();
  }

  private resetPremisesForm(): void {
    this.editingPremisesId.set(null);
    this.premisesForm.reset({
      name: '',
      addressLine: '',
      addressLine2: '',
      addressLine3: '',
      stateId: null,
      cityId: null,
      postCode: '',
      cityName: '',
      stateName: '',
    });
    this.premisesCities.set([]);
    this.premisesPostcodes.set([]);
  }

  private executeRemovePremises(premisesId: number): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    if (this.editingPremisesId() === premisesId) {
      this.closePremisesModal();
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.deletePremises(id, premisesId).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadSstInfo(id);
        this.message.set('Premises removed.');
      },
      error: (err) => this.onError(err),
    });
  }

  addTariffCode(): void {
    const id = this.caseId();
    if (!id || this.tariffForm.invalid) {
      this.tariffForm.markAllAsTouched();
      this.error.set(this.validationSummary(this.tariffForm, this.editingTariffId() ? 'Update tariff code' : 'Add tariff code'));
      this.message.set(null);
      return;
    }
    if (!this.tariffForm.controls.tariffCodeSalesTypeId.value) {
      this.error.set('Search and select a tariff code before saving.');
      this.message.set(null);
      return;
    }
    const editingId = this.editingTariffId();
    const body = {
      tariffCodeSalesTypeId: this.tariffForm.controls.tariffCodeSalesTypeId.value!,
      contractTypeId: this.tariffForm.controls.contractTypeId.value,
      finishedGoods: this.tariffForm.controls.finishedGoods.value,
    };
    this.loading.set(true);
    this.error.set(null);
    const request$ = editingId
      ? this.registration.updateTariffCode(id, editingId, body)
      : this.registration.addTariffCode(id, body);
    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.closeTariffModal();
        this.loadSstInfo(id);
        this.message.set(editingId ? 'Tariff code updated.' : 'Tariff code added.');
      },
      error: (err) => this.onError(err),
    });
  }

  openTariffModal(tariff?: TempSstTariffCode): void {
    if (tariff) {
      this.editingTariffId.set(tariff.id);
      this.tariffForm.patchValue({
        tariffCodeSearch: tariff.tariffCode ?? '',
        tariffCodeSalesTypeId: tariff.tariffCodeSalesTypeId,
        tariffCode: tariff.tariffCode ?? '',
        tariffDescription: tariff.tariffDescription ?? '',
        contractTypeId: tariff.contractTypeId,
        finishedGoods: tariff.finishedGoods ?? '',
      });
    } else {
      this.resetTariffForm();
    }
    this.tariffSearchResults.set([]);
    this.tariffSearchAttempted.set(false);
    this.tariffModalNotice.set(null);
    this.error.set(null);
    this.tariffModalOpen.set(true);
  }

  closeTariffModal(): void {
    this.tariffModalOpen.set(false);
    this.resetTariffForm();
  }

  searchTariffCodes(): void {
    const term = this.tariffForm.controls.tariffCodeSearch.value.trim();
    if (!term) {
      this.tariffModalNotice.set('Enter a tariff code prefix to search.');
      return;
    }
    this.tariffSearchAttempted.set(true);
    this.tariffSearching.set(true);
    this.tariffModalNotice.set(null);
    this.referenceData.searchTariffCodeSalesTypes(term).subscribe({
      next: (rows) => {
        this.tariffSearching.set(false);
        this.tariffSearchResults.set(rows);
        if (rows.length === 0) {
          this.tariffModalNotice.set(
            'No tariff codes found for that prefix. Try 01011 or 01012. If search always fails, restart the backend (migration 0016 loads tariff reference data).',
          );
        }
      },
      error: (err) => {
        this.tariffSearching.set(false);
        this.tariffSearchResults.set([]);
        const httpErr = err instanceof HttpErrorResponse ? err : null;
        const body = httpErr?.error as { message?: string; defaultUserMessage?: string } | undefined;
        const serverMsg = body?.defaultUserMessage ?? body?.message ?? '';
        const notFound =
          httpErr?.status === 404 ||
          serverMsg.includes('404') ||
          serverMsg.toLowerCase().includes('not found');
        this.tariffModalNotice.set(
          notFound
            ? 'Tariff search API is not on the running server. Stop the backend, rebuild, and restart (run-dev.ps1) so the new endpoint and migration 0016 are loaded.'
            : serverMsg || 'Tariff search failed — check the API is running and restart the backend for migration 0016.',
        );
      },
    });
  }

  selectTariffCode(option: TariffCodeSalesTypeOption): void {
    this.tariffForm.patchValue({
      tariffCodeSalesTypeId: option.id,
      tariffCode: option.code,
      tariffCodeSearch: option.code,
      tariffDescription: option.description,
    });
    this.message.set(`Selected tariff code ${option.code}.`);
    this.tariffModalNotice.set(null);
  }

  openContactPersonModal(contact?: TempSstContactPerson): void {
    if (contact) {
      this.editingContactPersonId.set(contact.id);
      this.contactPersonForm.patchValue({ name: contact.name, email: contact.email });
    } else {
      this.editingContactPersonId.set(null);
      this.contactPersonForm.reset({ name: '', email: '' });
    }
    this.contactPersonModalOpen.set(true);
  }

  closeContactPersonModal(): void {
    this.contactPersonModalOpen.set(false);
    this.editingContactPersonId.set(null);
    this.contactPersonForm.reset({ name: '', email: '' });
  }

  addContactPerson(): void {
    if (this.contactPersonForm.invalid) {
      this.contactPersonForm.markAllAsTouched();
      this.error.set(
        this.validationSummary(
          this.contactPersonForm,
          this.editingContactPersonId() ? 'Update contact person' : 'Add contact person',
        ),
      );
      this.message.set(null);
      return;
    }
    const editingId = this.editingContactPersonId();
    const body = this.contactPersonForm.getRawValue();
    const persistContact = (id: number, refNo: string | null): void => {
      this.loading.set(true);
      this.error.set(null);
      const request$ = editingId
        ? this.registration.updateContactPerson(id, editingId, body)
        : this.registration.addContactPerson(id, body);
      request$.subscribe({
        next: () => {
          this.loading.set(false);
          this.closeContactPersonModal();
          this.loadSstInfo(id);
          const action = editingId ? 'updated' : 'added';
          this.message.set(
            refNo && !editingId ? `Draft ${refNo} saved. Contact person ${action}.` : `Contact person ${action}.`,
          );
        },
        error: (err) => this.onError(err),
      });
    };
    const caseId = this.caseId();
    if (editingId && caseId) {
      persistContact(caseId, this.caseRefNo());
      return;
    }
    this.ensureDraft((id, refNo) => {
      this.syncRouteAfterDraft(id);
      persistContact(id, refNo);
    });
  }

  private executeRemoveSupportingDocument(documentId: number): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.deleteSupportingDocument(id, documentId).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadSstInfo(id);
        this.message.set('Supporting document removed.');
      },
      error: (err) => this.onError(err),
    });
  }

  private executeRemoveContactPerson(contactPersonId: number): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    if (this.editingContactPersonId() === contactPersonId) {
      this.closeContactPersonModal();
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.deleteContactPerson(id, contactPersonId).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadSstInfo(id);
        this.message.set('Contact person removed.');
      },
      error: (err) => this.onError(err),
    });
  }

  requestDeleteTariff(tariffId: number): void {
    this.pendingDelete.set({ kind: 'tariff', id: tariffId });
  }

  private resetTariffForm(): void {
    this.editingTariffId.set(null);
    this.tariffSearchResults.set([]);
    this.tariffSearchAttempted.set(false);
    this.tariffModalNotice.set(null);
    this.tariffSearching.set(false);
    this.tariffForm.reset({
      tariffCodeSearch: '',
      tariffCodeSalesTypeId: null,
      tariffCode: '',
      tariffDescription: '',
      contractTypeId: MAIN_CONTRACT,
      finishedGoods: '',
    });
  }

  private executeRemoveTariff(tariffId: number): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    if (this.editingTariffId() === tariffId) {
      this.closeTariffModal();
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.deleteTariffCode(id, tariffId).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadSstInfo(id);
        this.message.set('Tariff code removed.');
      },
      error: (err) => this.onError(err),
    });
  }

  tariffDisplayLabel(t: TempSstTariffCode): string {
    if (t.tariffCode) {
      return t.tariffCode;
    }
    return String(t.tariffCodeSalesTypeId);
  }

  submitCase(): void {
    const id = this.caseId();
    if (!id) {
      return;
    }
    const issues = this.previewIssues();
    if (issues.length) {
      this.showFormError(`Cannot submit yet: ${issues[0]}`);
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
          const smk = result.changes?.['salesTaxSmkRegNo'];
          if (typeof smk === 'string') {
            this.salesTaxSmkRegNo.set(smk);
          }
          this.message.set(`Case approved. Employer code: ${result.resourceIdentifier}`);
        } else if (status === 'SUBMITTED' || status === 'IN_PROGRESS') {
          this.message.set(`Case routed. Status: ${status} — awaiting officer approval.`);
        } else {
          this.message.set(`Submitted. Status: ${status}`);
        }
        this.loadCase(id);
        this.scrollToWizardTop();
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
        const smk = result.changes?.['salesTaxSmkRegNo'];
        if (typeof smk === 'string') {
          this.salesTaxSmkRegNo.set(smk);
        }
        this.message.set(`Approved. Employer code: ${result.resourceIdentifier}`);
        this.loadCase(id);
      },
      error: (err) => this.onError(err),
    });
  }

  goToStep(next: number): void {
    if (next === 3) {
      this.enterPreview();
      return;
    }
    this.step.set(next);
    this.message.set(null);
  }

  enterPreview(): void {
    const id = this.caseId();
    if (!id) {
      this.showFormError('Create a draft before opening preview.');
      return;
    }
    this.error.set(null);
    this.loadCase(id, () => {
      this.step.set(3);
      this.message.set(null);
      this.scrollToWizardTop();
    });
  }

  previewIssues(): string[] {
    const issues: string[] = [];
    const f1 = this.form1.getRawValue();
    const f2 = this.form2.getRawValue();

    if (!f1.employerName?.trim()) {
      issues.push('Business name is required — save Form 1');
    }
    if (!f1.registrationNo?.trim()) {
      issues.push('BRN is required — save Form 1');
    }
    if (this.directors().length === 0) {
      issues.push('At least one director is required');
    }
    if (!f2.manComDate || !f2.dateSaleValTaxGoods || !f2.businessComDate) {
      issues.push('Part B dates are incomplete — save Part B');
    }
    if (f2.anTotalTaxSalesVal == null || f2.anTotalTaxSalesVal <= 0) {
      issues.push('Annual taxable sales value must be greater than zero — save Part B');
    }
    if (this.mainTariffCount === 0) {
      issues.push('At least one main-contract tariff code is required');
    }
    if (f2.subContractWork && this.subTariffCount === 0) {
      issues.push('Sub-contract tariff codes are required when sub-contract work is flagged');
    }
    if (!f2.declareTrue) {
      issues.push('Part C declaration must be accepted — save Part C');
    }
    if (f2.declareTrue && !f2.applicantName?.trim()) {
      issues.push('Applicant name is required on Part C');
    }
    return issues;
  }

  get canSubmitFromPreview(): boolean {
    return this.isEditable && this.previewIssues().length === 0;
  }

  previewText(value: string | number | null | undefined): string {
    if (value == null || value === '') {
      return '—';
    }
    return String(value);
  }

  previewDate(value: string | null | undefined): string {
    return value?.trim() ? value : '—';
  }

  previewMoney(value: number | null | undefined): string {
    if (value == null) {
      return '—';
    }
    return `RM ${value.toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  previewYesNo(value: boolean | null | undefined): string {
    return value ? 'Yes' : 'No';
  }

  stateLabel(stateId: number | null | undefined): string {
    if (stateId == null) {
      return '—';
    }
    return this.states().find((s) => s.id === stateId)?.label ?? String(stateId);
  }

  cityLabel(cityId: number | null | undefined, options: RefOption[]): string {
    if (cityId == null) {
      return '—';
    }
    return options.find((c) => c.id === cityId)?.label ?? String(cityId);
  }

  formatBusinessAddressPreview(): string {
    const v = this.form1.getRawValue();
    const parts = [
      v.addressLine1,
      v.addressLine2,
      v.addressLine3,
      v.cityName,
      v.postCode,
      this.stateLabel(v.stateId),
    ].filter((part) => !!part?.toString().trim());
    return parts.length ? parts.join(', ') : '—';
  }

  formatPremiseAddressPreview(): string {
    if (this.form1.controls.sameAsBusinessAddr.value) {
      return 'Same as business correspondence address';
    }
    const v = this.form1.getRawValue();
    const parts = [
      v.corrAddressLine1,
      v.corrAddressLine2,
      v.corrAddressLine3,
      v.corrCityName,
      v.corrPostCode,
      this.stateLabel(v.corrStateId),
    ].filter((part) => !!part?.toString().trim());
    return parts.length ? parts.join(', ') : '—';
  }

  formatPhoneLinesPreview(kind: 'phones' | 'faxes'): string {
    const lines = kind === 'phones' ? this.phoneLines.controls : this.faxLines.controls;
    const formatted = lines
      .map((group) => {
        const head = String(group.get('head')?.value ?? '').trim();
        const back = String(group.get('back')?.value ?? '').trim();
        if (!head && !back) {
          return null;
        }
        return back ? `${head}-${back}` : head;
      })
      .filter((line): line is string => !!line);
    return formatted.length ? formatted.join('; ') : '—';
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

  onSupportingDocumentFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    const typeId = this.selectedDocumentTypeId();
    const caseId = this.caseId();
    if (!caseId || typeId == null) {
      this.showFormError('Select a document type before uploading.');
      input.value = '';
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.uploadSupportingDocument(caseId, typeId, file).subscribe({
      next: () => {
        this.loading.set(false);
        input.value = '';
        this.loadSstInfo(caseId);
        this.message.set(`Uploaded ${file.name}.`);
      },
      error: (err) => {
        input.value = '';
        this.onError(err);
      },
    });
  }

  requestDeleteSupportingDocument(documentId: number): void {
    this.pendingDelete.set({ kind: 'supportingDocument', id: documentId });
  }

  downloadSupportingDocument(doc: TempSstSupportingDocument): void {
    const caseId = this.caseId();
    if (!caseId) {
      return;
    }
    this.registration.downloadSupportingDocument(caseId, doc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = doc.fileName;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.onError(err),
    });
  }

  downloadAcknowledgementLetter(): void {
    const caseId = this.caseId();
    if (!caseId) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.registration.downloadSalesTaxAcknowledgementLetter(caseId).subscribe({
      next: (blob) => {
        this.loading.set(false);
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        const ref = this.caseRefNo()?.replace(/\//g, '-') ?? String(caseId);
        anchor.download = `sales-tax-acknowledgement-${ref}.pdf`;
        anchor.click();
        URL.revokeObjectURL(url);
        this.message.set('Acknowledgement letter downloaded.');
      },
      error: (err) => {
        this.loading.set(false);
        this.onError(err);
      },
    });
  }

  contractLabel(contractTypeId: number): string {
    return contractTypeId === SUB_CONTRACT ? 'Sub-contract' : 'Main contract';
  }

  officeLocationLabel(branchId: number | null): string {
    if (branchId == null) {
      return '';
    }
    return this.officeLocations().find((o) => o.id === branchId)?.label ?? `Branch ${branchId}`;
  }

  businessEntityLabel(entityTypeId: number | null): string {
    if (entityTypeId == null) {
      return '';
    }
    return this.businessEntityTypes().find((e) => e.id === entityTypeId)?.label ?? `Type ${entityTypeId}`;
  }

  identificationTypeLabel(identificationTypeId: number | null): string {
    if (identificationTypeId == null) {
      return '';
    }
    return (
      this.identificationTypes().find((t) => t.id === identificationTypeId)?.label ??
      `ID type ${identificationTypeId}`
    );
  }

  formatPremisesAddress(p: TempPremises): string {
    return [p.addressLine, p.addressLine2, p.addressLine3].filter((line) => !!line?.trim()).join(', ');
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
      return 'Enter a valid email';
    }
    if (control.errors['min']) {
      return `Minimum value is ${control.errors['min'].min}`;
    }
    return 'Invalid value';
  }

  private validationSummary(form: { controls: Record<string, AbstractControl> }, action: string): string {
    const missing = Object.entries(form.controls)
      .filter(([, control]) => control.hasValidator(Validators.required) && control.invalid)
      .map(([name]) => this.fieldLabel(name));
    if (missing.length === 0) {
      return `${action} failed — please check highlighted fields.`;
    }
    return `${action} failed — required: ${missing.join(', ')}.`;
  }

  private fieldLabel(name: string): string {
    const labels: Record<string, string> = {
      employerName: 'Employer name',
      registrationNo: 'BRN',
      businessEntityTypeId: 'Business entity type',
      msicId: 'MSIC / industry code',
      postCode: 'Postcode',
      stateId: 'State',
      cityId: 'City',
      email: 'Email',
      addressLine1: 'Registered address line 1',
      corrAddressLine1: 'Correspondence address line 1',
      corrPostCode: 'Correspondence postcode',
      manComDate: 'Manufacturing commencement',
      dateSaleValTaxGoods: 'Date sale/value taxable',
      businessComDate: 'Business commencement',
      finYrEndMon: 'Financial year end month',
      anTotalTaxSalesVal: 'Annual taxable sales value',
      localSales: 'Local sales',
      exportSales: 'Export',
      salesToDesignArea: 'Sales to design area',
      othersSales: 'Others sales',
      name: 'Director name',
      identificationTypeId: 'Director ID type',
      identificationNo: 'Director ID no',
      addressLine: 'Premises address',
      tariffCodeSalesTypeId: 'Tariff code type',
      contractTypeId: 'Contract type',
      finishedGoods: 'Finished goods',
    };
    return labels[name] ?? name;
  }

  private loadCase(caseId: number, onReady?: () => void): void {
    this.loading.set(true);
    this.error.set(null);
    this.registration.getCase(caseId).subscribe({
      next: (c) => {
        this.caseId.set(c.id);
        this.caseRefNo.set(c.caseRefNo);
        this.appStatus.set(c.appStatus);
        this.form1.patchValue({
          employerName: c.employerName ?? '',
          registrationNo: c.registrationNo ?? '',
          businessEntityTypeId: c.businessEntityTypeId ?? 1,
          msicId: c.msicId,
          serviceTypeId: c.serviceTypeId ?? 1,
          pksBranchId: c.pksBranchId ?? 2,
          methodContributionPaymentId: c.methodContributionPaymentId,
          postCode: c.postCode ?? '',
          email: c.email ?? '',
          addressLine1: c.addressLine1 ?? '',
          addressLine2: c.addressLine2 ?? '',
          addressLine3: c.addressLine3 ?? '',
          stateId: c.stateId,
          cityId: c.cityId,
          cityName: c.cityName ?? '',
          corrAddressLine1: c.corrAddressLine1 ?? '',
          corrAddressLine2: c.corrAddressLine2 ?? '',
          corrAddressLine3: c.corrAddressLine3 ?? '',
          corrPostCode: c.corrPostCode ?? '',
          corrStateId: c.corrStateId,
          corrCityId: c.corrCityId,
          corrCityName: c.corrCityName ?? '',
          sameAsBusinessAddr: this.isCorrespondenceSameAsBusiness(c),
        });
        this.searchForm.patchValue({
          searchRegType: 'BRN',
          searchRegValue: c.registrationNo ?? '',
        });
        this.setContactLines(c.contactPhones, c.contactFaxes, c.phone);
        this.refreshBusinessAddressOptions();
        this.refreshCorrespondenceAddressOptions();
        this.syncForm1EntryState();
        this.loadSstInfo(caseId, onReady);
      },
      error: (err) => this.onError(err),
    });
  }

  private loadSstInfo(caseId: number, onReady?: () => void): void {
    this.registration.getSstInfo(caseId).subscribe({
      next: (sst) => {
        this.loading.set(false);
        this.directors.set(sst.directors ?? []);
        this.premises.set(sst.premises ?? []);
        this.tariffCodes.set(sst.tariffCodes ?? []);
        this.contactPersons.set(sst.contactPersons ?? []);
        this.supportingDocuments.set(sst.supportingDocuments ?? []);
        this.form1.patchValue({
          tradeName: sst.tradeName ?? '',
          tourTaxRegNo: sst.tourTaxRegNo ?? '',
          inTaxRefNo: sst.inTaxRefNo ?? '',
          cusAudRefNo: sst.cusAudRefNo ?? '',
          preRegNo: sst.preRegNo ?? '',
          preRegName: sst.preRegName ?? '',
          dateOfReplacement: sst.dateOfReplacement ?? '',
        });
        if (sst.preRegNo || sst.preRegName || sst.dateOfReplacement) {
          this.showPreRegBlock.set(true);
        }
        if (sst.manComDate) {
          this.form2.patchValue({
            manComDate: sst.manComDate,
            dateSaleValTaxGoods: sst.dateSaleValTaxGoods ?? '',
            finYrEndMon: sst.finYrEndMon ?? 12,
            businessComDate: sst.businessComDate ?? '',
            anTotalTaxSalesVal: sst.anTotalTaxSalesVal ?? 0,
            localSales: sst.localSales ?? 0,
            exportSales: sst.exportSales ?? 0,
            salesToDesignArea: sst.salesToDesignArea ?? 0,
            othersSales: sst.othersSales ?? 0,
            subContractWork: sst.subContractWork ?? false,
          });
        }
        this.form2.patchValue({
          declareTrue: sst.declareTrue ?? false,
          declareDate: sst.declareDate ?? '',
          applicantName: sst.applicantName ?? '',
          identityCard: sst.identityCard ?? '',
          designation: sst.designation ?? '',
          applicantEmail: sst.applicantEmail ?? '',
          applicantTelNo: sst.applicantTelNo ?? '',
        });
        if (sst.declareTrue) {
          this.form2.controls.applicantName.setValidators(Validators.required);
          this.form2.controls.applicantName.updateValueAndValidity({ emitEvent: false });
        }
        const status = this.appStatus();
        if (status && status !== 'NEW' && this.caseId()) {
          this.step.set(3);
        }
        onReady?.();
      },
      error: (err) => this.onError(err),
    });
  }

  private loadReferenceData(): void {
    this.referenceData.listStates().subscribe({
      next: (items) => this.states.set(items),
      error: (err) => this.onError(err),
    });
    this.referenceData.listBusinessEntityTypes().subscribe({
      next: (items) => this.businessEntityTypes.set(items),
      error: (err) => this.onError(err),
    });
    this.referenceData.listIdentificationTypes(true).subscribe({
      next: (items) => this.identificationTypes.set(items),
      error: (err) => this.onError(err),
    });
    this.referenceData.listSupportingDocumentTypes().subscribe({
      next: (items) => {
        this.supportingDocumentTypes.set(items);
        if (items.length > 0 && this.selectedDocumentTypeId() == null) {
          this.selectedDocumentTypeId.set(items[0].id);
        }
      },
      error: (err) => this.onError(err),
    });
  }

  private refreshBusinessAddressOptions(): void {
    const stateId = this.form1.controls.stateId.value;
    const cityId = this.form1.controls.cityId.value;
    this.referenceData.listCities(stateId).subscribe((items) => this.cities.set(items));
    this.referenceData.listPostcodes(stateId, cityId).subscribe((items) => this.postcodes.set(items));
    this.refreshOfficeLocations(this.form1.controls.postCode.value);
  }

  private refreshOfficeLocations(postcode: string | null): void {
    this.referenceData.listOfficeLocations(postcode).subscribe({
      next: (items) => {
        this.officeLocations.set(items);
        if (items.length === 1) {
          this.form1.patchValue({ pksBranchId: items[0].id }, { emitEvent: false });
        } else if (items.length > 0) {
          const current = this.form1.controls.pksBranchId.value;
          if (!items.some((item) => item.id === current)) {
            this.form1.patchValue({ pksBranchId: items[0].id }, { emitEvent: false });
          }
        }
      },
      error: (err) => this.onError(err),
    });
  }

  private refreshCorrespondenceAddressOptions(): void {
    const stateId = this.form1.controls.corrStateId.value;
    const cityId = this.form1.controls.corrCityId.value;
    this.referenceData.listCities(stateId).subscribe((items) => this.corrCities.set(items));
    this.referenceData.listPostcodes(stateId, cityId).subscribe((items) => this.corrPostcodes.set(items));
  }

  private refreshPremisesAddressOptions(): void {
    const stateId = this.premisesForm.controls.stateId.value;
    const cityId = this.premisesForm.controls.cityId.value;
    this.referenceData.listCities(stateId).subscribe((items) => this.premisesCities.set(items));
    this.referenceData.listPostcodes(stateId, cityId).subscribe((items) => this.premisesPostcodes.set(items));
  }

  private onError(err: unknown): void {
    this.loading.set(false);
    const body = (err as { error?: { message?: string; defaultUserMessage?: string } })?.error;
    this.error.set(body?.defaultUserMessage ?? body?.message ?? 'Request failed. Check API is running.');
  }

  private newBrn(): string {
    return `BRN${Date.now().toString().slice(-10)}`;
  }

  private syncTradeNameFromBusiness(): void {
    if (this.form1.controls.sameAsTradeName.value) {
      this.form1.patchValue({ tradeName: this.form1.controls.employerName.value }, { emitEvent: false });
    }
  }

  private showFormError(message: string): void {
    this.error.set(message);
    this.message.set(null);
    this.scrollToWizardTop();
  }

  private scrollToWizardTop(): void {
    queueMicrotask(() => {
      document.getElementById('sales-tax-wizard-top')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      const banner = document.querySelector('#sales-tax-wizard-top .error.banner') as HTMLElement | null;
      banner?.focus({ preventScroll: true });
    });
  }

  private syncForm1EntryState(): void {
    const allowed = this.form1EntryAllowed;
    for (const [name, control] of Object.entries(this.form1.controls)) {
      if (name === 'newRegisterTax') {
        continue;
      }
      if (allowed && control.disabled) {
        control.enable({ emitEvent: false });
      } else if (!allowed && control.enabled) {
        control.disable({ emitEvent: false });
      }
    }
    if (allowed && this.contactForm.disabled) {
      this.contactForm.enable({ emitEvent: false });
    } else if (!allowed && this.contactForm.enabled) {
      this.contactForm.disable({ emitEvent: false });
    }
  }

  private syncCorrespondenceFromBusiness(): void {
    if (this.form1.controls.sameAsBusinessAddr.value) {
      const v = this.form1.getRawValue();
      this.form1.patchValue(
        {
          corrAddressLine1: v.addressLine1,
          corrAddressLine2: v.addressLine2,
          corrAddressLine3: v.addressLine3,
          corrPostCode: v.postCode,
          corrStateId: v.stateId,
          corrCityId: v.cityId,
          corrCityName: v.cityName,
        },
        { emitEvent: false },
      );
      this.refreshCorrespondenceAddressOptions();
    }
  }

  private clearCorrespondenceAddress(): void {
    this.form1.patchValue(
      {
        corrAddressLine1: '',
        corrAddressLine2: '',
        corrAddressLine3: '',
        corrPostCode: '',
        corrStateId: null,
        corrCityId: null,
        corrCityName: '',
      },
      { emitEvent: false },
    );
    this.corrCities.set([]);
    this.corrPostcodes.set([]);
  }

  private isCorrespondenceSameAsBusiness(c: {
    addressLine1: string | null;
    addressLine2: string | null;
    addressLine3: string | null;
    postCode: string | null;
    stateId: number | null;
    cityId: number | null;
    corrAddressLine1: string | null;
    corrAddressLine2: string | null;
    corrAddressLine3: string | null;
    corrPostCode: string | null;
    corrStateId: number | null;
    corrCityId: number | null;
  }): boolean {
    const corrEmpty =
      !c.corrAddressLine1?.trim() &&
      !c.corrAddressLine2?.trim() &&
      !c.corrAddressLine3?.trim() &&
      !c.corrPostCode?.trim() &&
      c.corrStateId == null &&
      c.corrCityId == null;
    if (corrEmpty) {
      return false;
    }
    return (
      (c.corrAddressLine1 ?? '') === (c.addressLine1 ?? '') &&
      (c.corrAddressLine2 ?? '') === (c.addressLine2 ?? '') &&
      (c.corrAddressLine3 ?? '') === (c.addressLine3 ?? '') &&
      (c.corrPostCode ?? '') === (c.postCode ?? '') &&
      c.corrStateId === c.stateId &&
      c.corrCityId === c.cityId
    );
  }

  private createCasePayload() {
    const v = this.form1.getRawValue();
    return {
      employerName: v.employerName,
      registrationNo: v.registrationNo,
      serviceTypeId: v.serviceTypeId,
      pksBranchId: v.pksBranchId,
      postCode: v.postCode,
      msicId: v.msicId ?? undefined,
      isBranch: false,
      methodContributionPaymentId: v.methodContributionPaymentId ?? undefined,
    };
  }

  private employerUpdatePayload() {
    this.syncCorrespondenceFromBusiness();
    const v = this.form1.getRawValue();
    const contacts = this.contactLinesPayload();
    return {
      employerName: v.employerName,
      businessEntityTypeId: v.businessEntityTypeId,
      msicId: v.msicId ?? undefined,
      isBranch: false,
      serviceTypeId: v.serviceTypeId,
      pksBranchId: v.pksBranchId,
      methodContributionPaymentId: v.methodContributionPaymentId ?? undefined,
      postCode: v.postCode,
      email: v.email,
      phone: contacts.phone,
      contactPhones: contacts.contactPhones,
      contactFaxes: contacts.contactFaxes,
      addressLine1: v.addressLine1,
      addressLine2: v.addressLine2,
      addressLine3: v.addressLine3,
      stateId: v.stateId ?? undefined,
      cityId: v.cityId ?? undefined,
      cityName: v.cityName || undefined,
      corrAddressLine1: v.corrAddressLine1 || undefined,
      corrAddressLine2: v.corrAddressLine2 || undefined,
      corrAddressLine3: v.corrAddressLine3 || undefined,
      corrPostCode: v.corrPostCode || undefined,
      corrStateId: v.corrStateId ?? undefined,
      corrCityId: v.corrCityId ?? undefined,
      corrCityName: v.corrCityName || undefined,
    };
  }

  private sstForm1Payload() {
    const v = this.form1.getRawValue();
    return {
      tradeName: v.tradeName || undefined,
      tourTaxRegNo: v.tourTaxRegNo || undefined,
      inTaxRefNo: v.inTaxRefNo || undefined,
      cusAudRefNo: v.cusAudRefNo || undefined,
      preRegNo: this.showPreRegBlock() ? v.preRegNo || undefined : undefined,
      preRegName: this.showPreRegBlock() ? v.preRegName || undefined : undefined,
      dateOfReplacement: this.showPreRegBlock() ? v.dateOfReplacement || undefined : undefined,
    };
  }

  private persistForm1(caseId: number, onSuccess: () => void): void {
    this.registration.updateCase(caseId, this.employerUpdatePayload()).subscribe({
      next: () => {
        this.registration.upsertSstInfo(caseId, this.sstForm1Payload()).subscribe({
          next: onSuccess,
          error: (err) => this.onError(err),
        });
      },
      error: (err) => this.onError(err),
    });
  }

  private applyTaxPayerProfile(profile: TaxPayerRegistrationProfile): void {
    this.pendingImportDirectors = profile.directors ?? [];
    this.pendingImportPremises = profile.premises ?? [];
    this.form1.patchValue({
      employerName: profile.employerName ?? '',
      registrationNo: profile.registrationNo ?? '',
      businessEntityTypeId: profile.businessEntityTypeId ?? 1,
      msicId: profile.msicId,
      serviceTypeId: profile.serviceTypeId ?? 1,
      pksBranchId: profile.pksBranchId ?? 2,
      email: profile.email ?? '',
    });
    if (profile.phone?.trim()) {
      this.setContactLines(null, null, profile.phone);
    }
    if (profile.pksBranchId) {
      this.refreshOfficeLocations(this.form1.controls.postCode.value);
    }
  }

  private importTaxPayerChildren(caseId: number, onComplete: () => void): void {
    const directors = [...this.pendingImportDirectors];
    const premises = [...this.pendingImportPremises];
    this.pendingImportDirectors = [];
    this.pendingImportPremises = [];

    const importNextDirector = (index: number): void => {
      if (index >= directors.length) {
        importNextPremises(0);
        return;
      }
      const director = directors[index];
      this.registration
        .addDirector(caseId, {
          name: director.name,
          identificationTypeId: director.identificationTypeId ?? 2,
          identificationNo: director.identificationNo,
          email: director.email ?? undefined,
          designation: director.designation ?? undefined,
        })
        .subscribe({
          next: () => importNextDirector(index + 1),
          error: (err) => this.onError(err),
        });
    };

    const importNextPremises = (index: number): void => {
      if (index >= premises.length) {
        this.loadSstInfo(caseId);
        onComplete();
        return;
      }
      const p = premises[index];
      this.registration
        .addPremises(caseId, {
          name: p.name,
          addressLine: p.addressLine,
          addressLine2: p.addressLine2 ?? undefined,
          addressLine3: p.addressLine3 ?? undefined,
          postCode: p.postCode ?? undefined,
          cityName: p.cityName ?? undefined,
          stateName: p.stateName ?? undefined,
        })
        .subscribe({
          next: () => importNextPremises(index + 1),
          error: (err) => this.onError(err),
        });
    };

    if (directors.length === 0) {
      importNextPremises(0);
    } else {
      importNextDirector(0);
    }
  }

  private newContactLine(head = '+60', back = ''): FormGroup {
    return this.fb.nonNullable.group({ head: [head], back: [back] });
  }

  private contactLinesPayload(): { contactPhones: string; contactFaxes: string; phone?: string } {
    const phones = this.serializeContactLines(this.phoneLines);
    const faxes = this.serializeContactLines(this.faxLines);
    return {
      contactPhones: JSON.stringify(phones),
      contactFaxes: JSON.stringify(faxes),
      phone: this.formatPrimaryContact(phones) || undefined,
    };
  }

  private serializeContactLines(lines: FormArray<FormGroup>): ContactLine[] {
    return (lines.getRawValue() as ContactLine[]).filter((line) => line.back?.trim());
  }

  private formatPrimaryContact(lines: ContactLine[]): string {
    const first = lines.find((line) => line.back?.trim());
    if (!first) {
      return '';
    }
    return `${first.head}${first.back}`.replace(/\s+/g, '');
  }

  private setContactLines(
    phonesJson: string | null | undefined,
    faxesJson: string | null | undefined,
    legacyPhone?: string | null,
  ): void {
    this.phoneLines.clear();
    this.faxLines.clear();
    const phones = this.parseContactLines(phonesJson, legacyPhone);
    const faxes = this.parseContactLines(faxesJson);
    for (const line of phones.length ? phones : [{ head: '+60', back: '' }]) {
      this.phoneLines.push(this.newContactLine(line.head, line.back));
    }
    for (const line of faxes.length ? faxes : [{ head: '+60', back: '' }]) {
      this.faxLines.push(this.newContactLine(line.head, line.back));
    }
  }

  private parseContactLines(json: string | null | undefined, legacyPhone?: string | null): ContactLine[] {
    if (json) {
      try {
        const parsed = JSON.parse(json) as ContactLine[];
        if (Array.isArray(parsed) && parsed.length) {
          return parsed;
        }
      } catch {
        // use legacy fallback below
      }
    }
    if (legacyPhone?.trim()) {
      return [{ head: '+60', back: legacyPhone.replace(/^\+60/, '') }];
    }
    return [];
  }
}
