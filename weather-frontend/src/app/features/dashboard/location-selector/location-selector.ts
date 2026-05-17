import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Output,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-location-selector',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],

  templateUrl: './location-selector.html',
  styleUrl: './location-selector.scss',
})
export class LocationSelectorComponent {
  @Output() locationSelected = new EventEmitter<string>();
  locationInput = '';

  onSearch(): void {
    const trimmed = this.locationInput.trim();
    if (trimmed) {
      this.locationSelected.emit(trimmed);
    }
  }
}
