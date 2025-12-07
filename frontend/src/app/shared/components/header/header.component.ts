import { Component, OnDestroy, Renderer2, inject } from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Observable } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';

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
export class HeaderComponent implements OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);
  private mensajeService = inject(MensajeService);
  private renderer = inject(Renderer2);
  private document = inject(DOCUMENT);
  private dialog = inject(MatDialog);

  readonly links = [
    { path: '/capital', label: 'Dashboard' },
    { path: '/productos', label: 'Productos' },
    { path: '/movimientos', label: 'Movimientos' },
    { path: '/clientes', label: 'Clientes' },
    { path: '/ventas', label: 'Ventas' },
    { path: '/cartera', label: 'Cartera' },
  ];

  readonly today = new Date();
  readonly usuario$: Observable<LoginResponse | null> = this.authService.usuario$;
  cerrandoSesion = false;
  menuAbierto = false;

  cerrarSesion(): void {
    if (this.cerrandoSesion) {
      return;
    }
    this.menuAbierto = false;
    this.actualizarDifuminado();
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

  toggleMenu(): void {
    this.menuAbierto = !this.menuAbierto;
    if (this.menuAbierto) {
      this.dialog.closeAll();
    }
    this.actualizarDifuminado();
  }

  cerrarMenu(): void {
    this.menuAbierto = false;
    this.actualizarDifuminado();
  }

  ngOnDestroy(): void {
    const body = this.document?.body;
    if (body) {
      this.renderer.removeClass(body, 'menu-open');
    }
  }

  private actualizarDifuminado(): void {
    const body = this.document?.body;
    if (!body) {
      return;
    }
    if (this.menuAbierto) {
      this.renderer.addClass(body, 'menu-open');
    } else {
      this.renderer.removeClass(body, 'menu-open');
    }
  }
}
