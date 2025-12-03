import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import {
  CarteraAbonoDetalleResponse,
  CarteraAbonoRequest,
  CarteraCreditoDetalleResponse,
  CarteraResumenResponse,
} from '../../../models/cartera.model';
import { CarteraService } from '../../../services/cartera.service';
import { MensajeService } from '../../../services/mensaje.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { EliminarAbonoDialogComponent } from '../eliminar-abono-dialog/eliminar-abono-dialog.component';

type EstadoDetalle = 'cargando' | 'error' | 'listo';
type DialogMode = 'abonos' | 'creditos';

@Component({
  selector: 'app-detalle-cartera-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  templateUrl: './detalle-cartera-dialog.component.html',
  styleUrls: ['./detalle-cartera-dialog.component.css'],
})
export class DetalleCarteraDialogComponent implements OnInit {
  private carteraService = inject(CarteraService);
  private dialogRef = inject(MatDialogRef<DetalleCarteraDialogComponent>);
  private dialog = inject(MatDialog);
  private mensajeService = inject(MensajeService);
  private apiErrorService = inject(ApiErrorService);
  data = inject<{ cliente: CarteraResumenResponse; tipo: DialogMode }>(MAT_DIALOG_DATA);

  estado: EstadoDetalle = 'cargando';
  abonosActivos: CarteraAbonoDetalleResponse[] = [];
  abonosEliminados: CarteraAbonoDetalleResponse[] = [];
  creditos: CarteraCreditoDetalleResponse[] = [];
  huboCambios = false;
  private abonosProcesando = new Set<number>();
  mostrarEliminados = false;

  ngOnInit(): void {
    this.cargarDetalle();
  }

  cerrar(): void {
    this.dialogRef.close(this.huboCambios);
  }

  recargar(): void {
    this.cargarDetalle();
  }

  private cargarDetalle(): void {
    this.estado = 'cargando';
    if (this.data.tipo === 'abonos') {
      this.carteraService.listarAbonos(this.data.cliente.clienteId).subscribe({
        next: (movimientos) => {
          this.estado = 'listo';
          const lista = movimientos ?? [];
          this.abonosActivos = lista.filter((mov) => (mov.monto ?? 0) >= 0);
          this.abonosEliminados = lista.filter((mov) => (mov.monto ?? 0) < 0);
          if (!this.abonosEliminados.length) {
            this.mostrarEliminados = false;
          }
        },
        error: () => {
          this.estado = 'error';
        },
      });
    } else {
      this.carteraService.listarCreditos(this.data.cliente.clienteId).subscribe({
        next: (creditos) => {
          this.estado = 'listo';
          this.creditos = creditos ?? [];
        },
        error: () => {
          this.estado = 'error';
        },
      });
    }
  }

  eliminarAbono(abono: CarteraAbonoDetalleResponse): void {
    if (this.data.tipo !== 'abonos' || this.abonosProcesando.has(abono.movimientoId)) {
      return;
    }
    const dialogRef = this.dialog.open(EliminarAbonoDialogComponent, {
      width: '520px',
      disableClose: true,
      data: {
        clienteNombre: this.data.cliente.clienteNombre,
        monto: abono.monto,
        fecha: abono.fecha,
        usuarioNombre: abono.usuarioNombre,
      },
    });

    dialogRef.afterClosed().subscribe((payload?: CarteraAbonoRequest | null) => {
      if (!payload) {
        return;
      }
      this.abonosProcesando.add(abono.movimientoId);
      this.carteraService
        .eliminarAbono(this.data.cliente.clienteId, abono.movimientoId, payload)
        .pipe(
          finalize(() => {
            this.abonosProcesando.delete(abono.movimientoId);
          }),
        )
        .subscribe({
          next: () => {
            this.mensajeService.success('Abono eliminado correctamente.');
            this.huboCambios = true;
            this.cargarDetalle();
          },
          error: (error) => {
            this.apiErrorService.handle(error, { contextMessage: 'No se pudo eliminar el abono.' });
          },
        });
    });
  }

  estaEliminando(movimientoId: number): boolean {
    return this.abonosProcesando.has(movimientoId);
  }

  toggleEliminados(): void {
    this.mostrarEliminados = !this.mostrarEliminados;
  }
}
