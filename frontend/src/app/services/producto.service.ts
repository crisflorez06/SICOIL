import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { MovimientoEntradaMasivaRequest, ProductoRequest, ProductoResponse } from '../models/producto.model';
import { MovimientoResponse, MovimientoTipo } from '../models/movimiento.model';
import { environment } from '../../environments/environment';
import { Page } from '../core/types/page';

@Injectable({ providedIn: 'root' })
export class ProductoService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/productos`;
  private movimientosUrl = `${environment.apiUrl}/movimientos`;

  obtenerTodos(
    page: number,
    size: number,
    filtros?: {
      nombre?: string;
      categoria?: null;
      estado?: boolean;
    },
    sort: string = 'nombre',
    direction: 'asc' | 'desc' = 'asc'
  ): Observable<Page<ProductoResponse>> {
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

    return this.http.get<Page<ProductoResponse>>(this.apiUrl, { params, observe: 'response' }).pipe(
      map((response: HttpResponse<Page<ProductoResponse>>) => {
        if (response.status === 204) {
          return {
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: page,
            size: size,
          } as Page<ProductoResponse>;
        }
        return response.body as Page<ProductoResponse>;
      })
    );
  }

  crearProducto(producto: ProductoRequest): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(this.apiUrl, producto);
  }

  actualizar(id: number, producto: ProductoRequest): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(`${this.apiUrl}/${id}`, producto);
  }

  cambiarEstadoProducto(id: number): Observable<ProductoResponse> {
    return this.http.patch<ProductoResponse>(`${this.apiUrl}/${id}/estado`, {});
  }

  sumarStock(id: number, cantidad: number, observacion?: string | null): Observable<ProductoResponse> {
    const body: Record<string, unknown> = { cantidad };
    if (observacion !== undefined && observacion !== null && observacion !== '') {
      body['observacion'] = observacion;
    }
    return this.http.patch<ProductoResponse>(`${this.apiUrl}/${id}/agregar`, body);
  }

  actualizarStockMasivo(request: MovimientoEntradaMasivaRequest): Observable<ProductoResponse[]> {
    return this.http.patch<ProductoResponse[]>(`${this.apiUrl}/agregar-masivo`, request);
  }

  obtenerMovimientos(
    page: number,
    size: number,
    filtros?: {
      productoId?: number | null;
      tipo?: MovimientoTipo | null;
      desde?: string | null;
      hasta?: string | null;
    },
    sort: string = 'fechaMovimiento',
    direction: 'asc' | 'desc' = 'desc'
  ): Observable<Page<MovimientoResponse>> {
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

    return this.http.get<Page<MovimientoResponse>>(this.movimientosUrl, { params, observe: 'response' }).pipe(
      map((response: HttpResponse<Page<MovimientoResponse>>) => {
        if (response.status === 204) {
          return {
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: page,
            size: size,
          } as Page<MovimientoResponse>;
        }
        return response.body as Page<MovimientoResponse>;
      })
    );
  }

  actualizarMovimiento(
    id: number,
    payload: { cantidad: number; observacion?: string | null }
  ): Observable<MovimientoResponse> {
    return this.http.put<MovimientoResponse>(`${this.movimientosUrl}/${id}`, payload);
  }

  eliminarMovimiento(id: number, observacion?: string | null): Observable<MovimientoResponse> {
    let params = new HttpParams();
    if (observacion !== undefined && observacion !== null && observacion.trim() !== '') {
      params = params.set('observacion', observacion.trim());
    }
    return this.http.delete<MovimientoResponse>(`${this.movimientosUrl}/${id}`, {
      params,
    });
  }
}
