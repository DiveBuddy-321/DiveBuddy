import { Router } from 'express';

import { UserController } from '../controllers/user.controller';
import { UpdateProfileRequest, GetProfileResponse, IUser, updateProfileSchema } from '../types/user.types';
import { validateBody } from '../middleware/validation.middleware';
import { asyncHandler } from '../utils/asyncHandler.util';
import type { ParamsDictionary } from 'express-serve-static-core';

const router = Router();
const userController = new UserController();

router.get('/', asyncHandler<ParamsDictionary, { message: string; data?: { users: IUser[] } }>(
  async (req, res, next) => {
    await userController.getAllProfiles(req, res, next);
  }
));

router.get('/profile', asyncHandler<ParamsDictionary, GetProfileResponse>(
  (req, res, next) => {
    userController.getProfile(req, res, next);
  }
));

router.get('/:id', asyncHandler<ParamsDictionary, GetProfileResponse>(
  async (req, res, next) => {
    await userController.getProfileById(req, res, next);
  }
));

router.delete('/', asyncHandler<ParamsDictionary, unknown>(
  async (req, res, next) => {
    await userController.deleteProfile(req, res, next);
  }
));

router.delete('/:id', asyncHandler<ParamsDictionary, unknown>(
  async (req, res, next) => {
    await userController.deleteProfileById(req, res, next);
  }
));

router.put(
  '/:id',
  validateBody<UpdateProfileRequest>(updateProfileSchema),
  asyncHandler<unknown, GetProfileResponse, UpdateProfileRequest>(
    async (req, res, next) => {
      await userController.updateProfileById(req, res, next);
    }
  )
);

router.post(
  '/',
  validateBody<UpdateProfileRequest>(updateProfileSchema),
  asyncHandler<unknown, GetProfileResponse, UpdateProfileRequest>(
    async (req, res, next) => {
      await userController.updateProfile(req, res, next);
    }
  )
);

export default router;
