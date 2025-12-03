import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Observable } from 'rxjs';

import { AuthService } from '../../../services/auth.service';
import { LoginResponse } from '../../../models/auth.model';
import { MensajeService } from '../../../services/mensaje.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
})
export class HeaderComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private mensajeService = inject(MensajeService);

  readonly links = [
    { path: '/capital', label: 'Dashboard' },
    { path: '/productos', label: 'Productos' },
    { path: '/clientes', label: 'Clientes' },
    { path: '/ventas', label: 'Ventas' },
    { path: '/cartera', label: 'Cartera' },
  ];

  readonly today = new Date();
  readonly usuario$: Observable<LoginResponse | null> = this.authService.usuario$;
  cerrandoSesion = false;

  cerrarSesion(): void {
    if (this.cerrandoSesion) {
      return;
    }
    this.cerrandoSesion = true;
    this.authService.logout().subscribe({
      next: () => {
        this.mensajeService.success('Sesión finalizada.');
        this.router.navigateByUrl('/login');
      },
      error: () => {
        this.mensajeService.error('No se pudo cerrar sesión, intenta nuevamente.');
      },
      complete: () => {
        this.cerrandoSesion = false;
      },
    });
  }
}
