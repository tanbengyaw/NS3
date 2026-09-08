import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'assist-role-home-redirect',
  standalone: true,
  template: '',
})
export class RoleHomeRedirectComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    if (this.auth.isPortalEmployer()) {
      void this.router.navigateByUrl('/base/tax-registrant');
      return;
    }
    void this.router.navigateByUrl('/employers');
  }
}
