import { Router } from 'express';
import type { ParamsDictionary } from 'express-serve-static-core';

import { BuddyController } from '../controllers/buddy.controller';
import type { GetAllBuddiesResponse } from '../types/buddy.types';
import { asyncHandler } from '../utils/asyncHandler.util';

const router = Router();
const buddyController = new BuddyController();

router.get('/', asyncHandler<ParamsDictionary, GetAllBuddiesResponse>(
  async (req, res, next) => {
    await buddyController.getAllBuddies(req, res, next);
  }
));

export default router;