// controllers/media.controller.ts
import type { RequestHandler } from 'express';
import logger from '../utils/logger.util';
import { MediaService } from '../services/media.service';
import type { UploadImageResponse } from '../types/media.types';
import { sanitizeInput } from '../utils/sanitizeInput.util';

export class MediaController {
  uploadImage: RequestHandler<Record<string, never>, UploadImageResponse, unknown> =
    async (req, res, next): Promise<void> => {
      try {
        if (!req.file) {
          res.status(400).json({ message: 'No file uploaded' });
          return;
        }

        const user = req.user;
        if (!user?._id) {
          res.status(401).json({ message: 'Unauthorized' });
          return;
        }

        // Multer ensures path is a string; still sanitize
        const sanitizedFilePath = sanitizeInput(req.file.path);

        const image = await MediaService.saveImage(
          sanitizedFilePath,
          user._id.toString()
        );

        res.status(200).json({
          message: 'Image uploaded successfully',
          data: { image },
        });
        return; // ensure Promise<void>
      } catch (error) {
        logger.error('Error uploading profile picture:', error);
        next(error);
      }
    };
}
