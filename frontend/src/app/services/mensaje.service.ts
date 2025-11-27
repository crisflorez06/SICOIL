import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class MensajeService {
  private document = inject(DOCUMENT);

  success(mensaje: string) {
    this.createToast(mensaje, 'success');
  }

  error(mensaje: string) {
    this.createToast(mensaje, 'error');
  }

  private createToast(mensaje: string, tipo: 'success' | 'error') {
    const contenedor =
      this.document.getElementById('toast-container') ?? this.crearContenedor();

    const toast = this.document.createElement('div');
    toast.className = [
      'toast',
      'align-items-center',
      'text-white',
      'border-0',
      'show',
      `bg-${tipo === 'success' ? 'success' : 'danger'}`,
    ].join(' ');
    toast.role = 'alert';
    toast.ariaLive = 'assertive';
    toast.ariaAtomic = 'true';
    toast.style.minWidth = '280px';
    toast.style.marginBottom = '10px';

    toast.innerHTML = `
      <div class="d-flex">
        <div class="toast-body">
          ${mensaje}
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" aria-label="Cerrar"></button>
      </div>
    `;

    contenedor.appendChild(toast);

    const botonCerrar = toast.querySelector('button');
    botonCerrar?.addEventListener('click', () => toast.remove());

    setTimeout(() => toast.remove(), 2500);
  }

  private crearContenedor(): HTMLElement {
    const contenedor = this.document.createElement('div');
    contenedor.id = 'toast-container';
    contenedor.className = 'position-fixed bottom-0 end-0 p-3';
    contenedor.style.zIndex = '1060';
    this.document.body.appendChild(contenedor);
    return contenedor;
  }
}
