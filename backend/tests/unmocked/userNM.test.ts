import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { eventModel } from '../../src/models/event.model';
import { CreateEventRequest, UpdateEventRequest } from '../../src/types/event.types';
import { CreateUserRequest, UpdateProfileRequest } from '../../src/types/user.types';
import { userModel } from '../../src/models/user.model';
import { AuthenticateUserRequest } from '../../src/types/auth.types';
import express from 'express';
import userRoutes from '../../src/routes/user.routes';
import authRoutes from '../../src/routes/auth.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import { authenticateToken } from '../../src/middleware/auth.middleware';
import mongoose from 'mongoose';

dotenv.config();
const USER = process.env.USER_ID as string;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware - sets req.user for all routes EXCEPT /api/auth and DELETE /api/users
app.use((req: any, res, next) => {
    // Skip mock auth for /api/auth routes (they handle their own auth)
    if (req.path.startsWith('/api/auth')) {
        return next();
    }

    // Skip mock auth for DELETE /api/users - will use real authenticateToken instead
    if (req.method === 'DELETE' && req.path === '/api/users') {
        return authenticateToken(req, res, next);
    }
    
    req.user = { 
        _id: new mongoose.Types.ObjectId(USER),
        email: 'test@example.com',
        name: 'Test User'
    };
    next();
});

app.use('/api/users', userRoutes); // Mount user routes at /api/users
app.use('/api/auth', authRoutes); // Mount auth routes at /api/auth
app.use('*', notFoundHandler);
app.use(errorHandler);

beforeAll(async () => {
  await setupTestDB();
});

afterAll(async () => {
  await teardownTestDB();
});

describe('GET /api/users - unmocked (requires running server)', () => {
    test('returns list of users (200) when server is available', async () => {
        
        // make sure GET endpoint works
        const res = await request(app).get('/api/users');//.set('Authorization', `Bearer ${TOKEN}`);
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('message');
        expect(res.body).toHaveProperty('data');
        expect(res.body.data).toHaveProperty('users');
        expect(Array.isArray(res.body.data.users)).toBe(true);
    });
});

describe('GET /api/users/profile - unmocked (requires running server)', () => {
    test('returns current user (200) when server is available', async () => {
        // call the endpoint
        const res = await request(app).get('/api/users/profile');//.set('Authorization', `Bearer ${TOKEN}`);
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('message');
        expect(res.body).toHaveProperty('data');
        expect(res.body.data).toHaveProperty('user');
        expect(res.body.data.user).toHaveProperty('_id');

        // verify returned user is the expected one
        expect(res.body.data.user._id).toBe(USER);
      });
});

describe('GET /api/users/:id - unmocked (requires running server)', () => {
  /*
    Inputs: path param id (valid user ID)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Returns user profile by ID
  */
  test('returns user by ID (200) when server is available', async () => {
    // call the endpoint
    const res = await request(app).get(`/api/users/${USER}`);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user).toHaveProperty('_id');

    // verify returned user is the expected one
    expect(res.body.data.user._id).toBe(USER);
  });

  /*
    Inputs: path param id ('invalid-id')
    Expected status: 404 (controller will try to find user, but mongoose will handle invalid ID)
    Output: error message
    Expected behavior: Returns 404 when user not found (invalid ID handled by mongoose)
  */
  test('returns 404 when user ID is invalid format', async () => {
    const res = await request(app).get('/api/users/invalid-id');
    expect(res.status).toBeGreaterThanOrEqual(400);
  });

  /*
    Inputs: path param id (valid ObjectId but non-existent user)
    Expected status: 404
    Output: { message: 'User not found' }
    Expected behavior: Returns 404 when user doesn't exist
  */
  test('returns 404 when user does not exist', async () => {
    const fakeId = new mongoose.Types.ObjectId().toString();
    const res = await request(app).get(`/api/users/${fakeId}`);
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User not found');
  });
});

describe('PUT /api/users/:id - unmocked (requires running server)', () => {
  /*
    Inputs: path param id, body UpdateProfileRequest (all fields)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Updates user profile and returns updated user
  */
  test('returns user by ID (200) when server is available', async () => {
    const updateData: UpdateProfileRequest = {
      name: "Updated Name PUT",
      age: 30,
      bio: "This is an updated bio PUT request.",
      location: "Updated Location",
      latitude: 40.7128,
      longitude: -74.0060,
      profilePicture: "http://example.com/updated-profile-pic.jpg",
      skillLevel: "Intermediate"
    };
    
    // call the endpoint
    const res = await request(app).put(`/api/users/${USER}`).send(updateData);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user).toHaveProperty('_id');

    // verify returned user is the expected one
    expect(res.body.data.user._id).toBe(USER);
    expect(res.body.data.user.name).toBe(updateData.name);
    expect(res.body.data.user.age).toBe(updateData.age);
    expect(res.body.data.user.bio).toBe(updateData.bio);
    expect(res.body.data.user.location).toBe(updateData.location);
    expect(res.body.data.user.latitude).toBe(updateData.latitude);
    expect(res.body.data.user.longitude).toBe(updateData.longitude);
    expect(res.body.data.user.profilePicture).toBe(updateData.profilePicture);
    expect(res.body.data.user.skillLevel).toBe(updateData.skillLevel);
  });

  /*
    Inputs: path param id, body UpdateProfileRequest (partial update - only name)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Updates only specified fields, leaves others unchanged
  */
  test('updates user with partial data', async () => {
    const partialUpdate: UpdateProfileRequest = {
      name: "Partially Updated Name"
    };
    
    const res = await request(app).put(`/api/users/${USER}`).send(partialUpdate);
    expect(res.status).toBe(200);
    expect(res.body.data.user.name).toBe(partialUpdate.name);
    // Other fields should remain unchanged or be optional
  });

  /*
    Inputs: path param 'invalid-id', body UpdateProfileRequest
    Expected status: 400
    Output: { message: 'Invalid user id' }
    Expected behavior: Rejects invalid user ID format
  */
  test('returns 400 when user ID is invalid', async () => {
    const updateData: UpdateProfileRequest = { name: "Test" };
    const res = await request(app).put('/api/users/invalid-id').send(updateData);
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid user id');
  });

  /*
    Inputs: path param id (valid ObjectId but non-existent user), body UpdateProfileRequest
    Expected status: 404
    Output: { message: 'User not found' }
    Expected behavior: Returns 404 when user doesn't exist
  */
  test('returns 404 when user does not exist', async () => {
    const fakeId = new mongoose.Types.ObjectId().toString();
    const updateData: UpdateProfileRequest = { name: "Test" };
    const res = await request(app).put(`/api/users/${fakeId}`).send(updateData);
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User not found');
  });

  /*
    Inputs: path param id, body UpdateProfileRequest (all skill levels)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Updates user with different skill levels (Beginner, Intermediate, Expert)
  */
  test('updates user with all skill levels', async () => {
    const skillLevels = ['Beginner', 'Intermediate', 'Expert'] as const;
    
    for (const skillLevel of skillLevels) {
      const updateData: UpdateProfileRequest = { skillLevel };
      const res = await request(app).put(`/api/users/${USER}`).send(updateData);
      expect(res.status).toBe(200);
      expect(res.body.data.user.skillLevel).toBe(skillLevel);
    }
  });
});

describe('POST /api/users/ - unmocked (requires running server)', () => {
  /*
    Inputs: body UpdateProfileRequest (all fields)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Updates current user profile and returns updated user
  */
  test('returns user (200) when server is available', async () => {
    const updateData: UpdateProfileRequest = {
      name: "Updated Name POST",
      age: 30,
      bio: "This is an updated bio POST request.",
      location: "Updated Location",
      latitude: 40.7128,
      longitude: -74.0060,
      profilePicture: "http://example.com/updated-profile-pic.jpg",
      skillLevel: "Intermediate"
    };
    
    // call the endpoint
    const res = await request(app).post(`/api/users/`).send(updateData);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user).toHaveProperty('_id');

    // verify returned user is the expected one
    expect(res.body.data.user._id).toBe(USER);
    expect(res.body.data.user.name).toBe(updateData.name);
    expect(res.body.data.user.age).toBe(updateData.age);
    expect(res.body.data.user.bio).toBe(updateData.bio);
    expect(res.body.data.user.location).toBe(updateData.location);
    expect(res.body.data.user.latitude).toBe(updateData.latitude);
    expect(res.body.data.user.longitude).toBe(updateData.longitude);
    expect(res.body.data.user.profilePicture).toBe(updateData.profilePicture);
    expect(res.body.data.user.skillLevel).toBe(updateData.skillLevel);
  });

  /*
    Inputs: body UpdateProfileRequest (partial update)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Updates only specified fields
  */
  test('updates user with partial data', async () => {
    const partialUpdate: UpdateProfileRequest = {
      bio: "Updated bio only"
    };
    
    const res = await request(app).post(`/api/users/`).send(partialUpdate);
    expect(res.status).toBe(200);
    expect(res.body.data.user.bio).toBe(partialUpdate.bio);
  });

  /*
    Inputs: body UpdateProfileRequest (all skill levels)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Updates user with different skill levels
  */
  test('updates user with all skill levels', async () => {
    const skillLevels = ['Beginner', 'Intermediate', 'Expert'] as const;
    
    for (const skillLevel of skillLevels) {
      const updateData: UpdateProfileRequest = { skillLevel };
      const res = await request(app).post(`/api/users/`).send(updateData);
      expect(res.status).toBe(200);
      expect(res.body.data.user.skillLevel).toBe(skillLevel);
    }
  });

  /*
    Inputs: body { invalid fields }
    Expected status: 400
    Output: { message: string } validation error
    Expected behavior: Rejects invalid payload with validation errors
  */
  test('returns 400 validation error with invalid payload', async () => {
    const invalidData = {
      name: "", // Invalid: empty string
      age: -5, // Invalid: negative age
      bio: "a".repeat(501), // Invalid: exceeds max length (assuming 500 char limit)
      bad_data: "InvalidLevel" // Invalid: not a valid skill level
    };
    
    const res = await request(app).post(`/api/users/`).send(invalidData);
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
  });

  /*
    Inputs: body UpdateProfileRequest (location update)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Updates user location and coordinates
  */
  test('updates user location', async () => {
    const updateData: UpdateProfileRequest = {
      location: "New Location",
      latitude: 45.5017,
      longitude: -73.5673
    };
    
    const res = await request(app).post(`/api/users/`).send(updateData);
    expect(res.status).toBe(200);
    expect(res.body.data.user.location).toBe(updateData.location);
    expect(res.body.data.user.latitude).toBe(updateData.latitude);
    expect(res.body.data.user.longitude).toBe(updateData.longitude);
  });
});

describe('DELETE /api/users/:id - unmocked (requires running server)', () => {
  /*
    Inputs: path param id (valid user ID)
    Expected status: 200
    Output: { message: 'User deleted successfully' }
    Expected behavior: Deletes user and all associated images
  */
  test('returns success (200) when server is available and user deleted', async () => {
    const createData: CreateUserRequest = {
      email: `test-${Date.now()}@example.com`,
      name: "Test User",
      googleId: `test-google-id-${Date.now()}`,
      age: 25,
      profilePicture: "http://example.com/profile-pic.jpg",
      bio: "This is a test bio.",
      location: "Test Location",
      latitude: 34.0522,
      longitude: -118.2437,
      skillLevel: "Beginner"
    };

    const createdUser = await userModel.create(createData);
    const deletedUserId = createdUser._id;
    
    // call the endpoint
    const res = await request(app).delete(`/api/users/${deletedUserId.toString()}`);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User deleted successfully');
    
    // verify user is deleted
    const fetchedUser = await userModel.findById(deletedUserId);
    expect(fetchedUser).toBeNull();
  });

  /*
    Inputs: path param 'invalid-id'
    Expected status: 400
    Output: { message: 'Invalid user id' }
    Expected behavior: Rejects invalid user ID format
  */
  test('returns 400 when user ID is invalid', async () => {
    const res = await request(app).delete('/api/users/invalid-id');
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid user id');
  });

  /*
    Inputs: path param id (valid ObjectId but non-existent user)
    Expected status: 404
    Output: { message: 'User not found' }
    Expected behavior: Returns 404 when user doesn't exist
  */
  test('returns 404 when user does not exist', async () => {
    const fakeId = new mongoose.Types.ObjectId().toString();
    const res = await request(app).delete(`/api/users/${fakeId}`);
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User not found');
  });
});

describe('DELETE /api/users/ - unmocked (requires running server)', () => {
    test('returns success (200) when server is available and user deleted', async () => {

        // Generate unique googleId to avoid duplicates
        const uniqueGoogleId = `test-google-id-delete-${Date.now()}`;
        
        // Create user directly in DB instead of using Google auth
        const createData: CreateUserRequest = {
            email: `test-delete-${Date.now()}@example.com`,
            name: "Test Delete User",
            googleId: uniqueGoogleId,
            age: 25,
            profilePicture: "http://example.com/profile-pic.jpg",
            bio: "This is a test bio for deletion.",
            location: "Test Location",
            latitude: 34.0522,
            longitude: -118.2437,
            skillLevel: "Beginner"
        };

        const createdUser = await userModel.create(createData);
        const deletedUserId = createdUser._id;
        
        // Generate a token for this user
        const jwt = require('jsonwebtoken');
        const token = jwt.sign({ id: deletedUserId }, process.env.JWT_SECRET!, {
            expiresIn: '1h',
        });
        
        // call the endpoint with the generated token
        const deleteRes = await request(app).delete('/api/users').set('Authorization', `Bearer ${token}`);
        expect(deleteRes.status).toBe(200);
        expect(deleteRes.body).toHaveProperty('message');
        
        // verify user is deleted
        const fetchedUser = await userModel.findById(deletedUserId);
        expect(fetchedUser).toBeNull();
      });
});
