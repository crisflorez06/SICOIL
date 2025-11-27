import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FiltrosDTO } from '../models/filtros';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FiltroService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/filtros`;


  getFiltros(): Observable<FiltrosDTO> {
    return this.http.get<FiltrosDTO>(this.apiUrl);
  }
}
