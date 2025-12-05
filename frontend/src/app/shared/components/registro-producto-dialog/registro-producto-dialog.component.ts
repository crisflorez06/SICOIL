import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ProductoRequest } from '../../../models/producto.model';

@Component({
  selector: 'app-registro-producto-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './registro-producto-dialog.component.html',
  styleUrls: ['./registro-producto-dialog.component.css'],
})
export class RegistroProductoDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<RegistroProductoDialogComponent>);

  readonly formulario = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    presentacion: ['UNIDAD' as 'CUARTO' | 'UNIDAD' | 'PINTA' | 'GALON', Validators.required],
    cantidadPorCajas: [1, [Validators.required, Validators.min(1)]],
    precioCompra: [null as number | null, [Validators.required, Validators.min(0.01)]],
    stock: [null as number | null, [Validators.required, Validators.min(0)]],
  });

  cancelar(): void {
    this.dialogRef.close();
  }

  guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const payload = this.formulario.getRawValue();
    if (!payload) {
      return;
    }
    const precioFinal = payload.precioCompra ?? 0;
    const stockFinal = payload.stock ?? 0;
    const cantidadPorCajas = payload.cantidadPorCajas ?? 1;
    const nombreBase = (payload.nombre ?? '').trim();
    const presentacion = payload.presentacion ?? 'UNIDAD';
    const nombreNormalizado = nombreBase ? nombreBase.toLocaleUpperCase() : '';
    const nombreFinal = nombreNormalizado ? `${nombreNormalizado} ${presentacion}` : presentacion;

    const resultado: ProductoRequest = {
      nombre: nombreFinal.toLocaleUpperCase(),
      cantidadPorCajas,
      precioCompra: precioFinal,
      stock: stockFinal,
    };
    this.dialogRef.close(resultado);
  }
}
