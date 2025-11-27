import request from 'supertest';
import { describe, test, expect, beforeAll, afterAll } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import mongoose from 'mongoose';
import express from 'express';
import blockRoutes from '../../src/routes/block.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import { userModel } from '../../src/models/user.model';
import { CreateUserRequest } from '../../src/types/user.types';

dotenv.config();

let userA: any = null;
let userB: any = null;

// Express app with simple mock auth
const app = express();
app.use(express.json());

// Mock auth: by default uses userA. 
// - Set 'x-user-id' to userB._id to act as userB.
// - Set 'x-no-auth' to bypass setting req.user (unauthorized).
app.use((req: any, res, next) => {
  if (req.header('x-no-auth')) {
    return next();
  }
  if (userA) {
    const headerUserId = req.header('x-user-id');
    const acting = headerUserId && userB && String(userB._id) === String(headerUserId) ? userB : userA;
    req.user = {
      _id: acting._id,
      email: acting.email,
      name: acting.name
    };
  }
  next();
});

app.use('/api/block', blockRoutes);
app.use('*', notFoundHandler);
app.use(errorHandler);

beforeAll(async () => {
  await setupTestDB();

  const a: CreateUserRequest = {
    email: `a-${Date.now()}@example.com`,
    name: 'User A',
    googleId: `ga-${Date.now()}`,
    age: 25,
    profilePicture: 'http://example.com/a.jpg',
    bio: 'A',
    location: 'Vancouver',
    latitude: 49.2827,
    longitude: -123.1207,
    skillLevel: 'Beginner'
  };
  userA = await userModel.create(a);

  const b: CreateUserRequest = {
    email: `b-${Date.now()}@example.com`,
    name: 'User B',
    googleId: `gb-${Date.now()}`,
    age: 26,
    profilePicture: 'http://example.com/b.jpg',
    bio: 'B',
    location: 'Vancouver',
    latitude: 49.2827,
    longitude: -123.1207,
    skillLevel: 'Intermediate'
  };
  userB = await userModel.create(b);
});

afterAll(async () => {
  if (userA) {
    await userModel.delete(new mongoose.Types.ObjectId(userA._id));
  }
  if (userB) {
    await userModel.delete(new mongoose.Types.ObjectId(userB._id));
  }
  await teardownTestDB();
});

describe('Block Controller - Unmocked', () => {
  /*
    Inputs: POST request to /api/block with targetUserId being a valid ObjectId as an unauthenticated user
    Expected status: 401
    Output: { message: unauthorized }
    Expected behavior: Returns error when unauthenticated user tries to block another user
  */
  test('POST /api/block returns 401 when unauthorized', async () => {
    const res = await request(app)
      .post('/api/block')
      .set('x-no-auth', '1')
      .send({ targetUserId: String(userB._id) });
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message', 'Unauthorized');
  });

  /*
    Inputs: POST request to /api/block with targetUserId being an invalid ObjectId
    Expected status: 400
    Output: { message: Invalid target user id }
    Expected behavior: Returns error when invalid target user id is provided
  */
  test('POST /api/block returns 400 for invalid target user id', async () => {
    const res = await request(app)
      .post('/api/block')
      .send({ targetUserId: 'invalid-id' });
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message', 'Invalid target user id');
  });

  /*
    Inputs: POST request to /api/block with targetUserId being themselves
    Expected status: 400
    Output: { message: Cannot block yourself }
    Expected behavior: Returns error when attempting to block oneself
  */
  test('POST /api/block returns 400 when blocking self', async () => {
    const res = await request(app)
      .post('/api/block')
      .send({ targetUserId: String(userA._id) });
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message', 'Cannot block yourself');
  });

  /*
    Inputs: POST request to /api/block with targetUserId being a valid ObjectId
    Expected status: 201
    Output: { message: User blocked successfully }
    Expected behavior: Returns success message when successfully blocking another valid user
  */
  test('POST /api/block succeeds (201) when A blocks B', async () => {
    const res = await request(app)
      .post('/api/block')
      .send({ targetUserId: String(userB._id) });
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('message', 'User blocked successfully');
  });

  /*
    Inputs: GET request to /api/block as an unauthenticated user
    Expected status: 401
    Output: { message: unauthorized }
    Expected behavior: Returns error when unauthenticated user tries to get list of blocked users
  */
  test('GET /api/block returns 401 when unauthorized', async () => {
    const res = await request(app)
      .get('/api/block')
      .set('x-no-auth', '1');
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message', 'Unauthorized');
  });

  /*
    Inputs: GET request to /api/block lists blocked users
    Expected status: 200
    Output: { data: { blockedUserIds: [String] } }
    Expected behavior: Returns list of blocked user ids
  */
  test('GET /api/block lists blocked users', async () => {
    const res = await request(app).get('/api/block');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('blockedUserIds');
    expect(Array.isArray(res.body.data.blockedUserIds)).toBe(true);
    expect(res.body.data.blockedUserIds).toContain(String(userB._id));
  });

  /*
    Inputs: GET request to /api/block/check/:targetUserId as an unauthenticated user
    Expected status: 401
    Output: { message: unauthorized }
    Expected behavior: Returns error when unauthenticated user tries to check if someone has blocked them
  */
  test('GET /api/block/check/:targetUserId returns 401 when unauthorized', async () => {
    const res = await request(app)
      .get(`/api/block/check/${String(userB._id)}`)
      .set('x-no-auth', '1');
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message', 'Unauthorized');
  });

  /*
    Inputs: GET request to /api/block/check/:targetUserId with targetUserId being an invalid ObjectId
    Expected status: 400
    Output: { message: Invalid target user id }
    Expected behavior: Returns error when checking if someone with an invalid userId has blocked them
  */
  test('GET /api/block/check/:targetUserId returns 400 for invalid id', async () => {
    const res = await request(app).get('/api/block/check/invalid-id');
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message', 'Invalid target user id');
  });

  /*
    Inputs: GET request to /api/block/check/:targetUserId with targetUserId being a valid ObjectId
    Expected status: 200
    Output: { data: { isBlocked: boolean } }
    Expected behavior: Returns true if the user is blocked by the target user, false otherwise
  */
  test('GET /api/block/check/:targetUserId returns correct isBlocked status', async () => {
    // At this point A has blocked B, but B has not blocked A: A is not blocked by B
    const aCheckB = await request(app).get(`/api/block/check/${String(userB._id)}`);
    expect(aCheckB.status).toBe(200);
    expect(aCheckB.body).toHaveProperty('data');
    expect(aCheckB.body.data).toHaveProperty('isBlocked', false);

    // Now let B block A
    const bBlocksA = await request(app)
      .post('/api/block')
      .set('x-user-id', String(userB._id))
      .send({ targetUserId: String(userA._id) });
    expect(bBlocksA.status).toBe(201);

    // A is now blocked by B
    const aCheckBAgain = await request(app).get(`/api/block/check/${String(userB._id)}`);
    expect(aCheckBAgain.status).toBe(200);
    expect(aCheckBAgain.body).toHaveProperty('data');
    expect(aCheckBAgain.body.data).toHaveProperty('isBlocked', true);
  });

  /*
    Inputs: DELETE request to /api/block/:targetUserId as an unauthenticated user
    Expected status: 401
    Output: { message: unauthorized }
    Expected behavior: Returns error when unauthenticated user tries to unblock another user
  */
  test('DELETE /api/block/:targetUserId returns 401 when unauthorized', async () => {
    const res = await request(app)
      .delete(`/api/block/${String(userB._id)}`)
      .set('x-no-auth', '1');
    expect(res.status).toBe(401);
    expect(res.body).toHaveProperty('message', 'Unauthorized');
  });

  /*
    Inputs: DELETE request to /api/block/:targetUserId with targetUserId being an invalid ObjectId
    Expected status: 400
    Output: { message: Invalid target user id }
    Expected behavior: Returns error when unblocking a user with an invalid userId
  */
  test('DELETE /api/block/:targetUserId returns 400 for invalid id', async () => {
    const res = await request(app).delete('/api/block/not-an-id');
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message', 'Invalid target user id');
  });

    /*
    Inputs: DELETE request to /api/block/:targetUserId with targetUserId being a valid ObjectId
    Expected status: 200
    Output: { message: User unblocked successfully }
    Expected behavior: Returns success message when successfully unblocking another valid user
  */
  test('DELETE /api/block/:targetUserId succeeds (200) for A unblocking B', async () => {
    const res = await request(app).delete(`/api/block/${String(userB._id)}`);
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message', 'User unblocked successfully');
  });
});


