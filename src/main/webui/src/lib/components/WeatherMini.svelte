<script lang="ts">
  import { onMount } from 'svelte';

  interface Props {
    highContrast?: boolean;
  }

  let { highContrast = false }: Props = $props();

//emojis for api
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

//saves values for display
  let temp = $state<number | null>(null);
  let date = $state<string>('');
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
      date = new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
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
  <span class="weather-mini" class:high-contrast={highContrast}>...</span>
{:else if temp !== null}
  <span class="weather-mini" class:high-contrast={highContrast} title="Current weather">{emoji} {temp}°F · {date}</span>
{/if}

<style>
  .weather-mini {
    font-size: 1.0rem;
    color: rgb(0, 0, 0);
    white-space: nowrap;
    padding: 0.2rem 0.5rem;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 6px;
  }

  .weather-mini.high-contrast {
    color: #ffffff;
    background: rgba(0, 0, 0, 0.6);
    border: 1px solid #ffffff;
  }
</style>