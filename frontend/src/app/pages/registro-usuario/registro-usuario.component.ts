import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiErrorService } from '../../core/services/api-error.service';
import { MensajeService } from '../../services/mensaje.service';
import { UsuarioService } from '../../services/usuario.service';
import { CrearUsuarioRequest } from '../../models/usuario.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-registro-usuario',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './registro-usuario.component.html',
  styleUrls: ['./registro-usuario.component.css'],
})
export class RegistroUsuarioComponent implements OnInit {
  private fb = inject(FormBuilder);
  private apiErrorService = inject(ApiErrorService);
  private mensajeService = inject(MensajeService);
  private usuarioService = inject(UsuarioService);
  private authService = inject(AuthService);
  private router = inject(Router);

  formulario = this.fb.nonNullable.group({
    usuario: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(100)]),
    contrasena: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(120),
    ]),
    confirmarContrasena: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(120),
    ]),
  });

  enviando = false;

  ngOnInit(): void {
    if (this.authService.estaAutenticado()) {
      this.router.navigateByUrl('/capital');
    }
  }

  registrar(): void {
    const contrasenasValidas = this.contrasenasCoinciden();
    if (this.formulario.invalid || !contrasenasValidas) {
      this.formulario.markAllAsTouched();
      return;
    }

    const { usuario, contrasena } = this.formulario.getRawValue();
    const payload: CrearUsuarioRequest = {
      usuario: usuario.trim(),
      contrasena,
    };

    this.enviando = true;
    this.apiErrorService.clearFormErrors(this.formulario);

    this.usuarioService
      .crear(payload)
      .pipe(finalize(() => (this.enviando = false)))
      .subscribe({
        next: () => {
          this.mensajeService.success('Usuario creado correctamente. Ahora puedes iniciar sesión.');
          this.router.navigateByUrl('/login');
        },
        error: (error) => this.handleError(error),
      });
  }

  mostrarError(nombreControl: 'usuario' | 'contrasena' | 'confirmarContrasena'): boolean {
    const control = this.formulario.get(nombreControl);
    if (!control) {
      return false;
    }
    return Boolean(control.invalid && (control.dirty || control.touched));
  }

  private contrasenasCoinciden(): boolean {
    const { contrasena, confirmarContrasena } = this.formulario.getRawValue();
    const coinciden = contrasena === confirmarContrasena;
    const confirmarControl = this.formulario.get('confirmarContrasena');
    if (!confirmarControl) {
      return coinciden;
    }

    const currentErrors = confirmarControl.errors ?? {};

    if (!coinciden) {
      confirmarControl.setErrors({ ...currentErrors, mismatch: true });
    } else if ('mismatch' in currentErrors) {
      const { mismatch, ...rest } = currentErrors;
      confirmarControl.setErrors(Object.keys(rest).length > 0 ? rest : null);
    }

    return coinciden;
  }

  private handleError(error: unknown): void {
    const contextMessage =
      error instanceof HttpErrorResponse && (error.status === 0 || error.status >= 500)
        ? 'Error en el servidor. Intenta más tarde.'
        : 'No se pudo crear el usuario.';
    this.apiErrorService.handle(error, { form: this.formulario, contextMessage });
  }
}
