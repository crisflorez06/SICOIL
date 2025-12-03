import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CarteraAbonoRequest } from '../../../models/cartera.model';

@Component({
  selector: 'app-registrar-abono-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './registrar-abono-dialog.component.html',
  styleUrls: ['./registrar-abono-dialog.component.css'],
})
export class RegistrarAbonoDialogComponent {
  private dialogRef = inject(MatDialogRef<RegistrarAbonoDialogComponent, CarteraAbonoRequest | undefined>);
  data = inject<{ clienteNombre: string }>(MAT_DIALOG_DATA);
  private fb = inject(FormBuilder);

  formulario = this.fb.group({
    monto: [null as number | null, [Validators.required, Validators.min(1)]],
    observacion: ['', [Validators.maxLength(255)]],
  });

  cancelar(): void {
    this.dialogRef.close(undefined);
  }

  confirmar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const monto = Number(this.formulario.value.monto);
    if (!monto || isNaN(monto) || monto <= 0) {
      this.formulario.get('monto')?.setErrors({ min: true });
      return;
    }
    const observacion = this.formulario.value.observacion?.trim();
    const payload: CarteraAbonoRequest = {
      monto,
      observacion: observacion ? observacion : null,
    };
    this.dialogRef.close(payload);
  }
}
