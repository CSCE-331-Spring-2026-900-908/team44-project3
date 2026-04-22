<script lang="ts">
  import { onMount } from 'svelte';

  const WMO_CODES: Record<number, string> = {
    0:  '☀️',
    1:  '🌤️',
    2:  '⛅',
    3:  '☁️',
    45: '🌫️',
    48: '🌫️',
    51: '🌦️',
    53: '🌦️',
    55: '🌧️',
    61: '🌧️',
    63: '🌧️',
    65: '🌧️',
    66: '🌨️',
    67: '🌨️',
    71: '🌨️',
    73: '❄️',
    75: '❄️',
    77: '🌨️',
    80: '🌦️',
    81: '🌧️',
    82: '⛈️',
    85: '🌨️',
    86: '❄️',
    95: '⛈️',
    96: '⛈️',
    99: '⛈️',
  };

  let temp = $state<number | null>(null);
  let emoji = $state<string>('🌡️');
  let loading = $state(true);

  async function fetchWeather(): Promise<void> {
    try {
      const geoRes = await fetch('https://ipapi.co/json/');
      if (!geoRes.ok) throw new Error('Location fetch failed.');
      const geo = await geoRes.json();

      const wxUrl = `https://api.open-meteo.com/v1/forecast?latitude=${geo.latitude}&longitude=${geo.longitude}&current=temperature_2m,weather_code&temperature_unit=fahrenheit&timezone=auto`;
      const wxRes = await fetch(wxUrl);
      if (!wxRes.ok) throw new Error('Weather fetch failed.');
      const wx = await wxRes.json();

      const current = wx.current;
      if (!current) throw new Error('No current weather returned.');

      temp = Math.round(current.temperature_2m);
      emoji = WMO_CODES[current.weather_code] ?? '🌡️';
    } catch (e) {
      console.error(e);
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    fetchWeather();
  });
</script>

{#if loading}
  <span class="weather-mini">...</span>
{:else if temp !== null}
  <span class="weather-mini" title="Current weather">{emoji} {temp}°F</span>
{/if}

<style>
  .weather-mini {
    font-size: 0.85rem;
    color: rgba(255, 255, 255, 0.85);
    white-space: nowrap;
    padding: 0.25rem 0.5rem;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 6px;
  }
</style>