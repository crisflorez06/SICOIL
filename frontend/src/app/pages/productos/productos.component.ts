import { Component, inject, OnInit } from '@angular/core';
import * as bootstrap from 'bootstrap';
import { CommonModule } from '@angular/common';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatExpansionModule } from '@angular/material/expansion';

import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';

import { MovimientoEntradaProductoRequest, ProductoRequest, ProductoResponse } from '../../models/producto.model';
import { Page } from '../../core/types/page';
import { FiltroService } from '../../services/filtro.service';
import { take } from 'rxjs';
import { MensajeService } from '../../services/mensaje.service';
import { ProductoService } from '../../services/producto.service';
import { ApiErrorService } from '../../core/services/api-error.service';
import { MovimientoResponse, MovimientoTipo } from '../../models/movimiento.model';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [
    CommonModule,
    MatPaginatorModule,
    MatExpansionModule,
    FormsModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatInputModule,
  ],
  templateUrl: './productos.component.html',
  styleUrls: ['./productos.component.css'],
})
export class ProductosComponent implements OnInit {
  private productoService = inject(ProductoService);
  private mensajeService = inject(MensajeService);
  private apiErrorService = inject(ApiErrorService);
  public productos: ProductoResponse[] = [];
  modoCreacionProducto = false;
  public totalElementos = 0;
  public size = 10;
  public index = 0;
  public filtros = {
    nombre: '',
    categoria: null,
    estado: true,
  };

  public sortColumn = 'nombre';
  public sortDirection: 'asc' | 'desc' = 'asc';

  public filtrosAbiertos = false;

  public vistaActual: 'productos' | 'movimientos' = 'productos';

  public movimientos: MovimientoResponse[] = [];
  public totalMovimientos = 0;
  public sizeMovimientos = 10;
  public indexMovimientos = 0;
  public sortColumnMovimientos = 'fechaMovimiento';
  public sortDirectionMovimientos: 'asc' | 'desc' = 'desc';

  public filtrosMovimientos = {
    productoId: null as number | null,
    tipo: null as MovimientoTipo | null,
    desde: '',
    hasta: '',
  };

  public tiposMovimiento: MovimientoTipo[] = ['INGRESO'];

  private fb = inject(FormBuilder);
  formularioProducto: FormGroup;
  formularioMovimientos: FormGroup;
  formularioMovimientoEditar: FormGroup;

  public productosActivos: ProductoResponse[] = [];
  public productosFiltradosMovimientos: { [index: number]: { id: number; nombre: string }[] } = {};

  private filtroService = inject(FiltroService);
  nombresProductos: { id: number; nombre: string }[] = [];
  categoriasProductos: string[] = [];

  productoSeleccionado: ProductoResponse | null = null;

  productoParaSumar: ProductoResponse | null = null;
  cantidadAgregar: number | null = null;

  movimientoSeleccionado: MovimientoResponse | null = null;
  movimientoAEliminar: MovimientoResponse | null = null;
  observacionEliminacionMovimiento = '';

  constructor() {
    this.formularioProducto = this.fb.group({
      nombre: ['', Validators.required],
      descripcion: [null],
      precioCompra: [null, [Validators.required, Validators.min(0)]],
      precioVenta: [null, [Validators.required, Validators.min(0)]],
      stock: [null, [Validators.required, Validators.min(0)]],
      categoria: [null, Validators.required],
    });
    this.formularioMovimientos = this.fb.group({
      movimientos: this.fb.array([this.crearMovimientoFormGroup()]),
    });
    this.formularioMovimientoEditar = this.fb.group({
      cantidad: [null, [Validators.required, this.cantidadNoCeroValidator()]],
      observacion: [''],
    });
  }

  get movimientosFormArray(): FormArray {
    return this.formularioMovimientos.get('movimientos') as FormArray;
  }

  private crearMovimientoFormGroup(): FormGroup {
    return this.fb.group({
      productoId: [null, Validators.required],
      productoNombre: ['', Validators.required],
      cantidad: [null, [Validators.required, this.cantidadNoCeroValidator()]],
      observacion: [''],
    });
  }

  private cantidadNoCeroValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const rawValue = control.value;
      if (rawValue === null || rawValue === undefined || rawValue === '') {
        return null;
      }
      const numero = Number(rawValue);
      if (!Number.isFinite(numero) || numero === 0) {
        return { noZero: true };
      }
      return null;
    };
  }

  private resetFormularioMovimientos(): void {
    this.apiErrorService.clearFormErrors(this.formularioMovimientos);
    const movimientos = this.movimientosFormArray;
    while (movimientos.length > 0) {
      movimientos.removeAt(0);
    }
    movimientos.push(this.crearMovimientoFormGroup());
    this.formularioMovimientos.markAsPristine();
    this.formularioMovimientos.markAsUntouched();
    this.formularioMovimientos.updateValueAndValidity();
    this.reconstruirProductosFiltrados();
  }

  private reconstruirProductosFiltrados(): void {
    const opciones = this.obtenerOpcionesProductos();
    this.productosFiltradosMovimientos = {};
    this.movimientosFormArray.controls.forEach((control, index) => {
      const filtro = control.get('productoNombre')?.value?.toLowerCase() ?? '';
      this.productosFiltradosMovimientos[index] =
        filtro === ''
          ? opciones
          : opciones.filter((producto) => producto.nombre.toLowerCase().includes(filtro));
    });
  }

  private obtenerOpcionesProductos(): { id: number; nombre: string }[] {
    if (!this.productosActivos) {
      return [];
    }
    return this.productosActivos.map((producto) => ({
      id: producto.id,
      nombre: producto.nombre,
    }));
  }

  ngOnInit(): void {
    this.cargarProductos();
    this.filtroService
      .getFiltros()
      .pipe(take(1))
      .subscribe({
        next: (filtros) => {
          this.nombresProductos = filtros.nombresProductos ?? [];
          this.categoriasProductos = filtros.categoriasProductos ?? [];
        },
        error: () => {
          this.nombresProductos = [];
          this.categoriasProductos = [];
        },
      });
  }

  public mostrarProductos(): void {
    if (this.vistaActual !== 'productos') {
      this.vistaActual = 'productos';
      this.cargarProductos();
    }
  }

  public mostrarMovimientos(): void {
    if (this.vistaActual !== 'movimientos') {
      this.vistaActual = 'movimientos';
    }
    this.cargarMovimientos();
  }

  abrirModalCrearProducto(): void {
    this.productoSeleccionado = null;
    this.modoCreacionProducto = true;
    this.formularioProducto.reset();
    this.apiErrorService.clearFormErrors(this.formularioProducto);
  }

  public abrirModalIngresarProductos(): void {
    this.resetFormularioMovimientos();
    this.cargarProductosActivos();
  }

  public agregarFilaMovimiento(): void {
    this.movimientosFormArray.push(this.crearMovimientoFormGroup());
    this.reconstruirProductosFiltrados();
  }

  public eliminarFilaMovimiento(index: number): void {
    if (this.movimientosFormArray.length === 1) {
      return;
    }
    this.movimientosFormArray.removeAt(index);
    this.reconstruirProductosFiltrados();
  }

  public filtrarProductosMovimiento(index: number): void {
    const control = this.movimientosFormArray.at(index) as FormGroup;
    const valor = control.get('productoNombre')?.value?.toLowerCase() ?? '';

    const productoSeleccionado = this.productosActivos.find(
      (producto) => producto.id === control.get('productoId')?.value,
    );

    if (productoSeleccionado && productoSeleccionado.nombre.toLowerCase() !== valor) {
      control.get('productoId')?.setValue(null);
    }

    const opciones = this.obtenerOpcionesProductos();
    this.productosFiltradosMovimientos[index] =
      valor === '' ? opciones : opciones.filter((prod) => prod.nombre.toLowerCase().includes(valor));
  }

  public seleccionarProductoMovimiento(event: MatAutocompleteSelectedEvent, index: number): void {
    const producto = this.productosActivos.find(
      (p) => p.nombre.toLowerCase() === event.option.value.toLowerCase(),
    );
    if (!producto) {
      return;
    }
    const control = this.movimientosFormArray.at(index) as FormGroup;
    control.patchValue(
      {
        productoId: producto.id,
        productoNombre: producto.nombre,
      },
      { emitEvent: false },
    );
    control.get('productoId')?.setErrors(null);
    control.get('productoNombre')?.setErrors(null);
    control.get('productoId')?.updateValueAndValidity({ onlySelf: true });
    control.get('productoNombre')?.updateValueAndValidity({ onlySelf: true });
    this.productosFiltradosMovimientos[index] = this.obtenerOpcionesProductos();
  }

  private cargarProductosActivos(): void {
    this.productoService
      .obtenerTodos(0, 1000, { estado: true, categoria: null }, 'nombre', 'asc')
      .pipe(take(1))
      .subscribe({
        next: (page) => {
          this.productosActivos = page.content;
          this.reconstruirProductosFiltrados();
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            contextMessage: 'Error al obtener productos activos.',
          });
          this.productosActivos = [];
          this.reconstruirProductosFiltrados();
        },
      });
  }

  private cargarMovimientos(): void {
    const filtros = {
      productoId: this.filtrosMovimientos.productoId,
      tipo: this.filtrosMovimientos.tipo,
      desde: this.filtrosMovimientos.desde?.trim() ?? '',
      hasta: this.filtrosMovimientos.hasta?.trim() ?? '',
    };

    this.productoService
      .obtenerMovimientos(
        this.indexMovimientos,
        this.sizeMovimientos,
        {
          productoId: filtros.productoId,
          tipo: filtros.tipo,
          desde: filtros.desde !== '' ? filtros.desde : null,
          hasta: filtros.hasta !== '' ? filtros.hasta : null,
        },
        this.sortColumnMovimientos,
        this.sortDirectionMovimientos
      )
      .subscribe({
        next: (page) => {
          this.movimientos = page.content;
          this.totalMovimientos = page.totalElements;
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            contextMessage: 'Error al cargar los movimientos.',
          });
          this.movimientos = [];
          this.totalMovimientos = 0;
        },
      });
  }

  ordenarPor(columna: string): void {
    if (this.sortColumn === columna) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = columna;
      this.sortDirection = 'asc';
    }

    this.cargarProductos();
  }
  private cargarProductos(): void {
    this.productoService
      .obtenerTodos(this.index, this.size, this.filtros, this.sortColumn, this.sortDirection)
      .subscribe({
        next: (page) => {
          this.productos = page.content;
          this.totalElementos = page.totalElements;
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            contextMessage: 'Error al cargar los productos.',
          });
          this.productos = [];
          this.totalElementos = 0;
        },
      });
  }

  public cambiarPagina(event: PageEvent): void {
    this.index = event.pageIndex;
    this.size = event.pageSize;
    this.cargarProductos();
  }

  public aplicarFiltros(): void {
    this.index = 0; // resetear a primera página
    this.cargarProductos();
  }

  public limpiarFiltros(): void {
    this.filtros = {
      nombre: '',
      categoria: null,
      estado: true,
    };
    this.cargarProductos();
  }

  public aplicarFiltrosMovimientos(): void {
    this.indexMovimientos = 0;
    this.cargarMovimientos();
  }

  public limpiarFiltrosMovimientos(): void {
    this.filtrosMovimientos = {
      productoId: null,
      tipo: null,
      desde: '',
      hasta: '',
    };
    this.indexMovimientos = 0;
    this.cargarMovimientos();
  }

  public ordenarMovimientosPor(columna: string): void {
    if (this.sortColumnMovimientos === columna) {
      this.sortDirectionMovimientos =
        this.sortDirectionMovimientos === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumnMovimientos = columna;
      this.sortDirectionMovimientos = 'asc';
    }
    this.cargarMovimientos();
  }

  public cambiarPaginaMovimientos(event: PageEvent): void {
    this.indexMovimientos = event.pageIndex;
    this.sizeMovimientos = event.pageSize;
    this.cargarMovimientos();
  }

  public abrirModalEditarMovimiento(movimiento: MovimientoResponse): void {
    this.movimientoSeleccionado = movimiento;
    this.apiErrorService.clearFormErrors(this.formularioMovimientoEditar);
    this.formularioMovimientoEditar.reset({
      cantidad: movimiento.cantidad,
      observacion: movimiento.observacion ?? '',
    });
    this.mostrarModal('editarMovimientoModal');
  }

  public guardarMovimientoEditado(): void {
    if (!this.movimientoSeleccionado) {
      return;
    }

    if (this.formularioMovimientoEditar.invalid) {
      this.formularioMovimientoEditar.markAllAsTouched();
      this.mensajeService.error('Por favor completa la cantidad del movimiento.');
      return;
    }

    const cantidad = Number(this.formularioMovimientoEditar.value.cantidad);
    const observacionRaw = this.formularioMovimientoEditar.value.observacion as string | null | undefined;
    const observacion =
      observacionRaw && observacionRaw.trim() !== '' ? observacionRaw.trim() : null;

    this.apiErrorService.clearFormErrors(this.formularioMovimientoEditar);
    this.productoService
      .actualizarMovimiento(this.movimientoSeleccionado.id, { cantidad, observacion })
      .subscribe({
        next: () => {
          this.mensajeService.success('Movimiento actualizado con éxito.');
          this.cerrarModalEditarMovimiento();
          this.cargarMovimientos();
          this.cargarProductos();
          this.cargarProductosActivos();
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            form: this.formularioMovimientoEditar,
            contextMessage: 'Error al actualizar el movimiento.',
          });
        },
      });
  }

  public cerrarModalEditarMovimiento(): void {
    this.ocultarModal('editarMovimientoModal');
    this.limpiarBackdrop();
    this.movimientoSeleccionado = null;
    this.formularioMovimientoEditar.reset();
    this.apiErrorService.clearFormErrors(this.formularioMovimientoEditar);
  }

  public abrirModalEliminarMovimiento(movimiento: MovimientoResponse): void {
    this.movimientoAEliminar = movimiento;
    this.observacionEliminacionMovimiento = '';
    this.mostrarModal('eliminarMovimientoModal');
  }

  public confirmarEliminarMovimiento(): void {
    if (!this.movimientoAEliminar) {
      return;
    }

    const observacion =
      this.observacionEliminacionMovimiento && this.observacionEliminacionMovimiento.trim() !== ''
        ? this.observacionEliminacionMovimiento.trim()
        : null;

    this.productoService.eliminarMovimiento(this.movimientoAEliminar.id, observacion).subscribe({
      next: () => {
        this.mensajeService.success('Movimiento eliminado con éxito.');
        this.cerrarModalEliminarMovimiento();
        this.cargarMovimientos();
        this.cargarProductos();
        this.cargarProductosActivos();
      },
      error: (error) => {
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al eliminar el movimiento.',
        });
      },
    });
  }

  public cerrarModalEliminarMovimiento(): void {
    this.ocultarModal('eliminarMovimientoModal');
    this.limpiarBackdrop();
    this.movimientoAEliminar = null;
    this.observacionEliminacionMovimiento = '';
  }

  private mostrarModal(id: string): void {
    const modal = document.getElementById(id);
    if (!modal) {
      return;
    }
    bootstrap.Modal.getOrCreateInstance(modal).show();
  }

  private ocultarModal(id: string): void {
    const modal = document.getElementById(id);
    if (!modal) {
      return;
    }
    const instance = bootstrap.Modal.getInstance(modal);
    if (instance) {
      instance.hide();
    }
  }

  private limpiarBackdrop(): void {
    setTimeout(() => {
      const algunModalAbierto = document.querySelector('.modal.show');
      if (!algunModalAbierto) {
        document.body.classList.remove('modal-open');
        document.body.style.overflow = '';
        document.body.style.removeProperty('padding-right');
        document.querySelectorAll('.modal-backdrop').forEach((backdrop) => backdrop.remove());
      }
    }, 400);
  }

  guardarProducto() {
    if (this.formularioProducto.invalid) {
      this.mensajeService.error('Por favor rellenar todos los campos');
      return;
    }

    this.apiErrorService.clearFormErrors(this.formularioProducto);

    //logica por si es edicion o creacion
    if (this.productoSeleccionado) {
      const productoActualizado: ProductoResponse = {
        ...this.productoSeleccionado,
        ...this.formularioProducto.value,
      };
      this.productoService.actualizar(productoActualizado.id!, productoActualizado).subscribe({
        next: () => {
          this.mensajeService.success('Producto actualizado con éxito');
          this.reiniciarFormularioProducto();
          this.cargarProductos();
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            form: this.formularioProducto,
            contextMessage: 'Error al actualizar el producto.',
          });
        },
      });
    } else {
      // Modo creación
      this.productoService.crearProducto(this.formularioProducto.value).subscribe({
        next: () => {
          this.mensajeService.success('Producto registrado con éxito');
          this.reiniciarFormularioProducto();
          this.cargarProductos();
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            form: this.formularioProducto,
            contextMessage: 'Error al registrar el producto.',
          });
        },
      });
    }
  }

  editarProducto(producto: ProductoResponse) {
    this.apiErrorService.clearFormErrors(this.formularioProducto);
    this.productoSeleccionado = producto;
    this.modoCreacionProducto = true;
    this.formularioProducto.patchValue({
      nombre: producto.nombre,
      descripcion: producto.descripcion,
      precioCompra: producto.precioCompra,
      precioVenta: producto.precioVenta,
      stock: producto.stock,
      categoria: producto.categoria,
    });
  }

  cambiarEstado(producto: ProductoResponse) {
    this.productoService.cambiarEstadoProducto(producto.id!).subscribe({
      next: () => {
        this.mensajeService.success('Producto eliminado con éxito');
        this.cargarProductos();
      },
      error: (error) => {
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al eliminar el producto.',
        });
      },
    });
  }

  ocultarFormularioProducto(): void {
    this.modoCreacionProducto = false;
    this.productoSeleccionado = null;
    this.formularioProducto.reset();
    this.apiErrorService.clearFormErrors(this.formularioProducto);
    this.cargarProductos();
    this.cerrarModalProducto();
  }

  abrirModalAgregarStock(producto: ProductoResponse) {
    this.productoParaSumar = producto;
  }

  confirmarAgregarStock() {
    if (!this.productoParaSumar) {
      this.mensajeService.error('Debe seleccionar un producto.');
      return;
    }

    const cantidad = Number(this.cantidadAgregar);
    if (!Number.isFinite(cantidad) || cantidad === 0) {
      this.mensajeService.error('La cantidad debe ser distinta de cero.');
      return;
    }

    this.productoService.sumarStock(this.productoParaSumar.id!, cantidad).subscribe({
      next: () => {
        this.mensajeService.success('Stock actualizado con éxito.');
        this.productoParaSumar = null;
        this.cantidadAgregar = null;
        this.cargarProductos();
      },
      error: (error) => {
        this.apiErrorService.handle(error, {
          contextMessage: 'Error al actualizar el stock.',
        });
      },
    });
  }

  public confirmarIngresoMasivo(): void {
    if (this.formularioMovimientos.invalid) {
      this.formularioMovimientos.markAllAsTouched();
      this.mensajeService.error('Por favor completa los productos y cantidades antes de continuar.');
      return;
    }

    const movimientosFormValue = (this.formularioMovimientos.value.movimientos ??
      []) as MovimientoEntradaProductoRequest[];

    if (movimientosFormValue.length === 0) {
      this.mensajeService.error('Debe agregar al menos un producto.');
      return;
    }

    const productosYaAgregados = new Set<number>();
    const movimientos: MovimientoEntradaProductoRequest[] = [];

    for (let index = 0; index < movimientosFormValue.length; index++) {
      const movimiento = movimientosFormValue[index];
      const control = this.movimientosFormArray.at(index) as FormGroup;

      let productoId = Number(movimiento.productoId);
      const nombreIngresado = control.get('productoNombre')?.value?.trim() ?? '';

      if ((!productoId || !Number.isFinite(productoId)) && nombreIngresado) {
        const productoCoincidente = this.productosActivos.find(
          (producto) => producto.nombre.toLowerCase() === nombreIngresado.toLowerCase(),
        );
        if (productoCoincidente) {
          productoId = productoCoincidente.id;
          control.patchValue(
            {
              productoId: productoCoincidente.id,
              productoNombre: productoCoincidente.nombre,
            },
            { emitEvent: false },
          );
        }
      }

      if (!productoId || !Number.isFinite(productoId)) {
        const productoIdControl = control.get('productoId');
        const productoNombreControl = control.get('productoNombre');
        if (productoIdControl) {
          productoIdControl.setErrors({ ...(productoIdControl.errors ?? {}), required: true });
          productoIdControl.markAsTouched();
          productoIdControl.updateValueAndValidity({ onlySelf: true });
        }
        if (productoNombreControl) {
          productoNombreControl.setErrors({ ...(productoNombreControl.errors ?? {}), required: true });
          productoNombreControl.markAsTouched();
          productoNombreControl.updateValueAndValidity({ onlySelf: true });
        }
        this.mensajeService.error('Selecciona un producto válido en cada fila.');
        return;
      }

      const cantidad = Number(movimiento.cantidad);

      if (!Number.isFinite(cantidad) || cantidad === 0) {
        this.mensajeService.error('Cada producto debe tener una cantidad distinta de cero.');
        const cantidadControl = control.get('cantidad');
        if (cantidadControl) {
          cantidadControl.setErrors({ ...(cantidadControl.errors ?? {}), noZero: true });
          cantidadControl.markAsTouched();
          cantidadControl.updateValueAndValidity({ onlySelf: true });
        }
        return;
      }

      if (productosYaAgregados.has(productoId)) {
        this.mensajeService.error('No se puede repetir el mismo producto en la lista.');
        return;
      }

      productosYaAgregados.add(productoId);

      const observacion = control.get('observacion')?.value?.trim();

      movimientos.push({
        productoId,
        cantidad,
        observacion: observacion ? observacion : null,
      });
    }

    this.productoService.actualizarStockMasivo({ movimientos }).subscribe({
      next: () => {
        this.mensajeService.success('Stock actualizado con éxito.');
        this.cerrarModalIngresarProductos();
        this.cargarProductos();
        this.cargarProductosActivos();
      },
      error: (error) => {
        this.apiErrorService.handle(error, {
          form: this.formularioMovimientos,
          contextMessage: 'Error al actualizar el stock masivo.',
        });
      },
    });
  }

  cerrarModalAgregarStock() {
    const modal = document.getElementById('agregarStockModal');
    if (modal) {
      const bootstrapModal = bootstrap.Modal.getOrCreateInstance(modal);
      bootstrapModal?.hide();
    }
    this.productoParaSumar = null;
    this.cantidadAgregar = null;
    this.cargarProductos();
  }

  productoAEliminar: ProductoResponse | null = null;

  confirmarEliminacion(): void {
    if (!this.productoAEliminar) return;

    this.cambiarEstado(this.productoAEliminar);
    this.productoAEliminar = null;
  }

  private cerrarModalProducto(): void {
    const modal = document.getElementById('productoModal');
    if (modal) {
      bootstrap.Modal.getOrCreateInstance(modal).hide();
    }
  }

  public cerrarModalIngresarProductos(): void {
    const modal = document.getElementById('ingresarProductosModal');
    if (modal) {
      const instance = bootstrap.Modal.getInstance(modal) ?? bootstrap.Modal.getOrCreateInstance(modal);
      instance.hide();
    }
    this.limpiarBackdrop();
    this.resetFormularioMovimientos();
  }

  private reiniciarFormularioProducto(): void {
    this.productoSeleccionado = null;
    this.modoCreacionProducto = true;
    this.formularioProducto.reset();
    this.apiErrorService.clearFormErrors(this.formularioProducto);
  }
}
