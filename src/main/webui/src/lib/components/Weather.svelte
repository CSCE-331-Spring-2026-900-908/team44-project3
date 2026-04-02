<script lang="ts">
  import { onMount } from 'svelte';

  interface GeoData {
    city: string;
    lat: number;
    lon: number;
    status: string;
  }

  interface WeatherCurrent {
    temperature_2m: number;
    precipitation: number;
    weather_code: number;
  }

  interface WeatherData {
    current: WeatherCurrent;
  }

  interface WeatherInfo {
    city: string;
    date: string;
    tempF: number;
    precipMm: number;
    condition: string;
    emoji: string;
  }

  const WMO_CODES: Record<number, [string, string]> = {
    0:  ['Clear Sky',            '☀️'],
    1:  ['Mainly Clear',         '🌤️'],
    2:  ['Partly Cloudy',        '⛅'],
    3:  ['Overcast',             '☁️'],
    45: ['Fog',                  '🌫️'],
    48: ['Icy Fog',              '🌫️'],
    51: ['Light Drizzle',        '🌦️'],
    53: ['Drizzle',              '🌦️'],
    55: ['Heavy Drizzle',        '🌧️'],
    61: ['Slight Rain',          '🌧️'],
    63: ['Rain',                 '🌧️'],
    65: ['Heavy Rain',           '🌧️'],
    66: ['Freezing Rain',        '🌨️'],
    67: ['Heavy Freezing Rain',  '🌨️'],
    71: ['Slight Snow',          '🌨️'],
    73: ['Snow',                 '❄️'],
    75: ['Heavy Snow',           '❄️'],
    77: ['Snow Grains',          '🌨️'],
    80: ['Rain Showers',         '🌦️'],
    81: ['Showers',              '🌧️'],
    82: ['Violent Showers',      '⛈️'],
    85: ['Snow Showers',         '🌨️'],
    86: ['Heavy Snow Showers',   '❄️'],
    95: ['Thunderstorm',         '⛈️'],
    96: ['Thunderstorm w/ Hail', '⛈️'],
    99: ['Severe Thunderstorm',  '⛈️'],
  };

  let weather: WeatherInfo | null = null;
  let error: string | null = null;
  let loading = true;

  async function fetchWeather(): Promise<void> {
    // Step 1: Get city + coordinates from IP
    const geoRes = await fetch('https://ip-api.com/json/?fields=city,lat,lon,status');
    const geo: GeoData = await geoRes.json();

    if (geo.status !== 'success') {
      throw new Error('Could not determine location from IP.');
    }

    // Step 2: Fetch current weather from Open-Meteo (no API key needed)
    const wxUrl =
      `https://api.open-meteo.com/v1/forecast` +
      `?latitude=${geo.lat}&longitude=${geo.lon}` +
      `&current=temperature_2m,precipitation,weather_code` +
      `&temperature_unit=fahrenheit` +
      `&timezone=auto`;

    const wxRes = await fetch(wxUrl);
    const wx: WeatherData = await wxRes.json();
    const current = wx.current;

    // Step 3: Resolve WMO weather code to label + emoji
    const [condition, emoji] = WMO_CODES[current.weather_code] ?? ['Unknown', '🌡️'];

    // Step 4: Format the current date
    const date = new Date().toLocaleDateString('en-US', {
      weekday: 'long',
      month:   'long',
      day:     'numeric',
      year:    'numeric',
    });

    weather = {
      city:      geo.city,
      date,
      tempF:     Math.round(current.temperature_2m),
      precipMm:  current.precipitation,
      condition,
      emoji,
    };
  }

  onMount(async () => {
    try {
      await fetchWeather();
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load weather.';
    } finally {
      loading = false;
    }
  });
</script>

{#if loading}
  <p class="status">Loading weather...</p>
{:else if error}
  <p class="status error">{error}</p>
{:else if weather}
  <div class="weather-widget">
    <div class="header">
      <div>
        <p class="city">{weather.city}</p>
        <p class="date">{weather.date}</p>
      </div>
      <span class="emoji">{weather.emoji}</span>
    </div>

    <div class="divider" />

    <div class="stats">
      <div class="stat">
        <span class="label">Temperature</span>
        <span class="value">{weather.tempF}°F</span>
      </div>
      <div class="stat">
        <span class="label">Precipitation</span>
        <span class="value">{weather.precipMm.toFixed(1)} mm</span>
      </div>
      <div class="stat">
        <span class="label">Condition</span>
        <span class="value">{weather.condition}</span>
      </div>
    </div>
  </div>
{/if}

<style>
  .weather-widget {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 1.25rem 1.5rem;
    max-width: 420px;
    font-family: sans-serif;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .city {
    font-size: 1.1rem;
    font-weight: 600;
    margin: 0 0 2px;
    color: #111;
  }

  .date {
    font-size: 0.8rem;
    color: #6b7280;
    margin: 0;
  }

  .emoji {
    font-size: 2.4rem;
    line-height: 1;
  }

  .divider {
    border-top: 1px solid #e5e7eb;
    margin: 1rem 0;
  }

  .stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }

  .stat {
    background: #f9fafb;
    border-radius: 8px;
    padding: 0.75rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .label {
    font-size: 0.7rem;
    color: #6b7280;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .value {
    font-size: 1.1rem;
    font-weight: 600;
    color: #111;
  }

  .status {
    font-size: 0.9rem;
    color: #6b7280;
  }

  .status.error {
    color: #dc2626;
  }
</style>