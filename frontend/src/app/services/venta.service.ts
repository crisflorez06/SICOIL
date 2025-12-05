import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  PaginaVentaResponse,
  VentaAnulacionRequest,
  VentaListadoFiltro,
  VentaRequest,
  VentaResponse,
} from '../models/venta.model';
import { VentaFiltrosResponse } from '../models/filtros.model';

@Injectable({ providedIn: 'root' })
export class VentaService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/ventas`;

  listar(filtros: VentaListadoFiltro = {}): Observable<PaginaVentaResponse> {
    let params = new HttpParams()
      .set('page', (filtros.page ?? 0).toString())
      .set('size', (filtros.size ?? 10).toString());

    if (filtros.tipoVenta) {
      params = params.set('tipoVenta', filtros.tipoVenta);
    }
    if (filtros.nombreCliente) {
      params = params.set('nombreCliente', filtros.nombreCliente);
    }
    if (filtros.nombreUsuario) {
      params = params.set('nombreUsuario', filtros.nombreUsuario);
    }
    if (typeof filtros.activa === 'boolean') {
      params = params.set('activa', filtros.activa.toString());
    }
    if (filtros.desde) {
      params = params.set('desde', filtros.desde);
    }
    if (filtros.hasta) {
      params = params.set('hasta', filtros.hasta);
    }

    return this.http.get<PaginaVentaResponse>(this.baseUrl, { params });
  }

  crear(request: VentaRequest): Observable<VentaResponse> {
    return this.http.post<VentaResponse>(this.baseUrl, request);
  }

  anular(ventaId: number, request: VentaAnulacionRequest): Observable<VentaResponse> {
    return this.http.patch<VentaResponse>(`${this.baseUrl}/${ventaId}/anular`, request);
  }

  obtenerFiltrosRegistro(): Observable<VentaFiltrosResponse> {
    return this.http.get<VentaFiltrosResponse>(`${environment.apiUrl}/filtros`);
  }

  descargarComprobante(ventaId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${ventaId}/comprobante`, {
      responseType: 'blob',
    });
  }
}
