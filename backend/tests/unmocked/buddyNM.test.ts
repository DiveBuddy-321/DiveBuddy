import request from 'supertest';
import { describe, test, expect, beforeAll, afterAll } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { userModel } from '../../src/models/user.model';
import { CreateUserRequest } from '../../src/types/user.types';
import express from 'express';
import buddyRoutes from '../../src/routes/buddy.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import mongoose from 'mongoose';

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
});

