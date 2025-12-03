import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-anulacion-venta-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './anulacion-venta-dialog.component.html',
  styleUrls: ['./anulacion-venta-dialog.component.css'],
})
export class AnulacionVentaDialogComponent {
  private dialogRef = inject(MatDialogRef<AnulacionVentaDialogComponent, string | undefined>);
  private fb = inject(FormBuilder);

  formulario = this.fb.nonNullable.group({
    motivo: ['', [Validators.required, Validators.maxLength(255)]],
  });

  cancelar(): void {
    this.dialogRef.close(undefined);
  }

  confirmar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const motivoLimpio = this.formulario.value.motivo?.trim();
    if (!motivoLimpio) {
      this.formulario.get('motivo')?.setErrors({ required: true });
      return;
    }
    this.dialogRef.close(motivoLimpio);
  }
}
