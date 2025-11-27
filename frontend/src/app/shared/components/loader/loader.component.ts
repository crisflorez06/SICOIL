import { Component, computed, inject } from '@angular/core';
import { NgIf } from '@angular/common';
import { LoadingService } from '../../../core/services/loading.service';

@Component({
  selector: 'app-loader',
  imports: [NgIf],
  templateUrl: './loader.component.html',
  styleUrl: './loader.component.css',
})

export class LoaderComponent {

  private readonly loading = inject(LoadingService);
  
  visible = computed(() => this.loading.isLoading());
}
