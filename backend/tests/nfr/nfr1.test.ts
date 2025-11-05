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

dotenv.config();

// Test user will be created dynamically
let testUser: any = null;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware - BYPASSES real authenticateToken middleware
// Sets req.user directly so we don't need JWT tokens for testing
// This runs BEFORE routes, so req.user is available to controllers
app.use((req: any, res, next) => {
  // Use the test user from database (set in beforeAll)
  if (testUser) {
    req.user = testUser;
  }
  next();
});

app.use('/api/buddy', buddyRoutes);
app.use('*', notFoundHandler);
app.use(errorHandler);

beforeAll(async () => {
  await setupTestDB();
  
  // Create a test user with complete profile
  const newUser: CreateUserRequest = {
    email: 'test@example.com',
    name: 'Test User',
    googleId: `test-google-${Date.now()}`,
    age: 25,
    profilePicture: 'http://example.com/pic.jpg',
    bio: 'Test bio',
    location: 'Vancouver, BC',
    latitude: 49.2827,
    longitude: -123.1207,
    skillLevel: 'Intermediate'
  };
  testUser = await userModel.create(newUser);
});

afterAll(async () => {
  // Clean up test user
  if (testUser) {
    await userModel.delete(new mongoose.Types.ObjectId(testUser._id));
  }
  await teardownTestDB();
});

describe('NFR1 - Buddy matching performance with 10,000 users', () => {
  /*
    NFR1: The buddy matching algorithm must return results within 1 second 
    even with 10,000 users in the database
    
    Inputs: Database populated with 10,000 random users
    Expected behavior: GET /api/buddy completes in < 1000ms
    Expected status: 200
    Output: { message: string, data: { buddies: [{ user: IUser, distance: number }] } }
  */
  test('GET /api/buddy completes within 1 second with 10,000 users', async () => {
    if (!testUser || !testUser.age || !testUser.skillLevel || !testUser.latitude || !testUser.longitude) {
      throw new Error('Test user not set up properly');
    }

    console.log('Starting to populate database with 10,000 users...');
    const startPopulation = performance.now();

    // Generate 10,000 random users
    const skillLevels = ['Beginner', 'Intermediate', 'Expert'];
    const users = [];
    
    for (let i = 0; i < 10000; i++) {
      users.push({
        email: `testuser${i}@test.com`,
        name: `Test User ${i}`,
        googleId: `test-google-${i}-${Date.now()}`,
        age: Math.floor(Math.random() * 50) + 18, // Age between 18-68
        profilePicture: `http://example.com/pic${i}.jpg`,
        bio: `Bio for user ${i}`,
        location: 'Vancouver, BC',
        latitude: 49.2827 + (Math.random() - 0.5) * 2, // Random coords near Vancouver
        longitude: -123.1207 + (Math.random() - 0.5) * 2,
        skillLevel: skillLevels[Math.floor(Math.random() * 3)]
      });
    }

    // Bulk insert users (much faster than individual creates)
    const User = mongoose.model('User');
    await User.insertMany(users);
    
    const populationTime = performance.now() - startPopulation;
    console.log(`✓ Database populated in ${populationTime.toFixed(2)}ms`);

    // Now time the buddy matching algorithm
    console.log('Testing buddy matching performance...');
    const startTime = performance.now();
    
    const res = await request(app).get('/api/buddy');
    
    const endTime = performance.now();
    const duration = endTime - startTime;

    console.log(`✓ Buddy matching completed in ${duration.toFixed(2)}ms`);
    console.log(`  - Total users in DB: ${users.length + 1}`);
    console.log(`  - Buddies returned: ${res.body.data?.buddies?.length || 0}`);

    // Assertions
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('buddies');
    expect(Array.isArray(res.body.data.buddies)).toBe(true);
    
    // NFR1: Must complete within 1 second (1000ms)
    expect(duration).toBeLessThan(1000);
    console.log('Buddy matching took ' + duration.toFixed(2) + 'ms');

    // Cleanup: Delete all test users
    console.log('Cleaning up test data...');
    const emails = users.map(u => u.email);
    await User.deleteMany({ email: { $in: emails } });
    console.log('✓ Cleanup complete');
  }, 60000); // Increase test timeout to 60 seconds for population + execution
});
