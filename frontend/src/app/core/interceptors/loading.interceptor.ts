import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loading = inject(LoadingService);

  // Permitir saltar el loader agregando un header opcional
  const skip = req.headers.get('x-skip-loader') === 'true';

  if (!skip) {
    loading.show();
  }

  return next(req).pipe(
    finalize(() => {
      if (!skip) loading.hide();
    })
  );
};
