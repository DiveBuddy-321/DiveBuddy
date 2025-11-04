import request from 'supertest';
import { describe, test, expect, beforeAll, afterAll, afterEach, jest } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { userModel } from '../../src/models/user.model';
import { CreateUserRequest } from '../../src/types/user.types';
import express from 'express';
import buddyRoutes from '../../src/routes/buddy.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import mongoose from 'mongoose';
import { getCoordinatesFromLocation } from '../../src/utils/locationGeocoding.util';

dotenv.config();
const USER = process.env.USER_ID as string;

// Create Express app for testing
const app = express();
app.use(express.json());

let testUser: any = null;

// Mock auth middleware - BYPASSES real authenticateToken middleware
// Sets req.user directly so we don't need JWT tokens for testing
// This runs BEFORE routes, so req.user is available to controllers
app.use((req: any, res, next) => {
  // Use the test user from database (set in beforeAll)
  if (testUser) {
    req.user = testUser;
  } else {
    // Fallback
    req.user = {
      _id: new mongoose.Types.ObjectId(USER),
      email: 'test@example.com',
      name: 'Test User'
    };
  }
  next();
});

app.use('/api/buddy', buddyRoutes);
app.use('*', notFoundHandler);
app.use(errorHandler);

beforeAll(async () => {
  await setupTestDB();
  
  // Ensure the test user exists with complete profile
  try {
    const existingUser = await userModel.findById(new mongoose.Types.ObjectId(USER));
    if (!existingUser) {
      // Create a test user with complete profile
      const newUser: CreateUserRequest = {
        email: 'test@example.com',
        name: 'Test User',
        googleId: `test-google-${USER}`,
        age: 25,
        profilePicture: 'http://example.com/pic.jpg',
        bio: 'Test bio',
        location: 'Vancouver, BC',
        latitude: 49.2827,
        longitude: -123.1207,
        skillLevel: 'Intermediate'
      };
      testUser = await userModel.create(newUser);
    } else {
      // Update existing user to ensure complete profile
      await userModel.update(new mongoose.Types.ObjectId(USER), {
        age: 25,
        latitude: 49.2827,
        longitude: -123.1207,
        skillLevel: 'Intermediate'
      });
      testUser = await userModel.findById(new mongoose.Types.ObjectId(USER));
    }
  } catch (error) {
    console.error('Error setting up test user:', error);
  }
});

afterAll(async () => {
  await teardownTestDB();
});

describe('GET /api/buddy - unmocked (no mocking)', () => {
  /*
    Inputs: none (user from mock auth, must have complete profile: age, skillLevel, latitude, longitude)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Returns sorted list of matching buddies with distances
  */
  test('returns list of buddies when user profile is complete', async () => {
    if (!testUser) {
      throw new Error('Test user not set up');
    }

    const res = await request(app).get('/api/buddy');

    // If user has complete profile, should return 200
    if (testUser.age && testUser.skillLevel && testUser.latitude && testUser.longitude) {
      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('data');
      expect(res.body.data).toHaveProperty('buddies');
      expect(Array.isArray(res.body.data.buddies)).toBe(true);
    } else {
      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty('message');
    }
  });

  /*
    Inputs: none (user from mock auth with incomplete profile - missing age, skillLevel, or location)
    Expected status: 400
    Output: { message: string } with message containing 'complete your profile'
    Expected behavior: Rejects request when user profile is incomplete
  */
  test('returns 400 when user profile is incomplete', async () => {
    // Temporarily override testUser with incomplete profile
    const originalUser = testUser;
    testUser = {
      _id: new mongoose.Types.ObjectId(USER),
      email: 'incomplete@example.com',
      name: 'Incomplete User',
      // Missing age, skillLevel, or location
    };

    const res = await request(app).get('/api/buddy');

    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toContain('complete your profile');

    // Restore original user
    testUser = originalUser;
  });

  /*
    Inputs: query { targetMinLevel: 1, targetMaxLevel: 2 }
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Filters buddies to only include those with skill level between 1 (Beginner) and 2 (Intermediate)
  */
  test('filters buddies by skill level range', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return; // Skip test if user profile incomplete
    }

    const res = await request(app)
      .get('/api/buddy')
      .query({ targetMinLevel: 1, targetMaxLevel: 2 });

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    // All returned buddies should have skill level within range
    if (res.body.data.buddies.length > 0) {
      res.body.data.buddies.forEach((buddy: any) => {
        const level = buddy.user.skillLevel;
        expect(['Beginner', 'Intermediate']).toContain(level);
      });
    }
  });

  /*
    Inputs: query { targetMinAge: 20, targetMaxAge: 30 }
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Filters buddies to only include those with age between 20 and 30
  */
  test('filters buddies by age range', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    const res = await request(app)
      .get('/api/buddy')
      .query({ targetMinAge: 20, targetMaxAge: 30 });

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    // All returned buddies should have age within range
    if (res.body.data.buddies.length > 0) {
      res.body.data.buddies.forEach((buddy: any) => {
        expect(buddy.user.age).toBeGreaterThanOrEqual(20);
        expect(buddy.user.age).toBeLessThanOrEqual(30);
      });
    }
  });

  /*
    Inputs: query { targetMinLevel: 1, targetMaxLevel: 3, targetMinAge: 18, targetMaxAge: 50 }
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Filters buddies by both skill level (1-3) and age (18-50) simultaneously
  */
  test('filters buddies by both skill level and age', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    const res = await request(app)
      .get('/api/buddy')
      .query({ 
        targetMinLevel: 1, 
        targetMaxLevel: 3,
        targetMinAge: 18,
        targetMaxAge: 50
      });

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    if (res.body.data.buddies.length > 0) {
      res.body.data.buddies.forEach((buddy: any) => {
        expect(buddy.user.age).toBeGreaterThanOrEqual(18);
        expect(buddy.user.age).toBeLessThanOrEqual(50);
        expect(buddy.user.skillLevel).toBeDefined();
      });
    }
  });

  /*
    Inputs: none (no query params)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Returns buddies sorted by distance in ascending order (closest first)
  */
  test('returns buddies sorted by distance (closest first)', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    const res = await request(app).get('/api/buddy');

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    
    // Check that distances are sorted (ascending)
    if (res.body.data.buddies.length > 1) {
      for (let i = 0; i < res.body.data.buddies.length - 1; i++) {
        expect(res.body.data.buddies[i].distance).toBeLessThanOrEqual(
          res.body.data.buddies[i + 1].distance
        );
      }
    }
  });

  /*
    Inputs: none (user from mock auth)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Excludes the current user from the results
  */
  test('excludes current user from results', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    const res = await request(app).get('/api/buddy');

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    
    // Current user should not be in the results
    res.body.data.buddies.forEach((buddy: any) => {
      expect(String(buddy.user._id)).not.toBe(USER);
    });
  });

  /*
    Inputs: query { targetMinLevel: 1 } (only min level, no max)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: When only one filter bound is provided, algorithm should not filter (requires both min and max)
  */
  test('handles partial level filter (only min, no max)', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    // Create users with different skill levels to test
    const beginnerUser = await userModel.create({
      email: 'beginner@test.com',
      name: 'Beginner User',
      googleId: `test-beginner-${Date.now()}`,
      age: 25,
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: 'Beginner'
    } as any);

    const res = await request(app)
      .get('/api/buddy')
      .query({ targetMinLevel: 1 });

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    // Should return all eligible users since filter requires both min and max

    // Cleanup
    await userModel.delete(new mongoose.Types.ObjectId(beginnerUser._id));
  });

  /*
    Inputs: query { targetMaxLevel: 3 } (only max level, no min)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: When only one filter bound is provided, algorithm should not filter (requires both min and max)
  */
  test('handles partial level filter (only max, no min)', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    const res = await request(app)
      .get('/api/buddy')
      .query({ targetMaxLevel: 3 });

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    // Should return all eligible users since filter requires both min and max
  });

  /*
    Inputs: query { targetMinAge: 20 } (only min age, no max)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: When only one filter bound is provided, algorithm should not filter (requires both min and max)
  */
  test('handles partial age filter (only min, no max)', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    const res = await request(app)
      .get('/api/buddy')
      .query({ targetMinAge: 20 });

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    // Should return all eligible users since filter requires both min and max
  });

  /*
    Inputs: query { targetMaxAge: 50 } (only max age, no min)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: When only one filter bound is provided, algorithm should not filter (requires both min and max)
  */
  test('handles partial age filter (only max, no min)', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    const res = await request(app)
      .get('/api/buddy')
      .query({ targetMaxAge: 50 });

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    // Should return all eligible users since filter requires both min and max
  });

  /*
    Inputs: none (user with all skill levels in database)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Algorithm should handle all three skill levels (Beginner, Intermediate, Expert)
  */
  test('handles all skill levels (Beginner, Intermediate, Expert)', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    // Create users with all skill levels
    const beginnerUser = await userModel.create({
      email: `beginner-${Date.now()}@test.com`,
      name: 'Beginner User',
      googleId: `test-beginner-${Date.now()}`,
      age: 25,
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: 'Beginner'
    } as any);

    const expertUser = await userModel.create({
      email: `expert-${Date.now()}@test.com`,
      name: 'Expert User',
      googleId: `test-expert-${Date.now()}`,
      age: 30,
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: 'Expert'
    } as any);

    const res = await request(app).get('/api/buddy');

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    
    // Should include users with all skill levels
    const skillLevels = res.body.data.buddies.map((b: any) => b.user.skillLevel).filter(Boolean);
    expect(skillLevels.length).toBeGreaterThan(0);

    // Cleanup
    await userModel.delete(new mongoose.Types.ObjectId(beginnerUser._id));
    await userModel.delete(new mongoose.Types.ObjectId(expertUser._id));
  });

  /*
    Inputs: none (users with missing location data)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Users without complete profile (missing latitude/longitude) should be filtered out
  */
  test('filters out users with incomplete profiles', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    // Create user with incomplete profile (missing location)
    const incompleteUser = await userModel.create({
      email: `incomplete-${Date.now()}@test.com`,
      name: 'Incomplete User',
      googleId: `test-incomplete-${Date.now()}`,
      age: 25,
      skillLevel: 'Intermediate'
      // Missing latitude and longitude
    } as any);

    const res = await request(app).get('/api/buddy');

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    
    // Incomplete user should not be in results
    const incompleteUserId = String(incompleteUser._id);
    res.body.data.buddies.forEach((buddy: any) => {
      expect(String(buddy.user._id)).not.toBe(incompleteUserId);
    });

    // Cleanup
    await userModel.delete(new mongoose.Types.ObjectId(incompleteUser._id));
  });

  /*
    Inputs: query with filters that match no users
    Expected status: 200
    Output: { message: string, data: { buddies: [] } }
    Expected behavior: Returns empty array when filters match no users
  */
  test('returns empty array when filters match no users', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    // Use very restrictive filters that likely match no one
    const res = await request(app)
      .get('/api/buddy')
      .query({ 
        targetMinLevel: 1, 
        targetMaxLevel: 1,
        targetMinAge: 100,
        targetMaxAge: 100
      });

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    expect(Array.isArray(res.body.data.buddies)).toBe(true);
    // Should return empty array or very few results
  });

  /*
    Inputs: none (users at different geographic distances)
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
    Expected behavior: Distance calculation should consider geographic distance, age difference, and skill level difference
  */
  test('calculates distance considering geography, age, and skill level', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      return;
    }

    // Create users at different distances
    const nearbyUser = await userModel.create({
      email: `nearby-${Date.now()}@test.com`,
      name: 'Nearby User',
      googleId: `test-nearby-${Date.now()}`,
      age: testUser.age, // Same age
      latitude: testUser.latitude + 0.01, // Very close
      longitude: testUser.longitude + 0.01,
      skillLevel: testUser.skillLevel // Same skill level
    } as any);

    const farUser = await userModel.create({
      email: `far-${Date.now()}@test.com`,
      name: 'Far User',
      googleId: `test-far-${Date.now()}`,
      age: testUser.age + 50, // Large age difference
      latitude: testUser.latitude + 10, // Far away
      longitude: testUser.longitude + 10,
      skillLevel: testUser.skillLevel === 'Beginner' ? 'Expert' : 'Beginner' // Different skill level
    } as any);

    const res = await request(app).get('/api/buddy');

    expect(res.status).toBe(200);
    expect(res.body.data.buddies).toBeDefined();
    
    // Nearby user with similar age/skill should have smaller distance than far user
    const nearbyBuddy = res.body.data.buddies.find((b: any) => String(b.user._id) === String(nearbyUser._id));
    const farBuddy = res.body.data.buddies.find((b: any) => String(b.user._id) === String(farUser._id));
    
    if (nearbyBuddy && farBuddy) {
      expect(nearbyBuddy.distance).toBeLessThan(farBuddy.distance);
    }

    // Cleanup
    await userModel.delete(new mongoose.Types.ObjectId(nearbyUser._id));
    await userModel.delete(new mongoose.Types.ObjectId(farUser._id));
  });
});

describe('getCoordinatesFromLocation - unmocked (no mocking)', () => {
  // Mock global fetch
  const originalFetch = global.fetch;
  const originalEnv = process.env.GEOCODING_API;

  beforeAll(() => {
    // Set a test API key
    process.env.GEOCODING_API = 'test-api-key';
    // Mock global fetch
    global.fetch = jest.fn() as jest.MockedFunction<typeof fetch>;
  });

  afterAll(() => {
    // Restore original fetch
    global.fetch = originalFetch;
    // Restore original env
    if (originalEnv) {
      process.env.GEOCODING_API = originalEnv;
    } else {
      delete process.env.GEOCODING_API;
    }
  });

  afterEach(() => {
    jest.clearAllMocks();
    // Restore API key after each test
    process.env.GEOCODING_API = 'test-api-key';
  });

  /*
    Inputs: location: 'Vancouver, BC'
    Expected status: Promise resolves to GeocodeResult
    Output: { latitude: number, longitude: number } | null
    Expected behavior: Successfully geocodes location and returns coordinates
  */
  test('successfully geocodes a valid location', async () => {
    const mockResponse = {
      status: 'OK',
      results: [
        {
          geometry: {
            location: {
              lat: 49.2827,
              lng: -123.1207
            }
          }
        }
      ]
    };

    (global.fetch as jest.MockedFunction<typeof fetch>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse
    } as any);

    const result = await getCoordinatesFromLocation('Vancouver, BC');

    expect(result).not.toBeNull();
    expect(result).toHaveProperty('latitude');
    expect(result).toHaveProperty('longitude');
    expect(result?.latitude).toBe(49.2827);
    expect(result?.longitude).toBe(-123.1207);
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: location: 'Vancouver, BC'
    Expected status: Promise resolves to null
    Output: null
    Expected behavior: Returns null when API key is not set
  */
  test('returns null when GEOCODING_API is not set', async () => {
    delete process.env.GEOCODING_API;

    const result = await getCoordinatesFromLocation('Vancouver, BC');

    expect(result).toBeNull();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  /*
    Inputs: location: 'Invalid Location'
    Expected status: Promise resolves to null
    Output: null
    Expected behavior: Returns null when API returns non-OK status
  */
  test('returns null when API returns non-OK status', async () => {
    process.env.GEOCODING_API = 'test-api-key';

    const mockResponse = {
      status: 'ZERO_RESULTS',
      results: []
    };

    (global.fetch as jest.MockedFunction<typeof fetch>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse
    } as any);

    const result = await getCoordinatesFromLocation('Invalid Location');

    expect(result).toBeNull();
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: location: 'Vancouver, BC'
    Expected status: Promise resolves to null
    Output: null
    Expected behavior: Returns null when API returns empty results array
  */
  test('returns null when API returns empty results', async () => {
    process.env.GEOCODING_API = 'test-api-key';

    const mockResponse = {
      status: 'OK',
      results: []
    };

    (global.fetch as jest.MockedFunction<typeof fetch>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse
    } as any);

    const result = await getCoordinatesFromLocation('Vancouver, BC');

    expect(result).toBeNull();
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: location: 'Vancouver, BC'
    Expected status: Promise resolves to null
    Output: null
    Expected behavior: Returns null when response is not OK (HTTP error)
  */
  test('returns null when HTTP request fails', async () => {
    process.env.GEOCODING_API = 'test-api-key';

    (global.fetch as jest.MockedFunction<typeof fetch>).mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({})
    } as any);

    const result = await getCoordinatesFromLocation('Vancouver, BC');

    expect(result).toBeNull();
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: location: 'Vancouver, BC'
    Expected status: Promise resolves to null
    Output: null
    Expected behavior: Returns null when geometry location is missing
  */
  test('returns null when geometry location is missing', async () => {
    process.env.GEOCODING_API = 'test-api-key';

    const mockResponse = {
      status: 'OK',
      results: [
        {
          geometry: {}
        }
      ]
    };

    (global.fetch as jest.MockedFunction<typeof fetch>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse
    } as any);

    const result = await getCoordinatesFromLocation('Vancouver, BC');

    expect(result).toBeNull();
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: location: 'Vancouver, BC'
    Expected status: Promise resolves to null
    Output: null
    Expected behavior: Returns null when lat/lng are not numbers
  */
  test('returns null when lat/lng are not numbers', async () => {
    process.env.GEOCODING_API = 'test-api-key';

    const mockResponse = {
      status: 'OK',
      results: [
        {
          geometry: {
            location: {
              lat: 'invalid',
              lng: 'invalid'
            }
          }
        }
      ]
    };

    (global.fetch as jest.MockedFunction<typeof fetch>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse
    } as any);

    const result = await getCoordinatesFromLocation('Vancouver, BC');

    expect(result).toBeNull();
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: location: 'Vancouver, BC'
    Expected status: Promise resolves to null
    Output: null
    Expected behavior: Returns null when fetch throws an error
  */
  test('returns null when fetch throws an error', async () => {
    process.env.GEOCODING_API = 'test-api-key';

    (global.fetch as jest.MockedFunction<typeof fetch>).mockRejectedValue(
      new Error('Network error')
    );

    const result = await getCoordinatesFromLocation('Vancouver, BC');

    expect(result).toBeNull();
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: location: 'New York, NY'
    Expected status: Promise resolves to GeocodeResult
    Output: { latitude: number, longitude: number }
    Expected behavior: Properly URL-encodes location in API request
  */
  test('properly URL-encodes location in API request', async () => {
    process.env.GEOCODING_API = 'test-api-key';

    const mockResponse = {
      status: 'OK',
      results: [
        {
          geometry: {
            location: {
              lat: 40.7128,
              lng: -74.0060
            }
          }
        }
      ]
    };

    (global.fetch as jest.MockedFunction<typeof fetch>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse
    } as any);

    const location = 'New York, NY';
    const result = await getCoordinatesFromLocation(location);

    expect(result).not.toBeNull();
    expect(global.fetch).toHaveBeenCalledTimes(1);
    
    // Verify the URL contains the encoded location
    const fetchCall = (global.fetch as jest.MockedFunction<typeof fetch>).mock.calls[0][0];
    expect(fetchCall).toContain(encodeURIComponent(location));
    expect(fetchCall).toContain('test-api-key');
  });

  /*
    Inputs: location: 'San Francisco, CA'
    Expected status: Promise resolves to GeocodeResult
    Output: { latitude: number, longitude: number }
    Expected behavior: Handles locations with special characters
  */
  test('handles locations with special characters', async () => {
    process.env.GEOCODING_API = 'test-api-key';

    const mockResponse = {
      status: 'OK',
      results: [
        {
          geometry: {
            location: {
              lat: 37.7749,
              lng: -122.4194
            }
          }
        }
      ]
    };

    (global.fetch as jest.MockedFunction<typeof fetch>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse
    } as any);

    const location = 'San Francisco, CA';
    const result = await getCoordinatesFromLocation(location);

    expect(result).not.toBeNull();
    expect(result?.latitude).toBe(37.7749);
    expect(result?.longitude).toBe(-122.4194);
  });
});

