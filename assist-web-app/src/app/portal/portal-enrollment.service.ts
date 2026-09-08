import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  PortalDraftDocument,
  PortalEnrollmentRequest,
  PortalEnrollmentResubmitRequest,
  PortalUser,
} from '../core/models/portal.model';

@Injectable({ providedIn: 'root' })
export class PortalEnrollmentService {
  private readonly http = inject(HttpClient);
  private readonly enrollmentsBase = `${environment.apiBaseUrl}/portal-enrollments`;
  private readonly usersBase = `${environment.apiBaseUrl}/portal-users`;

  submitEnrollment(body: PortalEnrollmentRequest): Observable<PortalUser> {
    return this.http.post<PortalUser>(this.enrollmentsBase, body);
  }

  getPortalUser(username: string): Observable<PortalUser> {
    return this.http.get<PortalUser>(`${this.usersBase}/${encodeURIComponent(username)}`);
  }

  queryEnrollment(username: string, remark: string): Observable<PortalUser> {
    return this.http.post<PortalUser>(`${this.usersBase}/${encodeURIComponent(username)}/query`, { remark });
  }

  approveEnrollment(username: string, password: string): Observable<PortalUser> {
    return this.http.post<PortalUser>(`${this.usersBase}/${encodeURIComponent(username)}/approve`, { password });
  }

  rejectEnrollment(username: string): Observable<PortalUser> {
    return this.http.post<PortalUser>(`${this.usersBase}/${encodeURIComponent(username)}/reject`, {});
  }

  resubmitEnrollment(username: string, body: PortalEnrollmentResubmitRequest): Observable<PortalUser> {
    return this.http.put<PortalUser>(
      `${this.enrollmentsBase}/${encodeURIComponent(username)}/resubmit`,
      body,
    );
  }

  listDraftDocuments(draftToken: string): Observable<PortalDraftDocument[]> {
    return this.http.get<PortalDraftDocument[]>(
      `${environment.apiBaseUrl}/portal-enrollment-drafts/${draftToken}/documents`,
    );
  }

  uploadDraftDocument(draftToken: string, documentTypeId: number, file: File): Observable<PortalDraftDocument> {
    const formData = new FormData();
    formData.append('documentTypeId', String(documentTypeId));
    formData.append('file', file, file.name);
    return this.http.post<PortalDraftDocument>(
      `${environment.apiBaseUrl}/portal-enrollment-drafts/${draftToken}/documents`,
      formData,
    );
  }

  deleteDraftDocument(draftToken: string, documentId: number): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiBaseUrl}/portal-enrollment-drafts/${draftToken}/documents/${documentId}`,
    );
  }
}
