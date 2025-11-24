import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll, jest } from '@jest/globals';
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

// Mock auth middleware to set req.user with complete profile
app.use('/api/buddy', (req: any, res: any, next: any) => {
  if (testUser) {
    req.user = testUser;
  }
  next();
}, buddyRoutes);

// Error handling middleware
app.use('*', notFoundHandler);
app.use((err: any, req: any, res: any, next: any) => {
  errorHandler(err, req, res);
});

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

afterEach(() => {
  jest.restoreAllMocks();
});

describe('GET /api/buddy - mocked', () => {
  /*
    Inputs: GET request with mocked database connection failure
    Expected status: 500
    Output: { message: string }
    Expected behavior: Returns error when database connection fails
  */
  test('returns 500 when userModel.findByQuery fails', async () => {
    // Mock userModel.findByQuery to throw an error
    jest.spyOn(userModel, 'findByQuery').mockRejectedValue(new Error('Database connection failed'));

    // Make request
    const res = await request(app).get('/api/buddy');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(userModel.findByQuery).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: GET request with mocked unexpected error
    Expected status: 500
    Output: { message: string }
    Expected behavior: Returns error when unexpected exception occurs
  */
  test('returns 500 when unexpected error occurs', async () => {
    // Mock userModel.findByQuery to throw an unexpected error
    jest.spyOn(userModel, 'findByQuery').mockRejectedValue(new Error('Unexpected error'));

    // Make request
    const res = await request(app).get('/api/buddy');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(userModel.findByQuery).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: GET request with mocked database timeout
    Expected status: 500
    Output: { message: string }
    Expected behavior: Returns error when database query times out
  */
  test('returns 500 when database timeout occurs', async () => {
    // Mock userModel.findByQuery to throw a timeout error
    jest.spyOn(userModel, 'findByQuery').mockRejectedValue(new Error('Connection timeout'));

    // Make request
    const res = await request(app).get('/api/buddy');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(userModel.findByQuery).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: GET request with mocked null pointer exception
    Expected status: 500
    Output: { message: string }
    Expected behavior: Returns error when null reference is accessed
  */
  test('returns 500 when null pointer exception occurs', async () => {
    // Mock userModel.findByQuery to throw null error
    jest.spyOn(userModel, 'findByQuery').mockRejectedValue(new TypeError('Cannot read property of null'));

    // Make request
    const res = await request(app).get('/api/buddy');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(userModel.findByQuery).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: GET request with mocked network error
    Expected status: 500
    Output: { message: string }
    Expected behavior: Returns error when network connectivity fails
  */
  test('returns 500 when network error occurs', async () => {
    // Mock userModel.findByQuery to throw a network error
    jest.spyOn(userModel, 'findByQuery').mockRejectedValue(new Error('Network error'));

    // Make request
    const res = await request(app).get('/api/buddy');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(userModel.findByQuery).toHaveBeenCalledTimes(1);
  });

  /*
    Inputs: GET request with mocked memory error
    Expected status: 500
    Output: { message: string }
    Expected behavior: Returns error when system runs out of memory
  */
  test('returns 500 when memory error occurs', async () => {
    // Mock userModel.findByQuery to throw a memory error
    jest.spyOn(userModel, 'findByQuery').mockRejectedValue(new Error('Out of memory'));

    // Make request
    const res = await request(app).get('/api/buddy');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(userModel.findByQuery).toHaveBeenCalledTimes(1);
  });
});

describe('GET /api/buddy - profile validation mocked', () => {
  /*
    Inputs: GET request with user missing age field
    Expected status: 400
    Output: { message: string } (contains 'complete your profile')
    Expected behavior: Rejects buddy matching when user profile lacks age
  */
  test('returns 400 when user profile is incomplete (missing age)', async () => {
    // Override middleware for this test with incomplete profile
    const incompleteApp = express();
    incompleteApp.use(express.json());
    incompleteApp.use('/api/buddy', (req: any, res: any, next: any) => {
      req.user = {
        _id: testUser ? testUser._id : new mongoose.Types.ObjectId(),
        email: 'test@example.com',
        name: 'Test User',
        // Missing age
        skillLevel: 'Intermediate',
        latitude: 49.2827,
        longitude: -123.1207
      };
      next();
    }, buddyRoutes);
    incompleteApp.use('*', notFoundHandler);
    incompleteApp.use((err: any, req: any, res: any, next: any) => {
      errorHandler(err, req, res);
    });

    const res = await request(incompleteApp).get('/api/buddy');

    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toContain('complete your profile');
  });

  /*
    Inputs: GET request with user missing skillLevel field
    Expected status: 400
    Output: { message: string } (contains 'complete your profile')
    Expected behavior: Rejects buddy matching when user profile lacks skill level
  */
  test('returns 400 when user profile is incomplete (missing skillLevel)', async () => {
    const incompleteApp = express();
    incompleteApp.use(express.json());
    incompleteApp.use('/api/buddy', (req: any, res: any, next: any) => {
      req.user = {
        _id: testUser ? testUser._id : new mongoose.Types.ObjectId(),
        email: 'test@example.com',
        name: 'Test User',
        age: 25,
        // Missing skillLevel
        latitude: 49.2827,
        longitude: -123.1207
      };
      next();
    }, buddyRoutes);
    incompleteApp.use('*', notFoundHandler);
    incompleteApp.use((err: any, req: any, res: any, next: any) => {
      errorHandler(err, req, res);
    });

    const res = await request(incompleteApp).get('/api/buddy');

    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toContain('complete your profile');
  });

  /*
    Inputs: GET request with user missing latitude/longitude coordinates
    Expected status: 400
    Output: { message: string } (contains 'complete your profile')
    Expected behavior: Rejects buddy matching when user profile lacks location data
  */
  test('returns 400 when user profile is incomplete (missing location)', async () => {
    const incompleteApp = express();
    incompleteApp.use(express.json());
    incompleteApp.use('/api/buddy', (req: any, res: any, next: any) => {
      req.user = {
        _id: testUser ? testUser._id : new mongoose.Types.ObjectId(),
        email: 'test@example.com',
        name: 'Test User',
        age: 25,
        skillLevel: 'Intermediate',
        // Missing latitude/longitude
      };
      next();
    }, buddyRoutes);
    incompleteApp.use('*', notFoundHandler);
    incompleteApp.use((err: any, req: any, res: any, next: any) => {
      errorHandler(err, req, res);
    });

    const res = await request(incompleteApp).get('/api/buddy');

    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toContain('complete your profile');
  });
});

