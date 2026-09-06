import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

const STORAGE_KEY = 'assist.auth';

export interface AuthSession {
  username: string;
  password: string;
  roles: string[];
  branchId?: number | null;
}

interface CurrentStaffUserResponse {
  username: string;
  roles: string[];
  branchId: number | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<AuthSession | null>(this.loadSession());

  readonly currentSession = this.session.asReadonly();

  login(username: string, password: string): void {
    // Roles are unknown until the post-login profile fetch in loginAndValidate() completes —
    // store credentials now (needed for the Authorization header on that very next request).
    const session: AuthSession = { username, password, roles: [] };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.session.set(session);
  }

  /**
   * Stores credentials, then fetches the user's own staff profile (roles + branch) from the
   * backend to both verify the credentials and populate real, DB-backed roles for the nav/route
   * guards — no hardcoded username-to-role mapping to maintain on the frontend.
   */
  loginAndValidate(username: string, password: string): Observable<void> {
    this.login(username, password);
    return this.http.get<CurrentStaffUserResponse>(`${environment.apiBaseUrl}/staff-users/me`).pipe(
      map((profile) => {
        this.updateSessionProfile(profile.roles ?? [], profile.branchId ?? null);
      }),
      catchError((err) => {
        this.logout();
        return throwError(() => err);
      }),
    );
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }

  isLoggedIn(): boolean {
    return this.session() !== null;
  }

  hasRole(role: string): boolean {
    return this.session()?.roles.includes(role) ?? false;
  }

  hasStaffAccess(): boolean {
    return this.hasAnyRole('ADMIN', 'OFFICER', 'RO', 'UO', 'PKR_BO');
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  getAuthorizationHeader(): string | null {
    const current = this.session();
    if (!current) {
      return null;
    }
    const token = btoa(`${current.username}:${current.password}`);
    return `Basic ${token}`;
  }

  private loadSession(): AuthSession | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthSession;
    } catch {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private updateSessionProfile(roles: string[], branchId: number | null): void {
    const current = this.session();
    if (!current) {
      return;
    }
    const updated: AuthSession = { ...current, roles, branchId };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    this.session.set(updated);
  }
}
