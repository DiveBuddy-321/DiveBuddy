import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll, jest } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { userModel } from '../../src/models/user.model';
import { UpdateProfileRequest, CreateUserRequest } from '../../src/types/user.types';
import express from 'express';
import userRoutes from '../../src/routes/user.routes';
import mongoose from 'mongoose';

dotenv.config();

// Test user will be created dynamically
let testUser: any = null;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware to set req.user
app.use('/api/users', (req: any, res: any, next: any) => {
    if (testUser) {
        req.user = { 
            _id: testUser._id,
            email: testUser.email,
            name: testUser.name
        };
    }
    next();
}, userRoutes);

// Error handling middleware
app.use((err: any, req: any, res: any, next: any) => {
    res.status(500).json({
        message: err.message || 'Internal server error',
    });
});

beforeAll(async () => {
  await setupTestDB();
  
  // Create a test user
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
    skillLevel: 'Intermediate',
    eventsCreated: [],
    eventsJoined: [],
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

describe('GET /api/users - mocked', () => {
    /**
     * Inputs: Request to fetch all users, database connection failure
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database connection fails during user list retrieval
     */
    test('returns 500 when database query fails', async () => {
        // Mock userModel.findAll to throw an error
        jest.spyOn(userModel, 'findAll').mockRejectedValue(new Error('Database connection failed'));

        // Make request
        const res = await request(app).get('/api/users');

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.findAll).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Request to fetch all users, unexpected error in database layer
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when unexpected exception occurs during user list query
     */
    test('returns 500 when unexpected error occurs', async () => {
        // Mock userModel.findAll to throw an unexpected error
        jest.spyOn(userModel, 'findAll').mockRejectedValue(new Error('Unexpected error'));

        // Make request
        const res = await request(app).get('/api/users');

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.findAll).toHaveBeenCalledTimes(1);
    });
});

describe('GET /api/users/profile - mocked', () => {
    /**
     * Inputs: Authenticated user requesting own profile
     * Expected status: 200
     * Output: Success message with user data from req.user
     * Expected behavior: Returns current user profile without database query
     */
    test('returns current user profile (200) from req.user', async () => {
        // This endpoint doesn't hit the database, just returns req.user
        const res = await request(app).get('/api/users/profile');

        // Assertions
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Profile fetched successfully');
        expect(res.body).toHaveProperty('data');
        expect(res.body.data).toHaveProperty('user');
    });
});

describe('GET /api/users/:id - mocked', () => {
    /**
     * Inputs: Valid userId that doesn't exist in database
     * Expected status: 404
     * Output: Error message 'User not found'
     * Expected behavior: Returns 404 when attempting to access non-existent user
     */
    test('returns 404 when user not found', async () => {
        const mockUserId = new mongoose.Types.ObjectId();

        // Mock userModel.findById to return null
        jest.spyOn(userModel, 'findById').mockResolvedValue(null);

        // Make request
        const res = await request(app).get(`/api/users/${mockUserId.toString()}`);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('User not found');
        expect(userModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid userId, database error during query
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database query fails during user retrieval
     */
    test('returns 500 when database query fails', async () => {
        const mockUserId = new mongoose.Types.ObjectId();

        // Mock userModel.findById to throw an error
        jest.spyOn(userModel, 'findById').mockRejectedValue(new Error('Database error'));

        // Make request
        const res = await request(app).get(`/api/users/${mockUserId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to userModel.findById(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to find user'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('findById throws "Failed to find user" when database operation fails', async () => {
        // Mock the User model's findOne method directly
        const User = mongoose.model('User');
        const findOneSpy = jest.spyOn(User, 'findOne').mockRejectedValue(new Error('Database connection lost'));

        const testId = new mongoose.Types.ObjectId();
        await expect(userModel.findById(testId)).rejects.toThrow('Failed to find user');
    });
});

describe('PUT /api/users/:id - mocked', () => {
    /**
     * Inputs: Valid userId that doesn't exist, valid update data
     * Expected status: 404
     * Output: Error message 'User not found'
     * Expected behavior: Returns 404 when attempting to update non-existent user
     */
    test('returns 404 when user not found', async () => {
        const mockUserId = new mongoose.Types.ObjectId();
        const updateData: UpdateProfileRequest = {
            name: 'Updated Name',
            age: 30,
            bio: 'Updated bio',
        };

        // Mock userModel.findById to return null
        jest.spyOn(userModel, 'findById').mockResolvedValue(null);

        // Make request
        const res = await request(app).put(`/api/users/${mockUserId.toString()}`).send(updateData);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('User not found');
        expect(userModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid userId and update data, update operation returns null
     * Expected status: 500
     * Output: Error message 'Failed to update user'
     * Expected behavior: Returns error when update succeeds but returns no result
     */
    test('returns 500 when update fails', async () => {
        const mockUserId = new mongoose.Types.ObjectId();
        const existingUser = {
            _id: mockUserId,
            email: 'test@example.com',
            name: 'Test User',
        };
        const updateData: UpdateProfileRequest = {
            name: 'Updated Name',
            age: 30,
            bio: 'Updated bio',
        };

        // Mock userModel.findById to succeed but update to return null
        jest.spyOn(userModel, 'findById').mockResolvedValue(existingUser as any);
        jest.spyOn(userModel, 'update').mockResolvedValue(null);

        // Make request
        const res = await request(app).put(`/api/users/${mockUserId.toString()}`).send(updateData);

        // Assertions
        expect(res.status).toBe(500);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Failed to update user');
        expect(userModel.findById).toHaveBeenCalledTimes(1);
        expect(userModel.update).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid userId and update data, database error during user lookup
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database fails during user existence check
     */
    test('returns 500 when database error occurs during findById', async () => {
        const mockUserId = new mongoose.Types.ObjectId();
        const updateData: UpdateProfileRequest = {
            name: 'Updated Name',
            age: 30,
            bio: 'Updated bio',
        };

        // Mock userModel.findById to throw an error
        jest.spyOn(userModel, 'findById').mockRejectedValue(new Error('Database connection error'));

        // Make request
        const res = await request(app).put(`/api/users/${mockUserId.toString()}`).send(updateData);

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to userModel.update(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to update user'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('update throws "Failed to update user" when database operation fails', async () => {
        // Mock the User model's findByIdAndUpdate method directly
        const User = mongoose.model('User');
        const updateSpy = jest.spyOn(User, 'findByIdAndUpdate').mockRejectedValue(new Error('Database connection lost'));

        const testId = new mongoose.Types.ObjectId();
        const updateData: any = {
            name: 'Updated Name',
            age: 30
        };

        await expect(userModel.update(testId, updateData)).rejects.toThrow('Failed to update user');
    });
});

describe('POST /api/users - mocked', () => {
    /**
     * Inputs: Valid update data, update operation returns null (user not found)
     * Expected status: 404
     * Output: Error message 'User not found'
     * Expected behavior: Returns 404 when user doesn't exist during profile update
     */
    test('returns 404 when user not found during update', async () => {
        const updateData: UpdateProfileRequest = {
            name: 'Updated Name',
            age: 30,
            bio: 'Updated bio',
        };

        // Mock userModel.update to return null
        jest.spyOn(userModel, 'update').mockResolvedValue(null);

        // Make request
        const res = await request(app).post('/api/users').send(updateData);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('User not found');
        expect(userModel.update).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid update data, database error during update operation
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database fails during profile update
     */
    test('returns 500 when database error occurs during update', async () => {
        const updateData: UpdateProfileRequest = {
            name: 'Updated Name',
            age: 30,
            bio: 'Updated bio',
        };

        // Mock userModel.update to throw an error
        jest.spyOn(userModel, 'update').mockRejectedValue(new Error('Database connection error'));

        // Make request
        const res = await request(app).post('/api/users').send(updateData);

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.update).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to userModel.create(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to create user'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('create throws "Failed to create user" when database operation fails', async () => {
        // We need to mock the actual mongoose model's create method
        // Get the User model and mock its create method
        const User = mongoose.model('User');
        const createSpy = jest.spyOn(User, 'create').mockRejectedValue(new Error('Database connection lost'));

        const validUserInfo: any = {
            googleId: "test-google-id",
            email: "test@example.com",
            name: "Test User"
        };

        await expect(userModel.create(validUserInfo)).rejects.toThrow('Failed to create user');
    });

    /**
     * Inputs: User creation with location but no coordinates, geocoding API unavailable/fails
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to geocode location'
     * Expected behavior: Fails when location requires geocoding but geocoding service is unavailable
     */
    test('create throws error when location provided without coordinates and geocoding fails', async () => {
        // Test the case where location is provided but no coordinates
        // and geocoding fails (no GEOCODING_API or API failure)
        const userWithLocationNoCoords: CreateUserRequest = {
            email: 'test-geocode@example.com',
            name: 'Test User',
            googleId: `test-google-${Date.now()}`,
            age: 25,
            profilePicture: 'http://example.com/pic.jpg',
            bio: 'Test bio',
            location: 'Vancouver, BC',
            skillLevel: 'Intermediate'
            // Note: Missing latitude and longitude
        };

        // This should throw an error because geocoding will fail
        await expect(userModel.create(userWithLocationNoCoords)).rejects.toThrow('Failed to geocode location');
    });

    /**
     * Inputs: Direct call to userModel.findByGoogleId(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to find user'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('findByGoogleId throws "Failed to find user" when database operation fails', async () => {
        // Mock the User model's findOne method directly
        const User = mongoose.model('User');
        const findOneSpy = jest.spyOn(User, 'findOne').mockRejectedValue(new Error('Database connection lost'));

        await expect(userModel.findByGoogleId('test-google-id')).rejects.toThrow('Failed to find user');
    });
});

describe('DELETE /api/users/:id - mocked', () => {
    /**
     * Inputs: Valid userId that doesn't exist in database
     * Expected status: 404
     * Output: Error message 'User not found'
     * Expected behavior: Returns 404 when attempting to delete non-existent user
     */
    test('returns 404 when user not found', async () => {
        const mockUserId = new mongoose.Types.ObjectId();

        // Mock userModel.findById to return null
        jest.spyOn(userModel, 'findById').mockResolvedValue(null);

        // Make request
        const res = await request(app).delete(`/api/users/${mockUserId.toString()}`);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('User not found');
        expect(userModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid userId, database error during user lookup
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database fails during delete pre-check
     */
    test('returns 500 when database error occurs during findById', async () => {
        const mockUserId = new mongoose.Types.ObjectId();

        // Mock userModel.findById to throw an error
        jest.spyOn(userModel, 'findById').mockRejectedValue(new Error('Database connection error'));

        // Make request
        const res = await request(app).delete(`/api/users/${mockUserId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid userId, user exists, delete operation throws error
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database delete operation fails
     */
    test('returns 500 when delete operation fails', async () => {
        const mockUserId = new mongoose.Types.ObjectId();
        const existingUser = {
            _id: mockUserId,
            email: 'test@example.com',
            name: 'Test User',
        };

        // Mock userModel.findById to succeed but delete to fail
        jest.spyOn(userModel, 'findById').mockResolvedValue(existingUser as any);
        jest.spyOn(userModel, 'delete').mockRejectedValue(new Error('Delete failed'));

        // Make request
        const res = await request(app).delete(`/api/users/${mockUserId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.findById).toHaveBeenCalledTimes(1);
        expect(userModel.delete).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to userModel.delete(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to delete user'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('delete throws "Failed to delete user" when database operation fails', async () => {
        // Mock the User model's findByIdAndDelete method directly
        const User = mongoose.model('User');
        const deleteSpy = jest.spyOn(User, 'findByIdAndDelete').mockRejectedValue(new Error('Database connection lost'));

        const testId = new mongoose.Types.ObjectId();
        await expect(userModel.delete(testId)).rejects.toThrow('Failed to delete user');
    });
});

describe('DELETE /api/users - mocked', () => {
    /**
     * Inputs: Delete current user request, delete operation throws error
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database delete operation fails
     */
    test('returns 500 when delete operation fails', async () => {
        // Mock userModel.delete to throw an error
        jest.spyOn(userModel, 'delete').mockRejectedValue(new Error('Delete failed'));

        // Make request
        const res = await request(app).delete('/api/users');

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.delete).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Delete current user request, database connection failure
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database connection fails during delete
     */
    test('returns 500 when database connection error occurs', async () => {
        // Mock userModel.delete to throw a database error
        jest.spyOn(userModel, 'delete').mockRejectedValue(new Error('Database connection error'));

        // Make request
        const res = await request(app).delete('/api/users');

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.delete).toHaveBeenCalledTimes(1);
    });
});
