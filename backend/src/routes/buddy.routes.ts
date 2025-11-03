import { Router } from 'express';
import type { Request, Response, NextFunction } from 'express';

import { BuddyController } from '../controllers/buddy.controller';
import type { GetAllBuddiesResponse } from '../types/buddy.types';

const router = Router();
const buddyController = new BuddyController();

router.get('/', async (req: Request, res: Response<GetAllBuddiesResponse>, next: NextFunction) => {
  try { await buddyController.getAllBuddies(req, res, next); }
  catch (err: unknown) { next(err); }
});

export default router;