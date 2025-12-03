import { DetalleVentaRequest, DetalleVentaResponse } from './detalle-venta.model';

export type TipoVenta = 'CONTADO' | 'CREDITO';

export interface VentaItemResponse {
  productoNombre: string;
  precioCompra: number;
  cantidad: number;
  precioVenta: number;
}

export interface VentaListadoResponse {
  ventaId: number;
  clienteNombre: string;
  totalVenta: number;
  tipoVenta: TipoVenta;
  activa: boolean;
  motivoAnulacion?: string | null;
  usuarioNombre: string;
  fechaRegistro: string;
  items: VentaItemResponse[];
}

export interface PaginaVentaResponse {
  content: VentaListadoResponse[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface VentaRequest {
  clienteId: number;
  tipoVenta: TipoVenta;
  items: DetalleVentaRequest[];
}

export interface VentaResponse {
  id: number;
  clienteId: number;
  clienteNombre: string;
  usuarioId: number;
  usuarioNombre: string;
  tipoVenta: TipoVenta;
  activa: boolean;
  motivoAnulacion?: string | null;
  total: number;
  fechaRegistro: string;
  detalles: DetalleVentaResponse[];
}

export interface VentaAnulacionRequest {
  motivo: string;
}

export interface VentaListadoFiltro {
  page?: number;
  size?: number;
  tipoVenta?: TipoVenta;
  nombreCliente?: string;
  nombreUsuario?: string;
  activa?: boolean;
  desde?: string;
  hasta?: string;
}
