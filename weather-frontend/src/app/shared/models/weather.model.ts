export interface WeatherData {
  locationName: string;
  temperature: number;
  apparentTemperature: number;
  relativeHumidity: number;
  windSpeed: number;
  weatherCode: number;
  description: string;
  isDay: boolean;
  recordedAt: string;
}

export interface WeatherForecast {
  locationName: string;
  forecastTime: string;
  temperature: number;
  precipitationProbability: number;
  precipitation: number;
  weatherCode: number;
  description: string;
}

export function getWeatherEmoji(code: number): string {
  if (code === 0) return '☀️';
  if (code <= 2) return '🌤️';
  if (code === 3) return '☁️';
  if (code <= 48) return '🌫️';
  if (code <= 55) return '🌦️';
  if (code <= 65) return '🌧️';
  if (code <= 77) return '❄️';
  if (code <= 82) return '🌧️';
  if (code <= 86) return '🌨️';
  return '⛈️';
}
