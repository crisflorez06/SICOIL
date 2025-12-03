export type CapitalOrigen = 'VENTA' | 'COMPRA' | 'INYECCION';

export interface CapitalMovimientoResponse {
  id: number;
  origen: CapitalOrigen;
  referenciaId?: number | null;
  monto: number;
  esCredito: boolean;
  descripcion?: string | null;
  creadoEn: string;
  usuarioId: number;
  usuarioNombre: string;
}

export interface CapitalResumenResponse {
  saldoReal: number;
  totalEntradas: number;
  totalSalidas: number;
  totalCreditoPendiente: number;
  totalCredito: number;
  capitalNeto: number;
  totalGanancias: number;
  totalAbonos: number;
  totalUnidadesVendidas: number;
  totalCajasVendidas: number;
  topProductos: CapitalTopProducto[];
  topClientes: CapitalTopCliente[];
}

export interface CapitalTopProducto {
  productoId: number;
  productoNombre: string;
  cantidadVendida: number;
  totalVendido: number;
  participacionPorcentaje: number;
}

export interface CapitalTopCliente {
  clienteId: number;
  clienteNombre: string;
  totalVentas: number;
  montoComprado: number;
  participacionPorcentaje: number;
}

export interface CapitalInyeccionRequest {
  monto: number;
  descripcion?: string | null;
}

export interface CapitalMovimientoFiltro {
  origen?: CapitalOrigen;
  esCredito?: boolean;
  referenciaId?: number;
  descripcion?: string;
  desde?: string;
  hasta?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CapitalResumenFiltro {
  desde?: string;
  hasta?: string;
}
