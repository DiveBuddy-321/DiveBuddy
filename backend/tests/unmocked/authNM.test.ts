// Mock Google OAuth2Client for testing - MUST be before any imports
// We need to mock this because we can't get real Google tokens in tests
// But we're still testing the actual auth service logic, database operations, and token generation
const mockVerifyIdToken = jest.fn() as jest.Mock<Promise<any>, [any]>;

jest.mock('google-auth-library', () => {
  return {
    OAuth2Client: jest.fn().mockImplementation(() => ({
      verifyIdToken: mockVerifyIdToken,
    })),
  };
});

import request from 'supertest';
import { describe, test, expect, beforeAll, afterAll, beforeEach } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { userModel } from '../../src/models/user.model';
import express from 'express';
import authRoutes from '../../src/routes/auth.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import mongoose from 'mongoose';
import jwt from 'jsonwebtoken';

dotenv.config();

// Create Express app for testing
const app = express();
app.use(express.json());

app.use('/api/auth', authRoutes);
app.use('*', notFoundHandler);
app.use(errorHandler);

beforeAll(async () => {
  await setupTestDB();
});

afterAll(async () => {
  await teardownTestDB();
});

beforeEach(() => {
  // Reset mocks before each test
  mockVerifyIdToken.mockReset();
});

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
  test('creates new user and returns JWT token', async () => {
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
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-google-token' });
    
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

  /*
    Inputs: body { idToken: string } (invalid Google token)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Google OAuth2Client throws error
      - Error is caught and re-thrown as 'Invalid Google token'
      - Controller returns 401
    Mock behavior: Google OAuth2Client.verifyIdToken throws error
  */
  test('returns 401 when Google token verification fails', async () => {
    // Mock Google token verification failure - covers error handling in verifyGoogleToken
    mockVerifyIdToken.mockRejectedValue(new Error('Invalid token'));
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'invalid-token' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (token where getPayload returns null)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: ticket.getPayload() returns null
      - Throws 'Invalid token payload' which is caught and re-thrown as 'Invalid Google token'
    Mock behavior: Google OAuth2Client returns ticket with null payload
  */
  test('returns 401 when token payload is null', async () => {
    // Mock Google token verification with null payload - covers verifyGoogleToken null check
    const mockTicket = {
      getPayload: () => null,
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'token-with-null-payload' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (valid token for existing user)
    Expected status: 409
    Output: { message: 'User already exists, please sign in instead.' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Successfully verifies token
      - auth.service.signUpWithGoogle: userModel.findByGoogleId finds existing user
      - Throws 'User already exists' error
      - Controller returns 409
    Mock behavior: Google OAuth2Client verifies token, but user already exists in DB
  */
  test('returns 409 when user already exists', async () => {
    const mockGoogleId = `google-existing-${Date.now()}`;
    const mockEmail = `existing-${Date.now()}@example.com`;
    
    // Create user first - covers userModel.findByGoogleId check in signUpWithGoogle
    const existingUser = await userModel.create({
      googleId: mockGoogleId,
      email: mockEmail,
      name: 'Existing User',
    });
    
    // Mock Google token verification
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: 'Existing User',
        picture: 'https://example.com/picture.jpg',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-google-token' });
    
    expect(res.status).toBe(409);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User already exists, please sign in instead.');
    
    // Cleanup
    await userModel.delete(existingUser._id);
  });

  /*
    Inputs: body { idToken: string } (token missing email in payload)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Payload exists but missing email
      - Throws 'Missing required user information from Google' which is caught and re-thrown
    Mock behavior: Google OAuth2Client returns payload without email
  */
  test('returns 401 when token payload missing email', async () => {
    // Mock Google token verification with missing email - covers verifyGoogleToken email check
    const mockTicket = {
      getPayload: () => ({
        sub: 'google-id',
        name: 'Test User',
        // Missing email
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'token-missing-email' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (token missing name in payload)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Payload exists but missing name
      - Throws 'Missing required user information from Google' which is caught and re-thrown
    Mock behavior: Google OAuth2Client returns payload without name
  */
  test('returns 401 when token payload missing name', async () => {
    // Mock Google token verification with missing name - covers verifyGoogleToken name check
    const mockTicket = {
      getPayload: () => ({
        sub: 'google-id',
        email: 'test@example.com',
        // Missing name
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'token-missing-name' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (token missing both email and name)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Payload exists but missing both email and name
      - Throws 'Missing required user information from Google' which is caught and re-thrown
    Mock behavior: Google OAuth2Client returns payload without email and name
  */
  test('returns 401 when token payload missing both email and name', async () => {
    // Mock Google token verification with missing email and name
    const mockTicket = {
      getPayload: () => ({
        sub: 'google-id',
        // Missing email and name
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'token-missing-both' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (token with empty email string)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Payload has empty email string
      - Throws 'Missing required user information from Google' (empty string is falsy)
    Mock behavior: Google OAuth2Client returns payload with empty email
  */
  test('returns 401 when token payload has empty email', async () => {
    // Mock Google token verification with empty email
    const mockTicket = {
      getPayload: () => ({
        sub: 'google-id',
        email: '', // Empty string
        name: 'Test User',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'token-empty-email' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (token with empty name string)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Payload has empty name string
      - Throws 'Missing required user information from Google' (empty string is falsy)
    Mock behavior: Google OAuth2Client returns payload with empty name
  */
  test('returns 401 when token payload has empty name', async () => {
    // Mock Google token verification with empty name
    const mockTicket = {
      getPayload: () => ({
        sub: 'google-id',
        email: 'test@example.com',
        name: '', // Empty string
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'token-empty-name' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (valid token, creates user with profilePicture)
    Expected status: 201
    Output: { message: string, data: { token: string, user: IUser } }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Extracts profilePicture from payload.picture
      - auth.service.signUpWithGoogle: Creates user with profilePicture
    Mock behavior: Google OAuth2Client returns payload with picture
  */
  test('creates user with profilePicture from Google', async () => {
    const mockGoogleId = `google-picture-${Date.now()}`;
    const mockEmail = `picture-${Date.now()}@example.com`;
    const mockName = 'Picture User';
    const mockPicture = 'https://example.com/profile.jpg';
    
    // Mock Google token verification
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: mockName,
        picture: mockPicture,
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-google-token' });
    
    expect(res.status).toBe(201);
    expect(res.body.data.user.profilePicture).toBe(mockPicture);
    
    // Cleanup
    const createdUser = await userModel.findByGoogleId(mockGoogleId);
    if (createdUser) {
      await userModel.delete(createdUser._id);
    }
  });

  /*
    Inputs: body { idToken: string } (valid token, creates user without profilePicture)
    Expected status: 201
    Output: { message: string, data: { token: string, user: IUser } }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Extracts user info, picture is undefined
      - auth.service.signUpWithGoogle: Creates user without profilePicture
    Mock behavior: Google OAuth2Client returns payload without picture
  */
  test('creates user without profilePicture when not provided', async () => {
    const mockGoogleId = `google-no-picture-${Date.now()}`;
    const mockEmail = `no-picture-${Date.now()}@example.com`;
    const mockName = 'No Picture User';
    
    // Mock Google token verification without picture
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: mockName,
        // No picture field
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-google-token' });
    
    expect(res.status).toBe(201);
    // profilePicture should be undefined or not set
    expect(res.body.data.user.profilePicture).toBeUndefined();
    
    // Cleanup
    const createdUser = await userModel.findByGoogleId(mockGoogleId);
    if (createdUser) {
      await userModel.delete(createdUser._id);
    }
  });

  /*
    Inputs: body {} (missing idToken)
    Expected status: 400
    Output: validation error message
    Expected behavior: Validation middleware rejects request without idToken
  */
  test('returns 400 when idToken is missing', async () => {
    const res = await request(app)
      .post('/api/auth/signup')
      .send({});
    
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
  });

  /*
    Inputs: body { idToken: '' } (empty idToken)
    Expected status: 400
    Output: validation error message
    Expected behavior: Validation middleware rejects empty idToken
  */
  test('returns 400 when idToken is empty', async () => {
    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: '' });
    
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
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
  test('signs in existing user and returns JWT token', async () => {
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
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-google-token' });
    
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

  /*
    Inputs: body { idToken: string } (valid token for non-existent user)
    Expected status: 404
    Output: { message: 'User not found, please sign up first.' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Successfully verifies token
      - auth.service.signInWithGoogle: userModel.findByGoogleId returns null
      - Throws 'User not found' error
      - Controller returns 404
    Mock behavior: Google OAuth2Client verifies token, but user not found in DB
  */
  test('returns 404 when user does not exist', async () => {
    const mockGoogleId = `google-nonexistent-${Date.now()}`;
    const mockEmail = `nonexistent-${Date.now()}@example.com`;
    
    // Mock Google token verification - covers verifyGoogleToken
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: 'Non-existent User',
        picture: 'https://example.com/picture.jpg',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-google-token' });
    
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User not found, please sign up first.');
  });

  /*
    Inputs: body { idToken: string } (invalid Google token)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Google OAuth2Client throws error
      - Error is caught and re-thrown as 'Invalid Google token'
      - Controller returns 401
    Mock behavior: Google OAuth2Client throws error on verification
  */
  test('returns 401 for invalid Google token', async () => {
    // Mock Google token verification failure - covers error handling in verifyGoogleToken
    mockVerifyIdToken.mockRejectedValue(new Error('Invalid token'));
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'invalid-token' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (token where getPayload returns null)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: ticket.getPayload() returns null
      - Throws 'Invalid token payload' which is caught and re-thrown as 'Invalid Google token'
    Mock behavior: Google OAuth2Client returns ticket with null payload
  */
  test('returns 401 when token payload is null', async () => {
    // Mock Google token verification with null payload - covers verifyGoogleToken null check
    const mockTicket = {
      getPayload: () => null,
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'token-with-null-payload' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body {} (missing idToken)
    Expected status: 400
    Output: validation error message
    Expected behavior: Validation middleware rejects request without idToken
  */
  test('returns 400 when idToken is missing', async () => {
    const res = await request(app)
      .post('/api/auth/signin')
      .send({});
    
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
  });

  /*
    Inputs: body { idToken: '' } (empty idToken)
    Expected status: 400
    Output: validation error message
    Expected behavior: Validation middleware rejects empty idToken
  */
  test('returns 400 when idToken is empty', async () => {
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: '' });
    
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
  });

  /*
    Inputs: body { idToken: string } (valid token, JWT token has correct expiration)
    Expected status: 200
    Output: { message: string, data: { token: string, user: IUser } }
    Expected behavior: 
      - auth.service.generateAccessToken: Creates JWT with expiresIn: '19h'
      - Token expiration is 19 hours (68400 seconds) from issuance
    Mock behavior: Google OAuth2Client verifies token successfully
  */
  test('returns JWT token with correct expiration (19h)', async () => {
    const mockGoogleId = `google-exp-${Date.now()}`;
    const mockEmail = `exp-${Date.now()}@example.com`;
    const mockName = 'Expiration Test User';
    
    // Create user first
    const existingUser = await userModel.create({
      googleId: mockGoogleId,
      email: mockEmail,
      name: mockName,
    });
    
    // Mock Google token verification
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: mockName,
        picture: 'https://example.com/picture.jpg',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-google-token' });
    
    expect(res.status).toBe(200);
    
    // Verify JWT token expiration - covers generateAccessToken expiresIn: '19h'
    const decoded = jwt.verify(res.body.data.token, process.env.JWT_SECRET!) as any;
    expect(decoded).toHaveProperty('exp');
    expect(decoded).toHaveProperty('iat');
    
    // Verify expiration is approximately 19 hours (68400 seconds) from now
    const expirationTime = decoded.exp - decoded.iat;
    expect(expirationTime).toBeGreaterThanOrEqual(68400 - 60); // Allow 1 minute tolerance
    expect(expirationTime).toBeLessThanOrEqual(68400 + 60);
    
    // Cleanup
    await userModel.delete(existingUser._id);
  });

  /*
    Inputs: body { idToken: string } (valid token, returns JWT that can be decoded)
    Expected status: 200
    Output: { message: string, data: { token: string, user: IUser } }
    Expected behavior: 
      - auth.service.generateAccessToken: Creates JWT with user._id in payload
      - Token is valid and can be decoded with JWT_SECRET
    Mock behavior: Google OAuth2Client verifies token successfully
  */
  test('returns valid JWT token with correct user ID', async () => {
    const mockGoogleId = `google-jwt-${Date.now()}`;
    const mockEmail = `jwt-${Date.now()}@example.com`;
    const mockName = 'JWT Test User';
    
    // Create user first
    const existingUser = await userModel.create({
      googleId: mockGoogleId,
      email: mockEmail,
      name: mockName,
    });
    
    // Mock Google token verification
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: mockName,
        picture: 'https://example.com/picture.jpg',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-google-token' });
    
    expect(res.status).toBe(200);
    expect(res.body.data.token).toBeDefined();
    
    // Verify JWT token is valid and contains user ID - covers generateAccessToken
    const decoded = jwt.verify(res.body.data.token, process.env.JWT_SECRET!) as any;
    expect(decoded).toHaveProperty('id');
    expect(String(decoded.id)).toBe(String(existingUser._id));
    
    // Cleanup
    await userModel.delete(existingUser._id);
  });

  /*
    Inputs: body { idToken: string } (token missing email in payload)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Payload exists but missing email
      - Throws 'Missing required user information from Google' which is caught and re-thrown
    Mock behavior: Google OAuth2Client returns payload without email
  */
  test('returns 401 when token payload missing email', async () => {
    // Mock Google token verification with missing email
    const mockTicket = {
      getPayload: () => ({
        sub: 'google-id',
        name: 'Test User',
        // Missing email
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'token-missing-email' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (token missing name in payload)
    Expected status: 401
    Output: { message: 'Invalid Google token' }
    Expected behavior: 
      - auth.service.verifyGoogleToken: Payload exists but missing name
      - Throws 'Missing required user information from Google' which is caught and re-thrown
    Mock behavior: Google OAuth2Client returns payload without name
  */
  test('returns 401 when token payload missing name', async () => {
    // Mock Google token verification with missing name
    const mockTicket = {
      getPayload: () => ({
        sub: 'google-id',
        email: 'test@example.com',
        // Missing name
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'token-missing-name' });
    
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  /*
    Inputs: body { idToken: string } (valid token, signin returns same user data)
    Expected status: 200
    Output: { message: string, data: { token: string, user: IUser } }
    Expected behavior: 
      - auth.service.signInWithGoogle: Returns user object from database
      - User data matches what's stored in database
    Mock behavior: Google OAuth2Client verifies token successfully
  */
  test('returns user data matching database', async () => {
    const mockGoogleId = `google-data-${Date.now()}`;
    const mockEmail = `data-${Date.now()}@example.com`;
    const mockName = 'Data Test User';
    const mockBio = 'Test bio';
    const mockAge = 25;
    
    // Create user with additional profile data
    const existingUser = await userModel.create({
      googleId: mockGoogleId,
      email: mockEmail,
      name: mockName,
      bio: mockBio,
      age: mockAge,
    });
    
    // Mock Google token verification
    const mockTicket = {
      getPayload: () => ({
        sub: mockGoogleId,
        email: mockEmail,
        name: mockName,
        picture: 'https://example.com/picture.jpg',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket);
    
    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-google-token' });
    
    expect(res.status).toBe(200);
    expect(res.body.data.user._id).toBe(String(existingUser._id));
    expect(res.body.data.user.email).toBe(existingUser.email);
    expect(res.body.data.user.name).toBe(existingUser.name);
    expect(res.body.data.user.bio).toBe(existingUser.bio);
    expect(res.body.data.user.age).toBe(existingUser.age);
    
    // Cleanup
    await userModel.delete(existingUser._id);
  });

  /*
    Inputs: body { idToken: string } (valid token, different users get different tokens)
    Expected status: 200
    Output: { message: string, data: { token: string, user: IUser } }
    Expected behavior: 
      - auth.service.generateAccessToken: Creates unique JWT for each user
      - Different users get different tokens with different user IDs
    Mock behavior: Google OAuth2Client verifies token successfully
  */
  test('generates different tokens for different users', async () => {
    const mockGoogleId1 = `google-user1-${Date.now()}`;
    const mockEmail1 = `user1-${Date.now()}@example.com`;
    const mockGoogleId2 = `google-user2-${Date.now()}`;
    const mockEmail2 = `user2-${Date.now()}@example.com`;
    
    // Create two users
    const user1 = await userModel.create({
      googleId: mockGoogleId1,
      email: mockEmail1,
      name: 'User 1',
    });
    
    const user2 = await userModel.create({
      googleId: mockGoogleId2,
      email: mockEmail2,
      name: 'User 2',
    });
    
    // Sign in user 1
    const mockTicket1 = {
      getPayload: () => ({
        sub: mockGoogleId1,
        email: mockEmail1,
        name: 'User 1',
        picture: 'https://example.com/picture.jpg',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket1);
    
    const res1 = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'token-user1' });
    
    expect(res1.status).toBe(200);
    const decoded1 = jwt.verify(res1.body.data.token, process.env.JWT_SECRET!) as any;
    expect(String(decoded1.id)).toBe(String(user1._id));
    
    // Sign in user 2
    const mockTicket2 = {
      getPayload: () => ({
        sub: mockGoogleId2,
        email: mockEmail2,
        name: 'User 2',
        picture: 'https://example.com/picture.jpg',
      }),
    };
    
    mockVerifyIdToken.mockResolvedValue(mockTicket2);
    
    const res2 = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'token-user2' });
    
    expect(res2.status).toBe(200);
    const decoded2 = jwt.verify(res2.body.data.token, process.env.JWT_SECRET!) as any;
    expect(String(decoded2.id)).toBe(String(user2._id));
    
    // Verify tokens are different
    expect(res1.body.data.token).not.toBe(res2.body.data.token);
    expect(decoded1.id.toString()).not.toBe(decoded2.id.toString());
    
    // Cleanup
    await userModel.delete(user1._id);
    await userModel.delete(user2._id);
  });
});

