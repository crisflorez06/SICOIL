import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSelectModule } from '@angular/material/select';
import { VentaRequest } from '../../../models/venta.model';
import { VentaService } from '../../../services/venta.service';
import {
  FiltroClienteResponse,
  FiltroProductoPrecio,
  FiltroProductoResponse,
} from '../../../models/filtros.model';
import { MensajeService } from '../../../services/mensaje.service';

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
  ],
  templateUrl: './registro-venta-dialog.component.html',
  styleUrls: ['./registro-venta-dialog.component.css'],
})

export class RegistroVentaDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<RegistroVentaDialogComponent>);
  private ventaService = inject(VentaService);
  private mensajeService = inject(MensajeService);

  clientesDisponibles: FiltroClienteResponse[] = [];
  productosDisponibles: FiltroProductoResponse[] = [];
  cargandoFiltros = true;
  errorFiltros = false;

  readonly formulario = this.fb.group({
    clienteId: [null as number | null, Validators.required],
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
    this.cargarFiltros();
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
    if (!raw || !raw.productoNombre || raw.precioId == null || raw.precioVenta == null) {
      return;
    }

    const precioSeleccionado = this.buscarPrecio(raw.productoNombre, raw.precioId);
    if (!precioSeleccionado) {
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
    if (!nombreProducto) {
      return [];
    }
    const producto = this.productosDisponibles.find((p) => p.nombreProducto === nombreProducto);
    return producto?.precios ?? [];
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
    if (!raw.clienteId) {
      this.formulario.get('clienteId')?.setErrors({ required: true });
      return;
    }
    const items = this.itemsRegistrados.map((item) => ({
      productoId: item.precioId,
      cantidad: item.cantidad,
      subtotal: item.subtotal,
    }));

    const resultado: VentaRequest = {
      clienteId: raw.clienteId,
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
    return this.productosDisponibles
      .find((producto) => producto.nombreProducto === nombreProducto)
      ?.precios.find((precio) => precio.id === precioId);
  }

}
