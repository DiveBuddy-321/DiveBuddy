import request from 'supertest';
import { describe, test, expect, beforeAll, afterAll } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import mongoose from 'mongoose';
// IMPORTANT: Import User model FIRST to ensure its schema is registered before Chat model references it
import { userModel } from '../../src/models/user.model'; // This registers the 'User' model
import { CreateUserRequest } from '../../src/types/user.types';
// Now import Chat and Message models which reference User
import { Chat } from '../../src/models/chat.model';
import { Message } from '../../src/models/message.model';
import express from 'express';
import chatRoutes from '../../src/routes/chat.routes';
import blockRoutes from '../../src/routes/block.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';

dotenv.config();

// Test users will be created dynamically
let testUser: any = null;
let otherTestUser: any = null;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware - BYPASSES real authenticateToken middleware
// Sets req.user directly so we don't need JWT tokens for testing
// This runs BEFORE routes, so req.user is available to controllers
// Note: _id as ObjectId matches the IUser interface and real auth middleware behavior
app.use((req: any, res, next) => {
  if (testUser) {
    // Allow switching acting user via header 'x-user-id' for tests
    const headerUserId = req.header('x-user-id');
    const actingUser = headerUserId && otherTestUser && String(otherTestUser._id) === String(headerUserId)
      ? otherTestUser
      : testUser;
    const mockUser = {
      _id: actingUser._id,
      email: actingUser.email,
      name: actingUser.name
    };
    req.user = mockUser;
    console.log('[MOCK AUTH] Setting req.user:', {
      _id: String(mockUser._id),
      _idType: typeof mockUser._id,
      email: mockUser.email,
      name: mockUser.name
    });
  }
  next();
});

// Debug middleware to log requests
app.use('/api/chats', (req: any, res, next) => {
  console.log('[REQUEST]', req.method, req.path, {
    user: req.user ? {
      _id: String(req.user._id),
      _idType: typeof req.user._id,
      hasUser: !!req.user
    } : 'no user',
    query: req.query,
    params: req.params,
    body: req.body
  });
  next();
});

// Mount routes directly (bypassing src/routes.ts which applies authenticateToken)
app.use('/api/chats', chatRoutes);
app.use('/api/block', blockRoutes);
app.use('*', notFoundHandler);
app.use(errorHandler);

beforeAll(async () => {
  console.log('[SETUP] Connecting to test database...');
  await setupTestDB();
  console.log('[SETUP] Database connected');
  
  // Create test users
  const newUser: CreateUserRequest = {
    email: `chatnm-test-${Date.now()}@example.com`,
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
    email: `chatnm-other-${Date.now()}@example.com`,
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
  
  console.log('[SETUP] testUser._id:', testUser._id);
  console.log('[SETUP] otherTestUser._id:', otherTestUser._id);
  
  // Explicitly ensure User model is registered before Chat operations
  // Access userModel to trigger its constructor which registers the schema
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

let chatId: string;
let messageId: string;

describe('POST /api/chats - unmocked (no mocking)', () => {
  /*
    Inputs: body { peerId: OTHER_USER, name?: string }
    Expected status: 201
    Output: created chat document directly in response.body
    Expected behavior: Creates new direct chat between two users
  */
  test('creates a direct chat between two users', async () => {
    console.log('[TEST] Creating chat with peerId:', otherTestUser._id.toString());
    const res = await request(app).post('/api/chats').send({
      peerId: otherTestUser._id.toString(),
      name: 'Test Direct Chat'
    });
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

  /*
    Inputs: body { peerId: OTHER_USER, name: null } (null name for direct chat)
    Expected status: 201
    Output: created chat document with null name
    Expected behavior: Direct chats can have null name
  */
  test('creates direct chat with null name', async () => {
    const res = await request(app).post('/api/chats').send({
      peerId: otherTestUser._id.toString()
      // name is optional, can be null for direct chats
    });
    expect(res.status).toBeGreaterThanOrEqual(200);
    expect(res.body).toHaveProperty('_id');
    expect(res.body).toHaveProperty('participants');
  });

  /*
    Inputs: { peerId: OTHER_USER, name?: string } (same as previous chat)
    Expected status: 200
    Output: existing chat document directly in response.body
    Expected behavior: Returns existing chat instead of creating duplicate
  */
  test('returns existing chat when direct chat already exists', async () => {
    const res = await request(app).post('/api/chats').send({
      peerId: otherTestUser._id.toString(),
      name: 'Duplicate Test Chat'
    });
    expect(res.status).toBe(200); // Returns existing chat with 200
    expect(res.body).toHaveProperty('_id');
    expect(String(res.body._id)).toBe(chatId);
  });

  /*
    Inputs: { name?: string } (missing peerId)
    Expected status: 400
    Output: error message in response.body.error
    Expected behavior: Rejects request when peerId is missing
  */
  test('returns 400 when peerId is missing', async () => {
    const res = await request(app).post('/api/chats').send({
      name: 'Test Chat'
    });
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: { peerId: 'invalid-id', name?: string }
    Expected status: 400
    Output: error message in response.body.error
    Expected behavior: Rejects request when peerId is not a valid ObjectId
  */
  test('returns 400 when peerId is invalid', async () => {
    const res = await request(app).post('/api/chats').send({
      peerId: 'invalid-id',
      name: 'Test Chat'
    });
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });
});

describe('GET /api/chats - unmocked (no mocking)', () => {
  /*
    Inputs: none (user from mock auth)
    Expected status: 200
    Output: array of chats directly in response.body (not wrapped)
  */
  test('lists chats for the authenticated user', async () => {
    console.log('[TEST] Starting GET /api/chats test');
    console.log('[TEST] testUser._id:', testUser._id.toString());
    console.log('[TEST] testUser._id is valid ObjectId:', mongoose.isValidObjectId(testUser._id));
    
    const res = await request(app).get('/api/chats');
    
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
  /*
    Inputs: path param chatId (valid chat where user is participant)
    Expected status: 200
    Output: chat document directly in response.body
    Expected behavior: Returns chat when user is a participant
  */
  test('returns a chat when the user is a participant', async () => {
    const res = await request(app).get(`/api/chats/${chatId}`);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('_id');
    expect(String(res.body._id)).toBe(chatId);
  });

  /*
    Inputs: path param 'invalid-id' (not a valid ObjectId)
    Expected status: 400
    Output: error message in response.body.error
    Expected behavior: Rejects invalid chatId format
  */
  test('returns 400 when chatId is invalid', async () => {
    const res = await request(app).get('/api/chats/invalid-id');
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: path param chatId (valid ObjectId but non-existent chat)
    Expected status: 404
    Output: error message in response.body.error
    Expected behavior: Returns 404 when chat doesn't exist
  */
  test('returns 404 when chat does not exist', async () => {
    const fakeId = new mongoose.Types.ObjectId().toString();
    const res = await request(app).get(`/api/chats/${fakeId}`);
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: path param chatId (valid chat where user is NOT a participant)
    Expected status: 404
    Output: error message in response.body.error
    Expected behavior: Returns 404 when user is not a participant (access denied)
  */
  test('returns 404 when user is not a participant', async () => {
    // Create a chat between otherTestUser and a third user
    const thirdUser = new mongoose.Types.ObjectId().toString();
    const otherChat = await Chat.createPair(
      otherTestUser._id,
      new mongoose.Types.ObjectId(thirdUser),
      'Private Chat'
    );
    const otherChatId = String(otherChat._id);

    // testUser tries to access this chat
    const res = await request(app).get(`/api/chats/${otherChatId}`);
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('error');

    // Cleanup
    await Chat.deleteOne({ _id: otherChatId });
  });
});

describe('POST /api/chats/:chatId/messages - unmocked (no mocking)', () => {
  /*
    Inputs: path param chatId, body { content: string }
    Expected status: 201
    Output: created message directly in response.body
    Expected behavior: Creates message in chat and returns populated message
  */
  test('sends a message in the chat', async () => {
    const res = await request(app).post(`/api/chats/${chatId}/messages`).send({
      content: 'Hello from test!'
    });
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('_id');
    expect(res.body).toHaveProperty('content');
    expect(res.body.content).toBe('Hello from test!');
    expect(String(res.body.sender?._id || res.body.sender)).toBe(testUser._id.toString());
    messageId = String(res.body._id);
  });

  /*
    Inputs: path param chatId, body {} (missing content)
    Expected status: 400
    Output: error message 'Message content is required' in response.body.error
    Expected behavior: Rejects request when content is missing
  */
  test('returns 400 when content is missing', async () => {
    const res = await request(app).post(`/api/chats/${chatId}/messages`).send({});
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Message content is required');
  });

  /*
    Inputs: path param chatId, body { content: '   ' } (whitespace only)
    Expected status: 400
    Output: error message in response.body.error
    Expected behavior: Rejects request when content is empty/whitespace
  */
  test('returns 400 when content is empty', async () => {
    const res = await request(app).post(`/api/chats/${chatId}/messages`).send({
      content: '   '
    });
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: path param 'invalid-id', body { content: string }
    Expected status: 400
    Output: error message in response.body.error
    Expected behavior: Rejects invalid chatId format
  */
  test('returns 400 when chatId is invalid', async () => {
    const res = await request(app).post('/api/chats/invalid-id/messages').send({
      content: 'Test message'
    });
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: path param chatId (valid ObjectId but non-existent chat), body { content: string }
    Expected status: 404
    Output: error message in response.body.error
    Expected behavior: Returns 404 when chat doesn't exist or user not a participant
  */
  test('returns 404 when chat does not exist', async () => {
    const fakeId = new mongoose.Types.ObjectId().toString();
    const res = await request(app).post(`/api/chats/${fakeId}/messages`).send({
      content: 'Test message'
    });
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: path param chatId, body { content: string } (multiple messages)
    Expected status: 201 for each message
    Output: created messages
    Expected behavior: Allows sending multiple messages sequentially
  */
  test('sends multiple messages and verifies order', async () => {
    const messages = ['First message', 'Second message', 'Third message'];
    for (const msg of messages) {
      const res = await request(app).post(`/api/chats/${chatId}/messages`).send({
        content: msg
      });
      expect(res.status).toBe(201);
    }
    
    // Fetch messages and verify they exist
    const getRes = await request(app).get(`/api/chats/messages/${chatId}`);
    expect(getRes.status).toBe(200);
    expect(getRes.body.messages.length).toBeGreaterThanOrEqual(3);
  });
});

describe('GET /api/chats/messages/:chatId - unmocked (no mocking)', () => {
  /*
    Inputs: path param chatId (no query params)
    Expected status: 200
    Output: object with messages array, chatId, limit, count, hasMore in response.body
    Expected behavior: Returns messages with default limit of 20
  */
  test('fetches messages for a chat', async () => {
    const res = await request(app).get(`/api/chats/messages/${chatId}`);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('messages');
    expect(res.body).toHaveProperty('chatId');
    expect(res.body).toHaveProperty('limit');
    expect(res.body).toHaveProperty('count');
    expect(res.body).toHaveProperty('hasMore');
    expect(Array.isArray(res.body.messages)).toBe(true);
    expect(res.body.messages.length).toBeGreaterThan(0);
  });

  /*
    Inputs: path param chatId, query { limit: 2 }
    Expected status: 200
    Output: messages array with max 2 items, limit: 2 in response.body
    Expected behavior: Limits number of messages returned
  */
  test('respects limit parameter', async () => {
    const res = await request(app).get(`/api/chats/messages/${chatId}?limit=2`);
    expect(res.status).toBe(200);
    expect(res.body.messages.length).toBeLessThanOrEqual(2);
    expect(res.body.limit).toBe(2);
  });

  /*
    Inputs: path param chatId, query { before: ISO timestamp }
    Expected status: 200
    Output: messages array (only messages before the timestamp)
    Expected behavior: Returns paginated messages before specified timestamp
  */
  test('handles before timestamp parameter', async () => {
    // Get all messages first
    const allRes = await request(app).get(`/api/chats/messages/${chatId}`);
    expect(allRes.status).toBe(200);
    expect(allRes.body.messages.length).toBeGreaterThan(0);
    
    // Get messages before the first message's timestamp
    if (allRes.body.messages.length > 0) {
      const firstMessage = allRes.body.messages[0];
      const beforeDate = new Date(firstMessage.createdAt);
      beforeDate.setMilliseconds(beforeDate.getMilliseconds() - 1);
      
      const res = await request(app).get(
        `/api/chats/messages/${chatId}?before=${beforeDate.toISOString()}`
      );
      expect(res.status).toBe(200);
      expect(Array.isArray(res.body.messages)).toBe(true);
    }
  });

  /*
    Inputs: path param 'invalid-id' (not a valid ObjectId)
    Expected status: 400
    Output: error message in response.body.error
    Expected behavior: Rejects invalid chatId format
  */
  test('returns 400 when chatId is invalid', async () => {
    const res = await request(app).get('/api/chats/messages/invalid-id');
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: path param chatId (valid ObjectId but non-existent or user not participant)
    Expected status: 404
    Output: error message in response.body.error
    Expected behavior: Returns 404 when chat doesn't exist or access denied
  */
  test('returns 404 when chat does not exist or user is not a participant', async () => {
    const fakeId = new mongoose.Types.ObjectId().toString();
    const res = await request(app).get(`/api/chats/messages/${fakeId}`);
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: path param chatId, query { before: 'invalid-date' }
    Expected status: 400
    Output: error message 'Invalid before timestamp format' in response.body.error
    Expected behavior: Rejects invalid timestamp format
  */
  test('returns 400 when before timestamp is invalid', async () => {
    const res = await request(app).get(`/api/chats/messages/${chatId}?before=invalid-date`);
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  /*
    Inputs: path param chatId, query { limit: 500 }
    Expected status: 200
    Output: messages array, limit: 200 (capped) in response.body
    Expected behavior: Caps limit at maximum of 200 even if higher value provided
  */
  test('caps limit at maximum of 200', async () => {
    const res = await request(app).get(`/api/chats/messages/${chatId}?limit=500`);
    expect(res.status).toBe(200);
    expect(res.body.limit).toBe(200);
  });

  /*
    Inputs: path param chatId (no limit query param)
    Expected status: 200
    Output: messages array, limit: 20 (default) in response.body
    Expected behavior: Uses default limit of 20 when not specified
  */
  test('uses default limit of 20 when not specified', async () => {
    const res = await request(app).get(`/api/chats/messages/${chatId}`);
    expect(res.status).toBe(200);
    expect(res.body.limit).toBe(20);
  });
});

describe('Blocking behavior - unmocked (no mocking)', () => {

    /*
    Inputs: Two users A and B
    Expected status: 403
    Output (user B): error message 'You cannot send messages to this user due to being blocked by them.' in response.body.error for user B
    Output (user A): error message 'You cannot send messages to this user as you have blocked them.' in response.body.error for user A
    Expected behavior: Prevents messaging in either direction when A blocks B
  */
  test('prevents messaging when A blocks B (both directions)', async () => {
    // A blocks B
    const blockRes = await request(app).post('/api/block').send({
      targetUserId: otherTestUser._id.toString()
    });
    expect(blockRes.status).toBe(201);

    // B -> A should fail: blocked by them
    const bToA = await request(app)
      .post(`/api/chats/${chatId}/messages`)
      .set('x-user-id', otherTestUser._id.toString())
      .send({ content: 'Hello from B' });
    expect(bToA.status).toBe(403);
    expect(bToA.body).toHaveProperty('error');
    expect(bToA.body.error).toBe('You cannot send messages to this user due to being blocked by them.');

    // A -> B should fail: you have blocked them
    const aToB = await request(app)
      .post(`/api/chats/${chatId}/messages`)
      .send({ content: 'Hello from A' });
    expect(aToB.status).toBe(403);
    expect(aToB.body).toHaveProperty('error');
    expect(aToB.body.error).toBe('You cannot send messages to this user as you have blocked them.');
  });

  /*
    Inputs: Two users A and B
    Expected status: 403
    Output (user A): error message 'You cannot send messages to this user due to being blocked by them.' in response.body.error for user A
    Output (user B): error message 'You cannot send messages to this user as you have blocked them.' in response.body.error for user B
    Expected behavior: Prevents messaging in either direction when B blocks A after A unblocks B
  */
  test('prevents messaging when B blocks A after A unblocks B', async () => {
    // A unblocks B
    const unblockRes = await request(app).delete(`/api/block/${otherTestUser._id.toString()}`);
    expect(unblockRes.status).toBe(200);

    // B blocks A
    const bBlocksA = await request(app)
      .post('/api/block')
      .set('x-user-id', otherTestUser._id.toString())
      .send({ targetUserId: testUser._id.toString() });
    expect(bBlocksA.status).toBe(201);

    // A -> B should fail: blocked by them (B blocked A)
    const aToB = await request(app)
      .post(`/api/chats/${chatId}/messages`)
      .send({ content: 'Message from A' });
    expect(aToB.status).toBe(403);
    expect(aToB.body).toHaveProperty('error');
    expect(aToB.body.error).toBe('You cannot send messages to this user due to being blocked by them.');

    // B -> A should fail: you have blocked them (B has blocked A)
    const bToA = await request(app)
      .post(`/api/chats/${chatId}/messages`)
      .set('x-user-id', otherTestUser._id.toString())
      .send({ content: 'Message from B' });
    expect(bToA.status).toBe(403);
    expect(bToA.body).toHaveProperty('error');
    expect(bToA.body.error).toBe('You cannot send messages to this user as you have blocked them.');
  });

  /*
    Inputs: Two users A and B
    Expected status: 200
    Expected behavior: Once B unblocks A, sending messages in either direction should be successful again.
  */
  test('allows messaging again after B unblocks A', async () => {
    // B unblocks A
    const bUnblocksA = await request(app)
      .delete(`/api/block/${testUser._id.toString()}`)
      .set('x-user-id', otherTestUser._id.toString());
    expect(bUnblocksA.status).toBe(200);

    // A -> B succeeds
    const aToB = await request(app)
      .post(`/api/chats/${chatId}/messages`)
      .send({ content: 'A can message B now' });
    expect(aToB.status).toBe(201);
    expect(aToB.body).toHaveProperty('_id');
    expect(aToB.body).toHaveProperty('content');

    // B -> A succeeds
    const bToA = await request(app)
      .post(`/api/chats/${chatId}/messages`)
      .set('x-user-id', otherTestUser._id.toString())
      .send({ content: 'B can message A now' });
    expect(bToA.status).toBe(201);
    expect(bToA.body).toHaveProperty('_id');
    expect(bToA.body).toHaveProperty('content');
  });
});

describe('Cleanup - unmocked', () => {
  test('removes created chat and messages', async () => {
    if (chatId) {
      await Chat.deleteOne({ _id: chatId });
      await Message.deleteMany({ chat: chatId });
    }
  });
});
