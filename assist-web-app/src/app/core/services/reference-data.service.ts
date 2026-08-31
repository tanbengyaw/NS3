import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PostcodeOption, RefOption, TariffCodeSalesTypeOption } from '../models/reference.model';

@Injectable({ providedIn: 'root' })
export class ReferenceDataService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/reference/address`;

  listStates(): Observable<RefOption[]> {
    return this.http.get<RefOption[]>(`${this.base}/states`);
  }

  listCities(stateId: number | null | undefined): Observable<RefOption[]> {
    if (stateId == null) {
      return this.http.get<RefOption[]>(`${this.base}/cities`);
    }
    const params = new HttpParams().set('stateId', String(stateId));
    return this.http.get<RefOption[]>(`${this.base}/cities`, { params });
  }

  listPostcodes(stateId?: number | null, cityId?: number | null): Observable<PostcodeOption[]> {
    let params = new HttpParams();
    if (stateId != null) {
      params = params.set('stateId', String(stateId));
    }
    if (cityId != null) {
      params = params.set('cityId', String(cityId));
    }
    return this.http.get<PostcodeOption[]>(`${this.base}/postcodes`, { params });
  }

  listOfficeLocations(postcode: string | null | undefined): Observable<RefOption[]> {
    if (!postcode?.trim()) {
      return this.http.get<RefOption[]>(`${this.base}/office-locations`);
    }
    const params = new HttpParams().set('postcode', postcode.trim());
    return this.http.get<RefOption[]>(`${this.base}/office-locations`, { params });
  }

  listBusinessEntityTypes(): Observable<RefOption[]> {
    return this.http.get<RefOption[]>(`${environment.apiBaseUrl}/reference/business-entities`);
  }

  listIdentificationTypes(directorFormOnly = true): Observable<RefOption[]> {
    const params = new HttpParams().set('directorFormOnly', String(directorFormOnly));
    return this.http.get<RefOption[]>(`${environment.apiBaseUrl}/reference/identification-types`, { params });
  }

  searchTariffCodeSalesTypes(search: string): Observable<TariffCodeSalesTypeOption[]> {
    const params = new HttpParams().set('search', search.trim());
    return this.http.get<TariffCodeSalesTypeOption[]>(
      `${environment.apiBaseUrl}/reference/tariff-code-sales-types`,
      { params },
    );
  }
}
