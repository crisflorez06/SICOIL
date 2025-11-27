import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  // señal pública para que el componente la observe
  readonly isLoading = signal(false);

  // manejamos un contador para soportar peticiones concurrentes
  private activeRequests = 0;

  show(): void {
    this.activeRequests++;
    if (!this.isLoading()) this.isLoading.set(true);
  }

  hide(): void {
    this.activeRequests = Math.max(0, this.activeRequests - 1);
    if (this.activeRequests === 0 && this.isLoading()) {
      this.isLoading.set(false);
    }
  }

  // opcional: reset total si algo queda “colgado”
  reset(): void {
    this.activeRequests = 0;
    this.isLoading.set(false);
  }
}
