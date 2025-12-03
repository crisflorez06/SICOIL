import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiErrorService } from '../../core/services/api-error.service';
import { MensajeService } from '../../services/mensaje.service';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private apiErrorService = inject(ApiErrorService);
  private authService = inject(AuthService);
  private mensajeService = inject(MensajeService);
  private router = inject(Router);

  formulario = this.fb.group({
    usuario: ['', Validators.required],
    contrasena: ['', Validators.required],
  });

  enviando = false;

  ngOnInit(): void {
    if (this.authService.estaAutenticado()) {
      this.router.navigateByUrl('/capital');
    }
  }

  iniciarSesion(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const payload = this.formulario.getRawValue() as LoginRequest;

    this.enviando = true;
    this.apiErrorService.clearFormErrors(this.formulario);

    this.authService
      .login(payload)
      .pipe(finalize(() => (this.enviando = false)))
      .subscribe({
        next: () => {
          this.mensajeService.success('Bienvenido a SICOIL.');
          this.router.navigateByUrl('/capital');
        },
        error: (error) => {
          this.apiErrorService.handle(error, {
            form: this.formulario,
            contextMessage: 'Usuario o contraseña incorrectos.',
          });
        },
      });
  }

  mostrarError(nombreControl: 'usuario' | 'contrasena'): boolean {
    const control = this.formulario.get(nombreControl);
    if (!control) {
      return false;
    }
    return Boolean(control.invalid && (control.dirty || control.touched));
  }
}
