import { Router } from 'express';
import type { Request, Response, NextFunction } from 'express';

import { AuthController } from '../controllers/auth.controller';
import { AuthenticateUserRequest, AuthenticateUserResponse, authenticateUserSchema } from '../types/auth.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const authController = new AuthController();

router.post(
  '/signup',
  validateBody<AuthenticateUserRequest>(authenticateUserSchema),
  async (req: Request<unknown, unknown, AuthenticateUserRequest>, res: Response<AuthenticateUserResponse>, next: NextFunction) => {
    try { await authController.signUp(req, res, next); }
    catch (err: unknown) { next(err); }
  }
);

router.post(
  '/signin',
  validateBody(authenticateUserSchema),
  async (req: Request<unknown, unknown, AuthenticateUserRequest>, res: Response<AuthenticateUserResponse>, next: NextFunction) => {
    try { await authController.signIn(req, res, next); }
    catch (err: unknown) { next(err); }
  }
);

export default router;
