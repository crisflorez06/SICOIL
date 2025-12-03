import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CarteraAbonoDetalleResponse,
  CarteraAbonoRequest,
  CarteraCreditoDetalleResponse,
  CarteraFechaFiltro,
  CarteraPendienteFiltro,
  CarteraResumenResponse,
} from '../models/cartera.model';

@Injectable({ providedIn: 'root' })
export class CarteraService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/cartera`;

  listarPendientes(filtro: CarteraPendienteFiltro = {}): Observable<CarteraResumenResponse[]> {
    let params = new HttpParams();
    if (filtro.cliente) {
      params = params.set('cliente', filtro.cliente);
    }
    if (filtro.desde) {
      params = params.set('desde', filtro.desde);
    }
    if (filtro.hasta) {
      params = params.set('hasta', filtro.hasta);
    }
    return this.http.get<CarteraResumenResponse[]>(`${this.baseUrl}/pendientes`, { params });
  }

  listarAbonos(clienteId: number, filtro: CarteraFechaFiltro = {}): Observable<CarteraAbonoDetalleResponse[]> {
    return this.http.get<CarteraAbonoDetalleResponse[]>(
      `${this.baseUrl}/clientes/${clienteId}/abonos`,
      { params: this.buildFechaParams(filtro) }
    );
  }

  listarCreditos(clienteId: number, filtro: CarteraFechaFiltro = {}): Observable<CarteraCreditoDetalleResponse[]> {
    return this.http.get<CarteraCreditoDetalleResponse[]>(
      `${this.baseUrl}/clientes/${clienteId}/creditos`,
      { params: this.buildFechaParams(filtro) }
    );
  }

  registrarAbono(clienteId: number, request: CarteraAbonoRequest): Observable<CarteraAbonoDetalleResponse[]> {
    return this.http.post<CarteraAbonoDetalleResponse[]>(`${this.baseUrl}/clientes/${clienteId}/abonos`, request);
  }

  eliminarAbono(clienteId: number, movimientoId: number, request: CarteraAbonoRequest): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/clientes/${clienteId}/abonos/${movimientoId}/eliminar`, request);
  }

  private buildFechaParams(filtro: CarteraFechaFiltro = {}): HttpParams {
    let params = new HttpParams();
    if (filtro.desde) {
      params = params.set('desde', filtro.desde);
    }
    if (filtro.hasta) {
      params = params.set('hasta', filtro.hasta);
    }
    return params;
  }
}
