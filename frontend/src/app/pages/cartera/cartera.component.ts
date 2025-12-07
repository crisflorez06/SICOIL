import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import {
  CarteraAbonoRequest,
  CarteraPendienteFiltro,
  CarteraResumenResponse,
} from '../../models/cartera.model';
import { CarteraService } from '../../services/cartera.service';
import { MensajeService } from '../../services/mensaje.service';
import { RegistrarAbonoDialogComponent } from '../../shared/components/registrar-abono-dialog/registrar-abono-dialog.component';
import { ApiErrorService } from '../../core/services/api-error.service';
import { DetalleCarteraDialogComponent } from '../../shared/components/detalle-cartera-dialog/detalle-cartera-dialog.component';

type EstadoCarga = 'idle' | 'cargando' | 'error' | 'listo';

@Component({
  selector: 'app-cartera',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cartera.component.html',
  styleUrls: ['./cartera.component.css'],
})
export class CarteraComponent implements OnInit {
  private carteraService = inject(CarteraService);
  private mensajeService = inject(MensajeService);
  private dialog = inject(MatDialog);
  private apiErrorService = inject(ApiErrorService);

  cartera: CarteraResumenResponse[] = [];
  estado: EstadoCarga = 'idle';

  filtroCliente = '';
  filtroDesde = '';
  filtroHasta = '';

  private abonosEnProceso = new Set<number>();
  totalClientesConSaldo = 0;
  saldoPendienteTotal = 0;
  totalAbonosAcumulados = 0;
  totalCreditosAcumulados = 0;

  ngOnInit(): void {
    this.cargarCartera();
  }

  cargarCartera(): void {
    this.estado = 'cargando';
    const filtros: CarteraPendienteFiltro = {};
    if (this.filtroCliente.trim()) {
      filtros.cliente = this.filtroCliente.trim();
    }
    if (this.filtroDesde) {
      filtros.desde = this.filtroDesde;
    }
    if (this.filtroHasta) {
      filtros.hasta = this.filtroHasta;
    }
    this.carteraService.listarPendientes(filtros).subscribe({
      next: (respuesta) => {
        this.cartera = respuesta ?? [];
        this.actualizarResumenes();
        this.estado = 'listo';
      },
      error: () => {
        this.estado = 'error';
        this.mensajeService.error('No se pudo obtener la cartera. Intenta nuevamente.');
      },
    });
  }

  buscarCartera(): void {
    this.cargarCartera();
  }

  limpiarFiltros(): void {
    if (!this.filtroCliente && !this.filtroDesde && !this.filtroHasta) {
      return;
    }
    this.filtroCliente = '';
    this.filtroDesde = '';
    this.filtroHasta = '';
    this.cargarCartera();
  }

  get hayClientesConSaldo(): boolean {
    return this.cartera.length > 0;
  }

  verAbonos(cliente: CarteraResumenResponse): void {
    this.abrirDetalle(cliente, 'abonos');
  }

  verCreditos(cliente: CarteraResumenResponse): void {
    this.abrirDetalle(cliente, 'creditos');
  }

  private abrirDetalle(cliente: CarteraResumenResponse, tipo: 'abonos' | 'creditos'): void {
    const dialogRef = this.dialog.open(DetalleCarteraDialogComponent, {
      width: '720px',
      data: { cliente, tipo },
      disableClose: true,
    });
    dialogRef.afterClosed().subscribe((huboCambios?: boolean) => {
      if (huboCambios) {
        this.cargarCartera();
      }
    });
  }

  registrarAbono(cliente: CarteraResumenResponse): void {
    const dialogRef = this.dialog.open(RegistrarAbonoDialogComponent, {
      width: '460px',
      disableClose: true,
      data: { clienteNombre: cliente.clienteNombre },
    });

    dialogRef.afterClosed().subscribe((payload?: CarteraAbonoRequest | null) => {
      if (!payload) {
        return;
      }
      this.abonosEnProceso.add(cliente.clienteId);
      this.carteraService
        .registrarAbono(cliente.clienteId, payload)
        .pipe(
          finalize(() => {
            this.abonosEnProceso.delete(cliente.clienteId);
          }),
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Abono registrado correctamente.');
            this.cargarCartera();
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo registrar el abono.' });
          },
        });
    });
  }

  estaRegistrandoAbono(clienteId: number): boolean {
    return this.abonosEnProceso.has(clienteId);
  }

  private actualizarResumenes(): void {
    const clientes = Array.isArray(this.cartera) ? this.cartera : [];
    this.totalClientesConSaldo = clientes.length;
    this.saldoPendienteTotal = clientes.reduce((total, cliente) => total + Number(cliente.saldoPendiente ?? 0), 0);
    this.totalAbonosAcumulados = clientes.reduce((total, cliente) => total + Number(cliente.totalAbonos ?? 0), 0);
    this.totalCreditosAcumulados = clientes.reduce((total, cliente) => total + Number(cliente.totalCreditos ?? 0), 0);
  }
}
