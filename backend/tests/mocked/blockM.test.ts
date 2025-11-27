import { describe, test, expect, afterEach, beforeAll, afterAll, jest } from '@jest/globals';
import mongoose from 'mongoose';
import express from 'express';
import request from 'supertest';
import blockRoutes from '../../src/routes/block.routes';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import { Block, blockModel } from '../../src/models/block.model';

// Minimal app with mocked auth user
const app = express();
app.use(express.json());
const authedUserId = new mongoose.Types.ObjectId();
app.use('/api/block', (req: any, res: any, next: any) => {
  req.user = { _id: authedUserId };
  next();
}, blockRoutes);
app.use('*', notFoundHandler);
app.use(errorHandler);

afterEach(() => {
  jest.restoreAllMocks();
});

beforeAll(() => {
  // nothing needed
});

afterAll(() => {
  // nothing needed
});

describe('BlockModel - mocked', () => {
  /*
    Inputs: Function call to blockUser with targetUserId being the same as the authenticated user
    Output: { error: Cannot block yourself }
    Expected behavior: Returns error when attempting to block oneself
  */
  test('blockUser throws when blocking self', async () => {
    const id = new mongoose.Types.ObjectId();
    await expect(blockModel.blockUser(id, id)).rejects.toThrow('Cannot block yourself');
  });

  /*
    Inputs: Function call to blockUser with target being different from the authenticated user
    Output: { error: Failed to create block record }
    Expected behavior: Returns an error when database fails in findOneAndUpdate operation
  */
  test('blockUser throws when findOneAndUpdate returns null (failed to create)', async () => {
    const a = new mongoose.Types.ObjectId();
    const b = new mongoose.Types.ObjectId();
    const spy = jest.spyOn(Block, 'findOneAndUpdate').mockReturnValue({
      exec: () => Promise.resolve(null)
    } as any);
    await expect(blockModel.blockUser(a, b)).rejects.toThrow('Failed to create block record');
    expect(spy).toHaveBeenCalled();
  });
});

describe('BlockController - mocked error paths', () => {
  /*
    Inputs: POST request to /api/block with targetUserId being a valid ObjectId
    Expected status: 500
    Output: { error }
    Expected behavior: Returns error when there is a database error trying to add to the blocked table
  */
  test('POST /api/block returns 500 when blockUser throws', async () => {
    const targetId = new mongoose.Types.ObjectId();
    const spy = jest.spyOn(blockModel, 'blockUser').mockRejectedValue(new Error('DB error'));
    const res = await request(app).post('/api/block').send({ targetUserId: targetId.toString() });
    expect(res.status).toBe(500);
    expect(spy).toHaveBeenCalled();
  });

  /*
    Inputs: DELETE request to /api/block/:targetUserId with targetUserId being a valid ObjectId
    Expected status: 500
    Output: { error }
    Expected behavior: Returns error when there is a database error trying to add to the blocked table
  */
  test('DELETE /api/block/:targetUserId returns 500 when unblockUser throws', async () => {
    const targetId = new mongoose.Types.ObjectId();
    const spy = jest.spyOn(blockModel, 'unblockUser').mockRejectedValue(new Error('DB error'));
    const res = await request(app).delete(`/api/block/${targetId.toString()}`);
    expect(res.status).toBe(500);
    expect(spy).toHaveBeenCalled();
  });

  /*
    Inputs: GET request to /api/block
    Expected status: 500
    Output: { error }
    Expected behavior: Returns error when there is a database error trying to get list of blocked users
  */
  test('GET /api/block returns 500 when getBlockedUsers throws', async () => {
    const spy = jest.spyOn(blockModel, 'getBlockedUsers').mockRejectedValue(new Error('DB error'));
    const res = await request(app).get('/api/block');
    expect(res.status).toBe(500);
    expect(spy).toHaveBeenCalled();
  });

  /*
    Inputs: GET request to /api/block/check/:targetUserId with targetUserId being a valid ObjectId
    Expected status: 500
    Output: { error }
    Expected behavior: Returns error when there is a network error trying to check if the user is blocked by the target user
  */
  test('GET /api/block/check/:targetUserId returns 500 when isBlockedBy throws', async () => {
    const otherId = new mongoose.Types.ObjectId();
    const spy = jest.spyOn(blockModel, 'isBlockedBy').mockRejectedValue(new Error('Network error'));
    const res = await request(app).get(`/api/block/check/${otherId.toString()}`);
    expect(res.status).toBe(500);
    expect(spy).toHaveBeenCalled();
  });
});


