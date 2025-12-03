import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { ProductoActualizarRequest } from '../../../models/producto.model';

interface EditarProductoDialogData {
  nombre: string;
  cantidadPorCajas: number;
}

@Component({
  selector: 'app-editar-producto-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './editar-producto-dialog.component.html',
  styleUrls: ['./editar-producto-dialog.component.css'],
})
export class EditarProductoDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<EditarProductoDialogComponent>);

  readonly formulario = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    cantidadPorCajas: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: EditarProductoDialogData) {
    this.formulario.patchValue({
      nombre: data.nombre,
      cantidadPorCajas: data.cantidadPorCajas,
    });
  }

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
    const resultado: ProductoActualizarRequest = {
      nombre: payload.nombre?.trim() ?? this.data.nombre,
      cantidadPorCajas: payload.cantidadPorCajas ?? this.data.cantidadPorCajas,
    };
    this.dialogRef.close(resultado);
  }
}
