import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll, jest } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import mongoose from 'mongoose';
// Import User model to register its schema (Chat model references it)
import { userModel } from '../../src/models/user.model';
import { CreateUserRequest } from '../../src/types/user.types';
import { Chat } from '../../src/models/chat.model';
import { Message } from '../../src/models/message.model';
import express from 'express';
import chatRoutes from '../../src/routes/chat.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';

dotenv.config();

// Test users will be created dynamically
let testUser: any = null;
let otherTestUser: any = null;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware to set req.user
app.use('/api/chats', (req: any, res: any, next: any) => {
  if (testUser) {
    req.user = { 
      _id: testUser._id,
      email: testUser.email,
      name: testUser.name
    };
  }
  next();
}, chatRoutes);

// Error handling middleware
app.use('*', notFoundHandler);
app.use((err: any, req: any, res: any, next: any) => {
  errorHandler(err, req, res);
});

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

afterEach(() => {
  jest.restoreAllMocks();
});

describe('GET /api/chats - mocked', () => {
  /**
   * Inputs: Authenticated user requesting chat list, database connection error
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database connection fails during chat list retrieval
   */
  test('returns 500 when database query fails', async () => {
    // Mock Chat.listForUser to throw an error
    jest.spyOn(Chat, 'listForUser').mockRejectedValue(new Error('Database connection failed'));

    // Make request
    const res = await request(app).get('/api/chats');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.listForUser).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Authenticated user requesting chat list, unexpected error in database layer
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when unexpected exception occurs during chat list query
   */
  test('returns 500 when unexpected error occurs', async () => {
    // Mock Chat.listForUser to throw an unexpected error
    jest.spyOn(Chat, 'listForUser').mockRejectedValue(new Error('Unexpected error'));

    // Make request
    const res = await request(app).get('/api/chats');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.listForUser).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Authenticated user requesting chat list, database query timeout
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database query times out during chat list retrieval
   */
  test('returns 500 when database timeout occurs', async () => {
    // Mock Chat.listForUser to throw a timeout error
    jest.spyOn(Chat, 'listForUser').mockRejectedValue(new Error('Connection timeout'));

    // Make request
    const res = await request(app).get('/api/chats');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.listForUser).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Authenticated user requesting chat list, null reference access in chat query
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when null reference is accessed during chat list operation
   */
  test('returns 500 when null pointer exception occurs', async () => {
    // Mock Chat.listForUser to throw null error
    jest.spyOn(Chat, 'listForUser').mockRejectedValue(new TypeError('Cannot read property of null'));

    // Make request
    const res = await request(app).get('/api/chats');

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.listForUser).toHaveBeenCalledTimes(1);
  });
});

describe('GET /api/chats/:chatId - mocked', () => {
  /**
   * Inputs: Valid chatId that doesn't exist in database
   * Expected status: 404
   * Output: Error message 'Chat not found'
   * Expected behavior: Returns 404 error when attempting to access non-existent chat
   */
  test('returns 404 when chat not found', async () => {
    const mockChatId = new mongoose.Types.ObjectId();

    // Mock Chat.getForUser to return null
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(null);

    // Make request
    const res = await request(app).get(`/api/chats/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Chat not found');
    expect(Chat.getForUser).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Invalid chatId format (not a valid ObjectId)
   * Expected status: 400
   * Output: Error message 'Invalid chatId'
   * Expected behavior: Validates chatId format and rejects malformed IDs
   */
  test('returns 400 when invalid chat ID provided', async () => {
    // Make request with invalid ID
    const res = await request(app).get('/api/chats/invalid-id');

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Invalid chatId');
  });

  /**
   * Inputs: Valid chatId, database error during query
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database query fails during chat retrieval
   */
  test('returns 500 when database query fails', async () => {
    const mockChatId = new mongoose.Types.ObjectId();

    // Mock Chat.getForUser to throw an error
    jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Database error'));

    // Make request
    const res = await request(app).get(`/api/chats/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.getForUser).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid chatId, network connectivity failure
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when network error occurs during chat retrieval
   */
  test('returns 500 when network error occurs', async () => {
    const mockChatId = new mongoose.Types.ObjectId();

    // Mock Chat.getForUser to throw a network error
    jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Network error'));

    // Make request
    const res = await request(app).get(`/api/chats/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.getForUser).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid chatId, system out of memory during query
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when system runs out of memory during chat retrieval
   */
  test('returns 500 when memory error occurs', async () => {
    const mockChatId = new mongoose.Types.ObjectId();

    // Mock Chat.getForUser to throw a memory error
    jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Out of memory'));

    // Make request
    const res = await request(app).get(`/api/chats/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.getForUser).toHaveBeenCalledTimes(1);
  });
});

describe('POST /api/chats - mocked', () => {
  /**
   * Inputs: Chat creation request without peerId field
   * Expected status: 400
   * Output: Error message 'peerId is required and must be a valid ObjectId'
   * Expected behavior: Validates that peerId is present in request body
   */
  test('returns 400 when peerId is missing', async () => {
    // Make request without peerId
    const res = await request(app).post('/api/chats').send({
      name: 'Test Chat'
    });

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('peerId is required and must be a valid ObjectId');
  });

  /**
   * Inputs: Chat creation request with malformed peerId (not valid ObjectId)
   * Expected status: 400
   * Output: Error message 'peerId is required and must be a valid ObjectId'
   * Expected behavior: Validates peerId format and rejects invalid ObjectIds
   */
  test('returns 400 when peerId is invalid', async () => {
    // Make request with invalid peerId
    const res = await request(app).post('/api/chats').send({
      peerId: 'invalid-id',
      name: 'Test Chat'
    });

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('peerId is required and must be a valid ObjectId');
  });

  /**
   * Inputs: Chat creation request where peerId equals current user's ID
   * Expected status: 400
   * Output: Error message 'Cannot create a direct chat with yourself'
   * Expected behavior: Prevents users from creating chats with themselves
   */
  test('returns 400 when trying to create chat with yourself', async () => {
    // Make request with peerId same as testUser._id
    const res = await request(app).post('/api/chats').send({
      peerId: testUser ? testUser._id.toString() : new mongoose.Types.ObjectId().toString(),
      name: 'Self Chat'
    });

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Cannot create a direct chat with yourself');
  });

  /**
   * Inputs: Valid chat creation request, database error checking existing chats
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database fails during existing chat lookup
   */
  test('returns 500 when findDirectPair fails', async () => {
    const mockPeerId = new mongoose.Types.ObjectId();

    // Mock Chat.findDirectPair to throw an error
    jest.spyOn(Chat, 'findDirectPair').mockRejectedValue(new Error('Database error'));

    // Make request
    const res = await request(app).post('/api/chats').send({
      peerId: mockPeerId.toString(),
      name: 'Test Chat'
    });

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.findDirectPair).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid chat creation request, no existing chat, database error during creation
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database fails during new chat creation
   */
  test('returns 500 when createPair fails', async () => {
    const mockPeerId = new mongoose.Types.ObjectId();

    // Mock Chat.findDirectPair to return null (no existing chat)
    jest.spyOn(Chat, 'findDirectPair').mockResolvedValue(null);
    // Mock Chat.createPair to throw an error
    jest.spyOn(Chat, 'createPair').mockRejectedValue(new Error('Failed to create chat'));

    // Make request
    const res = await request(app).post('/api/chats').send({
      peerId: mockPeerId.toString(),
      name: 'Test Chat'
    });

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.findDirectPair).toHaveBeenCalledTimes(1);
    expect(Chat.createPair).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid chat creation request, no existing chat, network failure during creation
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when network connectivity fails during chat creation
   */
  test('returns 500 when network error occurs during creation', async () => {
    const mockPeerId = new mongoose.Types.ObjectId();

    // Mock Chat.findDirectPair to return null
    jest.spyOn(Chat, 'findDirectPair').mockResolvedValue(null);
    // Mock Chat.createPair to throw network error
    jest.spyOn(Chat, 'createPair').mockRejectedValue(new Error('Network error'));

    // Make request
    const res = await request(app).post('/api/chats').send({
      peerId: mockPeerId.toString(),
      name: 'Test Chat'
    });

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.createPair).toHaveBeenCalledTimes(1);
  });
});

describe('POST /api/chats/:chatId/messages - mocked', () => {
  /**
   * Inputs: Message creation request without content field
   * Expected status: 400
   * Output: Error message 'Message content is required'
   * Expected behavior: Validates that message content is present in request body
   */
  test('returns 400 when content is missing', async () => {
    const mockChatId = new mongoose.Types.ObjectId();
    // Mock Chat.getForUser to return a valid chat
    const mockChat = { _id: mockChatId, participants: [new mongoose.Types.ObjectId(testUser._id)] };
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);

    // Make request without content
    const res = await request(app).post(`/api/chats/${mockChatId.toString()}/messages`).send({});

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Message content is required');
  });

  /**
   * Inputs: Message creation request with whitespace-only content
   * Expected status: 400
   * Output: Error message in response body
   * Expected behavior: Validates that message content is not empty or whitespace-only
   */
  test('returns 400 when content is empty', async () => {
    const mockChatId = new mongoose.Types.ObjectId();
    // Mock Chat.getForUser to return a valid chat
    const mockChat = { _id: mockChatId, participants: [testUser._id] };
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);

    // Make request with empty content
    const res = await request(app).post(`/api/chats/${mockChatId.toString()}/messages`).send({
      content: '   '
    });

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  /**
   * Inputs: Message creation request with malformed chatId (not valid ObjectId)
   * Expected status: 400
   * Output: Error message 'Invalid chatId'
   * Expected behavior: Validates chatId format in URL parameter
   */
  test('returns 400 when invalid chatId provided', async () => {
    // Make request with invalid chatId
    const res = await request(app).post('/api/chats/invalid-id/messages').send({
      content: 'Test message'
    });

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Invalid chatId');
  });

  /**
   * Inputs: Valid chatId that doesn't exist or user has no access to
   * Expected status: 404
   * Output: Error message 'Chat not found or access denied'
   * Expected behavior: Returns 404 when user attempts to send message to inaccessible chat
   */
  test('returns 404 when chat not found', async () => {
    const mockChatId = new mongoose.Types.ObjectId();

    // Mock Chat.getForUser to return null
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(null);

    // Make request
    const res = await request(app).post(`/api/chats/${mockChatId.toString()}/messages`).send({
      content: 'Test message'
    });

    // Assertions
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Chat not found or access denied');
  });

  /**
   * Inputs: Valid message creation request, database error during chat verification
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database fails during chat access verification
   */
  test('returns 500 when Chat.getForUser fails', async () => {
    const mockChatId = new mongoose.Types.ObjectId();

    // Mock Chat.getForUser to throw an error
    jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Database error'));

    // Make request
    const res = await request(app).post(`/api/chats/${mockChatId.toString()}/messages`).send({
      content: 'Test message'
    });

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.getForUser).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid message creation request, database error during message creation
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database fails during message document creation
   */
  test('returns 500 when Message.createMessage fails', async () => {
    const mockChatId = new mongoose.Types.ObjectId();
    // Mock Chat.getForUser to return a valid chat
    const mockChat = { _id: mockChatId, participants: [testUser._id] };
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);
    // Mock Message.createMessage to throw an error
    jest.spyOn(Message, 'createMessage').mockRejectedValue(new Error('Failed to create message'));

    // Make request
    const res = await request(app).post(`/api/chats/${mockChatId.toString()}/messages`).send({
      content: 'Test message'
    });

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.getForUser).toHaveBeenCalledTimes(1);
    expect(Message.createMessage).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid message creation request, message created successfully, error fetching created message
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database fails during message retrieval after creation
   */
  test('returns 500 when Message.getMessageById fails', async () => {
    const mockChatId = new mongoose.Types.ObjectId();
    const mockMessageId = new mongoose.Types.ObjectId();
    // Mock Chat.getForUser to return a valid chat
    const mockChat = { _id: mockChatId, participants: [testUser._id] };
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);
    // Mock Message.createMessage to return a message
    const mockMessage = { _id: mockMessageId, content: 'Test message', sender: testUser._id.toString() };
    jest.spyOn(Message, 'createMessage').mockResolvedValue(mockMessage as any);
    // Mock Message.getMessageById to throw an error
    jest.spyOn(Message, 'getMessageById').mockRejectedValue(new Error('Failed to fetch message'));

    // Make request
    const res = await request(app).post(`/api/chats/${mockChatId.toString()}/messages`).send({
      content: 'Test message'
    });

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Message.createMessage).toHaveBeenCalledTimes(1);
    expect(Message.getMessageById).toHaveBeenCalledTimes(1);
  });
});

describe('GET /api/chats/messages/:chatId - mocked', () => {
  /**
   * Inputs: Message retrieval request with malformed chatId (not valid ObjectId)
   * Expected status: 400
   * Output: Error message 'Invalid chatId'
   * Expected behavior: Validates chatId format in URL parameter
   */
  test('returns 400 when invalid chatId provided', async () => {
    // Make request with invalid chatId
    const res = await request(app).get('/api/chats/messages/invalid-id');

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Invalid chatId');
  });

  test('returns 404 when chat not found', async () => {
    const mockChatId = new mongoose.Types.ObjectId();

    // Mock Chat.getForUser to return null
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(null);

    // Make request
    const res = await request(app).get(`/api/chats/messages/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe('Chat not found or access denied');
  });

  /**
   * Inputs: Valid chatId, malformed 'before' query parameter (not valid timestamp)
   * Expected status: 400
   * Output: Error message "Invalid 'before' timestamp format"
   * Expected behavior: Validates timestamp format for pagination parameter
   */
  test('returns 400 when invalid before timestamp provided', async () => {
    const mockChatId = new mongoose.Types.ObjectId();
    // Mock Chat.getForUser to return a valid chat
    const mockChat = { _id: mockChatId, participants: [new mongoose.Types.ObjectId(testUser._id)] };
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);

    // Make request with invalid before timestamp
    const res = await request(app).get(`/api/chats/messages/${mockChatId.toString()}?before=invalid-date`);

    // Assertions
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
    expect(res.body.error).toBe("Invalid 'before' timestamp format");
  });

  /**
   * Inputs: Valid message retrieval request, database error during chat verification
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database fails during chat access verification
   */
  test('returns 500 when Chat.getForUser fails', async () => {
    const mockChatId = new mongoose.Types.ObjectId();

    // Mock Chat.getForUser to throw an error
    jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Database error'));

    // Make request
    const res = await request(app).get(`/api/chats/messages/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.getForUser).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid message retrieval request, database error during message query
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database fails during message retrieval
   */
  test('returns 500 when Message.getMessagesForChat fails', async () => {
    const mockChatId = new mongoose.Types.ObjectId();
    // Mock Chat.getForUser to return a valid chat
    const mockChat = { _id: mockChatId, participants: [testUser._id] };
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);
    // Mock Message.getMessagesForChat to throw an error
    jest.spyOn(Message, 'getMessagesForChat').mockRejectedValue(new Error('Failed to fetch messages'));

    // Make request
    const res = await request(app).get(`/api/chats/messages/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Chat.getForUser).toHaveBeenCalledTimes(1);
    expect(Message.getMessagesForChat).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid message retrieval request, database connection failure during message query
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database connection fails during message retrieval
   */
  test('returns 500 when database connection fails', async () => {
    const mockChatId = new mongoose.Types.ObjectId();
    // Mock Chat.getForUser to return a valid chat
    const mockChat = { _id: mockChatId, participants: [new mongoose.Types.ObjectId(testUser._id)] };
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);
    // Mock Message.getMessagesForChat to throw connection error
    jest.spyOn(Message, 'getMessagesForChat').mockRejectedValue(new Error('Database connection failed'));

    // Make request
    const res = await request(app).get(`/api/chats/messages/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Message.getMessagesForChat).toHaveBeenCalledTimes(1);
  });

  /**
   * Inputs: Valid message retrieval request, database query timeout during message retrieval
   * Expected status: 500
   * Output: Error message in response body
   * Expected behavior: Returns error when database query times out during message retrieval
   */
  test('returns 500 when timeout occurs', async () => {
    const mockChatId = new mongoose.Types.ObjectId();
    // Mock Chat.getForUser to return a valid chat
    const mockChat = { _id: mockChatId, participants: [new mongoose.Types.ObjectId(testUser._id)] };
    jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);
    // Mock Message.getMessagesForChat to throw timeout error
    jest.spyOn(Message, 'getMessagesForChat').mockRejectedValue(new Error('Connection timeout'));

    // Make request
    const res = await request(app).get(`/api/chats/messages/${mockChatId.toString()}`);

    // Assertions
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(Message.getMessagesForChat).toHaveBeenCalledTimes(1);
  });
});

