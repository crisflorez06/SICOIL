import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, finalize, tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private storageKey = 'sicoil-auth-usuario';
  private authBaseUrl = environment.authUrl ?? `${this.normalizeBaseUrl(environment.apiUrl)}/auth`;

  private usuarioSubject = new BehaviorSubject<LoginResponse | null>(this.cargarUsuarioPersistido());
  readonly usuario$ = this.usuarioSubject.asObservable();

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authBaseUrl}/login`, payload, { withCredentials: true }).pipe(
      tap((usuario) => this.persistirUsuario(usuario))
    );
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.authBaseUrl}/logout`, {}, { withCredentials: true })
      .pipe(finalize(() => this.clearSession()));
  }

  clearSession(): void {
    sessionStorage.removeItem(this.storageKey);
    this.usuarioSubject.next(null);
  }

  estaAutenticado(): boolean {
    return this.usuarioSubject.getValue() !== null;
  }

  getUsuarioActual(): LoginResponse | null {
    return this.usuarioSubject.getValue();
  }

  private persistirUsuario(usuario: LoginResponse): void {
    sessionStorage.setItem(this.storageKey, JSON.stringify(usuario));
    this.usuarioSubject.next(usuario);
  }

  private cargarUsuarioPersistido(): LoginResponse | null {
    const raw = sessionStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as LoginResponse;
      if (parsed && typeof parsed.id === 'number' && typeof parsed.usuario === 'string') {
        return parsed;
      }
    } catch {
      sessionStorage.removeItem(this.storageKey);
    }
    return null;
  }

  private normalizeBaseUrl(url: string): string {
    if (!url) {
      return '';
    }
    return url.endsWith('/api') ? url.slice(0, -4) : url;
  }
}
