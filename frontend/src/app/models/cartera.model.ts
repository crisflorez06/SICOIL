export interface CarteraResumenResponse {
  clienteId: number;
  clienteNombre: string;
  saldoPendiente: number;
  totalAbonos: number;
  totalCreditos: number;
  ultimaActualizacion: string;
}

export interface CarteraAbonoDetalleResponse {
  movimientoId: number;
  monto: number;
  fecha: string;
  usuarioNombre: string;
  observacion?: string | null;
}

export interface CarteraCreditoDetalleResponse {
  movimientoId: number;
  ventaId: number;
  monto: number;
  fecha: string;
  usuarioNombre: string;
  observacion?: string | null;
}

export interface CarteraAbonoRequest {
  monto: number;
  observacion?: string | null;
}

export interface CarteraPendienteFiltro {
  cliente?: string;
  desde?: string;
  hasta?: string;
}

export interface CarteraFechaFiltro {
  desde?: string;
  hasta?: string;
}
