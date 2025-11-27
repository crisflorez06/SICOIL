import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'productos' },
  {
    path: 'productos',
    loadComponent: () =>
      import('./pages/productos/productos.component').then((m) => m.ProductosComponent),
  },
  {
    path: 'ventas',
    loadComponent: () => import('./pages/ventas/ventas.component').then((m) => m.VentasComponent),
  },
  {
    path: 'reportes',
    loadComponent: () =>
      import('./pages/reportes/reportes.component').then((m) => m.ReportesComponent),
  },
  { path: '**', redirectTo: 'productos' },
];
