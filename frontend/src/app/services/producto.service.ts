import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  IngresoProductoRequest,
  InventarioSalidaRequest,
  PaginaProductoResponse,
  ProductoActualizarRequest,
  ProductoRequest,
  ProductoResponse,
} from '../models/producto.model';

@Injectable({ providedIn: 'root' })
export class ProductoService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/productos`;

  listar(params: { page?: number; size?: number; nombre?: string } = {}): Observable<PaginaProductoResponse> {
    let httpParams = new HttpParams()
      .set('page', (params.page ?? 0).toString())
      .set('size', (params.size ?? 20).toString());

    if (params.nombre && params.nombre.trim() !== '') {
      httpParams = httpParams.set('nombre', params.nombre.trim());
    }

    return this.http.get<PaginaProductoResponse>(this.baseUrl, { params: httpParams });
  }

  crear(payload: ProductoRequest): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(this.baseUrl, payload);
  }

  actualizar(nombreAnterior: string, payload: ProductoActualizarRequest): Observable<boolean> {
    const nombreParam = encodeURIComponent(nombreAnterior.trim());
    return this.http.put<boolean>(`${this.baseUrl}/${nombreParam}`, payload);
  }

  registrarIngresos(lista: IngresoProductoRequest[]): Observable<ProductoResponse[]> {
    return this.http.post<ProductoResponse[]>(`${this.baseUrl}/ingreso`, lista);
  }

  eliminarStock(id: number, payload: InventarioSalidaRequest): Observable<ProductoResponse> {
    return this.http.patch<ProductoResponse>(`${this.baseUrl}/${id}/stock/eliminar`, payload);
  }
}
