import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { CapitalInyeccionRequest } from '../../../models/capital.model';

@Component({
  selector: 'app-inyeccion-capital-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './inyeccion-capital-dialog.component.html',
  styleUrls: ['./inyeccion-capital-dialog.component.css'],
})
export class InyeccionCapitalDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<InyeccionCapitalDialogComponent>);

  readonly formulario = this.fb.group({
    monto: [null as number | null, [Validators.required, Validators.min(0.01)]],
    descripcion: ['', [Validators.maxLength(250)]],
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

    const monto = Number(payload.monto ?? 0);
    if (Number.isNaN(monto) || monto <= 0) {
      this.formulario.get('monto')?.setErrors({ min: true });
      this.formulario.get('monto')?.markAsTouched();
      return;
    }

    const resultado: CapitalInyeccionRequest = {
      monto,
      descripcion: payload.descripcion?.trim() || undefined,
    };
    this.dialogRef.close(resultado);
  }
}
