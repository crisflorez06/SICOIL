import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MensajeService } from '../../services/mensaje.service';
import { ReporteGeneralResponse } from '../../models/reporte.model';
import { ProductoResponse } from '../../models/producto.model';
import { ReporteService } from '../../services/reporte.service';
import { ApiErrorService } from '../../core/services/api-error.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.css'],
})
export class ReportesComponent implements OnInit {
  private reporteService = inject(ReporteService);
  private mensajeService = inject(MensajeService);
  private apiErrorService = inject(ApiErrorService);

  reporteGeneral: ReporteGeneralResponse | null = null;
  productosStockBajo: ProductoResponse[] = [];

  fechaInicio = '';
  fechaFin = '';
  cargando = false;

  ngOnInit(): void {
    const hoy = new Date().toISOString().split('T')[0];
    this.fechaInicio = hoy;
    this.fechaFin = hoy;
    this.cargarProductosStockBajo();
  }

  generarReportes(): void {
    if (!this.fechaInicio || !this.fechaFin) {
      this.mensajeService.error('Debe seleccionar ambas fechas.');
      return;
    }

    this.cargando = true;

    this.reporteService.getReporteGeneral(this.fechaInicio, this.fechaFin).subscribe({
      next: (res) => {
        this.reporteGeneral = res as ReporteGeneralResponse;
        this.cargando = false;
      },

      error: (error) => {
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al cargar el reporte general.',
        });
        this.cargando = false;
      },
    });

    this.cargarProductosStockBajo();
  }

  private cargarProductosStockBajo(): void {
    this.reporteService.getProductosConStockBajo().subscribe({
      next: (res) => (this.productosStockBajo = res),
      error: (error) =>
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al cargar el stock bajo.',
        }),
    });
  }
}
