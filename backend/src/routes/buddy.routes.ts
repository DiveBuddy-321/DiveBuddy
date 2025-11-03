import { Router } from 'express';

import { BuddyController } from '../controllers/buddy.controller';

const router = Router();
const buddyController = new BuddyController();

router.get('/', (req, res, next) => { buddyController.getAllBuddies(req, res, next).catch(next); });

export default router;