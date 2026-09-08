import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  SstNotificationItem,
  TaxRegistrantRegistrationInfo,
  TaxRegistrantSummary,
} from './tax-registrant.model';

@Injectable({ providedIn: 'root' })
export class TaxRegistrantService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/tax-registrant/me`;

  getSummary(): Observable<TaxRegistrantSummary> {
    return this.http.get<TaxRegistrantSummary>(`${this.base}/summary`);
  }

  listNotifications(): Observable<SstNotificationItem[]> {
    return this.http.get<SstNotificationItem[]>(`${this.base}/notifications`);
  }

  getRegistrationInfo(): Observable<TaxRegistrantRegistrationInfo> {
    return this.http.get<TaxRegistrantRegistrationInfo>(`${this.base}/registration-info`);
  }
}
