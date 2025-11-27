import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
})
export class HeaderComponent {
  readonly links = [
    { path: '/productos', label: 'Productos' },
    { path: '/ventas', label: 'Ventas' },
    { path: '/reportes', label: 'Reportes' },
  ];

  readonly today = new Date();
}
