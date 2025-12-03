import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CarteraAbonoRequest } from '../../../models/cartera.model';

@Component({
  selector: 'app-eliminar-abono-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './eliminar-abono-dialog.component.html',
  styleUrls: ['./eliminar-abono-dialog.component.css'],
})
export class EliminarAbonoDialogComponent {
  private dialogRef = inject(MatDialogRef<EliminarAbonoDialogComponent, CarteraAbonoRequest | undefined>);
  data = inject<{
    clienteNombre: string;
    monto: number;
    fecha: string;
    usuarioNombre: string;
  }>(MAT_DIALOG_DATA);
  private fb = inject(FormBuilder);

  formulario = this.fb.group({
    observacion: ['', [Validators.required, Validators.maxLength(255)]],
  });

  cancelar(): void {
    this.dialogRef.close(undefined);
  }

  confirmar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const observacion = this.formulario.value.observacion?.trim();
    if (!observacion) {
      this.formulario.get('observacion')?.setErrors({ required: true });
      return;
    }
    const payload: CarteraAbonoRequest = {
      monto: this.data.monto,
      observacion,
    };
    this.dialogRef.close(payload);
  }
}
