import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'assist-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = this.fb.nonNullable.group({
    username: ['admin', Validators.required],
    password: ['password', Validators.required],
  });

  errorMessage: string | null = null;
  submitting = false;

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('reason') === 'unauthorized') {
      this.errorMessage = 'Session expired or login required. Sign in again.';
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, password } = this.form.getRawValue();
    this.errorMessage = null;
    this.submitting = true;
    this.auth.loginAndValidate(username, password).subscribe({
      next: () => {
        this.submitting = false;
        void this.router.navigate(['/employers']);
      },
      error: (err: unknown) => {
        this.submitting = false;
        this.errorMessage = this.describeLoginError(err);
      },
    });
  }

  private describeLoginError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 401) {
        return 'Invalid username or password.';
      }
      if (err.status === 0) {
        return 'Cannot reach the API. Start the backend with .\\run-dev.ps1 and use ng serve (proxy to port 8081).';
      }
      return `Login failed (HTTP ${err.status}). Check the backend logs.`;
    }
    return 'Login failed. Start the backend with .\\run-dev.ps1 and try again.';
  }
}
