import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';

import { CapitalService } from '../../services/capital.service';
import {
  CapitalMovimientoFiltro,
  CapitalMovimientoResponse,
  CapitalOrigen,
  CapitalResumenResponse,
  CapitalInyeccionRequest,
} from '../../models/capital.model';
import { MensajeService } from '../../services/mensaje.service';
import { ApiErrorService } from '../../core/services/api-error.service';
import { InyeccionCapitalDialogComponent } from '../../shared/components/inyeccion-capital-dialog/inyeccion-capital-dialog.component';

type EstadoCarga = 'idle' | 'cargando' | 'error' | 'listo';

@Component({
  selector: 'app-capital',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './capital.component.html',
  styleUrls: ['./capital.component.css'],
})
export class CapitalComponent implements OnInit {
  private capitalService = inject(CapitalService);
  private mensajeService = inject(MensajeService);
  private dialog = inject(MatDialog);
  private apiErrorService = inject(ApiErrorService);

  movimientos: CapitalMovimientoResponse[] = [];
  estadoMovimientos: EstadoCarga = 'idle';
  estadoResumen: EstadoCarga = 'idle';
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  tamanoPagina = 10;

  filtroOrigen: CapitalOrigen | '' = '';
  filtroCredito: '' | 'true' | 'false' = '';
  filtroReferencia = '';
  filtroDescripcion = '';
  filtroDesde = '';
  filtroHasta = '';

  resumen: CapitalResumenResponse | null = null;
  registrandoInyeccion = false;

  ngOnInit(): void {
    this.buscarMovimientos();
    this.cargarResumen();
  }

  cargarResumen(): void {
    this.estadoResumen = 'cargando';
    this.capitalService
      .obtenerResumen({
        desde: this.filtroDesde || undefined,
        hasta: this.filtroHasta || undefined,
      })
      .subscribe({
        next: (data) => {
          this.resumen = data;
          this.estadoResumen = 'listo';
        },
        error: () => {
          this.estadoResumen = 'error';
          this.mensajeService.error('No se pudo cargar el resumen de capital.');
        },
      });
  }

  buscarMovimientos(pagina = 0): void {
    this.estadoMovimientos = 'cargando';
    const filtro: CapitalMovimientoFiltro = {
      page: pagina,
      size: this.tamanoPagina,
      sort: 'creadoEn,desc',
    };

    if (this.filtroOrigen) {
      filtro.origen = this.filtroOrigen;
    }

    if (this.filtroCredito === 'true') {
      filtro.esCredito = true;
    } else if (this.filtroCredito === 'false') {
      filtro.esCredito = false;
    }

    const referenciaId = Number(this.filtroReferencia.trim());
    if (!Number.isNaN(referenciaId) && referenciaId > 0) {
      filtro.referenciaId = referenciaId;
    }

    if (this.filtroDescripcion.trim()) {
      filtro.descripcion = this.filtroDescripcion.trim();
    }

    if (this.filtroDesde) {
      filtro.desde = this.filtroDesde;
    }

    if (this.filtroHasta) {
      filtro.hasta = this.filtroHasta;
    }

    this.capitalService.listarMovimientos(filtro).subscribe({
      next: (page) => {
        this.movimientos = page.content ?? [];
        this.totalPaginas = page.totalPages ?? 0;
        this.totalElementos = page.totalElements ?? 0;
        this.paginaActual = page.number ?? pagina;
        this.estadoMovimientos = 'listo';
      },
      error: () => {
        this.estadoMovimientos = 'error';
        this.mensajeService.error('No se pudieron cargar los movimientos de capital.');
      },
    });
  }

  buscar(): void {
    this.paginaActual = 0;
    this.buscarMovimientos(0);
    this.cargarResumen();
  }

  limpiarFiltros(): void {
    if (
      this.filtroOrigen === '' &&
      this.filtroCredito === '' &&
      this.filtroReferencia === '' &&
      this.filtroDescripcion === '' &&
      this.filtroDesde === '' &&
      this.filtroHasta === ''
    ) {
      return;
    }
    this.filtroOrigen = '';
    this.filtroCredito = '';
    this.filtroReferencia = '';
    this.filtroDescripcion = '';
    this.filtroDesde = '';
    this.filtroHasta = '';
    this.buscar();
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 0 || pagina === this.paginaActual || pagina >= this.totalPaginas) {
      return;
    }
    this.buscarMovimientos(pagina);
  }

  registrarInyeccion(): void {
    const dialogRef = this.dialog.open(InyeccionCapitalDialogComponent, {
      width: '480px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((payload?: CapitalInyeccionRequest | null) => {
      if (!payload) {
        return;
      }
      this.registrandoInyeccion = true;
      this.capitalService
        .registrarInyeccion(payload)
        .pipe(
          finalize(() => {
            this.registrandoInyeccion = false;
          }),
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Inyección registrada correctamente.');
            this.buscar();
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo registrar la inyección.' });
          },
        });
    });
  }

  get hayMovimientos(): boolean {
    return this.movimientos.length > 0;
  }

  get mostrarPaginador(): boolean {
    return this.totalPaginas > 0;
  }

  get paginas(): number[] {
    return Array.from({ length: this.totalPaginas }, (_, index) => index);
  }

  origenLegible(origen: CapitalOrigen): string {
    switch (origen) {
      case 'VENTA':
        return 'Venta';
      case 'COMPRA':
        return 'Compra';
      case 'INYECCION':
        return 'Inyección';
      default:
        return origen;
    }
  }

  montoClase(origen: CapitalOrigen): string {
    return origen === 'COMPRA' ? 'text-danger' : 'text-success';
  }
}
