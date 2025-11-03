import { Router } from 'express';

import { UserController } from '../controllers/user.controller';
import { UpdateProfileRequest, updateProfileSchema } from '../types/user.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const userController = new UserController();

router.get('/', (req, res, next) => { void userController.getAllProfiles(req, res, next); });
router.get('/profile', (req, res) => { void userController.getProfile(req, res); });
router.get('/:id', (req, res, next) => { void userController.getProfileById(req, res, next); });
router.delete('/', (req, res, next) => { void userController.deleteProfile(req, res, next); });
router.delete('/:id', (req, res, next) => { void userController.deleteProfileById(req, res, next); });

router.put(
  '/:id',
  validateBody<UpdateProfileRequest>(updateProfileSchema),
  (req, res, next) => { void userController.updateProfileById(req, res, next); }
);

router.post(
  '/',
  validateBody<UpdateProfileRequest>(updateProfileSchema),
  (req, res, next) => { void userController.updateProfile(req, res, next); }
);

export default router;
