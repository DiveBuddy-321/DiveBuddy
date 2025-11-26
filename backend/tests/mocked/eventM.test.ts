import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll, jest } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { eventModel } from '../../src/models/event.model';
import logger from '../../src/utils/logger.util';
import { CreateUserRequest } from '../../src/types/user.types';
import { userModel } from '../../src/models/user.model';
import express from 'express';
import eventRoutes from '../../src/routes/event.routes';
import mongoose from 'mongoose';

dotenv.config();
// Test user will be created dynamically
let testUser: any = null;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware to set req.user
app.use('/api/events', (req: any, res: any, next: any) => {
    if (testUser) {
        req.user = { _id: testUser._id };
    }
    next();
}, eventRoutes);

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

describe('GET /api/events - mocked', () => {
    /**
     * Inputs: Request to fetch all events, database connection failure
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database connection fails during event list retrieval
     */
    test('returns 500 when database query fails', async () => {
        // Mock eventModel.findAll to throw an error
        jest.spyOn(eventModel, 'findAll').mockRejectedValue(new Error('Database connection failed'));

        // Make request
        const res = await request(app).get('/api/events');

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findAll).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Request to fetch all events, unexpected error in database layer
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when unexpected exception occurs during event list query
     */
    test('returns 500 when unexpected error occurs', async () => {
        // Mock eventModel.findAll to throw an unexpected error
        jest.spyOn(eventModel, 'findAll').mockRejectedValue(new Error('Unexpected error'));

        // Make request
        const res = await request(app).get('/api/events');

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findAll).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Request to fetch all events, database query timeout
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database query times out during event list retrieval
     */
    test('returns 500 when database timeout occurs', async () => {
        // Mock eventModel.findAll to throw a timeout error
        jest.spyOn(eventModel, 'findAll').mockRejectedValue(new Error('Connection timeout'));

        // Make request
        const res = await request(app).get('/api/events');

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findAll).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Request to fetch all events, null reference access in query
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when null reference is accessed during event list operation
     */
    test('returns 500 when null pointer exception occurs', async () => {
        // Mock eventModel.findAll to throw null error
        jest.spyOn(eventModel, 'findAll').mockRejectedValue(new TypeError('Cannot read property of null'));

        // Make request
        const res = await request(app).get('/api/events');

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findAll).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to eventModel.findAll(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to fetch events'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('findAll throws "Failed to fetch events" when database operation fails', async () => {
        // Mock the Event model's find method directly
        const Event = mongoose.model('Event');
        const mockExec = (jest.fn() as any).mockRejectedValue(new Error('Database connection lost'));
        const mockSort = (jest.fn() as any).mockReturnValue({ exec: mockExec });
        jest.spyOn(Event, 'find').mockReturnValue({ sort: mockSort } as any);

        await expect(eventModel.findAll()).rejects.toThrow('Failed to fetch events');
    });

    /**
     * Inputs: Logger input containing CRLF characters (newline/carriage return)
     * Expected status: N/A (direct logger test)
     * Output: Throws error 'CRLF injection attempt detected'
     * Expected behavior: Logger validates input and prevents log injection attacks
     */
    test('logger.info throws error when input contains CRLF characters', () => {
        // Test that logger rejects input with newline characters (CRLF injection attempt)
        const maliciousInput = 'Normal message\nInjected malicious log entry';
        
        // This should throw "CRLF injection attempt detected"
        expect(() => {
            logger.info(maliciousInput);
        }).toThrow('CRLF injection attempt detected');
    });
});

describe('GET /api/events/:id - mocked', () => {
    
    /**
     * Inputs: Valid eventId that doesn't exist in database
     * Expected status: 404
     * Output: Error message 'Event not found'
     * Expected behavior: Returns 404 when attempting to access non-existent event
     */
    test('returns 404 when event not found', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to return null
        jest.spyOn(eventModel, 'findById').mockResolvedValue(null);

        // Make request
        const res = await request(app).get(`/api/events/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Event not found');
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Malformed eventId (not valid ObjectId format)
     * Expected status: 400
     * Output: Error message 'Invalid event id'
     * Expected behavior: Validates eventId format and rejects malformed IDs
     */
    test('returns 400 when invalid event ID provided', async () => {
        // Make request with invalid ID
        const res = await request(app).get('/api/events/invalid-id');

        // Assertions
        expect(res.status).toBe(400);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Invalid event id');
    });

    /**
     * Inputs: Valid eventId, database error during query
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database query fails during event retrieval
     */
    test('returns 500 when database query fails', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to throw an error
        jest.spyOn(eventModel, 'findById').mockRejectedValue(new Error('Database error'));

        // Make request
        const res = await request(app).get(`/api/events/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid eventId, network connectivity failure
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when network error occurs during event retrieval
     */
    test('returns 500 when network error occurs', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to throw a network error
        jest.spyOn(eventModel, 'findById').mockRejectedValue(new Error('Network error'));

        // Make request
        const res = await request(app).get(`/api/events/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid eventId, system out of memory during query
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when system runs out of memory during event retrieval
     */
    test('returns 500 when memory error occurs', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to throw a memory error
        jest.spyOn(eventModel, 'findById').mockRejectedValue(new Error('Out of memory'));

        // Make request
        const res = await request(app).get(`/api/events/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to eventModel.findById(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to find event'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('findById throws "Failed to find event" when database operation fails', async () => {
        // Mock the Event model's findOne method directly
        const Event = mongoose.model('Event');
        jest.spyOn(Event, 'findOne').mockRejectedValue(new Error('Database connection lost'));

        const testId = new mongoose.Types.ObjectId();
        await expect(eventModel.findById(testId)).rejects.toThrow('Failed to find event');
    });
});

describe('POST /api/events - mocked', () => {

    /**
     * Inputs: Valid event creation data, database error during creation
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database fails during event creation
     */
    test('returns 500 when event creation fails', async () => {
        const newEventData = {
            title: 'New Test Event',
            description: 'New Test Description',
            date: new Date().toISOString(),
            capacity: 20,
            skillLevel: 'Intermediate',
            location: 'New Location',
            latitude: 40.7128,
            longitude: -74.0060,
            attendees: [],
            photo: '',
        };

        // Mock eventModel.create to throw an error
        jest.spyOn(eventModel, 'create').mockRejectedValue(new Error('Database error'));

        // Make request
        const res = await request(app).post('/api/events').send(newEventData);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.create).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Event creation data, database validation failure
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database validation fails during event creation
     */
    test('returns 500 when validation error occurs in database', async () => {
        const newEventData = {
            title: 'New Test Event',
            description: 'New Test Description',
            date: new Date().toISOString(),
            capacity: 20,
            skillLevel: 'Intermediate',
            location: 'New Location',
            latitude: 40.7128,
            longitude: -74.0060,
            attendees: [],
            photo: '',
        };

        // Mock eventModel.create to throw a validation error
        jest.spyOn(eventModel, 'create').mockRejectedValue(new Error('Validation failed'));

        // Make request
        const res = await request(app).post('/api/events').send(newEventData);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.create).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Event creation data with duplicate unique field
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when duplicate key constraint is violated
     */
    test('returns 500 when duplicate key error occurs', async () => {
        const newEventData = {
            title: 'New Test Event',
            description: 'New Test Description',
            date: new Date().toISOString(),
            capacity: 20,
            skillLevel: 'Intermediate',
            location: 'New Location',
            latitude: 40.7128,
            longitude: -74.0060,
            attendees: [],
            photo: '',
        };

        // Mock eventModel.create to throw a duplicate key error
        jest.spyOn(eventModel, 'create').mockRejectedValue(new Error('Duplicate key error'));

        // Make request
        const res = await request(app).post('/api/events').send(newEventData);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.create).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to eventModel.create(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to create event'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('create throws "Failed to create event" when database operation fails', async () => {
        // Mock the Event model's create method directly
        const Event = mongoose.model('Event');
        jest.spyOn(Event, 'create').mockRejectedValue(new Error('Database connection lost'));

        const validEventData: any = {
            title: 'Test Event',
            description: 'Test Description',
            date: new Date(),
            capacity: 10,
            createdBy: new mongoose.Types.ObjectId().toString(),
            attendees: []
        };

        await expect(eventModel.create(validEventData)).rejects.toThrow('Failed to create event');
    });
});

describe('PUT /api/events/:id - mocked', () => {
    
    /**
     * Inputs: Valid eventId that doesn't exist, valid update data
     * Expected status: 404
     * Output: Error message 'Event not found'
     * Expected behavior: Returns 404 when attempting to update non-existent event
     */
    test('returns 404 when event not found', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const updateData = { 
            title: 'Updated Title',
            description: 'Updated Description',
            date: new Date().toISOString(),
            capacity: 15,
            attendees: [],
        };

        // Mock eventModel.findById to return null
        jest.spyOn(eventModel, 'findById').mockResolvedValue(null);

        // Make request
        const res = await request(app).put(`/api/events/${mockEventId.toString()}`).send(updateData);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Event not found');
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid eventId, invalid update data (wrong type)
     * Expected status: 400
     * Output: Error message 'Invalid input data'
     * Expected behavior: Validates update data schema and rejects invalid types
     */
    test('returns 400 when invalid data provided', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const updateData = { title: 123 }; // Invalid type

        // Make request with invalid data
        const res = await request(app).put(`/api/events/${mockEventId.toString()}`).send(updateData);

        // Assertions
        expect(res.status).toBe(400);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Invalid input data');
    });

    /**
     * Inputs: Valid eventId and update data, database update operation returns null
     * Expected status: 500
     * Output: Error message 'Failed to update event'
     * Expected behavior: Returns error when update operation succeeds but returns no result
     */
    test('returns 500 when update fails', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const existingEvent = {
            _id: mockEventId,
            title: 'Old Title',
        };
        const updateData = { 
            title: 'Updated Title',
            description: 'Updated Description',
            date: new Date().toISOString(),
            capacity: 15,
            attendees: [],
        };

        // Mock eventModel.findById to succeed but update to return null
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);
        jest.spyOn(eventModel, 'update').mockResolvedValue(null);

        // Make request
        const res = await request(app).put(`/api/events/${mockEventId.toString()}`).send(updateData);

        // Assertions
        expect(res.status).toBe(500);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Failed to update event');
    });

    /**
     * Inputs: Valid eventId and update data, database error during event lookup
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database fails during event existence check
     */
    test('returns 500 when database error occurs during findById', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const updateData = { 
            title: 'Updated Title',
            description: 'Updated Description',
            date: new Date().toISOString(),
            capacity: 15,
            attendees: [],
        };

        // Mock eventModel.findById to throw error
        jest.spyOn(eventModel, 'findById').mockRejectedValue(new Error('Database connection lost'));

        // Make request
        const res = await request(app).put(`/api/events/${mockEventId.toString()}`).send(updateData);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid eventId and update data, database error during update operation
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database update operation throws error
     */
    test('returns 500 when update throws error', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const existingEvent = {
            _id: mockEventId,
            title: 'Old Title',
        };
        const updateData = { 
            title: 'Updated Title',
            description: 'Updated Description',
            date: new Date().toISOString(),
            capacity: 15,
            attendees: [],
        };

        // Mock eventModel.findById to succeed but update to throw error
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);
        jest.spyOn(eventModel, 'update').mockRejectedValue(new Error('Update failed'));

        // Make request
        const res = await request(app).put(`/api/events/${mockEventId.toString()}`).send(updateData);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.update).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to eventModel.update(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to update event'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('update throws "Failed to update event" when database operation fails', async () => {
        // Mock the Event model's findByIdAndUpdate method directly
        const Event = mongoose.model('Event');
        jest.spyOn(Event, 'findByIdAndUpdate').mockRejectedValue(new Error('Database connection lost'));

        const testId = new mongoose.Types.ObjectId();
        const updateData: any = {
            title: 'Updated Title',
            description: 'Updated Description',
            date: new Date(),
            capacity: 15,
            attendees: []
        };

        await expect(eventModel.update(testId, updateData)).rejects.toThrow('Failed to update event');
    });
});

describe('PUT /api/events/join/:id - mocked', () => {

    /**
     * Inputs: Valid eventId, user already in attendees list
     * Expected status: 400
     * Output: Error message 'User already joined the event'
     * Expected behavior: Prevents duplicate joins by checking attendees array
     */
    test('returns 400 when user already joined', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const mockUserId = testUser ? testUser._id : new mongoose.Types.ObjectId();
        
        // Create a mock attendees array with includes method
        const mockAttendees = [mockUserId];
        (mockAttendees as any).includes = function(id: any) {
            return this.some((attendeeId: any) => attendeeId.equals(id));
        };
        
        const existingEvent = {
            _id: mockEventId,
            attendees: mockAttendees,
            capacity: 10,
        };

        // Mock eventModel.findById
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);

        // Make request
        const res = await request(app).put(`/api/events/join/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(400);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('User already joined the event');
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid eventId, event at maximum capacity
     * Expected status: 400
     * Output: Error message 'Event is at full capacity'
     * Expected behavior: Prevents joining when attendees count equals capacity
     */
    test('returns 400 when event is at full capacity', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const existingEvent = {
            _id: mockEventId,
            attendees: [
                new mongoose.Types.ObjectId(),
                new mongoose.Types.ObjectId(),
            ],
            capacity: 2,
        };

        // Mock eventModel.findById
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);

        // Make request
        const res = await request(app).put(`/api/events/join/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(400);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Event is at full capacity');
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    test('returns 404 when event not found', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to return null
        jest.spyOn(eventModel, 'findById').mockResolvedValue(null);

        // Make request
        const res = await request(app).put(`/api/events/join/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Event not found');
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid join request, database error during event lookup
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database fails during join operation
     */
    test('returns 500 when database error occurs during join', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to throw error
        jest.spyOn(eventModel, 'findById').mockRejectedValue(new Error('Database error'));

        // Make request
        const res = await request(app).put(`/api/events/join/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid join request, event found, update operation returns null
     * Expected status: 500
     * Output: Error message 'Failed to update event'
     * Expected behavior: Returns error when update fails after successful validation
     */
    test('returns 500 when update fails after join', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const mockUserId = testUser ? testUser._id : new mongoose.Types.ObjectId();
        const existingEvent = {
            _id: mockEventId,
            title: 'Test Event',
            attendees: [],
            capacity: 10,
            toObject: () => ({
                _id: mockEventId,
                title: 'Test Event',
                attendees: [],
                capacity: 10,
                __v: 0,
                createdAt: new Date(),
                updatedAt: new Date(),
            }),
        };

        // Mock eventModel.findById to succeed but update to return null
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);
        jest.spyOn(eventModel, 'update').mockResolvedValue(null);

        // Make request
        const res = await request(app).put(`/api/events/join/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Failed to update event');
    });
});

describe('PUT /api/events/leave/:id - mocked', () => {

    /**
     * Inputs: Valid eventId, user not in attendees list
     * Expected status: 400
     * Output: Error message 'User is not an attendee of the event'
     * Expected behavior: Prevents leaving when user hasn't joined the event
     */
    test('returns 400 when user is not an attendee', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const existingEvent = {
            _id: mockEventId,
            attendees: [],
        };

        // Mock eventModel.findById
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);

        // Make request
        const res = await request(app).put(`/api/events/leave/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(400);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('User is not an attendee of the event');
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid eventId that doesn't exist
     * Expected status: 404
     * Output: Error message 'Event not found'
     * Expected behavior: Returns 404 when attempting to leave non-existent event
     */
    test('returns 404 when event not found', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to return null
        jest.spyOn(eventModel, 'findById').mockResolvedValue(null);

        // Make request
        const res = await request(app).put(`/api/events/leave/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Event not found');
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid leave request, database error during event lookup
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database fails during leave operation
     */
    test('returns 500 when database error occurs during leave', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to throw error
        jest.spyOn(eventModel, 'findById').mockRejectedValue(new Error('Database connection error'));

        // Make request
        const res = await request(app).put(`/api/events/leave/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid leave request, user in attendees, update operation returns null
     * Expected status: 500
     * Output: Error message 'Failed to update event'
     * Expected behavior: Returns error when update fails after successful validation
     */
    test('returns 500 when update fails after leave', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const mockUserId = testUser ? testUser._id : new mongoose.Types.ObjectId();
        
        // Create mock attendees array with all needed methods
        const mockAttendees = [mockUserId];
        (mockAttendees as any).includes = function(id: any) {
            return this.some((attendeeId: any) => attendeeId.equals(id));
        };
        (mockAttendees as any).filter = function(callback: any) {
            return Array.prototype.filter.call(this, callback);
        };
        (mockAttendees as any).map = function(callback: any) {
            return Array.prototype.map.call(this, callback);
        };
        
        const existingEvent = {
            _id: mockEventId,
            title: 'Test Event',
            description: 'Test Description',
            date: new Date(),
            capacity: 10,
            skillLevel: 'Beginner',
            location: 'Test Location',
            latitude: 37.7749,
            longitude: -122.4194,
            createdBy: new mongoose.Types.ObjectId(),
            attendees: mockAttendees,
            photo: '',
            toObject: () => ({
                _id: mockEventId,
                title: 'Test Event',
                description: 'Test Description',
                date: new Date(),
                capacity: 10,
                skillLevel: 'Beginner',
                location: 'Test Location',
                latitude: 37.7749,
                longitude: -122.4194,
                createdBy: new mongoose.Types.ObjectId(),
                attendees: mockAttendees,
                photo: '',
                __v: 0,
                createdAt: new Date(),
                updatedAt: new Date(),
            }),
        };

        // Mock eventModel.findById to succeed but update to return null
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);
        jest.spyOn(eventModel, 'update').mockResolvedValue(null);

        // Make request
        const res = await request(app).put(`/api/events/leave/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Failed to update event');
    });
});

describe('DELETE /api/events/:id - mocked', () => {

    /**
     * Inputs: Valid eventId that doesn't exist
     * Expected status: 404
     * Output: Error message 'Event not found'
     * Expected behavior: Returns 404 when attempting to delete non-existent event
     */
    test('returns 404 when event not found', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to return null
        jest.spyOn(eventModel, 'findById').mockResolvedValue(null);

        // Make request
        const res = await request(app).delete(`/api/events/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(404);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Event not found');
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Malformed eventId (not valid ObjectId format)
     * Expected status: 400
     * Output: Error message 'Invalid event id'
     * Expected behavior: Validates eventId format before delete operation
     */
    test('returns 400 when invalid event ID provided', async () => {
        // Make request with invalid ID
        const res = await request(app).delete('/api/events/invalid-id');

        // Assertions
        expect(res.status).toBe(400);
        expect(res.body).toHaveProperty('message');
        expect(res.body.message).toBe('Invalid event id');
    });

    /**
     * Inputs: Valid eventId, event exists, delete operation throws error
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database delete operation fails
     */
    test('returns 500 when delete fails', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const existingEvent = {
            _id: mockEventId,
            title: 'Test Event',
        };

        // Mock eventModel.findById to succeed but delete to fail
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);
        jest.spyOn(eventModel, 'delete').mockRejectedValue(new Error('Database error'));

        // Make request
        const res = await request(app).delete(`/api/events/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
        expect(eventModel.delete).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid eventId, database error during event lookup
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when database fails during delete pre-check
     */
    test('returns 500 when database error occurs during findById for delete', async () => {
        const mockEventId = new mongoose.Types.ObjectId();

        // Mock eventModel.findById to throw error
        jest.spyOn(eventModel, 'findById').mockRejectedValue(new Error('Cannot connect to database'));

        // Make request
        const res = await request(app).delete(`/api/events/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.findById).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Valid eventId, event exists, permission denied during delete
     * Expected status: 500
     * Output: Error message in response body
     * Expected behavior: Returns error when delete operation fails due to permissions
     */
    test('returns 500 when permission error occurs', async () => {
        const mockEventId = new mongoose.Types.ObjectId();
        const existingEvent = {
            _id: mockEventId,
            title: 'Test Event',
        };

        // Mock eventModel.findById to succeed but delete to throw permission error
        jest.spyOn(eventModel, 'findById').mockResolvedValue(existingEvent as any);
        jest.spyOn(eventModel, 'delete').mockRejectedValue(new Error('Permission denied'));

        // Make request
        const res = await request(app).delete(`/api/events/${mockEventId.toString()}`);

        // Assertions
        expect(res.status).toBe(500);
        expect(eventModel.delete).toHaveBeenCalledTimes(1);
    });

    /**
     * Inputs: Direct call to eventModel.delete(), database operation failure
     * Expected status: N/A (direct model test)
     * Output: Throws error with message 'Failed to delete event'
     * Expected behavior: Model method wraps database errors in custom error message
     */
    test('delete throws "Failed to delete event" when database operation fails', async () => {
        // Mock the Event model's findByIdAndDelete method directly
        const Event = mongoose.model('Event');
        jest.spyOn(Event, 'findByIdAndDelete').mockRejectedValue(new Error('Database connection lost'));

        const testId = new mongoose.Types.ObjectId();
        await expect(eventModel.delete(testId)).rejects.toThrow('Failed to delete event');
    });
});
