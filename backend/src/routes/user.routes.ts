import { Router } from 'express';

import { UserController } from '../controllers/user.controller';
import { UpdateProfileRequest, updateProfileSchema } from '../types/user.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const userController = new UserController();

router.get('/', userController.getAllProfiles);
router.get('/profile', userController.getProfile);
router.get('/:id', userController.getProfileById);
router.delete('/', userController.deleteProfile);
router.delete('/:id', userController.deleteProfileById);

router.put(
  '/:id',
  validateBody<UpdateProfileRequest>(updateProfileSchema),
  userController.updateProfileById
);

router.post(
  '/',
  validateBody<UpdateProfileRequest>(updateProfileSchema),
  userController.updateProfile
);

export default router;
