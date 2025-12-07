import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { KardexService } from '../../services/kardex.service';
import { KardexFiltro, KardexResponse, MovimientoTipo } from '../../models/kardex.model';
import { MensajeService } from '../../services/mensaje.service';

type EstadoCarga = 'idle' | 'cargando' | 'error' | 'listo';

@Component({
  selector: 'app-movimientos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './movimientos.component.html',
  styleUrls: ['./movimientos.component.css'],
})
export class MovimientosComponent implements OnInit {
  private kardexService = inject(KardexService);
  private mensajeService = inject(MensajeService);

  movimientos: KardexResponse[] = [];
  estado: EstadoCarga = 'idle';
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  tamanoPagina = 10;
  tablaScrollActiva = false;

  filtroNombreProducto = '';
  filtroTipo: MovimientoTipo | '' = '';
  filtroDesde = '';
  filtroHasta = '';

  ngOnInit(): void {
    this.buscarMovimientos();
  }

  private construirFiltro(pagina: number): KardexFiltro {
    const filtro: KardexFiltro = {
      page: pagina,
      size: this.tamanoPagina,
      sort: 'fechaRegistro,desc',
    };

    const nombreProducto = this.filtroNombreProducto.trim();
    if (nombreProducto) {
      filtro.nombreProducto = nombreProducto;
    }

    if (this.filtroTipo) {
      filtro.tipo = this.filtroTipo;
    }

    if (this.filtroDesde) {
      filtro.desde = this.filtroDesde;
    }

    if (this.filtroHasta) {
      filtro.hasta = this.filtroHasta;
    }

    return filtro;
  }

  buscarMovimientos(pagina = 0): void {
    this.estado = 'cargando';
    const filtro = this.construirFiltro(pagina);

    this.kardexService.listar(filtro).subscribe({
      next: (page) => {
        this.movimientos = page.content ?? [];
        this.totalPaginas = page.totalPages ?? 0;
        this.totalElementos = page.totalElements ?? 0;
        this.paginaActual = page.number ?? pagina;
        this.estado = 'listo';
      },
      error: () => {
        this.estado = 'error';
        this.mensajeService.error('No se pudieron cargar los movimientos del kardex.');
      },
    });
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.buscarMovimientos(0);
  }

  limpiarFiltros(): void {
    if (
      this.filtroNombreProducto.trim() === '' &&
      this.filtroTipo === '' &&
      this.filtroDesde === '' &&
      this.filtroHasta === ''
    ) {
      return;
    }
    this.filtroNombreProducto = '';
    this.filtroTipo = '';
    this.filtroDesde = '';
    this.filtroHasta = '';
    this.aplicarFiltros();
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 0 || pagina === this.paginaActual || pagina >= this.totalPaginas) {
      return;
    }
    this.buscarMovimientos(pagina);
  }

  movimientoChipClase(tipo: MovimientoTipo): string {
    switch (tipo) {
      case 'ENTRADA':
        return 'movement-chip--entrada';
      case 'SALIDA':
        return 'movement-chip--salida';
      default:
        return 'movement-chip--venta';
    }
  }

  movimientoCantidadClase(tipo: MovimientoTipo): string {
    switch (tipo) {
      case 'ENTRADA':
        return 'movement-qty--positive';
      case 'SALIDA':
        return 'movement-qty--negative';
      default:
        return 'movement-qty--neutral';
    }
  }

  cantidadPrefijo(tipo: MovimientoTipo): string {
    if (tipo === 'ENTRADA') {
      return '+';
    }
    if (tipo === 'SALIDA') {
      return '-';
    }
    return '';
  }

  onTablaScroll(event: Event): void {
    const target = event.target as HTMLElement | null;
    this.tablaScrollActiva = !!target && target.scrollTop > 0;
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
}
