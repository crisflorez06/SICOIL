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
import { RetiroCapitalDialogComponent } from '../../shared/components/retiro-capital-dialog/retiro-capital-dialog.component';

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
  registrandoRetiro = false;
  tablaScrollActiva = false;
  productoSeleccionadoIndex = 0;
  clienteSeleccionadoIndex = 0;
  private readonly participacionColores = ['#0ea5e9', '#f97316', '#10b981', '#a855f7', '#f43f5e', '#22d3ee'];

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
          this.reiniciarSeleccionParticipacion();
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

  registrarRetiroGanancia(): void {
    const dialogRef = this.dialog.open(RetiroCapitalDialogComponent, {
      width: '480px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((payload?: CapitalInyeccionRequest | null) => {
      if (!payload) {
        return;
      }
      this.registrandoRetiro = true;
      this.capitalService
        .registrarRetiroGanancia(payload)
        .pipe(
          finalize(() => {
            this.registrandoRetiro = false;
          }),
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Retiro registrado correctamente.');
            this.buscar();
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo registrar el retiro.' });
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

  get comparativoFlujo(): {
    label: string;
    valor: number;
    descripcion: string;
    clase: 'entradas' | 'salidas' | 'pendiente' | 'credito';
  }[] {
    if (!this.resumen) {
      return [];
    }
    return [
      {
        label: 'Total ventas contado',
        valor: Math.max(this.resumen.totalEntradas, 0),
        descripcion: 'Ventas cobradas o abonadas',
        clase: 'entradas',
      },
      {
        label: 'Total ventas crédito',
        valor: Math.max(this.resumen.totalCreditoPendiente, 0),
        descripcion: 'Saldo pendiente en ventas a crédito',
        clase: 'credito',
      },
      {
        label: 'Total compras',
        valor: Math.max(this.resumen.totalSalidas, 0),
        descripcion: 'Compras e inversión',
        clase: 'salidas',
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

  get productoSeleccionado(): CapitalTopProducto | null {
    const productos = this.productosDestacados;
    if (!productos.length) {
      return null;
    }
    const index = Math.min(this.productoSeleccionadoIndex, productos.length - 1);
    return productos[index];
  }

  get clienteSeleccionado(): CapitalTopCliente | null {
    const clientes = this.clientesDestacados;
    if (!clientes.length) {
      return null;
    }
    const index = Math.min(this.clienteSeleccionadoIndex, clientes.length - 1);
    return clientes[index];
  }

  get productosDonutBackground(): string {
    return this.generarConicGradient(this.productosDestacados);
  }

  get clientesDonutBackground(): string {
    return this.generarConicGradient(this.clientesDestacados);
  }

  colorParticipacion(index: number): string {
    return this.participacionColores[index % this.participacionColores.length];
  }

  seleccionarProducto(index: number): void {
    const maxIndex = Math.max(this.productosDestacados.length - 1, 0);
    this.productoSeleccionadoIndex = Math.min(Math.max(index, 0), maxIndex);
  }

  seleccionarCliente(index: number): void {
    const maxIndex = Math.max(this.clientesDestacados.length - 1, 0);
    this.clienteSeleccionadoIndex = Math.min(Math.max(index, 0), maxIndex);
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

  get ventasMensualesSerie(): { label: string; valor: number }[] {
    if (!this.resumen || !Array.isArray(this.resumen.ventasMensuales)) {
      return [];
    }
    return this.resumen.ventasMensuales.map((item) => ({
      label: this.formatearMes(item.mes),
      valor: Math.max(item.total ?? 0, 0),
    }));
  }

  get maxVentasMensuales(): number {
    return this.ventasMensualesSerie.reduce((max, item) => (item.valor > max ? item.valor : max), 0);
  }

  get ventasMensualesColumnas(): { label: string; valor: number; porcentaje: number }[] {
    const max = this.maxVentasMensuales || 1;
    return this.ventasMensualesSerie.map((item) => ({
      ...item,
      porcentaje: max > 0 ? (item.valor / max) * 100 : 0,
    }));
  }

  get totalVentasPeriodo(): number {
    if (!this.resumen) {
      return 0;
    }
    const contado = Math.max(this.resumen.totalEntradas ?? 0, 0);
    const creditoPendiente = Math.max(this.resumen.totalCreditoPendiente ?? 0, 0);
    return contado + creditoPendiente;
  }

  get rentabilidadPorcentaje(): number {
    const totalVentas = this.totalVentasPeriodo;
    if (!this.resumen || totalVentas === 0) {
      return 0;
    }
    return (this.resumen.totalGanancias / Math.abs(totalVentas)) * 100;
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
      case 'ABONO':
        return 'Abono';
      case 'COMPRA':
        return 'Compra';
      case 'INYECCION':
        return 'Inyección';
      case 'RETIROGANANCIA':
        return 'Retiro de ganancia';
      default:
        return origen;
    }
  }

  montoClase(origen: CapitalOrigen): string {
    return origen === 'COMPRA' || origen === 'RETIROGANANCIA' ? 'text-danger' : 'text-success';
  }

  onTablaScroll(event: Event): void {
    const target = event.target as HTMLElement | null;
    this.tablaScrollActiva = !!target && target.scrollTop > 0;
  }

  private formatearMes(mesIso: string | undefined | null): string {
    if (!mesIso) {
      return '';
    }
    const [anioStr, mesStr] = mesIso.split('-');
    const anio = Number(anioStr);
    const mes = Number(mesStr);
    if (Number.isNaN(anio) || Number.isNaN(mes)) {
      return mesIso;
    }
    const fecha = new Date(anio, mes - 1);
    return new Intl.DateTimeFormat('es-CO', { month: 'short' }).format(fecha);
  }

  private reiniciarSeleccionParticipacion(): void {
    this.productoSeleccionadoIndex = 0;
    this.clienteSeleccionadoIndex = 0;
  }

  private generarConicGradient<T extends { participacionPorcentaje: number }>(items: T[]): string {
    if (!items.length) {
      return 'conic-gradient(#e2e8f0 0deg 360deg)';
    }
    const total = items.reduce((sum, item) => sum + Math.max(item.participacionPorcentaje ?? 0, 0), 0) || 1;
    let acumulado = 0;
    const secciones = items.map((item, index) => {
      const valor = Math.max(item.participacionPorcentaje ?? 0, 0);
      const inicio = (acumulado / total) * 100;
      acumulado += valor;
      const fin = (acumulado / total) * 100;
      return `${this.colorParticipacion(index)} ${inicio}% ${fin}%`;
    });
    return `conic-gradient(${secciones.join(', ')})`;
  }
}
