import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { eventModel } from '../../src/models/event.model';
import { CreateEventRequest, UpdateEventRequest } from '../../src/types/event.types';
import { CreateUserRequest } from '../../src/types/user.types';
import { userModel } from '../../src/models/user.model';
import { Chat } from '../../src/models/chat.model';
import express from 'express';
import eventRoutes from '../../src/routes/event.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import mongoose from 'mongoose';

dotenv.config();

// Test user will be created dynamically
let testUser: any = null;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware - sets req.user for all routes
app.use((req: any, res, next) => {
	if (testUser) {
		req.user = { 
			_id: testUser._id,
			email: testUser.email,
			name: testUser.name
		};
	}
	next();
});

app.use('/api/events', eventRoutes); // Mount event routes at /api/events
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

describe('GET /api/events - unmocked (requires running server)', () => {
	/*
		Inputs: None (GET request to /api/events)
		Expected status: 200
		Output: { message: string, data: { events: IEvent[] } }
		Expected behavior: Returns list of all events
	*/
	test('returns list of events (200) when server is available', async () => {
		
		// make sure GET endpoint works
		const res = await request(app).get('/api/events');
		expect(res.status).toBe(200);
		expect(res.body).toHaveProperty('message');
		expect(res.body).toHaveProperty('data');
		expect(res.body.data).toHaveProperty('events');
		expect(Array.isArray(res.body.data.events)).toBe(true);
	});
});

describe('GET /api/events/:eventId - unmocked (requires running server)', () => {
	/*
		Inputs: Valid event ID as path parameter
		Expected status: 200
		Output: { message: string, data: { event: IEvent } }
		Expected behavior: Returns specific event by ID
	*/
	test('returns event by ID (200) when server is available', async () => {
		
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
		const res = await request(app).get(`/api/events/${createdId.toString()}`);

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

	/*
		Inputs: Invalid event ID format (non-ObjectId string)
		Expected status: 400
		Output: { message: string }
		Expected behavior: Returns 400 error for invalid event ID format
	*/
	test('returns 400 for invalid event ID format', async () => {
		// Try to fetch an event with an invalid ID format
		const invalidId = 'invalid-id-format';
		const res = await request(app).get(`/api/events/${invalidId}`);

		// verify error response
		expect(res.status).toBe(400);
		expect(res.body).toHaveProperty('message');
	});
});

describe('POST /api/events - unmocked (requires running server)', () => {
	/*
		Inputs: Valid CreateEventRequest in request body
		Expected status: 201
		Output: { message: string, data: { event: IEvent } }
		Expected behavior: Creates new event and adds to user's eventsCreated array
	*/
	test('creates a new event (201) when server is available', async () => {

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
		const res = (await request(app).post('/api/events').send(newEvent));
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
		expect(eventInDb?.createdBy.toString()).toBe(testUser._id.toString());
		expect(eventInDb?.attendees.length).toBe(1);
		expect(eventInDb?.photo).toBe(newEvent.photo);

		// cleanup - delete the created event
		await eventModel.delete(eventInDb!._id);
    	await Chat.deleteOne({ eventId: eventInDb!._id });
	});

	/*
		Inputs: CreateEventRequest with empty title string
		Expected status: Error thrown
		Output: Error: 'Invalid update data'
		Expected behavior: Rejects event creation with empty title
	*/
	test('returns error when creating event with invalid input - empty title', async () => {
		const invalidEvent: any = {
			title: "", // Invalid: empty string
			description: "TEST DESCRIPTION",
			date: new Date().toISOString(),
			capacity: 10,
			createdBy: testUser._id.toString(),
			attendees: []
		};

		await expect(eventModel.create(invalidEvent)).rejects.toThrow('Invalid update data');
	});

	/*
		Inputs: CreateEventRequest with negative capacity (-5)
		Expected status: Error thrown
		Output: Error: 'Invalid update data'
		Expected behavior: Rejects event creation with negative capacity
	*/
	test('returns error when creating event with invalid input - negative capacity', async () => {
		const invalidEvent: any = {
			title: "Valid Title",
			description: "Valid Description",
			date: new Date().toISOString(),
			capacity: -5, // Invalid: negative capacity
			createdBy: testUser._id.toString(),
			attendees: []
		};

		await expect(eventModel.create(invalidEvent)).rejects.toThrow('Invalid update data');
	});

	/*
		Inputs: CreateEventRequest with capacity of 0
		Expected status: Error thrown
		Output: Error: 'Invalid update data'
		Expected behavior: Rejects event creation with zero capacity (minimum is 1)
	*/
	test('returns error when creating event with invalid input - capacity of 0', async () => {
		const invalidEvent: any = {
			title: "Valid Title",
			description: "Valid Description",
			date: new Date().toISOString(),
			capacity: 0, // Invalid: capacity must be at least 1
			createdBy: testUser._id.toString(),
			attendees: []
		};

		await expect(eventModel.create(invalidEvent)).rejects.toThrow('Invalid update data');
	});

	/*
		Inputs: CreateEventRequest with title exceeding 100 characters
		Expected status: Error thrown
		Output: Error: 'Invalid update data'
		Expected behavior: Rejects event creation with title over max length
	*/
	test('returns error when creating event with invalid input - title exceeding max length', async () => {
		const invalidEvent: any = {
			title: "A".repeat(101), // Invalid: exceeds maxlength of 100
			description: "TEST DESCRIPTION",
			date: new Date().toISOString(),
			capacity: 10,
			createdBy: testUser._id.toString(),
			attendees: []
		};

		await expect(eventModel.create(invalidEvent)).rejects.toThrow('Invalid update data');
	});
});

describe('PUT /api/events/join/:eventId - unmocked (requires running server)', () => {
	/*
		Inputs: Valid event ID as path parameter, authenticated user
		Expected status: 200
		Output: { message: string, data: { event: IEvent } }
		Expected behavior: Adds user to event's attendees and event to user's eventsJoined
	*/
	test('user joins an event (200) when server is available', async () => {
		
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
		const res = await request(app).put(`/api/events/join/${createdId.toString()}`);
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

	/*
		Inputs: Invalid event ID format (non-ObjectId string)
		Expected status: 400
		Output: { message: 'Invalid event id' }
		Expected behavior: Rejects join request with invalid ID format
	*/
	test('returns 400 for invalid event ID format', async () => {
		// Try to fetch an event with an invalid ID format
		const invalidId = 'invalid-id-format';
		const res = await request(app).put(`/api/events/join/${invalidId}`);

		// verify error response
		expect(res.status).toBe(400);
		expect(res.body).toHaveProperty('message');
	});
});

describe('PUT /api/events/leave/:eventId - unmocked (requires running server)', () => {
	/*
		Inputs: Valid event ID as path parameter, authenticated user who is an attendee
		Expected status: 200
		Output: { message: string, data: { event: IEvent } }
		Expected behavior: Removes user from event's attendees and event from user's eventsJoined
	*/
	test('user leaves an event (200) when server is available', async () => {
		
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
		const res = await request(app).put(`/api/events/leave/${createdId.toString()}`);
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

	/*
		Inputs: Invalid event ID format (non-ObjectId string)
		Expected status: 400
		Output: { message: 'Invalid event id' }
		Expected behavior: Rejects leave request with invalid ID format
	*/
	test('returns 400 for invalid event ID format', async () => {
		// Try to fetch an event with an invalid ID format
		const invalidId = 'invalid-id-format';
		const res = await request(app).put(`/api/events/leave/${invalidId}`);

		// verify error response
		expect(res.status).toBe(400);
		expect(res.body).toHaveProperty('message');
	});

	/*
		Inputs: Valid event ID with existing chat, authenticated user who is an attendee
		Expected status: 200
		Output: { message: string, data: { event: IEvent } }
		Expected behavior: Removes user from event's attendees, event from user's eventsJoined, and user from event chat
	*/
	test('user leaves an event with existing chat and is removed from chat participants', async () => {
		// Create a second user to be the event creator (so testUser can leave)
		const creatorData: CreateUserRequest = {
			email: `creator-${Date.now()}@example.com`,
			name: 'Event Creator',
			googleId: `creator-google-${Date.now()}`,
			age: 30,
			profilePicture: 'http://example.com/creator.jpg',
			bio: 'Creator bio',
			location: 'Vancouver, BC',
			latitude: 49.2827,
			longitude: -123.1207,
			skillLevel: 'Expert',
			eventsCreated: [],
			eventsJoined: []
		};
		const creator = await userModel.create(creatorData);

		// Create an event with testUser already as an attendee
		const newEvent: CreateEventRequest = {
			title: "Event with Chat for Leave Test",
			description: "Testing leave event with chat",
			date: new Date(Date.now() + 86400000),
			capacity: 10,
			skillLevel: "Intermediate",
			location: "Chat Test Location",
			latitude: 49.2827,
			longitude: -123.1207,
			createdBy: creator._id.toString(),
			attendees: [creator._id.toString(), testUser._id.toString()],
			photo: ""
		};
		const created = await eventModel.create(newEvent);
		const eventId = created._id;

		// Manually create the event chat and add both users
		const eventChat = await Chat.findOrCreateEventChat(
			eventId,
			newEvent.title,
			creator._id
		);
		expect(eventChat).not.toBeNull();
		
		// Add testUser to the chat using addParticipant
		await Chat.addParticipant(String(eventChat!._id), testUser._id);

		// Verify both users are in the chat
		const chatBeforeLeave = await Chat.findById(eventChat!._id);
		expect(chatBeforeLeave).not.toBeNull();
		expect(chatBeforeLeave!.participants.map(p => p.toString())).toContain(creator._id.toString());
		expect(chatBeforeLeave!.participants.map(p => p.toString())).toContain(testUser._id.toString());
		const participantCountBeforeLeave = chatBeforeLeave!.participants.length;

		// TestUser leaves the event (this should remove them from the chat via removeParticipant)
		const leaveRes = await request(app).put(`/api/events/leave/${eventId.toString()}`);
		expect(leaveRes.status).toBe(200);
		expect(leaveRes.body).toHaveProperty('message');
		expect(leaveRes.body).toHaveProperty('data');
		expect(leaveRes.body.data).toHaveProperty('event');

		// Verify testUser was removed from the event
		const eventInDb = await eventModel.findById(eventId);
		expect(eventInDb).not.toBeNull();
		expect(eventInDb?.attendees.map(a => a.toString())).not.toContain(testUser._id.toString());

		// Verify testUser was removed from the event chat (covers removeParticipant in chat.model.ts)
		const chatAfterLeave = await Chat.findById(eventChat!._id);
		expect(chatAfterLeave).not.toBeNull();
		expect(chatAfterLeave!.participants.map(p => p.toString())).not.toContain(testUser._id.toString());
		expect(chatAfterLeave!.participants.length).toBe(participantCountBeforeLeave - 1);
		
		// Verify event chat still exists (event chats should be preserved even when users leave)
		expect(chatAfterLeave!.eventId).toBeDefined();
		expect(chatAfterLeave!.eventId?.toString()).toBe(eventId.toString());

		// Cleanup
		await eventModel.delete(eventId);
		await Chat.deleteOne({ _id: eventChat!._id });
		await userModel.delete(creator._id);
	});
});

describe('PUT /api/events/:eventId - unmocked (requires running server)', () => {
	/*
		Inputs: Valid event ID as path parameter, UpdateEventRequest in body
		Expected status: 200
		Output: { message: string, data: { event: IEvent } }
		Expected behavior: Updates event fields with provided data
	*/
	test('updates an event (200) when server is available', async () => {
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
		const res = await request(app).put(`/api/events/${createdId.toString()}`).send(updatedEvent);
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

	/*
		Inputs: Valid event ID, UpdateEventRequest with empty title
		Expected status: Error thrown
		Output: Error: 'Invalid update data'
		Expected behavior: Rejects event update with empty title
	*/
	test('returns error when updating event with invalid input - empty title', async () => {
		// first create an event to ensure it exists
		const newEvent: CreateEventRequest = {
			title: "TEST INVALID UPDATE EVENT",
			description: "TEST INVALID UPDATE DESCRIPTION",
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
		
		const invalidUpdate: any = {
			title: "" // Empty string should fail validation
		};
		await expect(eventModel.update(createdId, invalidUpdate)).rejects.toThrow('Invalid update data');
		
		// cleanup - delete the created event
		await eventModel.delete(createdId);
	});
});

describe('DELETE /api/events/:eventId - unmocked (requires running server)', () => {
	/*
		Inputs: Valid event ID as path parameter
		Expected status: 200
		Output: { message: 'Event deleted successfully' }
		Expected behavior: Deletes event from database
	*/
	test('delete an event (200) when server is available', async () => {
		
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
		const delRes = await request(app)
			.delete(`/api/events/${createdId.toString()}`);

		expect(delRes.status).toBe(200);

		// verify deletion: event should no longer exist in DB
		const eventInDb = await eventModel.findById(createdId);
		expect(eventInDb).toBeNull();
	});

	/*
		Inputs: Valid event ID with associated photo
		Expected status: 200
		Output: { message: 'Event deleted successfully' }
		Expected behavior: Deletes event and associated media from database
	*/
	test('delete an event (200) when server is available', async () => {
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
			photo: "uploads/images/test.jpg"  // use a non-empty string to differ from previous test
		};

		// create via model (this will validate against createEventSchema)
		const created = await eventModel.create(newEvent);
		const createdId = created._id;

		// delete the event through the API (server must point to same DB when running)
		const delRes = await request(app)
			.delete(`/api/events/${createdId.toString()}`);

		expect(delRes.status).toBe(200);

		// verify deletion: event should no longer exist in DB
		const eventInDb = await eventModel.findById(createdId);
		expect(eventInDb).toBeNull();
	});

	/*
		Inputs: Invalid event ID format (non-ObjectId string)
		Expected status: 400
		Output: { message: 'Invalid event id' }
		Expected behavior: Rejects delete request with invalid ID format
	*/
	test('returns 400 for invalid event ID format', async () => {
		// Try to fetch an event with an invalid ID format
		const invalidId = 'invalid-id-format';
		const res = await request(app).delete(`/api/events/${invalidId}`);

		// verify error response
		expect(res.status).toBe(400);
		expect(res.body).toHaveProperty('message');
	});

	/*
		Inputs: event ID with multiple attendees
		Expected status: 200
		Output: { message: 'Event deleted successfully' }
		Expected behavior: Deletes event and removes it from all attendees' eventsJoined arrays using updateMany
	*/
	test('deletes event with multiple attendees and removes from all eventsJoined', async () => {
		// Create multiple attendees
		const attendee1Data: CreateUserRequest = {
			email: `attendee1-${Date.now()}@example.com`,
			name: "Attendee One",
			googleId: `attendee1-google-${Date.now()}`,
			age: 28,
			profilePicture: "http://example.com/attendee1.jpg",
			bio: "Attendee 1 bio",
			location: "Vancouver, BC",
			latitude: 49.2827,
			longitude: -123.1207,
			skillLevel: "Intermediate"
		};
		const attendee1 = await userModel.create(attendee1Data);

		const attendee2Data: CreateUserRequest = {
			email: `attendee2-${Date.now()}@example.com`,
			name: "Attendee Two",
			googleId: `attendee2-google-${Date.now()}`,
			age: 30,
			profilePicture: "http://example.com/attendee2.jpg",
			bio: "Attendee 2 bio",
			location: "Vancouver, BC",
			latitude: 49.2827,
			longitude: -123.1207,
			skillLevel: "Beginner"
		};
		const attendee2 = await userModel.create(attendee2Data);

		// Create an event with multiple attendees
		const newEvent: CreateEventRequest = {
			title: "Multi Attendee Event",
			description: "Event with multiple attendees for deletion test",
			date: new Date(Date.now() + 86400000),
			capacity: 10,
			skillLevel: "Expert",
			location: "Test Location",
			latitude: 37.7749,
			longitude: -122.4194,
			createdBy: testUser._id.toString(),
			attendees: [attendee1._id.toString(), attendee2._id.toString()],
			photo: ""
		};

		const created = await eventModel.create(newEvent);

		// Update attendees' eventsJoined arrays
		const attendee1ToUpdate = await userModel.findById(attendee1._id);
		await userModel.update(attendee1._id, {
			...attendee1ToUpdate!,
			eventsJoined: [created._id.toString()],
			eventsCreated: attendee1ToUpdate!.eventsCreated.map(id => id.toString())
		} as any);

		const attendee2ToUpdate = await userModel.findById(attendee2._id);
		await userModel.update(attendee2._id, {
			...attendee2ToUpdate!,
			eventsJoined: [created._id.toString()],
			eventsCreated: attendee2ToUpdate!.eventsCreated.map(id => id.toString())
		} as any);

		// Delete the event
		const delRes = await request(app).delete(`/api/events/${created._id.toString()}`);
		expect(delRes.status).toBe(200);
		expect(delRes.body.message).toBe('Event deleted successfully');

		// Verify event is deleted
		const eventInDb = await eventModel.findById(created._id);
		expect(eventInDb).toBeNull();

		// Verify event was removed from both attendees' eventsJoined arrays
		const updatedAttendee1 = await userModel.findById(attendee1._id);
		expect(updatedAttendee1).not.toBeNull();
		expect(updatedAttendee1!.eventsJoined).toEqual([]);

		const updatedAttendee2 = await userModel.findById(attendee2._id);
		expect(updatedAttendee2).not.toBeNull();
		expect(updatedAttendee2!.eventsJoined).toEqual([]);

		// Cleanup
		await userModel.delete(attendee1._id);
		await userModel.delete(attendee2._id);
	});
});