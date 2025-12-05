import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Page } from '../core/types/page';
import { KardexFiltro, KardexResponse } from '../models/kardex.model';

@Injectable({ providedIn: 'root' })
export class KardexService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/kardex`;

  listar(filtro: KardexFiltro = {}): Observable<Page<KardexResponse>> {
    let params = new HttpParams()
      .set('page', (filtro.page ?? 0).toString())
      .set('size', (filtro.size ?? 20).toString());

    if (filtro.sort) {
      params = params.set('sort', filtro.sort);
    }
    if (filtro.productoId) {
      params = params.set('productoId', filtro.productoId.toString());
    }
    if (filtro.usuarioId) {
      params = params.set('usuarioId', filtro.usuarioId.toString());
    }
    if (filtro.nombreProducto) {
      params = params.set('nombreProducto', filtro.nombreProducto);
    }
    if (filtro.tipo) {
      params = params.set('tipo', filtro.tipo);
    }
    if (filtro.desde) {
      params = params.set('desde', filtro.desde);
    }
    if (filtro.hasta) {
      params = params.set('hasta', filtro.hasta);
    }

    return this.http
      .get<Page<KardexResponse>>(this.baseUrl, { params, observe: 'response' })
      .pipe(
        map((response: HttpResponse<Page<KardexResponse>>) => {
          if (response.body) {
            return response.body;
          }
          return {
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: filtro.size ?? 20,
            number: filtro.page ?? 0,
          };
        })
      );
  }
}
