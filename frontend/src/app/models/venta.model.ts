import { Page } from '../core/types/page';
import { DetalleVentaRequest, DetalleVentaResponse } from './detalle-venta.model';

export interface VentaRequest {
  usuarioId: number;
  metodoPago: string; // Efectivo, Tarjeta, Transferencia
  detalles: DetalleVentaRequest[]; // Lista de productos vendidos
}

export interface VentaResponse {
  id: number;
  fecha: string; // ISO string: "2025-10-14T19:43:00"
  metodoPago: string;
  total: number; // Total calculado por el backend
  usuarioId?: number;
  usuarioNombre?: string | null;
  detalles?: DetalleVentaResponse[]; // Opcional si se trae embebido
}

export interface VentaPageResponse {
  ventas: Page<VentaResponse>;
  totalGeneral: number;
}
