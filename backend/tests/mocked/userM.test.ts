import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll, jest } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { userModel } from '../../src/models/user.model';
import { UpdateProfileRequest } from '../../src/types/user.types';
import express from 'express';
import userRoutes from '../../src/routes/user.routes';
import mongoose from 'mongoose';

dotenv.config();
const USER = process.env.USER_ID as string;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware to set req.user
app.use('/api/users', (req: any, res: any, next: any) => {
    req.user = { 
        _id: new mongoose.Types.ObjectId(USER),
        email: 'test@example.com',
        name: 'Test User'
    };
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
});

afterAll(async () => {
  await teardownTestDB();
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('GET /api/users - mocked', () => {
    test('returns 500 when database query fails', async () => {
        // Mock userModel.findAll to throw an error
        jest.spyOn(userModel, 'findAll').mockRejectedValue(new Error('Database connection failed'));

        // Make request
        const res = await request(app).get('/api/users');

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.findAll).toHaveBeenCalledTimes(1);
    });

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
});

describe('PUT /api/users/:id - mocked', () => {
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
});

describe('POST /api/users - mocked', () => {
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
});

describe('DELETE /api/users/:id - mocked', () => {
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
});

describe('DELETE /api/users - mocked', () => {
    test('returns 500 when delete operation fails', async () => {
        // Mock userModel.delete to throw an error
        jest.spyOn(userModel, 'delete').mockRejectedValue(new Error('Delete failed'));

        // Make request
        const res = await request(app).delete('/api/users');

        // Assertions
        expect(res.status).toBe(500);
        expect(userModel.delete).toHaveBeenCalledTimes(1);
    });

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
