import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { VentaPageResponse, VentaRequest, VentaResponse } from '../models/venta.model';
import { environment } from '../../environments/environment';
import { Page } from '../core/types/page';

import { DetalleVentaResponse } from '../models/detalle-venta.model';

@Injectable({ providedIn: 'root' })
export class VentaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/ventas`;

  obtenerTodos(
    page: number,
    size: number,
    filtros?: {
      metodoPago?: string;
      desde?: string;
      hasta?: string;
      minTotal?: number | null;
      maxTotal?: number | null;
    },
    sort: string = 'fecha',
    direction: 'asc' | 'desc' = 'asc'
  ): Observable<VentaPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sort},${direction}`);

    if (filtros) {
      Object.entries(filtros).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params = params.set(key, value.toString());
        }
      });
    }

    return this.http.get<VentaPageResponse>(this.apiUrl, { params, observe: 'response' }).pipe(
      map((response: HttpResponse<VentaPageResponse>) => {
        if (response.status === 204 || !response.body) {
          return {
            ventas: {
              content: [],
              totalElements: 0,
              totalPages: 0,
              number: page,
              size: size,
            } as Page<VentaResponse>,
            totalGeneral: 0,
          };
        }
        return response.body as VentaPageResponse;
      })
    );
  }
  crearVenta(venta: VentaRequest): Observable<VentaResponse> {
    return this.http.post<VentaResponse>(this.apiUrl, venta);
  }

  actualizarVenta(ventaId: number, venta: VentaRequest): Observable<VentaResponse> {
    return this.http.put<VentaResponse>(`${this.apiUrl}/${ventaId}`, venta);
  }

  eliminarVenta(ventaId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${ventaId}`);
  }

  obtenerDetallesPorVenta(ventaId: number): Observable<DetalleVentaResponse[]> {
    return this.http.get<DetalleVentaResponse[]>(`${this.apiUrl}/${ventaId}/detalles`);
  }

  buscarProductos(
    page: number,
    size: number,
    filtros?: {
      nombreProducto?: string;
      desde?: string;
      hasta?: string;
    },
    sort: string = 'venta.fecha',
    direction: 'asc' | 'desc' = 'desc'
  ): Observable<Page<DetalleVentaResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sort},${direction}`);

    if (filtros) {
      Object.entries(filtros).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params = params.set(key, value.toString());
        }
      });
    }

    return this.http
      .get<Page<DetalleVentaResponse>>(`${this.apiUrl}/detalles`, { params, observe: 'response' })
      .pipe(
        map((response: HttpResponse<Page<DetalleVentaResponse>>) => {
          if (response.status === 204) {
            return {
              content: [],
              totalElements: 0,
              totalPages: 0,
              number: page,
              size: size,
            } as Page<DetalleVentaResponse>;
          }
          return response.body as Page<DetalleVentaResponse>;
        })
      );
  }
}
