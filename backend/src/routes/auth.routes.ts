import { Router } from 'express';

import { AuthController } from '../controllers/auth.controller';
import { AuthenticateUserRequest, AuthenticateUserResponse, authenticateUserSchema } from '../types/auth.types';
import { validateBody } from '../middleware/validation.middleware';
import { asyncHandler } from '../utils/asyncHandler.util';

const router = Router();
const authController = new AuthController();

router.post(
  '/signup',
  validateBody<AuthenticateUserRequest>(authenticateUserSchema),
  asyncHandler<unknown, AuthenticateUserResponse, AuthenticateUserRequest>(
    async (req, res, next) => {
      await authController.signUp(req, res, next);
    }
  )
);

router.post(
  '/signin',
  validateBody(authenticateUserSchema),
  asyncHandler<unknown, AuthenticateUserResponse, AuthenticateUserRequest>(
    async (req, res, next) => {
      await authController.signIn(req, res, next);
    }
  )
);

export default router;
