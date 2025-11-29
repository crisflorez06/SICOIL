import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as bootstrap from 'bootstrap';
import {
  FormArray,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatExpansionModule } from '@angular/material/expansion';
import { FiltroService } from '../../services/filtro.service';
import { DetalleVentaResponse } from '../../models/detalle-venta.model';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ApiErrorService } from '../../core/services/api-error.service';
import { MensajeService } from '../../services/mensaje.service';
import { VentaRequest, VentaResponse } from '../../models/venta.model';
import { VentaService } from '../../services/venta.service';
import { ActualizarGastoRequest, CrearGastoRequest, Gasto } from '../../models/compra.model';
import { GastoService } from '../../services/compra.service';

@Component({
  selector: 'app-ventas',
  standalone: true,
  imports: [
    MatPaginatorModule,
    MatExpansionModule,
    FormsModule,
    CommonModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatInputModule,
    MatFormFieldModule,
  ],
  templateUrl: './ventas.component.html',
  styleUrls: ['./ventas.component.css'],
})
export class VentasComponent implements OnInit {
  private ventaService = inject(VentaService);
  private mensajeService = inject(MensajeService);
  private filtroService = inject(FiltroService);
  private fb = inject(FormBuilder);
  private apiErrorService = inject(ApiErrorService);
  private gastoService = inject(GastoService);

  // Datos generales
  public ventas: VentaResponse[] = [];
  public productosVendidos: {
    productoNombre: string;
    cantidadVendida: number;
    totalGenerado: number;
    fecha: string;
  }[] = [];
  public vistaActual: 'ventas' | 'productos' | 'compras' = 'ventas';
  public usuarioIdPredeterminado = 1;

  public totalVentasEnTabla = 0;
  public totalElementos = 0;
  public size = 10;
  public index = 0;

  public filtrosVentas = {
    metodoPago: '',
    desde: '',
    hasta: '',
    minTotal: null,
    maxTotal: null,
  };

  // Filtros y estado para productos vendidos
  public filtrosProductos = {
    nombreProducto: '',
    desde: '',
    hasta: '',
  };
  public filtrosGastos = {
    nombre: '',
    desde: '',
    hasta: '',
  };

  public productosPaginados: DetalleVentaResponse[] = [];
  public totalProductosVendidos = 0;
  public indexProductos = 0;
  public sortColumnProductos = 'venta.fecha';
  public sortDirectionProductos: 'asc' | 'desc' = 'desc';

  public metodosPago: string[] = ['Efectivo', 'Tarjeta', 'Transferencia'];
  public sortColumn = 'fecha';
  public sortDirection: 'asc' | 'desc' = 'desc';
  public filtrosAbiertos = false;

  public nombresProductos: { id: number; nombre: string; precioVenta: number }[] = [];
  public totalModalVenta = 0;
  public ventaSeleccionada: VentaResponse | null = null;
  public ventaAEliminar: VentaResponse | null = null;
  public compras: Gasto[] = [];
  public cargandoGastos = false;
  public creandoGasto = false;
  public gastoSeleccionado: Gasto | null = null;
  public totalGastos = 0;
  public gastoAEliminar: Gasto | null = null;
  public eliminandoGasto = false;

  formularioVenta = this.fb.nonNullable.group({
    usuarioId: [1, [Validators.required, Validators.min(1)]],
    metodoPago: ['Efectivo', Validators.required],
    detalles: this.fb.array(this.crearDetalleArray()),
  });

  formularioGasto = this.fb.group({
    monto: [null as number | null, [Validators.required, Validators.min(1)]],
    descripcion: ['', [Validators.required, Validators.maxLength(500)]],
  });

  private crearDetalleArray() {
    return [] as ReturnType<typeof this.crearDetalleForm>[];
  }

  private crearDetalleForm(productoId: number | null = null, cantidad = 1, productoNombre = '') {
    return this.fb.nonNullable.group({
      productoId: [productoId],
      productoNombre: [productoNombre, Validators.required],
      cantidad: [cantidad, [Validators.required, Validators.min(1)]],
    });
  }

  // Inicialización
  ngOnInit(): void {
    this.cargarVentas();
    this.filtroService.getFiltros().subscribe({
      next: (filtros) => {
        this.nombresProductos = filtros.nombresProductos ?? [];
      },
    });

    // Suscribirse a los cambios en los detalles para recalcular el total
    this.detalles.valueChanges.subscribe(
      (detalles: { productoId: number | null; cantidad: number }[]) => {
        this.totalModalVenta = detalles.reduce((total, detalle) => {
          const producto = this.nombresProductos.find(
            (p) => p.id === detalle.productoId
          );
          const precio = producto ? producto.precioVenta : 0;
          const cantidad = detalle.cantidad ? detalle.cantidad : 0;
          return total + precio * cantidad;
        }, 0);
      }
    );
  }

  public mostrarVentas(): void {
    if (this.vistaActual !== 'ventas') {
      this.vistaActual = 'ventas';
      this.cargarVentas();
    }
  }

  public mostrarProductosVendidos(): void {
    if (this.vistaActual !== 'productos') {
      this.vistaActual = 'productos';
      this.cargarProductosVendidos();
    }
  }

  public mostrarGastos(): void {
    this.vistaActual = 'compras';
    this.formularioGasto.reset({ monto: null, descripcion: '' });
    this.apiErrorService.clearFormErrors(this.formularioGasto);
    this.gastoSeleccionado = null;
    this.gastoAEliminar = null;
    this.eliminandoGasto = false;
    this.cargarGastos();
  }

  private cargarGastos(): void {
    this.cargandoGastos = true;
    const filtros = {
      nombre: this.filtrosGastos.nombre?.trim() ?? '',
      desde: this.filtrosGastos.desde,
      hasta: this.filtrosGastos.hasta,
    };
    this.gastoService.listar(filtros).subscribe({
      next: (compras: Gasto[]) => {
        this.compras = compras;
        this.totalGastos = compras.reduce((total, compra) => total + Number(compra.monto ?? 0), 0);
        this.cargandoGastos = false;
      },
      error: (error: unknown) => {
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al cargar los compras.',
        });
        this.compras = [];
        this.totalGastos = 0;
        this.cargandoGastos = false;
      },
    });
  }

  private mostrarModalGasto(): void {
    const modalEl = document.getElementById('gastoModal');
    if (modalEl) {
      bootstrap.Modal.getOrCreateInstance(modalEl).show();
    }
  }

  private cerrarModalGasto(): void {
    const modalEl = document.getElementById('gastoModal');
    if (modalEl) {
      bootstrap.Modal.getInstance(modalEl)?.hide();
    }
    this.formularioGasto.reset({ monto: null, descripcion: '' });
    this.apiErrorService.clearFormErrors(this.formularioGasto);
    this.gastoSeleccionado = null;
    this.creandoGasto = false;
  }

  public abrirModalCrearGasto(): void {
    this.gastoSeleccionado = null;
    this.formularioGasto.reset({ monto: null, descripcion: '' });
    this.apiErrorService.clearFormErrors(this.formularioGasto);
    this.mostrarModalGasto();
  }

  guardarGasto(): void {
    if (this.formularioGasto.invalid) {
      this.formularioGasto.markAllAsTouched();
      this.mensajeService.error('Por favor completa monto y descripción del compra.');
      return;
    }

    this.creandoGasto = true;
    this.apiErrorService.clearFormErrors(this.formularioGasto);

    const payload = this.formularioGasto.getRawValue() as CrearGastoRequest;
    payload.monto = Number(payload.monto);
    const request$ = this.gastoSeleccionado
      ? this.gastoService.actualizar(this.gastoSeleccionado.id, payload as ActualizarGastoRequest)
      : this.gastoService.crear(payload);
    const mensajeExito = this.gastoSeleccionado
      ? 'Gasto actualizado correctamente.'
      : 'Gasto registrado correctamente.';

    request$.subscribe({
      next: () => {
        this.mensajeService.success(mensajeExito);
        this.formularioGasto.reset({ monto: null, descripcion: '' });
        this.gastoSeleccionado = null;
        this.creandoGasto = false;
        this.cerrarModalGasto();
        this.cargarGastos();
      },
      error: (error: unknown) => {
        this.creandoGasto = false;
        this.apiErrorService.handle(error, {
          form: this.formularioGasto,
          contextMessage: 'Error al guardar el compra.',
        });
      },
    });
  }

  iniciarEdicionGasto(compra: Gasto): void {
    this.gastoSeleccionado = compra;
    this.formularioGasto.patchValue({
      monto: Number(compra.monto),
      descripcion: compra.descripcion,
    });
    this.apiErrorService.clearFormErrors(this.formularioGasto);
    this.mostrarModalGasto();
  }

  cancelarEdicionGasto(): void {
    this.cerrarModalGasto();
  }

  prepararEliminacionGasto(compra: Gasto): void {
    this.gastoAEliminar = compra;
    this.eliminandoGasto = false;
  }

  confirmarEliminarGasto(): void {
    if (!this.gastoAEliminar) {
      return;
    }

    this.eliminandoGasto = true;
    const id = this.gastoAEliminar.id;
    this.gastoService.eliminar(id).subscribe({
      next: () => {
        this.mensajeService.success('Gasto eliminado correctamente.');
        this.gastoAEliminar = null;
        this.eliminandoGasto = false;
        this.cargarGastos();
      },
      error: (error: unknown) => {
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al eliminar el compra.',
        });
        this.gastoAEliminar = null;
        this.eliminandoGasto = false;
      },
    });
  }

  cancelarEliminarGasto(): void {
    this.gastoAEliminar = null;
    this.eliminandoGasto = false;
  }

  private cargarProductosVendidos(): void {
    this.ventaService
      .buscarProductos(
        this.indexProductos,
        this.size,
        this.filtrosProductos,
        this.sortColumnProductos,
        this.sortDirectionProductos
      )
      .subscribe({
        next: (page) => {
          this.productosPaginados = page.content;
          this.totalProductosVendidos = page.totalElements;
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            contextMessage: 'Error al cargar los productos vendidos.',
          });
          this.productosPaginados = [];
          this.totalProductosVendidos = 0;
        },
      });
  }

  public aplicarFiltrosProductos(): void {
    this.indexProductos = 0;
    this.cargarProductosVendidos();
  }

  public limpiarFiltrosProductos(): void {
    this.filtrosProductos = { nombreProducto: '', desde: '', hasta: '' };
    this.cargarProductosVendidos();
  }

  public aplicarFiltrosGastos(): void {
    this.cargarGastos();
  }

  public limpiarFiltrosGastos(): void {
    this.filtrosGastos = { nombre: '', desde: '', hasta: '' };
    this.cargarGastos();
  }

  public cambiarPaginaProductos(event: PageEvent): void {
    this.indexProductos = event.pageIndex;
    this.size = event.pageSize;
    this.cargarProductosVendidos();
  }

  // Cargar ventas
  private cargarVentas(): void {
    this.ventaService
      .obtenerTodos(this.index, this.size, this.filtrosVentas, this.sortColumn, this.sortDirection)
      .subscribe({
        next: (page) => {
          this.ventas = page.ventas.content;
          this.totalElementos = page.ventas.totalElements;
          this.totalVentasEnTabla = page.totalGeneral;
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            contextMessage: 'Error al cargar las ventas.',
          });
          this.ventas = [];
          this.totalElementos = 0;
          this.totalVentasEnTabla = 0;
        },
      });
  }

  // Paginación
  public cambiarPagina(event: PageEvent): void {
    this.index = event.pageIndex;
    this.size = event.pageSize;
    this.cargarVentas();
  }

  // Filtros
  public aplicarFiltros(): void {
    this.index = 0;
    this.cargarVentas();
  }

  public limpiarFiltros(): void {
    this.filtrosVentas = { metodoPago: '', desde: '', hasta: '', minTotal: null, maxTotal: null };
    this.cargarVentas();
  }

  // Lógica de guardado (crear o actualizar)
  guardarVenta() {
    if (this.formularioVenta.invalid) {
      this.mensajeService.error('Por favor completa todos los campos.');
      return;
    }

    this.apiErrorService.clearFormErrors(this.formularioVenta);

    const ventaRequest: VentaRequest = this.formularioVenta.getRawValue() as VentaRequest;

    const operation = this.ventaSeleccionada
      ? this.ventaService.actualizarVenta(this.ventaSeleccionada.id, ventaRequest)
      : this.ventaService.crearVenta(ventaRequest);

    const successMessage = this.ventaSeleccionada
      ? 'Venta actualizada correctamente.'
      : 'Venta registrada correctamente.';

    operation.subscribe({
      next: () => {
        this.mensajeService.success(successMessage);
        this.cerrarModalVenta();
        if (this.vistaActual === 'productos') {
          this.cargarProductosVendidos();
        } else {
          this.cargarVentas();
        }
      },
      error: (err) => {
        this.apiErrorService.handle(err, {
          form: this.formularioVenta,
          contextMessage: 'Error al guardar la venta.',
        });
      },
    });
  }

  // Abrir modal para crear
  abrirModalCrear(): void {
    this.ventaSeleccionada = null;
    this.detalles.clear();
    this.formularioVenta.reset({ metodoPago: 'Efectivo', usuarioId: this.usuarioIdPredeterminado });
    this.agregarDetalle(); // Agrega una fila por defecto
    this.apiErrorService.clearFormErrors(this.formularioVenta);
    this.abrirModalVenta();
  }

  // Abrir modal para editar
  abrirModalEditar(venta: VentaResponse): void {
    this.ventaSeleccionada = venta;
    this.detalles.clear();
    this.formularioVenta.patchValue({
      metodoPago: venta.metodoPago,
      usuarioId: venta.usuarioId ?? this.usuarioIdPredeterminado,
    });
    this.apiErrorService.clearFormErrors(this.formularioVenta);

    this.ventaService.obtenerDetallesPorVenta(venta.id).subscribe({
      next: (detalles) => {
        if (detalles.length === 0) {
          this.agregarDetalle();
        } else {
          detalles.forEach((detalle) => {
            this.detalles.push(
              this.crearDetalleForm(
                detalle.productoId,
                detalle.cantidad,
                detalle.productoNombre
              )
            );
          });
        }
        this.abrirModalVenta();
      },
      error: (error) => {
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al cargar los detalles para editar.',
        });
      },
    });
  }

  confirmarEliminarVenta(): void {
    if (!this.ventaAEliminar) {
      return;
    }

    const ventaId = this.ventaAEliminar.id;

    this.ventaService.eliminarVenta(ventaId).subscribe({
      next: () => {
        this.mensajeService.success('Venta eliminada correctamente.');
        if (this.vistaActual === 'productos') {
          this.cargarProductosVendidos();
        } else {
          this.cargarVentas();
        }
        this.ventaAEliminar = null;
      },
      error: (error) => {
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al eliminar la venta.',
        });
        this.ventaAEliminar = null;
      },
    });
  }

  cancelarEliminarVenta(): void {
    this.ventaAEliminar = null;
  }

  ordenarPor(columna: string): void {
    if (this.vistaActual === 'productos') {
      if (this.sortColumnProductos === columna) {
        this.sortDirectionProductos = this.sortDirectionProductos === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortColumnProductos = columna;
        this.sortDirectionProductos = 'asc';
      }
      this.cargarProductosVendidos();
    } else if (this.vistaActual === 'ventas') {
      if (this.sortColumn === columna) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortColumn = columna;
        this.sortDirection = 'asc';
      }
      this.cargarVentas();
    }
  }

  abrirModalVenta() {
    const modalEl = document.getElementById('ventaModal');
    if (modalEl) new bootstrap.Modal(modalEl).show();
  }

  cerrarModalVenta() {
    const modalEl = document.getElementById('ventaModal');
    if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();

    // Reiniciar el formulario
    this.detalles.clear();
    this.formularioVenta.reset({
      metodoPago: 'Efectivo',
    });
    this.apiErrorService.clearFormErrors(this.formularioVenta);
    this.ventaSeleccionada = null;
  }

  // Detalles del formulario
  get detalles(): FormArray {
    return this.formularioVenta.get('detalles') as FormArray;
  }

  agregarDetalle() {
    this.detalles.push(this.crearDetalleForm());
  }

  eliminarDetalle(i: number) {
    this.detalles.removeAt(i);
  }

  obtenerSubtotal(detalle: any): number {
    const productoId = detalle.get('productoId')?.value;
    const cantidad = detalle.get('cantidad')?.value || 0;
    const precio = this.obtenerPrecioProducto(productoId);
    return precio * cantidad;
  }

  obtenerPrecioProducto(productoId: number | null | undefined): number {
    if (!productoId) {
      return 0;
    }
    const producto = this.nombresProductos.find((p) => p.id === productoId);
    return producto ? producto.precioVenta : 0;
  }

  public productosFiltrados: { [index: number]: { id: number; nombre: string }[] } = {};

  filtrarProductos(index: number): void {
    const valor = this.detalles.at(index).get('productoNombre')?.value?.toLowerCase() || '';
    this.productosFiltrados[index] = this.nombresProductos.filter((p) =>
      p.nombre.toLowerCase().includes(valor)
    );
  }

  seleccionarProducto(event: any, index: number): void {
    const producto = this.nombresProductos.find((p) => p.nombre === event.option.value);
    if (producto) {
      this.detalles.at(index).patchValue({
        productoId: producto.id,
        productoNombre: producto.nombre,
      });
    }
  }
}
