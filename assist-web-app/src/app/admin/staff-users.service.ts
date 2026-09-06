import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CreateStaffUserRequest, StaffUser, UpdateStaffUserRequest } from '../core/models/staff-user.model';

@Injectable({ providedIn: 'root' })
export class StaffUsersService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/staff-users`;

  listStaffUsers(): Observable<StaffUser[]> {
    return this.http.get<StaffUser[]>(this.base);
  }

  getStaffUser(staffUserId: number): Observable<StaffUser> {
    return this.http.get<StaffUser>(`${this.base}/${staffUserId}`);
  }

  createStaffUser(body: CreateStaffUserRequest): Observable<StaffUser> {
    return this.http.post<StaffUser>(this.base, body);
  }

  updateStaffUser(staffUserId: number, body: UpdateStaffUserRequest): Observable<StaffUser> {
    return this.http.put<StaffUser>(`${this.base}/${staffUserId}`, body);
  }
}
