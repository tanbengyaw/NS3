import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const authorization = auth.getAuthorizationHeader();
  const authedReq =
    authorization && !req.headers.has('Authorization')
      ? req.clone({ setHeaders: { Authorization: authorization } })
      : req;

  return next(authedReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        auth.logout();
        if (!router.url.startsWith('/login')) {
          void router.navigate(['/login'], { queryParams: { reason: 'unauthorized' } });
        }
      }
      return throwError(() => err);
    }),
  );
};
