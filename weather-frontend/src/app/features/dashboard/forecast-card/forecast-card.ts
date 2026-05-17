import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import {
  getWeatherEmoji,
  WeatherForecast,
} from '../../../shared/models/weather.model';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-forecast-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCardModule],
  templateUrl: './forecast-card.html',
  styleUrl: './forecast-card.scss',
})
export class ForecastCardComponent {
  @Input() forecast!: WeatherForecast;
  getEmoji = getWeatherEmoji;
}
