import mongoose from 'mongoose';
import { config } from 'dotenv';
import { userModel } from '../models/user.model';
import { HOBBIES } from '../constants/hobbies';

config();

interface CityLocation {
  name: string;
  lat: number;
  long: number;
  count: number;
}

const cities: CityLocation[] = [
  { name: 'Vancouver', lat: 49.2827, long: -123.1207, count: 50 },
  { name: 'Toronto', lat: 43.6532, long: -79.3832, count: 25 },
  { name: 'Calgary', lat: 51.0447, long: -114.0719, count: 25 },
  { name: 'Edmonton', lat: 53.5461, long: -113.4938, count: 15 },
  { name: 'Seattle', lat: 47.6062, long: -122.3321, count: 20 },
  { name: 'San Francisco', lat: 37.7749, long: -122.4194, count: 30 },
  { name: 'Los Angeles', lat: 34.0522, long: -118.2437, count: 10 },
  { name: 'Chicago', lat: 41.8781, long: -87.6298, count: 10 },
  { name: 'Atlanta', lat: 33.7490, long: -84.3880, count: 15 },
  { name: 'Dallas', lat: 32.7767, long: -96.7970, count: 20 },
  { name: 'Houston', lat: 29.7604, long: -95.3698, count: 10 },
  { name: 'Miami', lat: 25.7617, long: -80.1918, count: 5 },
  { name: 'Tampa', lat: 27.9506, long: -82.4572, count: 25 },
  { name: 'New York', lat: 40.7128, long: -74.0060, count: 40 },
  { name: 'Boston', lat: 42.3601, long: -71.0589, count: 20 },
  { name: 'Washington DC', lat: 38.9072, long: -77.0369, count: 20 },
  { name: 'Montreal', lat: 45.5017, long: -73.5673, count: 30 },
  { name: 'Ottawa', lat: 45.4215, long: -75.6972, count: 10 },
  { name: 'Halifax', lat: 44.6488, long: -63.5752, count: 12 },
  { name: 'St. Johns', lat: 47.5615, long: -52.7126, count: 10 },
];

const firstNames = [
  'James', 'Mary', 'John', 'Patricia', 'Robert', 'Jennifer', 'Michael', 'Linda',
  'William', 'Elizabeth', 'David', 'Barbara', 'Richard', 'Susan', 'Joseph', 'Jessica',
  'Thomas', 'Sarah', 'Charles', 'Karen', 'Christopher', 'Nancy', 'Daniel', 'Lisa',
  'Matthew', 'Betty', 'Anthony', 'Margaret', 'Mark', 'Sandra', 'Donald', 'Ashley',
  'Steven', 'Kimberly', 'Paul', 'Emily', 'Andrew', 'Donna', 'Joshua', 'Michelle',
  'Kenneth', 'Carol', 'Kevin', 'Amanda', 'Brian', 'Dorothy', 'George', 'Melissa',
  'Emma', 'Olivia', 'Ava', 'Sophia', 'Isabella', 'Mia', 'Charlotte', 'Amelia',
  'Harper', 'Evelyn', 'Liam', 'Noah', 'Oliver', 'Elijah', 'Lucas', 'Mason',
  'Logan', 'Alexander', 'Ethan', 'Jacob', 'Benjamin', 'Jack', 'Henry', 'Owen', 'Tanish', 'Alex', 'Clara', 'Cindy', 'Albert', 'Chau'
];

const lastNames = [
  'Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis',
  'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson', 'Thomas',
  'Taylor', 'Moore', 'Jackson', 'Martin', 'Lee', 'Perez', 'Thompson', 'White',
  'Harris', 'Sanchez', 'Clark', 'Ramirez', 'Lewis', 'Robinson', 'Walker', 'Young',
  'Allen', 'King', 'Wright', 'Scott', 'Torres', 'Nguyen', 'Hill', 'Flores',
  'Green', 'Adams', 'Nelson', 'Baker', 'Hall', 'Rivera', 'Campbell', 'Mitchell',
  'Carter', 'Roberts', 'Gomez', 'Phillips', 'Evans', 'Turner', 'Diaz', 'Parker', 'Manias', 'Shah', 'Patel', 'Singh', 'Cheng',
  'Bae', 'Li', 'Lee', 'Park', 'Hwa', 'Hussain', 'Pham'
];

const bios = [
  'Love the outdoors and meeting new people!',
  'Always looking for adventure and new experiences.',
  'Passionate about fitness and healthy living.',
  'Coffee enthusiast and bookworm.',
  'Tech geek who loves gaming and coding.',
  'Music lover and concert goer.',
  'Foodie exploring local restaurants.',
  'Nature photographer and hiker.',
  'Sports fan and weekend warrior.',
  'Art enthusiast and creative soul.',
  'Traveler seeking new destinations.',
  'Yoga practitioner and mindfulness advocate.',
  'Movie buff and series binger.',
  'Board game enthusiast.',
  'Cyclist and marathon runner.',
  'I\'m gay da ba dee da ba daa',
  'I\'m a software engineer and I love to code',
  'I like trains',
  'I am a weeb',
  'I like biking to my crush\'s house and surprising them',
];

function getRandomElement<T>(array: T[]): T {
  return array[Math.floor(Math.random() * array.length)];
}

function getRandomInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function getRandomFloat(min: number, max: number): number {
  return Math.random() * (max - min) + min;
}

function addLocationNoise(coord: number, isLatitude: boolean): number {
  // Add approximately +/- 0.1 degrees (roughly 11km variation)
  const noise = getRandomFloat(-0.1, 0.1);
  const result = coord + noise;
  
  // Ensure we stay within valid bounds
  if (isLatitude) {
    return Math.max(-90, Math.min(90, result));
  } else {
    return Math.max(-180, Math.min(180, result));
  }
}

function generateRandomHobbies(): string[] {
  const numHobbies = getRandomInt(2, 6);
  const selectedHobbies = new Set<string>();
  
  while (selectedHobbies.size < numHobbies) {
    selectedHobbies.add(getRandomElement(HOBBIES));
  }
  
  return Array.from(selectedHobbies);
}

function generateUser(city: string, lat: number, long: number, index: number) {
  const firstName = getRandomElement(firstNames);
  const lastName = getRandomElement(lastNames);
  const name = `${firstName} ${lastName}`;
  const email = `${firstName.toLowerCase()}.${lastName.toLowerCase()}.${index}@example.com`;
  const googleId = `google_${city.replace(/\s+/g, '_').toLowerCase()}_${index}_${Date.now()}`;
  
  return {
    googleId,
    email,
    name,
    age: getRandomInt(18, 50),
    level: getRandomInt(1, 3),
    lat: addLocationNoise(lat, true),
    long: addLocationNoise(long, false),
    bio: Math.random() > 0.3 ? getRandomElement(bios) : undefined,
    hobbies: generateRandomHobbies(),
    profilePicture: undefined,
  };
}

function generateRandomNorthAmericanLocation(): { lat: number; long: number } {
  // North America roughly spans:
  // Latitude: 15°N to 70°N
  // Longitude: -170°W to -50°W
  return {
    lat: getRandomFloat(25, 60), // Focus on mainland US and Canada
    long: getRandomFloat(-130, -65),
  };
}

async function seedDatabase() {
  try {
    console.log('🔌 Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI!);
    console.log('✅ Connected to MongoDB');

    console.log('🗑️  Clearing existing users...');
    await mongoose.connection.collection('users').deleteMany({});
    console.log('✅ Cleared existing users');

    const usersToInsert = [];
    let userIndex = 0;

    // Generate users for specific cities
    console.log('👥 Generating users for specific cities...');
    for (const city of cities) {
      console.log(`   - Generating ${city.count} users for ${city.name}`);
      for (let i = 0; i < city.count; i++) {
        const user = generateUser(city.name, city.lat, city.long, userIndex++);
        usersToInsert.push(user);
      }
    }

    // Generate random users across North America
    console.log('🌎 Generating 100 users across North America...');
    for (let i = 0; i < 100; i++) {
      const location = generateRandomNorthAmericanLocation();
      const user = generateUser('Random_NA', location.lat, location.long, userIndex++);
      usersToInsert.push(user);
    }

    console.log(`💾 Inserting ${usersToInsert.length} users into database...`);
    await mongoose.connection.collection('users').insertMany(usersToInsert);
    
    console.log('✅ Successfully seeded database!');
    console.log(`📊 Total users created: ${usersToInsert.length}`);
    
    // Print some statistics
    const totalUsers = await mongoose.connection.collection('users').countDocuments();
    console.log(`📈 Users in database: ${totalUsers}`);
    
  } catch (error) {
    console.error('❌ Error seeding database:', error);
    process.exit(1);
  } finally {
    await mongoose.connection.close();
    console.log('👋 Disconnected from MongoDB');
  }
}

// Run the seed script
seedDatabase();

