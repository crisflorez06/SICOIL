export type MovimientoTipo = 'INGRESO' | 'SALIDA' | string;

export interface MovimientoResponse {
  id: number;
  productoId: number;
  productoNombre: string;
  cantidad: number;
  tipo: MovimientoTipo;
  fechaMovimiento: string;
  observacion: string | null;
}
