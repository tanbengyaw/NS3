import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CommandProcessingResult } from '../core/models/registration.model';
import { AuditCaseDetail, AuditCaseListing } from './audit.models';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/audit-cases`;

  search(search = ''): Observable<AuditCaseListing[]> {
    const params = new HttpParams().set('search', search);
    return this.http.get<AuditCaseListing[]>(this.base, { params });
  }

  createDraft(): Observable<CommandProcessingResult> {
    return this.http.post<CommandProcessingResult>(this.base, {});
  }

  get(caseId: number): Observable<AuditCaseDetail> {
    return this.http.get<AuditCaseDetail>(`${this.base}/${caseId}`);
  }

  save(caseId: number, body: unknown): Observable<CommandProcessingResult> {
    return this.http.put<CommandProcessingResult>(`${this.base}/${caseId}`, body);
  }

  submit(caseId: number, body: unknown): Observable<CommandProcessingResult> {
    return this.http.post<CommandProcessingResult>(`${this.base}/${caseId}`, body, {
      params: { command: 'submit' },
    });
  }

  savePlanning(caseId: number, body: unknown): Observable<CommandProcessingResult> {
    return this.http.put<CommandProcessingResult>(`${this.base}/${caseId}/planning`, body);
  }

  saveFieldWork(caseId: number, body: unknown): Observable<CommandProcessingResult> {
    return this.http.put<CommandProcessingResult>(`${this.base}/${caseId}/field-work`, body);
  }

  createWorkingPaper(caseId: number): Observable<CommandProcessingResult> {
    return this.http.post<CommandProcessingResult>(`${this.base}/${caseId}/working-papers`, {});
  }

  saveWorkingPaper(caseId: number, workingPaperId: number, body: unknown): Observable<CommandProcessingResult> {
    return this.http.put<CommandProcessingResult>(`${this.base}/${caseId}/working-papers/${workingPaperId}`, body);
  }

  saveFindings(caseId: number, body: unknown): Observable<CommandProcessingResult> {
    return this.http.put<CommandProcessingResult>(`${this.base}/${caseId}/findings`, body);
  }

  saveTaxpayerResponse(caseId: number, body: unknown): Observable<CommandProcessingResult> {
    return this.http.put<CommandProcessingResult>(`${this.base}/${caseId}/taxpayer-response`, body);
  }
}
