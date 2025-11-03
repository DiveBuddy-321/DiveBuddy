import { Router } from 'express';
import type { Request, Response, NextFunction } from 'express';

import { upload } from '../storage';
import { authenticateToken } from '../middleware/auth.middleware';
import { MediaController } from '../controllers/media.controller';
import type { UploadImageRequest, UploadImageResponse } from '../types/media.types';

const router = Router();
const mediaController = new MediaController();

router.post(
  '/upload',
  authenticateToken,
  upload.single('media'),
  async (req: Request<unknown, unknown, UploadImageRequest>, res: Response<UploadImageResponse>, next: NextFunction) => {
    try { await mediaController.uploadImage(req, res, next); }
    catch (err: unknown) { next(err); }
  }
);

export default router;
