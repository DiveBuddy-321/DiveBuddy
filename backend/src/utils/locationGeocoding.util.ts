type GeocodeResult = {
  latitude: number;
  longitude: number;
};

type GeocodeApiResponse = {
  status: string;
  results: Array<{
    geometry?: {
      location?: { lat?: number; lng?: number };
    };
  }>;
};

export async function getCoordinatesFromLocation(location: string): Promise<GeocodeResult | null> {
  try {
    const apiKey = process.env.GEOCODING_API;
    if (!apiKey) {
      throw new Error('GEOCODING_API is not set');
    }

    const url = `https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(location)}&key=${apiKey}`;
    const res = await fetch(url);
    if (!res.ok) {
      console.error(`Geocoding request failed for location "${location}": HTTP ${res.status}`);
      return null;
    }
    const data = (await res.json()) as GeocodeApiResponse;
    if (data.status !== 'OK' || !data.results || data.results.length === 0) {
      return null;
    }
    const first = data.results[0];
    const loc = first.geometry?.location;
    if (!loc || typeof loc.lat !== 'number' || typeof loc.lng !== 'number') {
      return null;
    }
    return { latitude: loc.lat, longitude: loc.lng };
  } catch (error) {
    console.error('Failed to geocode location', error);
    return null;
  }
}


