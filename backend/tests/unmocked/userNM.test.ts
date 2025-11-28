import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { eventModel } from '../../src/models/event.model';
import { CreateEventRequest, UpdateEventRequest } from '../../src/types/event.types';
import { CreateUserRequest, UpdateProfileRequest } from '../../src/types/user.types';
import { userModel } from '../../src/models/user.model';
import { blockModel } from '../../src/models/block.model';
import { AuthenticateUserRequest } from '../../src/types/auth.types';
import express from 'express';
import userRoutes from '../../src/routes/user.routes';
import authRoutes from '../../src/routes/auth.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import { authenticateToken } from '../../src/middleware/auth.middleware';
import mongoose from 'mongoose';

dotenv.config();

// Test user will be created dynamically
let testUser: any = null;

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
    
    if (testUser) {
        req.user = { 
            _id: testUser._id,
            email: testUser.email,
            name: testUser.name
        };
    }
    next();
});

app.use('/api/users', userRoutes); // Mount user routes at /api/users
app.use('/api/auth', authRoutes); // Mount auth routes at /api/auth
app.use('*', notFoundHandler);
app.use(errorHandler);

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

describe('GET /api/users - unmocked (requires running server)', () => {
    /**
     * Inputs: None (authenticated request)
     * Expected status: 200
     * Output: Object with message and data.users array
     * Expected behavior: Returns list of all users from database
     */
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
    /**
     * Inputs: None (authenticated request with req.user)
     * Expected status: 200
     * Output: Object with message and data.user containing current user profile
     * Expected behavior: Returns authenticated user's profile information
     */
    test('returns current user (200) when server is available', async () => {
        // call the endpoint
        const res = await request(app).get('/api/users/profile');//.set('Authorization', `Bearer ${TOKEN}`);
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
  test('returns user by ID (200) when server is available', async () => {
    // call the endpoint
    const res = await request(app).get(`/api/users/${testUser._id.toString()}`);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('user');
    expect(res.body.data.user).toHaveProperty('_id');

    // verify returned user is the expected one
    expect(res.body.data.user._id).toBe(testUser._id.toString());
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

  /*
    Inputs: path param id (valid user ID who has blocked the requesting user)
    Expected status: 200
    Output: { message: 'Profile fetched successfully', data: { user: anonymousUser } }
    Expected behavior: Returns anonymous user data (name: 'Unknown User', no bio, age, skillLevel, location, etc.) when requesting user is blocked
  */
  test('returns anonymous user data when requesting user is blocked by target user', async () => {
    // Create another user who will block testUser
    const blockerData: CreateUserRequest = {
      email: `blocker-${Date.now()}@example.com`,
      name: 'Blocker User',
      googleId: `blocker-google-${Date.now()}`,
      age: 28,
      profilePicture: 'http://example.com/blocker.jpg',
      bio: 'This is my private bio',
      location: 'Vancouver, BC',
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: 'Expert',
      eventsCreated: [],
      eventsJoined: []
    };
    const blocker = await userModel.create(blockerData);

    // Blocker blocks testUser using blockUser method
    await blockModel.blockUser(blocker._id, testUser._id);

    // TestUser tries to access blocker's profile
    const res = await request(app).get(`/api/users/${blocker._id.toString()}`);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Profile fetched successfully');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('user');

    // Verify anonymous user data is returned
    const user = res.body.data.user;
    expect(user._id).toBe(blocker._id.toString());
    expect(user.name).toBe('Unknown User'); // Anonymous name
    expect(user.bio).toBeUndefined(); // Personal info hidden
    expect(user.profilePicture).toBeUndefined();
    expect(user.age).toBeUndefined();
    expect(user.skillLevel).toBeUndefined();
    expect(user.location).toBeUndefined();
    expect(user.latitude).toBeUndefined();
    expect(user.longitude).toBeUndefined();
    // Public fields should still be present
    expect(user.googleId).toBe(blocker.googleId);
    expect(user.email).toBe(blocker.email);

    // Cleanup - unblock using unblockUser method
    await blockModel.unblockUser(blocker._id, testUser._id);
    await userModel.delete(blocker._id);
  });
});

describe('PUT /api/users/:id - unmocked (requires running server)', () => {
  /**
   * Inputs: Path param id (valid user ID), body UpdateProfileRequest with all fields
   * Expected status: 200
   * Output: Object with message and data.user containing updated user profile
   * Expected behavior: Updates all user profile fields and returns updated user
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
    const res = await request(app).put(`/api/users/${testUser._id.toString()}`).send(updateData);
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

  /**
   * Inputs: Path param id, body UpdateProfileRequest with only name field
   * Expected status: 200
   * Output: Object with message and data.user with updated name
   * Expected behavior: Updates only specified fields, leaves others unchanged
   */
  test('updates user with partial data', async () => {
    const partialUpdate: UpdateProfileRequest = {
      name: "Partially Updated Name"
    };
    
    const res = await request(app).put(`/api/users/${testUser._id.toString()}`).send(partialUpdate);
    expect(res.status).toBe(200);
    expect(res.body.data.user.name).toBe(partialUpdate.name);
    // Other fields should remain unchanged or be optional
  });

  /**
   * Inputs: Path param 'invalid-id', body UpdateProfileRequest
   * Expected status: 400
   * Output: Error message 'Invalid user id'
   * Expected behavior: Rejects request with invalid user ID format
   */
  test('returns 400 when user ID is invalid', async () => {
    const updateData: UpdateProfileRequest = { name: "Test" };
    const res = await request(app).put('/api/users/invalid-id').send(updateData);
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid user id');
  });

  /**
   * Inputs: Path param id (valid ObjectId but non-existent user), body UpdateProfileRequest
   * Expected status: 404
   * Output: Error message 'User not found'
   * Expected behavior: Returns 404 when attempting to update non-existent user
   */
  test('returns 404 when user does not exist', async () => {
    const fakeId = new mongoose.Types.ObjectId().toString();
    const updateData: UpdateProfileRequest = { name: "Test" };
    const res = await request(app).put(`/api/users/${fakeId}`).send(updateData);
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User not found');
  });

  /**
   * Inputs: Path param id, body UpdateProfileRequest with each skill level (Beginner, Intermediate, Expert)
   * Expected status: 200
   * Output: Object with message and data.user with updated skillLevel for each iteration
   * Expected behavior: Successfully updates user with all valid skill level values
   */
  test('updates user with all skill levels', async () => {
    const skillLevels = ['Beginner', 'Intermediate', 'Expert'] as const;
    
    for (const skillLevel of skillLevels) {
      const updateData: UpdateProfileRequest = { skillLevel };
      const res = await request(app).put(`/api/users/${testUser._id.toString()}`).send(updateData);
      expect(res.status).toBe(200);
      expect(res.body.data.user.skillLevel).toBe(skillLevel);
    }
  });
});

describe('POST /api/users/ - unmocked (requires running server)', () => {
  /**
   * Inputs: Body UpdateProfileRequest with all fields (authenticated request)
   * Expected status: 200
   * Output: Object with message and data.user containing updated user profile
   * Expected behavior: Updates current user's profile with all provided fields
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

  /**
   * Inputs: Body UpdateProfileRequest with only bio field
   * Expected status: 200
   * Output: Object with message and data.user with updated bio
   * Expected behavior: Updates only specified field (bio) for current user
   */
  test('updates user with partial data', async () => {
    const partialUpdate: UpdateProfileRequest = {
      bio: "Updated bio only"
    };
    
    const res = await request(app).post(`/api/users/`).send(partialUpdate);
    expect(res.status).toBe(200);
    expect(res.body.data.user.bio).toBe(partialUpdate.bio);
  });

  /**
   * Inputs: Body UpdateProfileRequest with each skill level (Beginner, Intermediate, Expert)
   * Expected status: 200
   * Output: Object with message and data.user with updated skillLevel for each iteration
   * Expected behavior: Successfully updates current user with all valid skill level values
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

  /**
   * Inputs: Body with invalid fields (empty name, negative age, oversized bio)
   * Expected status: 400
   * Output: Error message with validation error
   * Expected behavior: Rejects invalid payload and returns validation errors
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

  /**
   * Inputs: Body UpdateProfileRequest with location, latitude, and longitude fields
   * Expected status: 200
   * Output: Object with message and data.user with updated location and coordinates
   * Expected behavior: Successfully updates user's location and geographic coordinates
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

  /**
   * Inputs: Body UpdateProfileRequest without authentication (no req.user)
   * Expected status: 401
   * Output: Error message 'Unauthorized'
   * Expected behavior: Rejects request when user is not authenticated
   */
  test('returns 401 when user is not authenticated', async () => {
    // Create a temporary app without the mock auth middleware to test unauthorized case
    const tempApp = express();
    tempApp.use(express.json());
    tempApp.use('/api/users', userRoutes);
    tempApp.use('*', notFoundHandler);
    tempApp.use(errorHandler);

    const updateData: UpdateProfileRequest = { name: "Test" };
    const res = await request(tempApp).post(`/api/users/`).send(updateData);
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Unauthorized');
  });

  /**
   * Inputs: Body UpdateProfileRequest with authenticated non-existent user
   * Expected status: 404
   * Output: Error message 'User not found'
   * Expected behavior: Returns 404 when authenticated user doesn't exist in database
   */
  test('returns 404 when user does not exist in database', async () => {
    // Create a temporary app with mock auth pointing to non-existent user
    const tempApp = express();
    tempApp.use(express.json());
    
    const fakeUserId = new mongoose.Types.ObjectId();
    tempApp.use((req: any, res, next) => {
      req.user = { 
        _id: fakeUserId,
        email: 'fake@example.com',
        name: 'Fake User'
      };
      next();
    });
    
    tempApp.use('/api/users', userRoutes);
    tempApp.use('*', notFoundHandler);
    tempApp.use(errorHandler);

    const updateData: UpdateProfileRequest = { name: "Test" };
    const res = await request(tempApp).post(`/api/users/`).send(updateData);
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User not found');
  });
});

describe('DELETE /api/users/:id - unmocked (requires running server)', () => {
  /**
   * Inputs: Path param id (valid user ID)
   * Expected status: 200
   * Output: Success message 'User deleted successfully'
   * Expected behavior: Deletes user from database and removes all associated images
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

  /**
   * Inputs: Path param 'invalid-id'
   * Expected status: 400
   * Output: Error message 'Invalid user id'
   * Expected behavior: Rejects deletion request with invalid user ID format
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

  /**
   * Inputs: Path param id (user who joined events)
   * Expected status: 200
   * Output: Success message 'User deleted successfully'
   * Expected behavior: Deletes user and removes them from all events' attendees arrays
   */
  test('deletes user who joined events and removes from attendees', async () => {
    // Create an event creator
    const creatorData: CreateUserRequest = {
      email: `creator-${Date.now()}@example.com`,
      name: "Event Creator",
      googleId: `creator-google-${Date.now()}`,
      age: 30,
      profilePicture: "",
      bio: "Creator bio",
      location: "Vancouver, BC",
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: "Expert"
    };
    const creator = await userModel.create(creatorData);

    // Create an event
    const eventData: CreateEventRequest = {
      title: "Test Dive Event",
      location: "Vancouver Island",
      latitude: 49.0,
      longitude: -123.5,
      date: new Date(Date.now() + 86400000),
      description: "A test dive event",
      capacity: 5,
      skillLevel: "Intermediate",
      createdBy: creator._id.toString(),
      attendees: []
    };
    const event = await eventModel.create(eventData);

    // Create a user who will join the event
    const joinerData: CreateUserRequest = {
      email: `joiner-${Date.now()}@example.com`,
      name: "Event Joiner",
      googleId: `joiner-google-${Date.now()}`,
      age: 25,
      profilePicture: "http://example.com/joiner.jpg",
      bio: "Joiner bio",
      location: "Vancouver, BC",
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: "Beginner",
      eventsJoined: [event._id.toString()]
    };
    const joiner = await userModel.create(joinerData);

    // Add joiner to event's attendees
    const eventToUpdate = await eventModel.findById(event._id);
    await eventModel.update(event._id, { 
      title: eventToUpdate!.title,
      description: eventToUpdate!.description,
      date: eventToUpdate!.date,
      capacity: eventToUpdate!.capacity,
      skillLevel: eventToUpdate!.skillLevel,
      location: eventToUpdate!.location,
      latitude: eventToUpdate!.latitude,
      longitude: eventToUpdate!.longitude,
      attendees: [joiner._id.toString()],
      photo: eventToUpdate!.photo
    } as any);

    // Delete the joiner
    const res = await request(app).delete(`/api/users/${joiner._id.toString()}`);
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('User deleted successfully');

    // Verify joiner is deleted
    const fetchedJoiner = await userModel.findById(joiner._id);
    expect(fetchedJoiner).toBeNull();

    // Verify joiner is removed from event attendees
    const updatedEvent = await eventModel.findById(event._id);
    expect(updatedEvent).not.toBeNull();
    expect(updatedEvent!.attendees).toEqual([]);

    // Cleanup
    await eventModel.delete(event._id);
    await userModel.delete(creator._id);
  });

  /**
   * Inputs: Path param id (user who created events with attendees)
   * Expected status: 200
   * Output: Success message 'User deleted successfully'
   * Expected behavior: Deletes user, deletes their created events, and removes events from attendees' eventsJoined
   */
  test('deletes user who created events and cleans up attendees', async () => {
    // Create a host who will create events
    const hostData: CreateUserRequest = {
      email: `host-${Date.now()}@example.com`,
      name: "Event Host",
      googleId: `host-google-${Date.now()}`,
      age: 35,
      profilePicture: "http://example.com/host.jpg",
      bio: "Host bio",
      location: "Vancouver, BC",
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: "Expert"
    };
    const host = await userModel.create(hostData);

    // Create an attendee who will join the event
    const attendeeData: CreateUserRequest = {
      email: `attendee-${Date.now()}@example.com`,
      name: "Event Attendee",
      googleId: `attendee-google-${Date.now()}`,
      age: 28,
      profilePicture: "http://example.com/attendee.jpg",
      bio: "Attendee bio",
      location: "Vancouver, BC",
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: "Intermediate"
    };
    const attendee = await userModel.create(attendeeData);

    // Create an event by the host with the attendee
    const eventData: CreateEventRequest = {
      title: "Host's Dive Event",
      location: "Vancouver Island",
      latitude: 49.0,
      longitude: -123.5,
      date: new Date(Date.now() + 86400000),
      description: "A test dive event created by host",
      capacity: 5,
      skillLevel: "Expert",
      createdBy: host._id.toString(),
      attendees: [attendee._id.toString()]
    };
    const event = await eventModel.create(eventData);

    // Add event to host's eventsCreated and attendee's eventsJoined
    const hostToUpdate = await userModel.findById(host._id);
    await userModel.update(host._id, { 
      ...hostToUpdate!,
      eventsCreated: [event._id.toString()],
      eventsJoined: hostToUpdate!.eventsJoined.map(id => id.toString())
    } as any);
    const attendeeToUpdate = await userModel.findById(attendee._id);
    await userModel.update(attendee._id, { 
      ...attendeeToUpdate!,
      eventsJoined: [event._id.toString()],
      eventsCreated: attendeeToUpdate!.eventsCreated.map(id => id.toString())
    } as any);

    // Delete the host
    const res = await request(app).delete(`/api/users/${host._id.toString()}`);
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('User deleted successfully');

    // Verify host is deleted
    const fetchedHost = await userModel.findById(host._id);
    expect(fetchedHost).toBeNull();

    // Verify host's event is deleted
    const fetchedEvent = await eventModel.findById(event._id);
    expect(fetchedEvent).toBeNull();

    // Verify attendee's eventsJoined no longer contains the deleted event
    const updatedAttendee = await userModel.findById(attendee._id);
    expect(updatedAttendee).not.toBeNull();
    expect(updatedAttendee!.eventsJoined).toEqual([]);

    // Cleanup
    await userModel.delete(attendee._id);
  });
});

describe('DELETE /api/users/ - unmocked (requires running server)', () => {
    /**
     * Inputs: DELETE request with valid JWT token
     * Expected status: 200
     * Output: Success message 'User deleted successfully'
     * Expected behavior: Deletes authenticated user from database
     */
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

    /**
     * Inputs: DELETE request with valid token (user who joined events)
     * Expected status: 200
     * Output: Success message 'User deleted successfully'
     * Expected behavior: Deletes authenticated user and removes them from all events' attendees arrays
     */
    test('deletes user who joined events and removes from attendees', async () => {
      // Create an event creator
      const creatorData: CreateUserRequest = {
        email: `creator-self-${Date.now()}@example.com`,
        name: "Event Creator Self",
        googleId: `creator-self-google-${Date.now()}`,
        age: 30,
        profilePicture: "http://example.com/creator-self.jpg",
        bio: "Creator bio",
        location: "Vancouver, BC",
        latitude: 49.2827,
        longitude: -123.1207,
        skillLevel: "Expert"
      };
      const creator = await userModel.create(creatorData);

      // Create an event
      const eventData: CreateEventRequest = {
        title: "Self Delete Test Event",
        location: "Vancouver Island",
        latitude: 49.0,
        longitude: -123.5,
        date: new Date(Date.now() + 86400000),
        description: "A test dive event for self deletion",
        capacity: 5,
        skillLevel: "Intermediate",
        createdBy: creator._id.toString(),
        attendees: []
      };
      const event = await eventModel.create(eventData);

      // Create a user who will join the event
      const joinerData: CreateUserRequest = {
        email: `joiner-self-${Date.now()}@example.com`,
        name: "Event Joiner Self",
        googleId: `joiner-self-google-${Date.now()}`,
        age: 25,
        profilePicture: "http://example.com/joiner-self.jpg",
        bio: "Joiner bio",
        location: "Vancouver, BC",
        latitude: 49.2827,
        longitude: -123.1207,
        skillLevel: "Beginner",
        eventsJoined: []
      };
      const joiner = await userModel.create(joinerData);

      // Add joiner to event's attendees and event to joiner's eventsJoined
      const eventToUpdate = await eventModel.findById(event._id);
      await eventModel.update(event._id, { 
        title: eventToUpdate!.title,
        description: eventToUpdate!.description,
        date: eventToUpdate!.date,
        capacity: eventToUpdate!.capacity,
        skillLevel: eventToUpdate!.skillLevel,
        location: eventToUpdate!.location,
        latitude: eventToUpdate!.latitude,
        longitude: eventToUpdate!.longitude,
        attendees: [joiner._id.toString()],
        photo: eventToUpdate!.photo
      } as any);
      const joinerToUpdate = await userModel.findById(joiner._id);
      await userModel.update(joiner._id, { 
        ...joinerToUpdate!,
        eventsJoined: [event._id.toString()],
        eventsCreated: joinerToUpdate!.eventsCreated.map(id => id.toString())
      } as any);

      // Generate token for joiner
      const jwt = require('jsonwebtoken');
      const token = jwt.sign({ id: joiner._id }, process.env.JWT_SECRET!, {
        expiresIn: '1h',
      });

      // Delete the joiner
      const res = await request(app).delete('/api/users').set('Authorization', `Bearer ${token}`);
      expect(res.status).toBe(200);
      expect(res.body.message).toBe('User deleted successfully');

      // Verify joiner is deleted
      const fetchedJoiner = await userModel.findById(joiner._id);
      expect(fetchedJoiner).toBeNull();

      // Verify joiner is removed from event attendees
      const updatedEvent = await eventModel.findById(event._id);
      expect(updatedEvent).not.toBeNull();
      expect(updatedEvent!.attendees).toEqual([]);

      // Cleanup
      await eventModel.delete(event._id);
      await userModel.delete(creator._id);
    });

    /**
     * Inputs: DELETE request with valid token (user who created events with attendees)
     * Expected status: 200
     * Output: Success message 'User deleted successfully'
     * Expected behavior: Deletes authenticated user, deletes their created events, and removes events from attendees' eventsJoined
     */
    test('deletes user who created events and cleans up attendees', async () => {
      // Create a host who will create events
      const hostData: CreateUserRequest = {
        email: `host-self-${Date.now()}@example.com`,
        name: "Event Host Self",
        googleId: `host-self-google-${Date.now()}`,
        age: 35,
        profilePicture: "http://example.com/host-self.jpg",
        bio: "Host bio",
        location: "Vancouver, BC",
        latitude: 49.2827,
        longitude: -123.1207,
        skillLevel: "Expert"
      };
      const host = await userModel.create(hostData);

      // Create an attendee who will join the event
      const attendeeData: CreateUserRequest = {
        email: `attendee-self-${Date.now()}@example.com`,
        name: "Event Attendee Self",
        googleId: `attendee-self-google-${Date.now()}`,
        age: 28,
        profilePicture: "http://example.com/attendee-self.jpg",
        bio: "Attendee bio",
        location: "Vancouver, BC",
        latitude: 49.2827,
        longitude: -123.1207,
        skillLevel: "Intermediate"
      };
      const attendee = await userModel.create(attendeeData);

      // Create an event by the host with the attendee
      const eventData: CreateEventRequest = {
        title: "Host Self Delete Event",
        location: "Vancouver Island",
        latitude: 49.0,
        longitude: -123.5,
        date: new Date(Date.now() + 86400000),
        description: "A test dive event created by host for self deletion",
        capacity: 5,
        skillLevel: "Expert",
        createdBy: host._id.toString(),
        attendees: [attendee._id.toString()]
      };
      const event = await eventModel.create(eventData);

      // Add event to host's eventsCreated and attendee's eventsJoined
      const hostToUpdate = await userModel.findById(host._id);
      await userModel.update(host._id, { 
        ...hostToUpdate!,
        eventsCreated: [event._id.toString()],
        eventsJoined: hostToUpdate!.eventsJoined.map(id => id.toString())
      } as any);
      const attendeeToUpdate = await userModel.findById(attendee._id);
      await userModel.update(attendee._id, { 
        ...attendeeToUpdate!,
        eventsJoined: [event._id.toString()],
        eventsCreated: attendeeToUpdate!.eventsCreated.map(id => id.toString())
      } as any);

      // Generate token for host
      const jwt = require('jsonwebtoken');
      const token = jwt.sign({ id: host._id }, process.env.JWT_SECRET!, {
        expiresIn: '1h',
      });

      // Delete the host
      const res = await request(app).delete('/api/users').set('Authorization', `Bearer ${token}`);
      expect(res.status).toBe(200);
      expect(res.body.message).toBe('User deleted successfully');

      // Verify host is deleted
      const fetchedHost = await userModel.findById(host._id);
      expect(fetchedHost).toBeNull();

      // Verify host's event is deleted
      const fetchedEvent = await eventModel.findById(event._id);
      expect(fetchedEvent).toBeNull();

      // Verify attendee's eventsJoined no longer contains the deleted event
      const updatedAttendee = await userModel.findById(attendee._id);
      expect(updatedAttendee).not.toBeNull();
      expect(updatedAttendee!.eventsJoined).toEqual([]);

      // Cleanup
      await userModel.delete(attendee._id);
    });
});
