import dotenv from 'dotenv';
import mongoose from 'mongoose';
import { userModel } from '../models/user.model';
import { getLocationFromCoordinates } from '../utils/geoCoding.util';
import logger from '../utils/logger.util';

dotenv.config();

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/cpen321';

// Delay between API calls to respect rate limits (Google Maps allows ~50 requests/second)
const DELAY_MS = 100; // 100ms = max 10 requests/second to be safe

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function geocodeExistingUsers() {
  try {
    console.log('🔌 Connecting to MongoDB...');
    await mongoose.connect(MONGODB_URI);
    console.log('Connected to MongoDB\n');

    // Find all users with lat/long but missing city/province/country
    const users = await userModel.findAll();
    const usersToGeocode = users.filter(
      user => 
        user.lat !== undefined && 
        user.long !== undefined && 
        !user.city // If no city, assume they haven't been geocoded
    );

    console.log(`Found ${usersToGeocode.length} users to geocode (out of ${users.length} total users)\n`);

    if (usersToGeocode.length === 0) {
      console.log('All users are already geocoded!');
      process.exit(0);
    }

    let successCount = 0;
    let failCount = 0;

    // Process users one by one with rate limiting
    for (let i = 0; i < usersToGeocode.length; i++) {
      const user = usersToGeocode[i];
      
      try {
        console.log(`[${i + 1}/${usersToGeocode.length}] Geocoding user ${user.email}...`);
        
        const locationInfo = await getLocationFromCoordinates(
          user.lat!,
          user.long!
        );

        // Update user with geocoded data
        await userModel.update(user._id, {
          city: locationInfo.city,
          province: locationInfo.province,
          country: locationInfo.country,
        });
        successCount++;

        // Respect rate limits
        if (i < usersToGeocode.length - 1) {
          await delay(DELAY_MS);
        }
      } catch (error) {
        console.error(`Failed to geocode user ${user.email}:`, error);
        failCount++;
        
        // Continue with next user even if one fails
        await delay(DELAY_MS * 2); // Extra delay after error
      }
    }
    
    console.log(`\n Geocoding complete! ${successCount} users geocoded, ${failCount} users failed`);
    process.exit(0);
  } catch (error) {
    console.error('Error during geocoding:', error);
    process.exit(1);
  } finally {
    await mongoose.connection.close();
    console.log('Disconnected from MongoDB');
  }
}

// Run the geocoding script
geocodeExistingUsers();

