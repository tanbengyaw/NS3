import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'assist-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly session = this.auth.currentSession;

  get isOfficer(): boolean {
    return this.auth.hasStaffAccess();
  }

  get isAdmin(): boolean {
    return this.auth.hasRole('ADMIN');
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
