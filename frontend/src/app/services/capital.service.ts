import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Page } from '../core/types/page';
import {
  CapitalInyeccionRequest,
  CapitalMovimientoFiltro,
  CapitalMovimientoResponse,
  CapitalResumenFiltro,
  CapitalResumenResponse,
} from '../models/capital.model';

@Injectable({ providedIn: 'root' })
export class CapitalService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/capital`;

  listarMovimientos(filtro: CapitalMovimientoFiltro = {}): Observable<Page<CapitalMovimientoResponse>> {
    let params = new HttpParams()
      .set('page', (filtro.page ?? 0).toString())
      .set('size', (filtro.size ?? 10).toString());

    if (filtro.sort) {
      params = params.set('sort', filtro.sort);
    }
    if (filtro.origen) {
      params = params.set('origen', filtro.origen);
    }
    if (typeof filtro.esCredito === 'boolean') {
      params = params.set('esCredito', filtro.esCredito.toString());
    }
    if (filtro.referenciaId) {
      params = params.set('referenciaId', filtro.referenciaId.toString());
    }
    if (filtro.descripcion) {
      params = params.set('descripcion', filtro.descripcion);
    }
    if (filtro.desde) {
      params = params.set('desde', filtro.desde);
    }
    if (filtro.hasta) {
      params = params.set('hasta', filtro.hasta);
    }

    return this.http.get<Page<CapitalMovimientoResponse>>(`${this.baseUrl}/movimientos`, { params });
  }

  obtenerResumen(filtro: CapitalResumenFiltro = {}): Observable<CapitalResumenResponse> {
    let params = new HttpParams();
    if (filtro.desde) {
      params = params.set('desde', filtro.desde);
    }
    if (filtro.hasta) {
      params = params.set('hasta', filtro.hasta);
    }

    return this.http.get<CapitalResumenResponse>(`${this.baseUrl}/resumen`, { params });
  }

  registrarInyeccion(request: CapitalInyeccionRequest): Observable<CapitalMovimientoResponse> {
    return this.http.post<CapitalMovimientoResponse>(`${this.baseUrl}/inyecciones`, request);
  }
}
