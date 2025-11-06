import request from 'supertest';
import express from 'express';
import { describe, test, expect, jest, afterEach } from '@jest/globals';
import authRoutes from '../../src/routes/auth.routes';
import { authService } from '../../src/services/auth.service';
import { errorHandler } from '../../src/middleware/errorHandler.middleware';

// Create Express app for testing
const app = express();
app.use(express.json());
app.use('/api/auth', authRoutes);
// Error handler middleware needs 4 parameters to be recognized by Express
app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
  errorHandler(err, req, res);
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('POST /api/auth/signup - mocked', () => {
  test('returns 401 when Google token is invalid', async () => {
    jest.spyOn(authService, 'signUpWithGoogle').mockRejectedValue(
      new Error('Invalid Google token')
    );

    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'invalid-token' });

    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  test('returns 409 when user already exists', async () => {
    jest.spyOn(authService, 'signUpWithGoogle').mockRejectedValue(
      new Error('User already exists')
    );

    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(409);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User already exists, please sign in instead.');
  });

  test('returns 500 when failed to process user', async () => {
    jest.spyOn(authService, 'signUpWithGoogle').mockRejectedValue(
      new Error('Failed to process user')
    );

    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Failed to process user information');
  });

  test('returns 500 when database connection fails', async () => {
    jest.spyOn(authService, 'signUpWithGoogle').mockRejectedValue(
      new Error('Database connection failed')
    );

    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Internal server error');
  });

  test('returns 500 when service throws unexpected error', async () => {
    jest.spyOn(authService, 'signUpWithGoogle').mockRejectedValue(
      new Error('Unexpected error occurred')
    );

    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Internal server error');
  });

  test('returns 500 when user creation fails', async () => {
    jest.spyOn(authService, 'signUpWithGoogle').mockRejectedValue(
      new Error('Failed to create user in database')
    );

    const res = await request(app)
      .post('/api/auth/signup')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Internal server error');
  });
});

describe('POST /api/auth/signin - mocked', () => {
  test('returns 401 when Google token is invalid', async () => {
    jest.spyOn(authService, 'signInWithGoogle').mockRejectedValue(
      new Error('Invalid Google token')
    );

    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'invalid-token' });

    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Invalid Google token');
  });

  test('returns 404 when user not found', async () => {
    jest.spyOn(authService, 'signInWithGoogle').mockRejectedValue(
      new Error('User not found')
    );

    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('User not found, please sign up first.');
  });

  test('returns 500 when failed to process user', async () => {
    jest.spyOn(authService, 'signInWithGoogle').mockRejectedValue(
      new Error('Failed to process user')
    );

    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Failed to process user information');
  });

  test('returns 500 when database connection fails', async () => {
    jest.spyOn(authService, 'signInWithGoogle').mockRejectedValue(
      new Error('Database connection failed')
    );

    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Internal server error');
  });

  test('returns 500 when service throws unexpected error', async () => {
    jest.spyOn(authService, 'signInWithGoogle').mockRejectedValue(
      new Error('Unexpected error occurred')
    );

    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Internal server error');
  });

  test('returns 500 when token generation fails', async () => {
    jest.spyOn(authService, 'signInWithGoogle').mockRejectedValue(
      new Error('Failed to generate access token')
    );

    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Internal server error');
  });

  test('returns 500 when user lookup fails', async () => {
    jest.spyOn(authService, 'signInWithGoogle').mockRejectedValue(
      new Error('Failed to retrieve user from database')
    );

    const res = await request(app)
      .post('/api/auth/signin')
      .send({ idToken: 'valid-token' });

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Internal server error');
  });
});
