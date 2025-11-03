import { Router } from 'express';

import { upload } from '../storage';
import { authenticateToken } from '../middleware/auth.middleware';
import { MediaController } from '../controllers/media.controller';
import type { UploadImageRequest, UploadImageResponse } from '../types/media.types';
import { asyncHandler } from '../utils/asyncHandler.util';

const router = Router();
const mediaController = new MediaController();

router.post(
  '/upload',
  authenticateToken,
  upload.single('media'),
  asyncHandler<unknown, UploadImageResponse, UploadImageRequest>(
    (req, res, next) => {
      mediaController.uploadImage(req, res, next);
    }
  )
);

export default router;
