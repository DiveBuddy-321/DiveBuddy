import { Express, Request } from 'express';
import fs from 'fs';
import multer from 'multer';
import path from 'path';

import { IMAGES_DIR } from './constants/statics';

// Resolve the images directory path to ensure it's safe
const resolvedImagesDir = path.resolve(IMAGES_DIR);
if (!fs.existsSync(resolvedImagesDir)) {
  fs.mkdirSync(resolvedImagesDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, IMAGES_DIR);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
    cb(null, `${uniqueSuffix}${path.extname(file.originalname)} as string`);
  },
});

const fileFilter = (
  req: Request,
  file: Express.Multer.File,
  cb: multer.FileFilterCallback
) => {
  if (file.mimetype.startsWith('image/')) {
    cb(null, true);
  } else {
    cb(new Error('Only image files are allowed!'));
  }
};

export const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024,
  },
});
