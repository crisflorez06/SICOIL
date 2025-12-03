import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs';

import { CapitalService } from '../../services/capital.service';
import {
  CapitalMovimientoFiltro,
  CapitalMovimientoResponse,
  CapitalOrigen,
  CapitalResumenResponse,
  CapitalInyeccionRequest,
  CapitalTopProducto,
  CapitalTopCliente,
} from '../../models/capital.model';
import { MensajeService } from '../../services/mensaje.service';
import { ApiErrorService } from '../../core/services/api-error.service';
import { InyeccionCapitalDialogComponent } from '../../shared/components/inyeccion-capital-dialog/inyeccion-capital-dialog.component';

type EstadoCarga = 'idle' | 'cargando' | 'error' | 'listo';

@Component({
  selector: 'app-capital',
  standalone: true,
  imports: [CommonModule, FormsModule, MatTooltipModule],
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

  limpiarResumenFiltros(): void {
    if (!this.filtroDesde && !this.filtroHasta) {
      return;
    }
    this.filtroDesde = '';
    this.filtroHasta = '';
    this.cargarResumen();
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

  get resumenListo(): boolean {
    return this.estadoResumen === 'listo' && !!this.resumen;
  }

  get comparativoFlujo(): { label: string; valor: number; descripcion: string; clase: 'entradas' | 'salidas' | 'pendiente' }[] {
    if (!this.resumen) {
      return [];
    }
    return [
      {
        label: 'Total ventas',
        valor: Math.max(this.resumen.totalEntradas, 0),
        descripcion: 'Ventas registradas',
        clase: 'entradas',
      },
      {
        label: 'Total compras',
        valor: Math.max(this.resumen.totalSalidas, 0),
        descripcion: 'Compras e inversión',
        clase: 'salidas',
      },
      {
        label: 'Crédito pendiente',
        valor: Math.max(this.resumen.totalCreditoPendiente, 0),
        descripcion: 'Por cobrar',
        clase: 'pendiente',
      },
    ];
  }

  get maxFlujoValor(): number {
    if (!this.resumen) {
      return 0;
    }
    return this.comparativoFlujo.reduce((max, item) => (item.valor > max ? item.valor : max), 0);
  }

  get productosDestacados(): CapitalTopProducto[] {
    return this.resumen?.topProductos ?? [];
  }

  get clientesDestacados(): CapitalTopCliente[] {
    return this.resumen?.topClientes ?? [];
  }

  trackProducto(index: number, producto: CapitalTopProducto): number {
    return producto.productoId ?? index;
  }

  montoColor(valor: number): string {
    if (valor > 0) {
      return 'text-success';
    }
    if (valor < 0) {
      return 'text-danger';
    }
    return 'text-muted';
  }

  get creditoComparativo(): {
    label: string;
    valor: number;
    descripcion: string;
    clase: 'otorgado' | 'abonos' | 'pendiente';
  }[] {
    if (!this.resumen) {
      return [];
    }
    return [
      {
        label: 'Otorgado',
        valor: Math.max(this.resumen.totalCredito, 0),
        descripcion: 'Crédito entregado',
        clase: 'otorgado',
      },
      {
        label: 'Abonos',
        valor: Math.max(this.resumen.totalAbonos, 0),
        descripcion: 'Cobrado',
        clase: 'abonos',
      },
      {
        label: 'Pendiente',
        valor: Math.max(this.resumen.totalCreditoPendiente, 0),
        descripcion: 'Por cobrar',
        clase: 'pendiente',
      },
    ];
  }

  get maxCreditoValor(): number {
    return this.creditoComparativo.reduce((max, item) => (item.valor > max ? item.valor : max), 0);
  }

  get rentabilidadPorcentaje(): number {
    if (!this.resumen || this.resumen.totalEntradas === 0) {
      return 0;
    }
    return (this.resumen.totalGanancias / Math.abs(this.resumen.totalEntradas)) * 100;
  }

  get rentabilidadEsPositiva(): boolean {
    return this.rentabilidadPorcentaje >= 0;
  }

  get rentabilidadGaugeFill(): number {
    return Math.min(Math.abs(this.rentabilidadPorcentaje), 100);
  }

  get rentabilidadGaugeBackground(): string {
    const color = this.rentabilidadEsPositiva ? '#198754' : '#dc3545';
    const fill = this.rentabilidadGaugeFill;
    return `conic-gradient(${color} ${fill}%, #edf0f4 ${fill}% 100%)`;
  }

  trackCliente(index: number, cliente: CapitalTopCliente): number {
    return cliente.clienteId ?? index;
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
