import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CommandProcessingResult,
  CreateDirectorRequest,
  CreateContactPersonRequest,
  CreateEmployeeRequest,
  CreatePremisesRequest,
  CreateRegistrationCaseRequest,
  CreateTariffCodeRequest,
  RegistrationCase,
  RegistrationCaseSummary,
  TaxPayerRegistrationProfile,
  TempDirectorOwner,
  TempEmployee,
  TempPremises,
  TempSstContactPerson,
  TempSstInfo,
  TempSstSupportingDocument,
  TempSstTariffCode,
  UpdateRegistrationCaseRequest,
  UpsertSstInfoRequest,
} from '../core/models/registration.model';

@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/registration-cases`;

  getCase(caseId: number): Observable<RegistrationCase> {
    return this.http.get<RegistrationCase>(`${this.base}/id/${caseId}`);
  }

  listCases(params?: { appStatus?: string; sectionId?: number; limit?: number }): Observable<RegistrationCaseSummary[]> {
    let httpParams = new HttpParams();
    if (params?.appStatus) {
      httpParams = httpParams.set('appStatus', params.appStatus);
    }
    if (params?.sectionId != null) {
      httpParams = httpParams.set('sectionId', String(params.sectionId));
    }
    if (params?.limit != null) {
      httpParams = httpParams.set('limit', String(params.limit));
    }
    return this.http.get<RegistrationCaseSummary[]>(this.base, { params: httpParams });
  }

  createCase(body: CreateRegistrationCaseRequest): Observable<CommandProcessingResult> {
    return this.http.post<CommandProcessingResult>(this.base, body);
  }

  updateCase(caseId: number, body: UpdateRegistrationCaseRequest): Observable<CommandProcessingResult> {
    return this.http.put<CommandProcessingResult>(`${this.base}/${caseId}`, body);
  }

  submitCase(caseId: number): Observable<CommandProcessingResult> {
    return this.http.post<CommandProcessingResult>(`${this.base}/${caseId}?command=submit`, {});
  }

  approveCase(caseId: number): Observable<CommandProcessingResult> {
    return this.http.post<CommandProcessingResult>(`${this.base}/${caseId}?command=approve`, {});
  }

  queryCase(caseId: number, remark: string): Observable<CommandProcessingResult> {
    return this.http.post<CommandProcessingResult>(`${this.base}/${caseId}?command=query`, { remark });
  }

  rejectCase(caseId: number, reason: string): Observable<CommandProcessingResult> {
    return this.http.post<CommandProcessingResult>(`${this.base}/${caseId}?command=reject`, { reason });
  }

  listEmployees(caseId: number): Observable<TempEmployee[]> {
    return this.http.get<TempEmployee[]>(`${this.base}/${caseId}/employees`);
  }

  addEmployee(caseId: number, body: CreateEmployeeRequest): Observable<TempEmployee> {
    return this.http.post<TempEmployee>(`${this.base}/${caseId}/employees`, body);
  }

  getSstInfo(caseId: number): Observable<TempSstInfo> {
    return this.http.get<TempSstInfo>(`${this.base}/${caseId}/sst-info`);
  }

  upsertSstInfo(caseId: number, body: UpsertSstInfoRequest): Observable<TempSstInfo> {
    return this.http.put<TempSstInfo>(`${this.base}/${caseId}/sst-info`, body);
  }

  addDirector(caseId: number, body: CreateDirectorRequest): Observable<TempDirectorOwner> {
    return this.http.post<TempDirectorOwner>(`${this.base}/${caseId}/sst-info/directors`, body);
  }

  updateDirector(caseId: number, directorId: number, body: CreateDirectorRequest): Observable<TempDirectorOwner> {
    return this.http.put<TempDirectorOwner>(`${this.base}/${caseId}/sst-info/directors/${directorId}`, body);
  }

  deleteDirector(caseId: number, directorId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${caseId}/sst-info/directors/${directorId}`);
  }

  addPremises(caseId: number, body: CreatePremisesRequest): Observable<TempPremises> {
    return this.http.post<TempPremises>(`${this.base}/${caseId}/sst-info/premises`, body);
  }

  updatePremises(caseId: number, premisesId: number, body: CreatePremisesRequest): Observable<TempPremises> {
    return this.http.put<TempPremises>(`${this.base}/${caseId}/sst-info/premises/${premisesId}`, body);
  }

  deletePremises(caseId: number, premisesId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${caseId}/sst-info/premises/${premisesId}`);
  }

  searchTaxPayer(searchType: string, searchValue: string): Observable<TaxPayerRegistrationProfile> {
    return this.http.get<TaxPayerRegistrationProfile>(`${environment.apiBaseUrl}/registration/tax-payer-search`, {
      params: { searchType, searchValue },
    });
  }

  addTariffCode(caseId: number, body: CreateTariffCodeRequest): Observable<TempSstTariffCode> {
    return this.http.post<TempSstTariffCode>(`${this.base}/${caseId}/sst-info/tariff-codes`, body);
  }

  updateTariffCode(caseId: number, tariffId: number, body: CreateTariffCodeRequest): Observable<TempSstTariffCode> {
    return this.http.put<TempSstTariffCode>(`${this.base}/${caseId}/sst-info/tariff-codes/${tariffId}`, body);
  }

  deleteTariffCode(caseId: number, tariffId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${caseId}/sst-info/tariff-codes/${tariffId}`);
  }

  addContactPerson(caseId: number, body: CreateContactPersonRequest): Observable<TempSstContactPerson> {
    return this.http.post<TempSstContactPerson>(`${this.base}/${caseId}/sst-info/contact-persons`, body);
  }

  updateContactPerson(
    caseId: number,
    contactPersonId: number,
    body: CreateContactPersonRequest,
  ): Observable<TempSstContactPerson> {
    return this.http.put<TempSstContactPerson>(
      `${this.base}/${caseId}/sst-info/contact-persons/${contactPersonId}`,
      body,
    );
  }

  deleteContactPerson(caseId: number, contactPersonId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${caseId}/sst-info/contact-persons/${contactPersonId}`);
  }

  uploadSupportingDocument(
    caseId: number,
    documentTypeId: number,
    file: File,
  ): Observable<TempSstSupportingDocument> {
    const form = new FormData();
    form.append('documentTypeId', String(documentTypeId));
    form.append('file', file, file.name);
    return this.http.post<TempSstSupportingDocument>(
      `${this.base}/${caseId}/sst-info/supporting-documents`,
      form,
    );
  }

  deleteSupportingDocument(caseId: number, documentId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${caseId}/sst-info/supporting-documents/${documentId}`);
  }

  downloadSupportingDocument(caseId: number, documentId: number): Observable<Blob> {
    return this.http.get(`${this.base}/${caseId}/sst-info/supporting-documents/${documentId}/content`, {
      responseType: 'blob',
    });
  }

  downloadSalesTaxLetter(
    caseId: number,
    letterType: 'acknowledgement' | 'inquiry' | 'rejection' = 'acknowledgement',
    format: 'pdf' | 'html' = 'pdf',
  ): Observable<Blob> {
    return this.http.get(`${this.base}/${caseId}/sst-info/acknowledgement-letter`, {
      params: { letterType, format },
      responseType: 'blob',
    });
  }

  /** @deprecated use {@link #downloadSalesTaxLetter} */
  downloadSalesTaxAcknowledgementLetter(caseId: number, format: 'pdf' | 'html' = 'pdf'): Observable<Blob> {
    return this.downloadSalesTaxLetter(caseId, 'acknowledgement', format);
  }
}
