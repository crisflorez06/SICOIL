import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { InventarioSalidaRequest } from '../../../models/producto.model';

interface EliminarStockDialogData {
  id: number;
  nombre: string;
  stock: number;
}

@Component({
  selector: 'app-eliminar-stock-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './eliminar-stock-dialog.component.html',
  styleUrls: ['./eliminar-stock-dialog.component.css'],
})
export class EliminarStockDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<EliminarStockDialogComponent>);

  readonly formulario = this.fb.group({
    cantidad: [null as number | null, [Validators.required, Validators.min(1)]],
    observacion: [''],
  });

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: EliminarStockDialogData) {
    this.actualizarMaximo();
  }

  private actualizarMaximo(): void {
    const maximo = this.data.stock > 0 ? this.data.stock : 1;
    const cantidadControl = this.formulario.get('cantidad');
    cantidadControl?.setValidators([Validators.required, Validators.min(1), Validators.max(maximo)]);
    cantidadControl?.updateValueAndValidity({ emitEvent: false });
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
    const resultado: InventarioSalidaRequest = {
      cantidad: payload.cantidad ?? 0,
      observacion: payload.observacion?.trim() || undefined,
    };
    this.dialogRef.close(resultado);
  }
}
