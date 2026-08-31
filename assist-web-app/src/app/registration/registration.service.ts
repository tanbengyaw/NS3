import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CommandProcessingResult,
  CreateDirectorRequest,
  CreateEmployeeRequest,
  CreatePremisesRequest,
  CreateRegistrationCaseRequest,
  CreateTariffCodeRequest,
  RegistrationCase,
  TaxPayerRegistrationProfile,
  TempDirectorOwner,
  TempEmployee,
  TempPremises,
  TempSstInfo,
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
}
