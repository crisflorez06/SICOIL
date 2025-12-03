export type MovimientoTipo = 'ENTRADA' | 'SALIDA';

export interface KardexResponse {
  id: number;
  productoId: number;
  productoNombre: string;
  usuarioId: number;
  usuarioNombre: string;
  cantidad: number;
  tipo: MovimientoTipo;
  fechaRegistro: string;
  comentario?: string | null;
}

export interface KardexFiltro {
  page?: number;
  size?: number;
  sort?: string;
  productoId?: number;
  usuarioId?: number;
  tipo?: MovimientoTipo;
  desde?: string;
  hasta?: string;
}
