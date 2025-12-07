import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { VentaService } from '../../services/venta.service';
import { MensajeService } from '../../services/mensaje.service';
import {
  PaginaVentaResponse,
  TipoVenta,
  VentaListadoFiltro,
  VentaListadoResponse,
  VentaRequest,
} from '../../models/venta.model';
import { RegistroVentaDialogComponent } from '../../shared/components/registro-venta-dialog/registro-venta-dialog.component';
import { AnulacionVentaDialogComponent } from '../../shared/components/anulacion-venta-dialog/anulacion-venta-dialog.component';
import { ApiErrorService } from '../../core/services/api-error.service';

type EstadoCarga = 'idle' | 'cargando' | 'error' | 'listo';

@Component({
  selector: 'app-ventas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ventas.component.html',
  styleUrls: ['./ventas.component.css'],
})
export class VentasComponent implements OnInit {
  private ventaService = inject(VentaService);
  private mensajeService = inject(MensajeService);
  private dialog = inject(MatDialog);
  private apiErrorService = inject(ApiErrorService);

  ventas: VentaListadoResponse[] = [];
  estado: EstadoCarga = 'idle';
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  tamanoPagina = 10;
  filtroNombreCliente = '';
  filtroNombreUsuario = '';
  filtroTipoVenta: TipoVenta | '' = '';
  filtroEstado: 'predeterminado' | 'activas' | 'anuladas' = 'predeterminado';
  filtroDesde = '';
  filtroHasta = '';
  private filasExpandida = new Set<number>();
  registrandoVenta = false;
  private anulandoVentas = new Set<number>();
  private comprobantesDescargando = new Set<number>();
  tablaScrollActiva = false;

  ngOnInit(): void {
    this.cargarVentas();
  }

  registrarVenta(): void {
    const dialogRef = this.dialog.open(RegistroVentaDialogComponent, {
      width: '760px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((payload?: VentaRequest | null) => {
      if (!payload) {
        return;
      }
      this.registrandoVenta = true;
      this.ventaService
        .crear(payload)
        .pipe(
          finalize(() => {
            this.registrandoVenta = false;
          })
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Venta registrada correctamente.');
            this.cargarVentas(this.paginaActual);
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo registrar la venta.' });
          },
        });
    });
  }

  cargarVentas(pagina = 0): void {
    this.estado = 'cargando';
    const filtros: VentaListadoFiltro = {
      page: pagina,
      size: this.tamanoPagina,
    };
    if (this.filtroNombreCliente.trim() !== '') {
      filtros.nombreCliente = this.filtroNombreCliente.trim();
    }
    if (this.filtroNombreUsuario.trim() !== '') {
      filtros.nombreUsuario = this.filtroNombreUsuario.trim();
    }
    if (this.filtroTipoVenta) {
      filtros.tipoVenta = this.filtroTipoVenta;
    }
    if (this.filtroEstado === 'activas') {
      filtros.activa = true;
    } else if (this.filtroEstado === 'anuladas') {
      filtros.activa = false;
    }
    if (this.filtroDesde) {
      filtros.desde = this.filtroDesde;
    }
    if (this.filtroHasta) {
      filtros.hasta = this.filtroHasta;
    }
    this.ventaService.listar(filtros).subscribe({
      next: (respuesta: PaginaVentaResponse) => {
        this.ventas = respuesta.content ?? [];
        this.totalPaginas = respuesta.totalPages ?? 0;
        this.totalElementos = respuesta.totalElements ?? 0;
        this.paginaActual = respuesta.page ?? pagina;
        this.filasExpandida.clear();
        this.estado = 'listo';
      },
      error: () => {
        this.estado = 'error';
        this.mensajeService.error('No se pudieron obtener las ventas.');
      },
    });
  }

  buscarVentas(): void {
    this.paginaActual = 0;
    this.cargarVentas();
  }

  limpiarFiltros(): void {
    if (
      this.filtroNombreCliente === '' &&
      this.filtroNombreUsuario === '' &&
      this.filtroTipoVenta === '' &&
      this.filtroEstado === 'predeterminado' &&
      this.filtroDesde === '' &&
      this.filtroHasta === ''
    ) {
      return;
    }
    this.filtroNombreCliente = '';
    this.filtroNombreUsuario = '';
    this.filtroTipoVenta = '';
    this.filtroEstado = 'predeterminado';
    this.filtroDesde = '';
    this.filtroHasta = '';
    this.buscarVentas();
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 0 || pagina === this.paginaActual || pagina >= this.totalPaginas) {
      return;
    }
    this.cargarVentas(pagina);
  }

  get hayVentas(): boolean {
    return this.ventas.length > 0;
  }

  get mostrarPaginador(): boolean {
    return this.totalPaginas > 0;
  }

  get paginas(): number[] {
    return Array.from({ length: this.totalPaginas }, (_, index) => index);
  }

  toggleItems(index: number): void {
    if (this.filasExpandida.has(index)) {
      this.filasExpandida.delete(index);
    } else {
      this.filasExpandida.add(index);
    }
  }

  esFilaExpandida(index: number): boolean {
    return this.filasExpandida.has(index);
  }

  calcularGananciaItem(item: { precioCompra: number; precioVenta: number }): number {
    return Number(item.precioVenta ?? 0) - Number(item.precioCompra ?? 0);
  }

  estaAnulandoVenta(ventaId: number): boolean {
    return this.anulandoVentas.has(ventaId);
  }

  estaDescargandoComprobante(ventaId: number): boolean {
    return this.comprobantesDescargando.has(ventaId);
  }

  anularVenta(venta: VentaListadoResponse): void {
    if (!venta.activa || this.anulandoVentas.has(venta.ventaId)) {
      return;
    }
    const dialogRef = this.dialog.open(AnulacionVentaDialogComponent, {
      width: '520px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((motivo) => {
      if (!motivo) {
        return;
      }
      const motivoLimpio = motivo.trim();
      if (!motivoLimpio) {
        this.mensajeService.error('Debes ingresar un motivo válido para anular la venta.');
        return;
      }
      this.anulandoVentas.add(venta.ventaId);
      this.ventaService
        .anular(venta.ventaId, { motivo: motivoLimpio })
        .pipe(
          finalize(() => {
            this.anulandoVentas.delete(venta.ventaId);
          }),
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Venta anulada correctamente.');
            this.cargarVentas(this.paginaActual);
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo anular la venta.' });
          },
        });
    });
  }

  descargarComprobante(venta: VentaListadoResponse): void {
    if (this.comprobantesDescargando.has(venta.ventaId)) {
      return;
    }
    this.comprobantesDescargando.add(venta.ventaId);
    this.ventaService
      .descargarComprobante(venta.ventaId)
      .pipe(
        finalize(() => {
          this.comprobantesDescargando.delete(venta.ventaId);
        }),
      )
      .subscribe({
        next: (blob) => {
          const enlace = document.createElement('a');
          const url = URL.createObjectURL(blob);
          enlace.href = url;
          enlace.download = `comprobante-venta-${venta.ventaId}.pdf`;
          enlace.click();
          URL.revokeObjectURL(url);
          this.mensajeService.success('Comprobante generado correctamente.');
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            contextMessage: 'No se pudo generar el comprobante.',
          });
        },
      });
  }

  onTablaScroll(event: Event): void {
    const target = event.target as HTMLElement | null;
    this.tablaScrollActiva = !!target && target.scrollTop > 0;
  }
}
