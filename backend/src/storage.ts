import { Express, Request } from 'express';
import fs from 'fs';
import multer from 'multer';
import path from 'path';
import crypto from 'crypto';

import { IMAGES_DIR } from './constants/statics';

// Ensure images directory exists (IMAGES_DIR is a constant from statics)
try {
  fs.mkdirSync('uploads/images', { recursive: true });
} catch (error) {
  // Directory already exists or cannot be created
  console.error('Failed to create images directory:', error);
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, IMAGES_DIR);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(crypto.randomBytes(8).readUInt32LE(0));
    const stringName = String(file.originalname);
    if (!path.extname(stringName)) {
      cb(new Error('Invalid file extension'), '');
      return;
    }
    cb(null, `${uniqueSuffix}${path.extname(file.originalname)}`);
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
