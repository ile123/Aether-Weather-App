import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import {
  WeatherData,
  WeatherForecast,
} from '../../shared/models/weather.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WeatherApiService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getCurrentWeather(location: string): Observable<WeatherData> {
    return this.http
      .get<{ data: WeatherData }>(`${this.apiUrl}/api/weather/current`, {
        params: { location },
      })
      .pipe(map((r) => r.data));
  }

  getForecast(location: string, days = 7): Observable<WeatherForecast[]> {
    return this.http.get<WeatherForecast[]>(
      `${this.apiUrl}/api/weather/forecast`,
      {
        params: { location, days },
      },
    );
  }

  streamWeatherUpdates(location: string): Observable<WeatherData> {
    return new Observable((observer) => {
      const es = new EventSource(
        `${this.apiUrl}/api/weather/stream?location=${encodeURIComponent(location)}`,
      );
      es.addEventListener('weather-update', (e: MessageEvent) => {
        observer.next(JSON.parse(e.data));
      });
      es.onerror = () => observer.error('SSE connection error');
      return () => es.close();
    });
  }
}
