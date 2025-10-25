import { Router } from 'express';

import { BuddyController } from '../controllers/buddy.controller';

const router = Router();
const buddyController = new BuddyController();

router.get('/', buddyController.getAllBuddies.bind(buddyController));

export default router;