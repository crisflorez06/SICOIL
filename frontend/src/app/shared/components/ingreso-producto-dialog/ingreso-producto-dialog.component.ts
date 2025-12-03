import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { IngresoProductoRequest, ProductosAgrupadosResponse } from '../../../models/producto.model';
import { ProductoService } from '../../../services/producto.service';
import { MensajeService } from '../../../services/mensaje.service';

type IngresoProductoListaEntry = IngresoProductoRequest;

@Component({
  selector: 'app-ingreso-producto-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatAutocompleteModule,
  ],
  templateUrl: './ingreso-producto-dialog.component.html',
  styleUrls: ['./ingreso-producto-dialog.component.css'],
})
export class IngresoProductoDialogComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<IngresoProductoDialogComponent>);
  private productoService = inject(ProductoService);
  private mensajeService = inject(MensajeService);
  private destroy$ = new Subject<void>();

  readonly formulario = this.fb.group({
    nombreProducto: ['', Validators.required],
    precioCompra: [null as number | null, [Validators.required, Validators.min(0.01)]],
    cantidad: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  productosRegistrados: IngresoProductoListaEntry[] = [];
  sugerenciasNombres: string[] = [];
  cargandoSugerencias = false;
  nombreInvalido = false;
  private productosPorNombre = new Map<string, ProductosAgrupadosResponse>();

  ngOnInit(): void {
    this.inicializarAutocompletado();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private inicializarAutocompletado(): void {
    const control = this.formulario.get('nombreProducto');
    if (!control) {
      return;
    }

    control.valueChanges
      .pipe(
        startWith(control.value ?? ''),
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => {
          this.cargandoSugerencias = true;
          this.nombreInvalido = false;
        }),
        switchMap((valor) => {
          const termino = typeof valor === 'string' ? valor.trim() : '';
          return this.productoService
            .listar({ size: 15, nombre: termino || undefined })
            .pipe(
              map((resp) => {
                const productos = resp.content ?? [];
                this.productosPorNombre.clear();
                productos.forEach((producto) => {
                  this.productosPorNombre.set(producto.nombre.toLocaleLowerCase(), producto);
                });
                const nombres = productos.map((producto) => producto.nombre);
                return Array.from(new Set(nombres));
              }),
              catchError(() => {
                this.productosPorNombre.clear();
                return of<string[]>([]);
              })
            );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((nombres) => {
        this.sugerenciasNombres = nombres;
        this.cargandoSugerencias = false;
      });
  }

  agregarALista(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const payload = this.formulario.getRawValue();
    if (!payload) {
      return;
    }
    const nombreIngresado = (payload.nombreProducto ?? '').trim();
    const claveNombre = nombreIngresado.toLocaleLowerCase();
    const existeNombre = this.sugerenciasNombres.some(
      (nombre) => nombre.toLocaleLowerCase() === claveNombre
    );
    if (!existeNombre) {
      this.nombreInvalido = true;
      this.mensajeService.error('El producto ingresado no existe. Selecciona uno de la lista.');
      return;
    }
    const nuevo: IngresoProductoRequest = {
      nombreProducto: payload.nombreProducto ?? '',
      precioCompra: payload.precioCompra ?? 0,
      cantidad: payload.cantidad ?? 0,
    };
    this.productosRegistrados = [
      ...this.productosRegistrados,
      nuevo,
    ];
    this.formulario.reset({
      nombreProducto: '',
      precioCompra: null,
      cantidad: null,
    });
    this.nombreInvalido = false;
  }

  eliminarProducto(index: number): void {
    this.productosRegistrados = this.productosRegistrados.filter((_, i) => i !== index);
  }

  cancelar(): void {
    this.dialogRef.close();
  }

  guardar(): void {
    if (this.productosRegistrados.length === 0) {
      this.formulario.markAllAsTouched();
      return;
    }
    const soloRequests = this.productosRegistrados.map((item) => ({
      nombreProducto: item.nombreProducto,
      precioCompra: item.precioCompra,
      cantidad: item.cantidad,
    }));
    this.dialogRef.close(soloRequests);
  }
}
