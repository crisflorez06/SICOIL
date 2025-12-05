import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'capital' },
  {
    path: 'productos',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/productos/productos.component').then((m) => m.ProductosComponent),
  },
  {
    path: 'clientes',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/clientes/clientes.component').then((m) => m.ClientesComponent),
  },
  {
    path: 'capital',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/capital/capital.component').then((m) => m.CapitalComponent),
  },
  {
    path: 'movimientos',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/movimientos/movimientos.component').then((m) => m.MovimientosComponent),
  },
  {
    path: 'ventas',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/ventas/ventas.component').then((m) => m.VentasComponent),
  },
  {
    path: 'cartera',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/cartera/cartera.component').then((m) => m.CarteraComponent),
  },
  { path: '**', redirectTo: 'capital' },
];
