// routes/media.routes.ts
import { Router, type RequestHandler } from 'express';
import { upload } from '../storage';
import { authenticateToken } from '../middleware/auth.middleware';
import { MediaController } from '../controllers/media.controller';
import type { UploadImageResponse } from '../types/media.types';
import { asyncHandler } from '../utils/asyncHandler.util';

const router = Router();
const mediaController = new MediaController();

// Params = {}, ResBody = UploadImageResponse, ReqBody = unknown (multer-compatible)
router.post<
  Record<string, never>,
  UploadImageResponse,
  unknown
>(
  '/upload',
  authenticateToken as RequestHandler,             // (optional) align types
  upload.single('media') as unknown as RequestHandler, // (optional) align types
  asyncHandler(mediaController.uploadImage)        // no extra wrapper, no generics here
);

export default router; 

