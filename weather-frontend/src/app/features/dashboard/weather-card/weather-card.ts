import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import {
  getWeatherEmoji,
  WeatherData,
} from '../../../shared/models/weather.model';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-weather-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatProgressSpinnerModule, MatCardModule],
  templateUrl: './weather-card.html',
  styleUrl: './weather-card.scss',
})
export class WeatherCardComponent {
  @Input() weather: WeatherData | null = null;
  getEmoji = getWeatherEmoji;
}
