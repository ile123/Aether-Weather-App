import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Filler,
  ChartData,
  ChartOptions,
} from 'chart.js';

import { LocationSelectorComponent } from './location-selector/location-selector';
import { WeatherCardComponent } from './weather-card/weather-card';
import { ForecastCardComponent } from './forecast-card/forecast-card';
import {
  WeatherData,
  WeatherForecast,
} from '../../shared/models/weather.model';
import { WeatherApiService } from '../../core/services/weather-api';
import { BehaviorSubject, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

Chart.register(
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Filler,
);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    BaseChartDirective,
    LocationSelectorComponent,
    WeatherCardComponent,
    ForecastCardComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {
  private weatherService = inject(WeatherApiService);
  private destroyRef = inject(DestroyRef);

  currentWeather$ = new BehaviorSubject<WeatherData | null>(null);
  forecasts = signal<WeatherForecast[]>([]);
  loading = signal(false);

  private sseSubscription?: Subscription;

  chartData: ChartData<'line'> = { labels: [], datasets: [] };
  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    plugins: { tooltip: { enabled: true } },
    scales: {
      y: { title: { display: true, text: '°C' } },
    },
  };

  onLocationSelected(location: string): void {
    this.loading.set(true);
    this.currentWeather$.next(null);
    this.forecasts.set([]);

    this.sseSubscription?.unsubscribe();

    this.weatherService
      .getCurrentWeather(location)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.currentWeather$.next(data);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });

    this.sseSubscription = this.weatherService
      .streamWeatherUpdates(location)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => this.currentWeather$.next(data));

    this.weatherService
      .getForecast(location, 2)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((forecasts) => {
        this.forecasts.set(forecasts.slice(0, 24));
        this.updateChart(forecasts.slice(0, 24));
      });
  }

  private updateChart(forecasts: WeatherForecast[]): void {
    this.chartData = {
      labels: forecasts.map((f) =>
        new Date(f.forecastTime).toLocaleTimeString('en-GB', {
          hour: '2-digit',
          minute: '2-digit',
          month: 'short',
          day: 'numeric',
        }),
      ),
      datasets: [
        {
          label: 'Temperature (°C)',
          data: forecasts.map((f) => f.temperature),
          borderColor: '#1976d2',
          backgroundColor: 'rgba(25, 118, 210, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 3,
        },
      ],
    };
  }
}
