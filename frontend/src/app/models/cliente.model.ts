export interface ClienteRequest {
  nombre: string;
  telefono?: string;
  direccion?: string;
}

export interface ClienteResponse {
  id: number;
  nombre: string;
  telefono?: string | null;
  direccion?: string | null;
  fechaRegistro: string;
}
