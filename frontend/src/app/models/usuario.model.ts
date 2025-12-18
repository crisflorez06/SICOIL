export interface CrearUsuarioRequest {
  usuario: string;
  contrasena: string;
}

export interface UsuarioResponse {
  id: number;
  usuario: string;
}
