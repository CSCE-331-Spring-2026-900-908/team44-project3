<script lang="ts">
  import { onMount } from 'svelte';

  interface GeoData {
    city: string;
    lat: number;
    lon: number;
    status: string;
  }

  interface WeatherCurrent {
    temperature: number;
    weathercode: number;
  }

  interface WeatherData {
    current_weather: WeatherCurrent;
    hourly?: {
      precipitation?: number[];
    };
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

  let weather = $state<WeatherInfo | null>(null);
  let error = $state<string | null>(null);
  let loading = $state(true);

  async function fetchWeather(): Promise<void> {
    try {
      const geoRes = await fetch('https://ipapi.co/json/');
      if (!geoRes.ok) throw new Error('Location fetch failed.');
      const geo = await geoRes.json();

      const wxUrl = `https://api.open-meteo.com/v1/forecast?latitude=${geo.latitude}&longitude=${geo.longitude}&current=temperature_2m,weather_code&hourly=precipitation&temperature_unit=fahrenheit&timezone=auto`;

      const wxRes = await fetch(wxUrl);
      if (!wxRes.ok) throw new Error('Weather fetch failed.');
      const wx = await wxRes.json();

      const current = wx.current;
      if (!current) throw new Error('No current weather returned.');

      const [condition, emoji] = WMO_CODES[current.weather_code] ?? ['Unknown', '🌡️'];

      weather = {
        city: geo.city,
        date: new Date().toLocaleDateString('en-US', {
          weekday: 'long',
          month: 'long',
          day: 'numeric'
        }),
        tempF: Math.round(current.temperature_2m),
        precipMm: wx.hourly?.precipitation?.[0] ?? 0,
        condition,
        emoji,
      };
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load weather.';
      console.error(e);
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    fetchWeather();
  });
</script>

<div class="weather-anchor">
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

      <div class="divider"></div>

      <div class="stats">
        <div class="stat">
          <span class="label">Temp</span>
          <span class="value">{weather.tempF}°F</span>
        </div>
        <div class="stat">
          <span class="label">Precip</span>
          <span class="value">{weather.precipMm.toFixed(1)} mm</span>
        </div>
        <div class="stat">
          <span class="label">Condition</span>
          <span class="value">{weather.condition}</span>
        </div>
      </div>
    </div>
  {/if}
</div>

<style>
  .weather-anchor {
    display: flex;
    justify-content: flex-end;
    padding: 1rem 1rem 0 0;
  }

  .weather-widget {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    padding: 0.75rem 1rem;
    width: 280px;
    font-family: sans-serif;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .city {
    font-size: 0.85rem;
    font-weight: 600;
    margin: 0 0 1px;
    color: #111;
  }

  .date {
    font-size: 0.7rem;
    color: #6b7280;
    margin: 0;
  }

  .emoji {
    font-size: 1.6rem;
    line-height: 1;
  }

  .divider {
    border-top: 1px solid #e5e7eb;
    margin: 0.6rem 0;
  }

  .stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;
  }

  .stat {
    background: #f9fafb;
    border-radius: 6px;
    padding: 0.4rem 0.5rem;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .label {
    font-size: 0.6rem;
    color: #6b7280;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .value {
    font-size: 0.8rem;
    font-weight: 600;
    color: #111;
  }

  .status {
    font-size: 0.75rem;
    color: #6b7280;
  }

  .status.error {
    color: #dc2626;
  }
</style>