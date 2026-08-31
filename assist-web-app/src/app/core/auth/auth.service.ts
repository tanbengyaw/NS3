import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'assist.auth';

export interface AuthSession {
  username: string;
  password: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly session = signal<AuthSession | null>(this.loadSession());

  readonly currentSession = this.session.asReadonly();

  login(username: string, password: string): void {
    const roles = this.resolveRoles(username);
    const session: AuthSession = { username, password, roles };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.session.set(session);
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

  private resolveRoles(username: string): string[] {
    switch (username) {
      case 'admin':
        return ['ADMIN', 'OFFICER'];
      case 'ro':
        return ['RO'];
      case 'employer':
        return ['EMPLOYER'];
      default:
        return [];
    }
  }
}
