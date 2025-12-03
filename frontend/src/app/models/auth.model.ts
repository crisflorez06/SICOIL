export interface LoginRequest {
  usuario: string;
  contrasena: string;
}

export interface LoginResponse {
  id: number;
  usuario: string;
}
