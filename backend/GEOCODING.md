# Geocoding Guide

## Overview

The application automatically geocodes user locations (lat/long) to human-readable addresses (city, province, country) using the Google Maps Geocoding API.

## How It Works

### Automatic Geocoding

Geocoding happens automatically in two scenarios:

1. **When a new user is created** with location data
2. **When a user updates their location** (lat/long)

The geocoded data (city, province, country) is stored in the database alongside the coordinates.

### API Usage

**Update user location:**
```bash
PUT /user
{
  "lat": 49.2827,
  "long": -123.1207
}
```

**Backend automatically adds:**
```json
{
  "lat": 49.2827,
  "long": -123.1207,
  "city": "Vancouver",
  "province": "British Columbia",
  "country": "Canada"
}
```

## Geocoding Existing Users

If you have existing users with lat/long but no geocoded data, run the migration script:

```bash
cd backend
npm run geocode-users
```

### What the script does:

1. ✅ Connects to MongoDB
2. ✅ Finds all users with lat/long but no city
3. ✅ Geocodes each user using Google Maps API
4. ✅ Updates database with city, province, country
5. ✅ Respects API rate limits (100ms delay between requests)
6. ✅ Continues on errors
7. ✅ Provides progress and summary

### Example Output:

```
🔌 Connecting to MongoDB...
✅ Connected to MongoDB

📊 Found 503 users to geocode (out of 503 total users)

[1/503] Geocoding user lisa.patel.0@example.com...
   ✓ Vancouver, British Columbia, Canada
[2/503] Geocoding user karen.campbell.1@example.com...
   ✓ Toronto, Ontario, Canada
...

📈 Geocoding Summary:
   ✅ Successfully geocoded: 500
   ❌ Failed: 3
   📊 Total processed: 503

✨ Geocoding complete!
```

## Configuration

### Google Maps API Key

Set your API key in `.env`:
```bash
GEOCODING_API=your_google_maps_api_key_here
```

### Rate Limits

- Google Maps Geocoding API: 50 requests/second
- Script default: 10 requests/second (safe buffer)
- Adjust `DELAY_MS` in `geocodeExistingUsers.ts` if needed

## Cost Estimation

Google Maps Geocoding API pricing (as of 2024):
- Free tier: $200/month credit (~40,000 requests)
- After free tier: $5 per 1,000 requests

**One-time geocoding cost for 500 users:**
- Free ✅ (well within free tier)

**Ongoing costs:**
- Only charged when users update their location
- Minimal ongoing cost

## Troubleshooting

### "No results found" errors
- Coordinates might be invalid (0, 0) or in the ocean
- Check that lat/long are in valid ranges

### Rate limit errors
- Increase `DELAY_MS` in the script
- Check your Google Cloud Console quotas

### API key errors
- Verify `GEOCODING_API` is set in `.env`
- Check that Geocoding API is enabled in Google Cloud Console
- Verify billing is enabled (required even for free tier)

## Performance

### Response Times

- **getBuddies API**: < 100ms (data pre-geocoded)
- **updateProfile**: +200-500ms (one-time geocoding)
- **Scalability**: O(1) for read operations

### Why Backend Geocoding?

✅ **Fast**: Geocode once, use forever  
✅ **Consistent**: All clients get same data  
✅ **Cost-effective**: Only geocode on updates  
✅ **Reliable**: Centralized error handling  
✅ **Scalable**: Works for 1000+ users  

## Development

### Test Geocoding

```typescript
import { getLocationFromCoordinates } from './utils/geoCoding.util';

const location = await getLocationFromCoordinates(49.2827, -123.1207);
console.log(location);
// { city: 'Vancouver', province: 'British Columbia', country: 'Canada' }
```

### Running Tests

```bash
# View all users and their geocoded data
npm run db

# Update a specific user's location (will auto-geocode)
npm run update-user

# Re-geocode all users
npm run geocode-users
```

## Next Steps

1. ✅ Ensure `GEOCODING_API` is set in `.env`
2. ✅ Run `npm run geocode-users` for existing users
3. ✅ Update frontend to display city, province, country
4. ✅ Monitor API usage in Google Cloud Console
