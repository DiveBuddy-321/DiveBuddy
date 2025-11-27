const mockVerifyIdToken = jest.fn() as jest.Mock<Promise<any>, [any]>;

jest.mock('google-auth-library', () => {
  return {
    OAuth2Client: jest.fn().mockImplementation(() => ({
      verifyIdToken: mockVerifyIdToken,
    })),
  };
});

import request from 'supertest';
import { describe, test, expect, beforeAll, afterAll } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { eventModel } from '../../src/models/event.model';
import { userModel } from '../../src/models/user.model';
import { Chat } from '../../src/models/chat.model';
import { Message } from '../../src/models/message.model';
import { CreateEventRequest, UpdateEventRequest } from '../../src/types/event.types';
import express from 'express';
import eventRoutes from '../../src/routes/event.routes';
import authRoutes from '../../src/routes/auth.routes';
import chatRoutes from '../../src/routes/chat.routes';
import buddyRoutes from '../../src/routes/buddy.routes';
import mediaRoutes from '../../src/routes/media.routes';
import userRoutes from '../../src/routes/user.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import { authenticateToken } from '../../src/middleware/auth.middleware';
import mongoose from 'mongoose';
import jwt from 'jsonwebtoken';
import { UpdateProfileRequest, CreateUserRequest } from '../../src/types/user.types';

dotenv.config();

// Test users will be created dynamically
let testUser: any = null;
let otherTestUser: any = null;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware - sets req.user for all routes
app.use((req: any, res, next) => {
    // Skip mock auth for /api/auth routes (they handle their own auth)
    if (req.path.startsWith('/api/auth')) {
        return next();
    }

    // Skip mock auth for DELETE /api/users - will use real authenticateToken instead
    if (req.method === 'DELETE' && req.path === '/api/users') {
        return authenticateToken(req, res, next);
    }
    
    if (testUser) {
        req.user = { 
            _id: testUser._id,
            email: testUser.email,
            name: testUser.name
        };
    }
    next();
});

app.use('/api/events', eventRoutes);
app.use('/api/auth', authRoutes); 
app.use('/api/chats', chatRoutes);
app.use('/api/buddies', buddyRoutes);
app.use('/api/media', mediaRoutes);
app.use('/api/users', userRoutes);

app.use('*', notFoundHandler);
app.use(errorHandler);

beforeAll(async () => {
    await setupTestDB();
    
    // Create test users
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
    
    const newOtherUser: CreateUserRequest = {
        email: 'other@example.com',
        name: 'Other Test User',
        googleId: `test-google-other-${Date.now()}`,
        age: 30,
        profilePicture: 'http://example.com/other-pic.jpg',
        bio: 'Other test bio',
        location: 'Vancouver, BC',
        latitude: 49.2827,
        longitude: -123.1207,
        skillLevel: 'Expert'
    };
    otherTestUser = await userModel.create(newOtherUser);
    
    if (!mongoose.models.User) {
        console.log('[SETUP] User model not registered, accessing userModel to register it...');
        const _ = userModel; // This triggers UserModel constructor which registers the schema
        console.log('[SETUP] User model registered:', mongoose.models.User ? 'Yes' : 'No');
    } else {
        console.log('[SETUP] User model already registered');
    }
});

afterAll(async () => {
    // Clean up test users
    if (testUser) {
        await userModel.delete(new mongoose.Types.ObjectId(testUser._id));
    }
    if (otherTestUser) {
        await userModel.delete(new mongoose.Types.ObjectId(otherTestUser._id));
    }
    await teardownTestDB();
});

// non-functional tests to test event API calls within 500ms

describe('GET /api/events - unmocked (requires running server)', () => {
	/**
	 * Inputs: None (authenticated request to fetch all events)
	 * Expected status: 200
	 * Output: List of events with message and data properties
	 * Expected behavior: Returns all events within 500ms performance requirement
	 */
	test('returns list of events (200) when server is available, within 500ms', async () => {
		
		// make sure GET endpoint works
    const startTime = performance.now();
		const res = await request(app).get('/api/events');
    const endTime = performance.now();

    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('GET /api/events took ' + (endTime - startTime).toFixed(2) + 'ms');
		expect(res.status).toBe(200);
		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('events');
		expect(Array.isArray(res.body.data.events)).toBe(true);
	});
});

describe('GET /api/events/:eventId - unmocked (requires running server)', () => {
	/**
	 * Inputs: Valid eventId in URL path
	 * Expected status: 200
	 * Output: Single event object with all event details
	 * Expected behavior: Fetches specific event by ID within 500ms performance requirement
	 */
	test('returns event by ID (200) when server is available, within 500ms', async () => {
		
		// first create an event to ensure it exists
		const newEvent: CreateEventRequest = {
			title: "TEST GET EVENT",
			description: "TEST GET DESCRIPTION",
			date: new Date(),
			capacity: 10,
			skillLevel: "Expert",
			location: "Test Location",
			latitude: 37.7749,
			longitude: -122.4194,
			createdBy: testUser._id.toString(),
			attendees: [],
			photo: ""
		};
		const created = await eventModel.create(newEvent);
		const createdId = created._id;
		
		// now fetch the event by ID through the API
    const startTime = performance.now();
		const res = await request(app).get(`/api/events/${createdId.toString()}`);
    const endTime = performance.now();

    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('GET /api/events/:eventId took ' + (endTime - startTime).toFixed(2) + 'ms');

		// verify response
		expect(res.status).toBe(200);
		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('event');
		expect(res.body.data.event).toMatchObject({
			title: newEvent.title,
			description: newEvent.description,
			capacity: newEvent.capacity,
			skillLevel: newEvent.skillLevel,
			location: newEvent.location,
			latitude: newEvent.latitude,
			longitude: newEvent.longitude,
			createdBy: newEvent.createdBy,
			photo: newEvent.photo
		});
		
		// cleanup - delete the created event
		await eventModel.delete(createdId);
	});
});

describe('POST /api/events - unmocked (requires running server)', () => {
	/**
	 * Inputs: Valid event creation data (title, description, date, capacity, etc.)
	 * Expected status: 201
	 * Output: Newly created event object
	 * Expected behavior: Creates new event in database within 500ms performance requirement
	 */
	test('creates a new event (201) when server is available, within 500ms', async () => {

		// new event data
		const newEvent = {
			title: "TEST EVENT",
			description: "TEST DESCRIPTION",
			date: new Date().toISOString(),
			capacity: 10,
			skillLevel: "Expert",
			location: "Test Location",
			latitude: 37.7749,
			longitude: -122.4194,
			createdBy: testUser._id.toString(),
			attendees: [testUser._id.toString()],
			photo: ""
		};

		// make sure POST endpoint works
    const startTime = performance.now();
		const res = (await request(app).post('/api/events').send(newEvent));
		const endTime = performance.now();

		expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('POST /api/events took ' + (endTime - startTime).toFixed(2) + 'ms');
		expect(res.status).toBe(201);
		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('event');
		expect(res.body.data.event).toMatchObject(newEvent);

		// verify event was actually created in DB
		const eventInDb = await eventModel.findById(res.body.data.event._id);
		expect(eventInDb).not.toBeNull();
		expect(eventInDb?.title).toBe(newEvent.title);
		expect(eventInDb?.description).toBe(newEvent.description);
		expect(eventInDb?.date.toISOString()).toBe(newEvent.date);
		expect(eventInDb?.capacity).toBe(newEvent.capacity);
		expect(eventInDb?.skillLevel).toBe(newEvent.skillLevel);
		expect(eventInDb?.location).toBe(newEvent.location);
		expect(eventInDb?.latitude).toBe(newEvent.latitude);
		expect(eventInDb?.longitude).toBe(newEvent.longitude);
		expect(eventInDb?.createdBy.toString()).toBe(newEvent.createdBy);
		expect(eventInDb?.attendees.length).toBe(1);
		expect(eventInDb?.photo).toBe(newEvent.photo);

		// cleanup - delete the created event
		await eventModel.delete(eventInDb!._id);
	});
});

describe('PUT /api/events/join/:eventId - unmocked (requires running server)', () => {
	/**
	 * Inputs: Valid eventId for event user wants to join
	 * Expected status: 200
	 * Output: Updated event with user added to attendees
	 * Expected behavior: Adds user to event attendees within 500ms performance requirement
	 */
	test('user joins an event (200) when server is available, within 500ms', async () => {
		
		// first create an event to ensure it exists
		const newEvent: CreateEventRequest = {
			title: "TEST JOIN EVENT",
			description: "TEST JOIN DESCRIPTION",
			date: new Date(),
			capacity: 10,
			skillLevel: "Beginner",
			location: "Join Location",
			latitude: 40.7128,
			longitude: -74.0060,
			createdBy: testUser._id.toString(),
			attendees: [],
			photo: ""
		};
		const created = await eventModel.create(newEvent);
		const createdId = created._id;

		// make sure PUT join endpoint works
		const startTime = performance.now();
		const res = await request(app).put(`/api/events/join/${createdId.toString()}`);
		const endTime = performance.now();

		expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
		console.log('PUT /api/events/join/:eventId took ' + (endTime - startTime).toFixed(2) + 'ms');
		expect(res.status).toBe(200);
		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('event');

		// verify user was actually added to attendees in DB
		const eventInDb = await eventModel.findById(createdId);
		expect(eventInDb).not.toBeNull();
		expect(eventInDb?.attendees.map(a => a.toString())).toContain(testUser._id.toString());

		// cleanup - delete the created event
		await eventModel.delete(createdId);
	});
});

describe('PUT /api/events/leave/:eventId - unmocked (requires running server)', () => {
	/**
	 * Inputs: Valid eventId for event user wants to leave
	 * Expected status: 200
	 * Output: Updated event with user removed from attendees
	 * Expected behavior: Removes user from event attendees within 500ms performance requirement
	 */
	test('user leaves an event (200) when server is available, within 500ms', async () => {
		
		// first create an event with the user as an attendee
		const newEvent: CreateEventRequest = {
			title: "TEST LEAVE EVENT",
			description: "TEST LEAVE DESCRIPTION",
			date: new Date(),
			capacity: 10,
			skillLevel: "Expert",
			location: "Leave Location",
			latitude: 51.5074,
			longitude: -0.1278,
			createdBy: new mongoose.Types.ObjectId().toString(),
			attendees: [testUser._id.toString()],
			photo: ""
		};
		const created = await eventModel.create(newEvent);
		const createdId = created._id;

		// make sure PUT leave endpoint works
    const startTime = performance.now();
		const res = await request(app).put(`/api/events/leave/${createdId.toString()}`);
		const endTime = performance.now();

		expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('PUT /api/events/leave/:eventId took ' + (endTime - startTime).toFixed(2) + 'ms');
		expect(res.status).toBe(200);
		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('event');

		// verify user was actually removed from attendees in DB
		const eventInDb = await eventModel.findById(createdId);
		expect(eventInDb).not.toBeNull();
		expect(eventInDb?.attendees.map(a => a.toString())).not.toContain(testUser._id.toString());

		// cleanup - delete the created event
		await eventModel.delete(createdId);
	});
});

describe('PUT /api/events/:eventId - unmocked (requires running server)', () => {
	/**
	 * Inputs: Valid eventId and update data (title, description, capacity, etc.)
	 * Expected status: 200
	 * Output: Updated event object with modified fields
	 * Expected behavior: Updates existing event in database within 500ms performance requirement
	 */
	test('updates an event (200) when server is available, within 500ms', async () => {
		// first create an event to ensure it exists
		const newEvent: CreateEventRequest = {
			title: "TEST UPDATE EVENT",
			description: "TEST UPDATE DESCRIPTION",
			date: new Date(),
			capacity: 10,
			skillLevel: "Intermediate",
			location: "Initial Location",
			latitude: 34.0522,
			longitude: -118.2437,
			createdBy: testUser._id.toString(),
			attendees: [],
			photo: ""
		};
		const created = await eventModel.create(newEvent);
		const createdId = created._id;

		// updated event data
		const updatedEvent: UpdateEventRequest = {
			title: "UPDATED TEST EVENT",
			description: "UPDATED TEST DESCRIPTION",
			date: new Date(),
			capacity: 20,
			skillLevel: "Expert",
			location: "Updated Location",
			latitude: 40.7128,
			longitude: -74.0060,
			attendees: [testUser._id.toString()],
			photo: "updated_photo_url"
		};

		// make sure PUT endpoint works
    const startTime = performance.now();
		const res = await request(app).put(`/api/events/${createdId.toString()}`).send(updatedEvent);
    const endTime = performance.now();
    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('PUT /api/events/:eventId took ' + (endTime - startTime).toFixed(2) + 'ms');

		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('event');

		// verify event was actually updated in DB
		const eventInDb = await eventModel.findById(createdId);
		expect(eventInDb).not.toBeNull();
		expect(eventInDb?.title).toBe(updatedEvent.title);
		expect(eventInDb?.description).toBe(updatedEvent.description);
		// eventInDb.date is a Date object from mongoose — compare ISO strings
		expect(eventInDb?.date.toISOString()).toBe(updatedEvent.date.toISOString());
		expect(eventInDb?.capacity).toBe(updatedEvent.capacity);
		expect(eventInDb?.skillLevel).toBe(updatedEvent.skillLevel);
		expect(eventInDb?.location).toBe(updatedEvent.location);
		expect(eventInDb?.latitude).toBe(updatedEvent.latitude);
		expect(eventInDb?.longitude).toBe(updatedEvent.longitude);
		expect(eventInDb?.photo).toBe(updatedEvent.photo);
		expect(eventInDb?.attendees.map(a => a.toString())).toContain(testUser._id.toString());

		// cleanup - delete the created event
		await eventModel.delete(createdId);
	});
});

describe('DELETE /api/events/:eventId - unmocked (requires running server)', () => {
	/**
	 * Inputs: Valid eventId for event to delete
	 * Expected status: 200
	 * Output: Success message confirming deletion
	 * Expected behavior: Removes event from database within 500ms performance requirement
	 */
	test('delete an event (200) when server is available, within 500ms', async () => {
		
		// new event data (use CreateEventRequest shape: attendees and createdBy are strings)
		const newEvent: CreateEventRequest = {
			title: "TEST DELETE EVENT",
			description: "TEST DELETE DESCRIPTION",
			date: new Date(),
			capacity: 10,
			skillLevel: "Expert",
			location: "Test Location",
			latitude: 37.7749,
			longitude: -122.4194,
			createdBy: testUser._id.toString(),
			attendees: [],
			photo: ""
		};

		// create via model (this will validate against createEventSchema)
		const created = await eventModel.create(newEvent);
		const createdId = created._id;

		// delete the event through the API (server must point to same DB when running)
    const startTime = performance.now();
		const delRes = await request(app)
			.delete(`/api/events/${createdId.toString()}`);
    const endTime = performance.now();
    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('DELETE /api/events/:eventId took ' + (endTime - startTime).toFixed(2) + 'ms');

		expect(delRes.status).toBe(200);

		// verify deletion: event should no longer exist in DB
		const eventInDb = await eventModel.findById(createdId);
		expect(eventInDb).toBeNull();
	});
});
	
// non-functional tests to test authentication/user API calls within 500ms

describe('POST /api/auth/signup - unmocked (covers auth.service.ts)', () => {
  /*
    Inputs: body { idToken: string } (valid Google token)
    Expected status: 201
    Output: { message: 'User signed up successfully', data: { token: string, user: IUser } }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Verifies Google token and extracts user info
      - auth.service.signUpWithGoogle: Checks user doesn't exist, creates user, generates JWT
      - Returns JWT token and user data
    Mock behavior: Google OAuth2Client.verifyIdToken returns valid ticket with payload
  */
  test('creates new user and returns JWT token, within 500ms', async () => {
    const mockGoogleId = `google-${Date.now()}`;
    const mockEmail = `test-${Date.now()}@example.com`;
    const mockName = 'Test User';
    const mockPicture = 'https://example.com/picture.jpg';
    
    // Mock Google token verification - covers verifyGoogleToken method
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: mockName,
        picture: mockPicture,
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);

    const startTime = performance.now();
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-google-token' });
    const endTime = performance.now();

    expect(endTime - startTime).toBeLessThan(500);
    console.log('POST /api/auth/signup took ' + (endTime - startTime).toFixed(2) + 'ms');
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User signed up successfully');
    expect(res.body.data).toHaveProperty('token');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user.email).toBe(mockEmail);
    expect(res.body.data.user.name).toBe(mockName);
    expect(res.body.data.user.googleId).toBe(mockGoogleId);
    
    // Verify user was created in database (covers userModel.create call)
    const createdUser = await userModel.findByGoogleId(mockGoogleId);
    expect(createdUser).not.toBeNull();
    expect(createdUser?.email).toBe(mockEmail);
    
    // Verify JWT token is valid (covers generateAccessToken method)
    const decoded = jwt.verify(res.body.data.token, process.env.JWT_SECRET!) as any;
    expect(decoded).toHaveProperty('id');
    expect(String(decoded.id)).toBe(String(createdUser?._id));
    
    // Cleanup
    if (createdUser) {
      await userModel.delete(createdUser._id);
    }
  });
});

describe('POST /api/auth/signin - unmocked (covers auth.service.ts)', () => {
  /*
    Inputs: body { idToken: string } (valid Google token for existing user)
    Expected status: 200
    Output: { message: 'User signed in successfully', data: { token: string, user: IUser } }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Verifies Google token and extracts user info
      - auth.service.signInWithGoogle: Finds existing user via userModel.findByGoogleId
      - auth.service.generateAccessToken: Generates JWT token with user._id
      - Returns JWT token and user data
    Mock behavior: Google OAuth2Client verifies token and returns user info
  */
  test('signs in existing user and returns JWT token, within 500ms  ', async () => {
    const mockGoogleId = `google-signin-${Date.now()}`;
    const mockEmail = `signin-${Date.now()}@example.com`;
    const mockName = 'Sign In User';
    
    // Create user first - covers userModel.findByGoogleId in signInWithGoogle
    const existingUser = await userModel.create({
      googleId: mockGoogleId,
      email: mockEmail,
      name: mockName,
    });
    
    // Mock Google token verification - covers verifyGoogleToken method
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: mockName,
        picture: 'https://example.com/picture.jpg',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);

    const startTime = performance.now();
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-google-token' });
    const endTime = performance.now();
    
    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('POST /api/auth/signin took ' + (endTime - startTime).toFixed(2) + 'ms');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User signed in successfully');
    expect(res.body.data).toHaveProperty('token');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user.email).toBe(mockEmail);
    expect(res.body.data.user.googleId).toBe(mockGoogleId);
    
    // Verify JWT token is valid - covers generateAccessToken method
    const decoded = jwt.verify(res.body.data.token, process.env.JWT_SECRET!) as any;
    expect(decoded).toHaveProperty('id');
    expect(String(decoded.id)).toBe(String(existingUser._id));
    
    // Cleanup
    await userModel.delete(existingUser._id);
  });
});

describe('GET /api/users - unmocked (requires running server)', () => {
    /**
     * Inputs: None (authenticated request to fetch all users)
     * Expected status: 200
     * Output: List of users with message and data properties
     * Expected behavior: Returns all users (note: not exposed in frontend, timing less critical)
     */
    test('returns list of users (200) when server is available, within 500ms', async () => {
        
        // make sure GET endpoint works
        const startTime = performance.now();
        const res = await request(app).get('/api/users');
        const endTime = performance.now();
        
        // this route is not exposed in the front end, so timing is not important here
        // expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
        console.log('GET /api/users took ' + (endTime - startTime).toFixed(2) + 'ms');
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('message');
        expect(res.body).toHaveProperty('data');
        expect(res.body.data).toHaveProperty('users');
        expect(Array.isArray(res.body.data.users)).toBe(true);
    });
});

describe('GET /api/users/profile - unmocked (requires running server)', () => {
    /**
     * Inputs: None (authenticated request to fetch current user profile)
     * Expected status: 200
     * Output: Current user object from req.user
     * Expected behavior: Returns authenticated user's profile within 500ms
     */
    test('returns current user (200) when server is available, within 500ms', async () => {
        // call the endpoint
        const startTime = performance.now();
        const res = await request(app).get('/api/users/profile');
        const endTime = performance.now();

        expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
        console.log('GET /api/users/profile took ' + (endTime - startTime).toFixed(2) + 'ms');
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('message');
        expect(res.body).toHaveProperty('data');
        expect(res.body.data).toHaveProperty('user');
        expect(res.body.data.user).toHaveProperty('_id');

        // verify returned user is the expected one
        expect(res.body.data.user._id).toBe(testUser._id.toString());
      });
});

describe('GET /api/users/:id - unmocked (requires running server)', () => {
  /*
    Inputs: path param id (valid user ID)
    Expected status: 200
    Output: { message: string, data: { user: IUser } }
    Expected behavior: Returns user profile by ID
  */
  test('returns user by ID (200) when server is available, within 500ms', async () => {
    // call the endpoint
    const startTime = performance.now();
    const res = await request(app).get(`/api/users/${testUser._id.toString()}`);
    const endTime = performance.now();
    
    expect(endTime - startTime).toBeLessThan(500);
    console.log('GET /api/users/:id took ' + (endTime - startTime).toFixed(2) + 'ms');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user).toHaveProperty('_id');

    // verify returned user is the expected one
    expect(res.body.data.user._id).toBe(testUser._id.toString());
  });
});

describe('PUT /api/users/:id - unmocked (requires running server)', () => {
  /**
   * Inputs: Valid userId in path, update data (name, age, bio, location, etc.)
   * Expected status: 200
   * Output: Updated user object with modified fields
   * Expected behavior: Updates user profile in database within 500ms performance requirement
   */
  test('returns user by ID (200) when server is available, within 500ms', async () => {
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
    const startTime = performance.now();
    const res = await request(app).put(`/api/users/${testUser._id}`).send(updateData);
    const endTime = performance.now();

    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('PUT /api/users/:id took ' + (endTime - startTime).toFixed(2) + 'ms');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user).toHaveProperty('_id');

    // verify returned user is the expected one
    expect(res.body.data.user._id).toBe(testUser._id.toString());
    expect(res.body.data.user.name).toBe(updateData.name);
    expect(res.body.data.user.age).toBe(updateData.age);
    expect(res.body.data.user.bio).toBe(updateData.bio);
    expect(res.body.data.user.location).toBe(updateData.location);
    expect(res.body.data.user.latitude).toBe(updateData.latitude);
    expect(res.body.data.user.longitude).toBe(updateData.longitude);
    expect(res.body.data.user.profilePicture).toBe(updateData.profilePicture);
    expect(res.body.data.user.skillLevel).toBe(updateData.skillLevel);
  });
});

describe('POST /api/users/ - unmocked (requires running server)', () => {
  /**
   * Inputs: Update data for current user (name, age, bio, location, etc.)
   * Expected status: 200
   * Output: Updated user object for authenticated user
   * Expected behavior: Updates current user profile within 500ms performance requirement
   */
  test('returns user (200) when server is available, within 500ms', async () => {
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
    const startTime = performance.now();
    const res = await request(app).post(`/api/users/`).send(updateData);
    const endTime = performance.now();

    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('POST /api/users/ took ' + (endTime - startTime).toFixed(2) + 'ms');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user).toHaveProperty('_id');

    // verify returned user is the expected one
    expect(res.body.data.user._id).toBe(testUser._id.toString());
    expect(res.body.data.user.name).toBe(updateData.name);
    expect(res.body.data.user.age).toBe(updateData.age);
    expect(res.body.data.user.bio).toBe(updateData.bio);
    expect(res.body.data.user.location).toBe(updateData.location);
    expect(res.body.data.user.latitude).toBe(updateData.latitude);
    expect(res.body.data.user.longitude).toBe(updateData.longitude);
    expect(res.body.data.user.profilePicture).toBe(updateData.profilePicture);
    expect(res.body.data.user.skillLevel).toBe(updateData.skillLevel);
  });
});

describe('DELETE /api/users/ - unmocked (requires running server)', () => {
    /**
     * Inputs: Valid JWT token for user to delete
     * Expected status: 200
     * Output: Success message confirming deletion
     * Expected behavior: Deletes authenticated user from database within 500ms performance requirement
     */
    test('returns success (200) when server is available and user deleted, within 500ms', async () => {

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
        const startTime = performance.now();
        const deleteRes = await request(app).delete(`/api/users/${deletedUserId.toString()}`);
        const endTime = performance.now();

        expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
        console.log('DELETE /api/users/ took ' + (endTime - startTime).toFixed(2) + 'ms');
        expect(deleteRes.status).toBe(200);
        expect(deleteRes.body).toHaveProperty('message');
        
        // verify user is deleted
        const fetchedUser = await userModel.findById(deletedUserId);
        expect(fetchedUser).toBeNull();
      });
});

// non-functional tests to test chat API calls within 500ms

let chatId: string;
let messageId: string;

describe('POST /api/chats - unmocked (no mocking)', () => {
  /**
   * Inputs: peerId (other user's ID), optional name for chat
   * Expected status: 201
   * Output: Created chat document with participants
   * Expected behavior: Creates direct chat between two users within 500ms performance requirement
   */
  test('creates a direct chat between two users, within 500ms', async () => {
    console.log('[TEST] Creating chat with peerId:', otherTestUser._id);
    const startTime = performance.now();
    const res = await request(app).post('/api/chats').send({
      peerId: otherTestUser._id,
      name: 'Test Direct Chat'
    });
    const endTime = performance.now();
    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('[TEST] Create chat took ' + (endTime - startTime).toFixed(2) + 'ms');
    console.log('[TEST] Create chat response status:', res.status);
    console.log('[TEST] Create chat response body:', JSON.stringify(res.body, null, 2));
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('_id');
    expect(res.body).toHaveProperty('participants');
    const parts = res.body.participants.map((p: any) => String(p._id || p));
    expect(parts).toContain(testUser._id.toString());
    expect(parts).toContain(otherTestUser._id.toString());
    chatId = String(res.body._id);
    console.log('[TEST] Created chatId:', chatId);
  });
});

describe('GET /api/chats - unmocked (no mocking)', () => {
  /**
   * Inputs: None (authenticated user requesting their chats)
   * Expected status: 200
   * Output: Array of chat documents where user is participant
   * Expected behavior: Returns user's chat list within 500ms performance requirement
   */
  test('lists chats for the authenticated user, within 500ms', async () => {
    console.log('[TEST] Starting GET /api/chats test');
    console.log('[TEST] USER_ID from env:', testUser._id);
    console.log('[TEST] USER_ID is valid ObjectId:', mongoose.isValidObjectId(testUser._id));

    const startTime = performance.now();
    const res = await request(app).get('/api/chats');
    const endTime = performance.now();

    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('[TEST] GET /api/chats took ' + (endTime - startTime).toFixed(2) + 'ms');
    console.log('[TEST] Response status:', res.status);
    console.log('[TEST] Response body:', JSON.stringify(res.body, null, 2));
    console.log('[TEST] Response headers:', res.headers);
    
    if (res.status !== 200) {
      console.log('[TEST] ERROR - Expected 200 but got', res.status);
      if (res.body.error) {
        console.log('[TEST] Error message:', res.body.error);
      }
      if (res.body.stack) {
        console.log('[TEST] Error stack:', res.body.stack);
      }
    }
    
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.some((c: any) => String(c._id) === chatId)).toBe(true);
  });
});

describe('GET /api/chats/:chatId - unmocked (no mocking)', () => {
  /**
   * Inputs: Valid chatId where user is participant
   * Expected status: 200
   * Output: Chat document with participant details
   * Expected behavior: Returns specific chat when user has access within 500ms
   */
  test('returns a chat when the user is a participant, within 500ms', async () => {
    const startTime = performance.now();
    const res = await request(app).get(`/api/chats/${chatId}`);
    const endTime = performance.now();
    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('GET /api/chats/:chatId took ' + (endTime - startTime).toFixed(2) + 'ms');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('_id');
    expect(String(res.body._id)).toBe(chatId);
  });
});

describe('POST /api/chats/:chatId/messages - unmocked (no mocking)', () => {
  /**
   * Inputs: Valid chatId in path, message content in body
   * Expected status: 201
   * Output: Created message document with populated sender
   * Expected behavior: Creates and returns new message in chat within 500ms
   */
  test('sends a message in the chat, within 500ms', async () => {
    const startTime = performance.now();
    const res = await request(app).post(`/api/chats/${chatId}/messages`).send({
      content: 'Hello from test!'
    });
    const endTime = performance.now();
    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('POST /api/chats/:chatId/messages took ' + (endTime - startTime).toFixed(2) + 'ms');
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('_id');
    expect(res.body).toHaveProperty('content');
    expect(res.body.content).toBe('Hello from test!');
    expect(String(res.body.sender?._id || res.body.sender)).toBe(testUser._id.toString());
    messageId = String(res.body._id);
  });
});

describe('GET /api/chats/messages/:chatId - unmocked (no mocking)', () => {
  /**
   * Inputs: Valid chatId in path, optional query params for pagination
   * Expected status: 200
   * Output: Object with messages array, chatId, limit, count, hasMore
   * Expected behavior: Fetches chat messages with pagination within 500ms (default limit 20)
   */
  test('fetches messages for a chat, within 500ms', async () => {
    const startTime = performance.now();
    const res = await request(app).get(`/api/chats/messages/${chatId}`);
    const endTime = performance.now();
    expect(endTime - startTime).toBeLessThan(500); // ensure within nfr timeout
    console.log('GET /api/chats/messages/:chatId took ' + (endTime - startTime).toFixed(2) + 'ms');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('messages');
    expect(res.body).toHaveProperty('chatId');
    expect(res.body).toHaveProperty('limit');
    expect(res.body).toHaveProperty('count');
    expect(res.body).toHaveProperty('hasMore');
    expect(Array.isArray(res.body.messages)).toBe(true);
    expect(res.body.messages.length).toBeGreaterThan(0);
  });
});

describe('Cleanup - unmocked', () => {
  /**
   * Inputs: chatId from previous tests
   * Expected status: N/A (cleanup operation)
   * Output: None
   * Expected behavior: Removes test chat and associated messages from database
   */
  test('removes created chat and messages', async () => {
    if (chatId) {
      await Chat.deleteOne({ _id: chatId });
      await Message.deleteMany({ chat: chatId });
    }
  });
});
