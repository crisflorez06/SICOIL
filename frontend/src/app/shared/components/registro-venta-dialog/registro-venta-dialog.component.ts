import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { VentaRequest } from '../../../models/venta.model';
import { VentaService } from '../../../services/venta.service';
import {
  FiltroClienteResponse,
  FiltroProductoPrecio,
  FiltroProductoResponse,
} from '../../../models/filtros.model';
import { MensajeService } from '../../../services/mensaje.service';
import { Subject } from 'rxjs';
import { debounceTime, startWith, takeUntil } from 'rxjs/operators';

type ItemVentaRegistrado = {
  productoNombre: string;
  precioId: number;
  precioVenta: number;
  cantidad: number;
  costoUnitario: number;
  subtotal: number;
};

@Component({
  selector: 'app-registro-venta-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonToggleModule,
    MatSelectModule,
    MatAutocompleteModule,
  ],
  templateUrl: './registro-venta-dialog.component.html',
  styleUrls: ['./registro-venta-dialog.component.css'],
})
export class RegistroVentaDialogComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<RegistroVentaDialogComponent>);
  private ventaService = inject(VentaService);
  private mensajeService = inject(MensajeService);
  private destroy$ = new Subject<void>();

  clientesDisponibles: FiltroClienteResponse[] = [];
  productosDisponibles: FiltroProductoResponse[] = [];
  clientesFiltrados: FiltroClienteResponse[] = [];
  productosFiltrados: FiltroProductoResponse[] = [];
  cargandoFiltros = true;
  errorFiltros = false;
  clienteNombreInvalido = false;
  productoNombreInvalido = false;

  readonly formulario = this.fb.group({
    clienteNombre: ['', Validators.required],
    tipoVenta: ['CONTADO' as 'CONTADO' | 'CREDITO', Validators.required],
  });

  readonly itemForm = this.fb.group({
    productoNombre: [null as string | null, Validators.required],
    precioId: [null as number | null, Validators.required],
    precioVenta: [null as number | null, [Validators.required, Validators.min(0.01)]],
    cantidad: [1, [Validators.required, Validators.min(1)]],
  });

  itemsRegistrados: ItemVentaRegistrado[] = [];

  ngOnInit(): void {
    this.configurarAutocompletados();
    this.cargarFiltros();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get totalCalculado(): number {
    return this.itemsRegistrados.reduce((total, item) => total + item.subtotal, 0);
  }

  get preciosDisponiblesActuales(): FiltroProductoPrecio[] {
    const nombre = (this.itemForm.get('productoNombre')?.value as string | null) ?? null;
    return this.preciosPorProducto(nombre);
  }

  agregarItemALista(): void {
    if (this.itemForm.invalid) {
      this.itemForm.markAllAsTouched();
      return;
    }
    const raw = this.itemForm.getRawValue();
    if (!raw || raw.productoNombre == null || raw.precioId == null || raw.precioVenta == null) {
      return;
    }

    const productoSeleccionado = this.buscarProductoPorNombre(raw.productoNombre);
    if (!productoSeleccionado) {
      this.productoNombreInvalido = true;
      this.itemForm.get('productoNombre')?.setErrors({ optionInvalida: true });
      this.mensajeService.error('Debes seleccionar un producto válido.');
      return;
    }

    const precioSeleccionado = this.buscarPrecio(raw.productoNombre, raw.precioId);
    if (!precioSeleccionado) {
      this.itemForm.get('precioId')?.setErrors({ required: true });
      this.mensajeService.error('Debes seleccionar un precio disponible.');
      return;
    }

    const cantidadDisponible = Number(precioSeleccionado.cantidad ?? 0);
    const cantidadReservada = this.itemsRegistrados
      .filter((item) => item.precioId === raw.precioId)
      .reduce((total, item) => total + item.cantidad, 0);
    const stockRestante = cantidadDisponible - cantidadReservada;

    if (stockRestante <= 0) {
      this.itemForm.get('cantidad')?.setErrors({ stockInsuficiente: true });
      this.itemForm.get('cantidad')?.markAsTouched();
      this.mensajeService.error('No hay unidades disponibles para este producto.');
      return;
    }

    const cantidad = Number(raw.cantidad ?? 0);
    if (cantidad > stockRestante) {
      this.itemForm.get('cantidad')?.setErrors({ stockInsuficiente: true });
      this.itemForm.get('cantidad')?.markAsTouched();
      this.mensajeService.error(`Solo hay ${stockRestante} unidad(es) disponibles.`);
      return;
    }

    const precio = Number(raw.precioVenta ?? 0);
    const subtotal = precio;
    const costoUnitario = Number(precioSeleccionado.precioCompra ?? 0);
    this.itemsRegistrados = [
      ...this.itemsRegistrados,
      {
        productoNombre: raw.productoNombre,
        precioId: raw.precioId,
        precioVenta: precio,
        cantidad,
        costoUnitario,
        subtotal,
      },
    ];
    this.itemForm.reset({
      productoNombre: null,
      precioId: null,
      precioVenta: null,
      cantidad: 1,
    });
  }

  eliminarItem(index: number): void {
    this.itemsRegistrados = this.itemsRegistrados.filter((_, i) => i !== index);
  }

  cambioProducto(): void {
    this.itemForm.patchValue(
      {
        precioId: null,
        precioVenta: null,
      },
      { emitEvent: false },
    );
  }

  cambioPrecio(): void {
    const nombreProducto = this.itemForm.get('productoNombre')?.value as string | null;
    const precioId = this.itemForm.get('precioId')?.value as number | null;
    const precio = this.buscarPrecio(nombreProducto, precioId);
    this.itemForm.get('precioVenta')?.setValue(precio?.precioCompra ?? null);
  }

  preciosPorProducto(nombreProducto: string | null): FiltroProductoPrecio[] {
    return (
      this.buscarProductoPorNombre(nombreProducto)?.precios.filter(
        (precio) => Number(precio.cantidad ?? 0) > 0,
      ) ?? []
    );
  }

  cancelar(): void {
    this.dialogRef.close();
  }

  guardar(): void {
    if (this.formulario.invalid || this.itemsRegistrados.length === 0) {
      this.formulario.markAllAsTouched();
      if (this.itemsRegistrados.length === 0) {
        this.itemForm.markAllAsTouched();
      }
      return;
    }
    const raw = this.formulario.getRawValue();
    const nombreCliente = (raw.clienteNombre ?? '').trim();
    const clienteSeleccionado = this.buscarClientePorNombre(nombreCliente);
    if (!clienteSeleccionado) {
      this.formulario.get('clienteNombre')?.setErrors({ optionInvalida: true });
      this.clienteNombreInvalido = true;
      return;
    }
    const items = this.itemsRegistrados.map((item) => ({
      productoId: item.precioId,
      cantidad: item.cantidad,
      subtotal: item.subtotal,
    }));

    const resultado: VentaRequest = {
      clienteId: clienteSeleccionado.id,
      tipoVenta: raw.tipoVenta ?? 'CONTADO',
      items,
    };

    this.dialogRef.close(resultado);
  }

  reintentarFiltros(): void {
    this.cargarFiltros();
  }

  private cargarFiltros(): void {
    this.cargandoFiltros = true;
    this.errorFiltros = false;
    this.ventaService.obtenerFiltrosRegistro().subscribe({
      next: (respuesta) => {
        this.clientesDisponibles = respuesta.clientes ?? [];
        this.productosDisponibles = respuesta.productos ?? [];
        this.clientesFiltrados = this.filtrarClientes(
          (this.formulario.get('clienteNombre')?.value as string | null) ?? '',
        );
        this.productosFiltrados = this.filtrarProductos(
          (this.itemForm.get('productoNombre')?.value as string | null) ?? '',
        );
        this.cargandoFiltros = false;
      },
      error: () => {
        this.errorFiltros = true;
        this.cargandoFiltros = false;
      },
    });
  }

  private buscarPrecio(nombreProducto: string | null, precioId: number | null): FiltroProductoPrecio | undefined {
    if (!nombreProducto || precioId == null) {
      return undefined;
    }
    const producto = this.buscarProductoPorNombre(nombreProducto);
    return producto?.precios.find((precio) => precio.id === precioId);
  }

  private configurarAutocompletados(): void {
    const clienteControl = this.formulario.get('clienteNombre');
    if (clienteControl) {
      clienteControl.valueChanges
        .pipe(
          startWith(clienteControl.value ?? ''),
          debounceTime(200),
          takeUntil(this.destroy$),
        )
        .subscribe((valor) => {
          const termino = typeof valor === 'string' ? valor : '';
          this.clientesFiltrados = this.filtrarClientes(termino);
          this.clienteNombreInvalido = false;
        });
    }

    const productoControl = this.itemForm.get('productoNombre');
    if (productoControl) {
      productoControl.valueChanges
        .pipe(
          startWith(productoControl.value ?? ''),
          debounceTime(200),
          takeUntil(this.destroy$),
        )
        .subscribe((valor) => {
          const termino = typeof valor === 'string' ? valor : '';
          this.productosFiltrados = this.filtrarProductos(termino);
          this.productoNombreInvalido = false;
          this.cambioProducto();
        });
    }
  }

  seleccionarCliente(nombreCliente: string): void {
    const cliente = this.buscarClientePorNombre(nombreCliente);
    if (!cliente) {
      this.clienteNombreInvalido = true;
      this.formulario.get('clienteNombre')?.setErrors({ optionInvalida: true });
      return;
    }
    this.clienteNombreInvalido = false;
  }

  seleccionarProducto(nombreProducto: string): void {
    const producto = this.buscarProductoPorNombre(nombreProducto);
    if (!producto) {
      this.productoNombreInvalido = true;
      this.itemForm.get('productoNombre')?.setErrors({ optionInvalida: true });
      return;
    }
    this.productoNombreInvalido = false;
    this.cambioProducto();
  }

  private filtrarClientes(termino: string): FiltroClienteResponse[] {
    if (!termino) {
      return this.clientesDisponibles.slice(0, 15);
    }
    const terminoNormalizado = termino.toLocaleLowerCase();
    return this.clientesDisponibles
      .filter((cliente) => cliente.nombre.toLocaleLowerCase().includes(terminoNormalizado))
      .slice(0, 15);
  }

  private filtrarProductos(termino: string): FiltroProductoResponse[] {
    if (!termino) {
      return this.productosDisponibles.slice(0, 15);
    }
    const terminoNormalizado = termino.toLocaleLowerCase();
    return this.productosDisponibles
      .filter((producto) => producto.nombreProducto.toLocaleLowerCase().includes(terminoNormalizado))
      .slice(0, 15);
  }

  private buscarClientePorNombre(nombre: string | null): FiltroClienteResponse | undefined {
    if (!nombre) {
      return undefined;
    }
    const termino = nombre.trim().toLocaleLowerCase();
    return this.clientesDisponibles.find((cliente) => cliente.nombre.toLocaleLowerCase() === termino);
  }

  private buscarProductoPorNombre(nombreProducto: string | null): FiltroProductoResponse | undefined {
    if (!nombreProducto) {
      return undefined;
    }
    const termino = nombreProducto.trim().toLocaleLowerCase();
    return this.productosDisponibles.find(
      (producto) => producto.nombreProducto.toLocaleLowerCase() === termino,
    );
  }
}
