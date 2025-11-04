import request from 'supertest';
import { describe, test, expect, beforeAll, afterEach, afterAll, jest } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { MediaService } from '../../src/services/media.service';
import express from 'express';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import mongoose from 'mongoose';
import fs from 'fs';
import { IMAGES_DIR } from '../../src/constants/statics';
import { upload } from '../../src/storage';
import { MediaController } from '../../src/controllers/media.controller';

dotenv.config();
const USER = process.env.USER_ID as string;

// Create Express app for testing
const app = express();
app.use(express.json());

// Mock auth middleware to set req.user
app.use((req: any, res, next) => {
  req.user = { 
    _id: new mongoose.Types.ObjectId(USER),
    email: 'test@example.com',
    name: 'Test User'
  };
  next();
});

const mediaController = new MediaController();

// Create test routes that bypass authenticateToken but use upload middleware
app.post(
  '/api/media/upload',
  upload.single('media'),
  (req: any, res: any, next: any) => mediaController.uploadImage(req, res, next)
);

// Error handling middleware
app.use('*', notFoundHandler);
app.use((err: any, req: any, res: any, next: any) => {
  errorHandler(err, req, res);
});

// Create a test image buffer (1x1 PNG)
const createTestImage = (): Buffer => {
  const pngHeader = Buffer.from([
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    0x00, 0x00, 0x00, 0x0D,
    0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01,
    0x00, 0x00, 0x00, 0x01,
    0x08, 0x06, 0x00, 0x00, 0x00,
    0x1F, 0x15, 0xC4, 0x89,
    0x00, 0x00, 0x00, 0x0A,
    0x49, 0x44, 0x41, 0x54,
    0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
    0x0D, 0x0A, 0x2D, 0xB4,
    0x00, 0x00, 0x00, 0x00,
    0x49, 0x45, 0x4E, 0x44,
    0xAE, 0x42, 0x60, 0x82
  ]);
  return pngHeader;
};

beforeAll(async () => {
  await setupTestDB();
  
  // Ensure uploads/images directory exists
  if (!fs.existsSync(IMAGES_DIR)) {
    fs.mkdirSync(IMAGES_DIR, { recursive: true });
  }
});

afterAll(async () => {
  // Clean up any test images
  if (fs.existsSync(IMAGES_DIR)) {
    const files = fs.readdirSync(IMAGES_DIR);
    const testFiles = files.filter(file => file.includes(USER));
    testFiles.forEach(file => {
      try {
        fs.unlinkSync(require('path').join(IMAGES_DIR, file));
      } catch (error) {
        // Ignore errors during cleanup
      }
    });
  }
  
  await teardownTestDB();
});

afterEach(() => {
  jest.restoreAllMocks();
  
  // Clean up uploaded files after each test
  if (fs.existsSync(IMAGES_DIR)) {
    const files = fs.readdirSync(IMAGES_DIR);
    const testFiles = files.filter(file => file.includes(USER));
    testFiles.forEach(file => {
      try {
        fs.unlinkSync(require('path').join(IMAGES_DIR, file));
      } catch (error) {
        // Ignore errors during cleanup
      }
    });
  }
});

describe('POST /api/media/upload - mocked', () => {
  test('returns 500 when MediaService.saveImage fails', async () => {
    const imageBuffer = createTestImage();
    
    // Mock MediaService.saveImage to throw an error
    jest.spyOn(MediaService, 'saveImage').mockRejectedValue(new Error('Failed to save image'));

    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.png');

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });

  test('returns 500 when database connection fails', async () => {
    const imageBuffer = createTestImage();
    
    // Mock MediaService.saveImage to throw connection error
    jest.spyOn(MediaService, 'saveImage').mockRejectedValue(new Error('Database connection failed'));

    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.png');

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Database connection failed');
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });

  test('returns 500 when file system error occurs', async () => {
    const imageBuffer = createTestImage();
    
    // Mock MediaService.saveImage to throw file system error
    jest.spyOn(MediaService, 'saveImage').mockRejectedValue(new Error('File system error'));

    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.png');

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('File system error');
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });

  test('returns 500 when disk space error occurs', async () => {
    const imageBuffer = createTestImage();
    
    // Mock MediaService.saveImage to throw disk space error
    jest.spyOn(MediaService, 'saveImage').mockRejectedValue(new Error('No space left on device'));

    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.png');

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('No space left on device');
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });

  test('returns 500 when unexpected error occurs', async () => {
    const imageBuffer = createTestImage();
    
    // Mock MediaService.saveImage to throw unexpected error
    jest.spyOn(MediaService, 'saveImage').mockRejectedValue(new Error('Unexpected error'));

    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.png');

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });

  test('returns 500 when network timeout occurs', async () => {
    const imageBuffer = createTestImage();
    
    // Mock MediaService.saveImage to throw timeout error
    jest.spyOn(MediaService, 'saveImage').mockRejectedValue(new Error('Connection timeout'));

    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.png');

    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Connection timeout');
    expect(MediaService.saveImage).toHaveBeenCalledTimes(1);
  });
});

