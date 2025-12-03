import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';

import { ClienteService } from '../../services/cliente.service';
import { MensajeService } from '../../services/mensaje.service';
import { ClienteRequest, ClienteResponse } from '../../models/cliente.model';
import { RegistroClienteDialogComponent } from '../../shared/components/registro-cliente-dialog/registro-cliente-dialog.component';
import { ApiErrorService } from '../../core/services/api-error.service';

type EstadoCarga = 'idle' | 'cargando' | 'error' | 'listo';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.component.html',
  styleUrls: ['./clientes.component.css'],
})
export class ClientesComponent implements OnInit {
  private clienteService = inject(ClienteService);
  private mensajeService = inject(MensajeService);
  private dialog = inject(MatDialog);
  private apiErrorService = inject(ApiErrorService);

  clientes: ClienteResponse[] = [];
  estado: EstadoCarga = 'idle';
  terminoBusqueda = '';
  registrandoCliente = false;

  ngOnInit(): void {
    this.cargarClientes();
  }

  cargarClientes(): void {
    this.estado = 'cargando';
    this.clienteService
      .listar(this.terminoBusqueda.trim() || undefined)
      .subscribe({
        next: (clientes) => {
          this.clientes = clientes ?? [];
          this.estado = 'listo';
        },
        error: () => {
          this.estado = 'error';
          this.mensajeService.error('No se pudieron obtener los clientes.');
        },
      });
  }

  buscarClientes(): void {
    this.cargarClientes();
  }

  reiniciarBusqueda(): void {
    if (this.terminoBusqueda.trim() === '') {
      return;
    }
    this.terminoBusqueda = '';
    this.buscarClientes();
  }

  registrarCliente(): void {
    const dialogRef = this.dialog.open(RegistroClienteDialogComponent, {
      width: '520px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((payload?: ClienteRequest | null) => {
      if (!payload) {
        return;
      }

      this.registrandoCliente = true;
      this.clienteService
        .crear(payload)
        .pipe(
          finalize(() => {
            this.registrandoCliente = false;
          }),
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Cliente registrado correctamente.');
            this.cargarClientes();
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo registrar el cliente.' });
          },
        });
    });
  }

  get hayClientes(): boolean {
    return this.clientes.length > 0;
  }
}
