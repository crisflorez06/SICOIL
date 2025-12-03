import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { ProductoService } from '../../services/producto.service';
import { MensajeService } from '../../services/mensaje.service';
import {
  IngresoProductoRequest,
  InventarioSalidaRequest,
  ProductoActualizarRequest,
  ProductoRequest,
  ProductosAgrupadosResponse,
  ProductosDesagrupadosResponse,
} from '../../models/producto.model';
import { IngresoProductoDialogComponent } from '../../shared/components/ingreso-producto-dialog/ingreso-producto-dialog.component';
import { RegistroProductoDialogComponent } from '../../shared/components/registro-producto-dialog/registro-producto-dialog.component';
import { EditarProductoDialogComponent } from '../../shared/components/editar-producto-dialog/editar-producto-dialog.component';
import { EliminarStockDialogComponent } from '../../shared/components/eliminar-stock-dialog/eliminar-stock-dialog.component';
import { ApiErrorService } from '../../core/services/api-error.service';

type EstadoCarga = 'idle' | 'cargando' | 'error' | 'listo';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './productos.component.html',
  styleUrls: ['./productos.component.css'],
})
export class ProductosComponent implements OnInit {
  private productoService = inject(ProductoService);
  private mensajeService = inject(MensajeService);
  private dialog = inject(MatDialog);
  private apiErrorService = inject(ApiErrorService);

  productos: ProductosAgrupadosResponse[] = [];
  estado: EstadoCarga = 'idle';
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  tamanoPagina = 10;
  terminoBusqueda = '';
  private filasExpandida = new Set<number>();
  registrandoIngreso = false;
  creandoProducto = false;
  private variantesProcesando = new Set<number>();
  private productosProcesando = new Set<string>();

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(pagina = 0): void {
    this.estado = 'cargando';
    this.productoService
      .listar({ page: pagina, size: this.tamanoPagina, nombre: this.terminoBusqueda.trim() || undefined })
      .subscribe({
      next: (respuesta) => {
        this.productos = respuesta.content ?? [];
        this.paginaActual = respuesta.page ?? pagina;
        this.totalPaginas = respuesta.totalPages ?? 0;
        this.totalElementos = respuesta.totalElements ?? 0;
        this.filasExpandida.clear();
        this.estado = 'listo';
      },
      error: () => {
        this.estado = 'error';
        this.mensajeService.error('No se pudieron obtener los productos.');
      },
      });
  }

  cambiarPagina(page: number): void {
    if (page === this.paginaActual || page < 0 || page >= this.totalPaginas) {
      return;
    }
    this.cargarProductos(page);
  }

  buscarProductos(): void {
    this.paginaActual = 0;
    this.cargarProductos(0);
  }

  reiniciarBusqueda(): void {
    if (this.terminoBusqueda.trim() === '') {
      return;
    }
    this.terminoBusqueda = '';
    this.buscarProductos();
  }

  agregarIngreso(): void {
    const dialogRef = this.dialog.open(IngresoProductoDialogComponent, {
      width: '900px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((payload?: IngresoProductoRequest[] | null) => {
      if (!payload || payload.length === 0) {
        return;
      }
      this.registrandoIngreso = true;
      this.productoService
        .registrarIngresos(payload)
        .pipe(
          finalize(() => {
            this.registrandoIngreso = false;
          })
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Ingreso registrado correctamente.');
            this.cargarProductos(this.paginaActual);
          },
          error: () => {
            this.mensajeService.error('No se pudo registrar el ingreso.');
          },
        });
    });
  }

  registrarProducto(): void {
    const dialogRef = this.dialog.open(RegistroProductoDialogComponent, {
      width: '520px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((payload?: ProductoRequest | null) => {
      if (!payload) {
        return;
      }
      this.creandoProducto = true;
      this.productoService
        .crear(payload)
        .pipe(
          finalize(() => {
            this.creandoProducto = false;
          })
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Producto creado correctamente.');
            this.cargarProductos(this.paginaActual);
          },
          error: (error) => {
            this.apiErrorService.handle(error, {
              contextMessage: 'No se pudo crear el producto.',
            });
          },
        });
    });
  }

  editarProducto(producto: ProductosAgrupadosResponse): void {
    const dialogRef = this.dialog.open(EditarProductoDialogComponent, {
      width: '520px',
      disableClose: true,
      data: {
        nombre: producto.nombre,
        cantidadPorCajas: producto.cantidadPorCajas,
      },
    });

    dialogRef.afterClosed().subscribe((payload?: ProductoActualizarRequest | null) => {
      if (!payload) {
        return;
      }
      const nombreAnterior = producto.nombre;
      this.marcarProductoProcesando(nombreAnterior, true);
      this.productoService
        .actualizar(nombreAnterior, payload)
        .pipe(
          finalize(() => {
            this.marcarProductoProcesando(nombreAnterior, false);
          })
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Producto actualizado correctamente.');
            this.cargarProductos(this.paginaActual);
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo actualizar el producto.' });
          },
        });
    });
  }

  eliminarCantidadVariante(producto: ProductosAgrupadosResponse, variante: ProductosDesagrupadosResponse): void {
    const descripcionVariante = `${producto.nombre} - $${variante.precioCompra}`;
    const dialogRef = this.dialog.open(EliminarStockDialogComponent, {
      width: '480px',
      disableClose: true,
      data: {
        id: variante.id,
        nombre: descripcionVariante,
        stock: variante.stock,
      },
    });

    dialogRef.afterClosed().subscribe((payload?: InventarioSalidaRequest | null) => {
      if (!payload) {
        return;
      }
      this.marcarVarianteProcesando(variante.id, true);
      this.productoService
        .eliminarStock(variante.id, payload)
        .pipe(
          finalize(() => {
            this.marcarVarianteProcesando(variante.id, false);
          })
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Stock eliminado correctamente.');
            this.cargarProductos(this.paginaActual);
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo eliminar la cantidad.' });
          },
        });
    });
  }

  estaVarianteProcesando(id: number): boolean {
    return this.variantesProcesando.has(id);
  }

  estaProductoProcesando(nombre: string): boolean {
    return this.productosProcesando.has(nombre.toLowerCase());
  }

  private marcarVarianteProcesando(id: number, enProceso: boolean): void {
    if (enProceso) {
      this.variantesProcesando.add(id);
    } else {
      this.variantesProcesando.delete(id);
    }
  }

  private marcarProductoProcesando(nombre: string, enProceso: boolean): void {
    const clave = nombre.toLowerCase();
    if (enProceso) {
      this.productosProcesando.add(clave);
    } else {
      this.productosProcesando.delete(clave);
    }
  }

  toggleVariantes(index: number): void {
    if (this.filasExpandida.has(index)) {
      this.filasExpandida.delete(index);
    } else {
      this.filasExpandida.add(index);
    }
  }

  esFilaExpandida(index: number): boolean {
    return this.filasExpandida.has(index);
  }

  get hayProductos(): boolean {
    return this.productos.length > 0;
  }

  get mostrarPaginador(): boolean {
    return this.totalPaginas > 0;
  }

  get paginas(): number[] {
    return Array.from({ length: this.totalPaginas }, (_, index) => index);
  }
}
