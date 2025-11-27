export interface Gasto {
  id: number;
  monto: number;
  descripcion: string;
  fecha: string;
}

export type CrearGastoRequest = Pick<Gasto, 'monto' | 'descripcion'>;

export type ActualizarGastoRequest = CrearGastoRequest;
