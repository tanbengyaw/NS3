import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PostcodeOption, PortalDocTypeOption, RefOption, SstServiceTypeOption, SupportingDocumentTypeOption, TariffCodeSalesTypeOption } from '../models/reference.model';
import { DiscontinueTaxSearchResult, TaxPayerUpdateSearchResult } from '../models/registration.model';

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
      return of([]);
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

  listPortalIdentificationTypes(): Observable<RefOption[]> {
    const params = new HttpParams()
      .set('directorFormOnly', 'false')
      .set('portalFormOnly', 'true');
    return this.http.get<RefOption[]>(`${environment.apiBaseUrl}/reference/identification-types`, { params });
  }

  listRegNoTypes(): Observable<RefOption[]> {
    return this.http.get<RefOption[]>(`${environment.apiBaseUrl}/reference/reg-no-types`);
  }

  searchTariffCodeSalesTypes(search: string): Observable<TariffCodeSalesTypeOption[]> {
    const params = new HttpParams().set('search', search.trim());
    return this.http.get<TariffCodeSalesTypeOption[]>(
      `${environment.apiBaseUrl}/reference/tariff-code-sales-types`,
      { params },
    );
  }

  searchSstServiceTypes(search: string): Observable<SstServiceTypeOption[]> {
    const params = new HttpParams().set('search', search.trim());
    return this.http.get<SstServiceTypeOption[]>(
      `${environment.apiBaseUrl}/reference/sst-service-types`,
      { params },
    );
  }

  listSupportingDocumentTypes(): Observable<SupportingDocumentTypeOption[]> {
    return this.http.get<SupportingDocumentTypeOption[]>(
      `${environment.apiBaseUrl}/reference/supporting-document-types`,
    );
  }

  listPortalDocTypes(): Observable<PortalDocTypeOption[]> {
    return this.http.get<PortalDocTypeOption[]>(`${environment.apiBaseUrl}/reference/portal-doc-types`);
  }

  listBranches(): Observable<RefOption[]> {
    return this.http.get<RefOption[]>(`${environment.apiBaseUrl}/reference/branches`);
  }

  searchTaxPayerUpdates(taxType: string, search: string): Observable<TaxPayerUpdateSearchResult[]> {
    const params = new HttpParams().set('taxType', taxType).set('search', search.trim());
    return this.http.get<TaxPayerUpdateSearchResult[]>(
      `${environment.apiBaseUrl}/reference/tax-payer-updates`,
      { params },
    );
  }

  searchDiscontinueTaxPayers(taxType: string, search: string): Observable<DiscontinueTaxSearchResult[]> {
    const params = new HttpParams().set('taxType', taxType).set('search', search.trim());
    return this.http.get<DiscontinueTaxSearchResult[]>(
      `${environment.apiBaseUrl}/reference/discontinue-tax-payers`,
      { params },
    );
  }
}
