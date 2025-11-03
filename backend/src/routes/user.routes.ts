import { Router } from 'express';
import type { Request, Response, NextFunction } from 'express';

import { UserController } from '../controllers/user.controller';
import { UpdateProfileRequest, GetProfileResponse, IUser, updateProfileSchema } from '../types/user.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const userController = new UserController();

router.get('/', async (req: Request, res: Response<{ message: string; data?: { users: IUser[] } }>, next: NextFunction) => {
  try { await userController.getAllProfiles(req, res, next); }
  catch (err: unknown) { next(err); }
});
router.get('/profile', (req: Request, res: Response<GetProfileResponse>, next: NextFunction) => {
  try { userController.getProfile(req, res, next); }
  catch (err: unknown) { next(err); }
});
router.get('/:id', async (req: Request, res: Response<GetProfileResponse>, next: NextFunction) => {
  try { await userController.getProfileById(req, res, next); }
  catch (err: unknown) { next(err); }
});
router.delete('/', async (req: Request, res: Response, next: NextFunction) => {
  try { await userController.deleteProfile(req, res, next); }
  catch (err: unknown) { next(err); }
});
router.delete('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try { await userController.deleteProfileById(req, res, next); }
  catch (err: unknown) { next(err); }
});

router.put(
  '/:id',
  validateBody<UpdateProfileRequest>(updateProfileSchema),
  async (req: Request<unknown, unknown, UpdateProfileRequest>, res: Response<GetProfileResponse>, next: NextFunction) => {
    try { await userController.updateProfileById(req, res, next); }
    catch (err: unknown) { next(err); }
  }
);

router.post(
  '/',
  validateBody<UpdateProfileRequest>(updateProfileSchema),
  async (req: Request<unknown, unknown, UpdateProfileRequest>, res: Response<GetProfileResponse>, next: NextFunction) => {
    try { await userController.updateProfile(req, res, next); }
    catch (err: unknown) { next(err); }
  }
);

export default router;
