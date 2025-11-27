import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProductoResponse } from '../models/producto.model';
import { ReporteGeneralResponse } from '../models/reporte.model';

@Injectable({
  providedIn: 'root',
})
export class ReporteService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/reportes`;

  getReporteGeneral(
    fechaInicio: string,
    fechaFin: string
  ): Observable<ReporteGeneralResponse> {
    const params = new HttpParams()
      .set('fechaInicio', fechaInicio)
      .set('fechaFin', fechaFin);

    return this.http.get<ReporteGeneralResponse>(`${this.apiUrl}/generales`, {
      params,
    });
  }

  getProductosConStockBajo(
    umbral?: number
  ): Observable<ProductoResponse[]> {
    let params = new HttpParams();
    if (umbral !== undefined && umbral !== null) {
      params = params.set('umbral', umbral.toString());
    }

    return this.http.get<ProductoResponse[]>(`${this.apiUrl}/stock-bajo`, {
      params,
    });
  }
}
