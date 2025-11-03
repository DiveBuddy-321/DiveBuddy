import { Router } from 'express';

import { BuddyController } from '../controllers/buddy.controller';

const router = Router();
const buddyController = new BuddyController();

router.get('/', (req, res, next) => { void buddyController.getAllBuddies(req, res, next); });

export default router;