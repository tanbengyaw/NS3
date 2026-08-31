import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Employer, EmployerSearchType } from '../core/models/employer.model';
import { Page } from '../core/models/page.model';

@Injectable({ providedIn: 'root' })
export class EmployersService {
  private readonly http = inject(HttpClient);

  search(
    searchType: EmployerSearchType,
    searchValue: string,
    offset = 0,
    limit = 20,
  ): Observable<Page<Employer>> {
    let params = new HttpParams().set('offset', offset).set('limit', limit);
    if (searchValue.trim()) {
      params = params.set('searchType', searchType).set('searchValue', searchValue.trim());
    }
    return this.http.get<Page<Employer>>(`${environment.apiBaseUrl}/employers`, { params });
  }
}
