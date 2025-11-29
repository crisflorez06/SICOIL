import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ActualizarGastoRequest, CrearGastoRequest, Gasto } from '../models/compra.model';

@Injectable({ providedIn: 'root' })
export class GastoService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/compras`;

  listar(filtros?: { nombre?: string; desde?: string; hasta?: string }): Observable<Gasto[]> {
    let params = new HttpParams();
    if (filtros) {
      Object.entries(filtros).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params = params.set(key, value);
        }
      });
    }

    return this.http.get<Gasto[]>(this.apiUrl, { observe: 'response', params }).pipe(
      map((response: HttpResponse<Gasto[]>) => {
        if (response.status === 204 || !response.body) {
          return [];
        }
        return response.body;
      })
    );
  }

  obtenerPorId(id: number): Observable<Gasto> {
    return this.http.get<Gasto>(`${this.apiUrl}/${id}`);
  }

  crear(payload: CrearGastoRequest): Observable<Gasto> {
    return this.http.post<Gasto>(this.apiUrl, payload);
  }

  actualizar(id: number, payload: ActualizarGastoRequest): Observable<Gasto> {
    return this.http.put<Gasto>(`${this.apiUrl}/${id}`, payload);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
