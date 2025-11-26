import { Router } from 'express';

import { BlockController } from '../controllers/block.controller';
import { validateBody } from '../middleware/validation.middleware';
import { asyncHandler } from '../utils/asyncHandler.util';
import { blockUserSchema, BlockUserRequest, BlockResponse } from '../types/block.types';
import type { ParamsDictionary } from 'express-serve-static-core';

const router = Router();
const blockController = new BlockController();

router.post(
  '/',
  validateBody<BlockUserRequest>(blockUserSchema),
  asyncHandler<ParamsDictionary, BlockResponse, BlockUserRequest>(async (req, res, next) => {
    await blockController.blockUser(req, res, next);
  })
);

router.delete(
  '/:targetUserId',
  asyncHandler<ParamsDictionary & { targetUserId: string }, BlockResponse>(async (req, res, next) => {
    await blockController.unblockUser(req, res, next);
  })
);

router.get(
  '/',
  asyncHandler<ParamsDictionary, unknown>(async (req, res, next) => {
    await blockController.getBlockedUsers(req, res, next);
  })
);

export default router;


