import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { ClienteRequest } from '../../../models/cliente.model';

@Component({
  selector: 'app-registro-cliente-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './registro-cliente-dialog.component.html',
  styleUrls: ['./registro-cliente-dialog.component.css'],
})
export class RegistroClienteDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<RegistroClienteDialogComponent>);

  readonly formulario = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    telefono: ['', [Validators.maxLength(30)]],
    direccion: ['', [Validators.maxLength(200)]],
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

    const nombre = payload.nombre?.trim() ?? '';
    if (!nombre) {
      this.formulario.get('nombre')?.setErrors({ required: true });
      this.formulario.get('nombre')?.markAsTouched();
      return;
    }

    const resultado: ClienteRequest = {
      nombre,
      telefono: payload.telefono?.trim() || undefined,
      direccion: payload.direccion?.trim() || undefined,
    };

    this.dialogRef.close(resultado);
  }
}
