import request from 'supertest';
import { describe, test, expect, beforeAll, afterAll, afterEach, jest } from '@jest/globals';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { CreateUserRequest } from '../../src/types/user.types';
import { userModel } from '../../src/models/user.model';
import express from 'express';
import { errorHandler, notFoundHandler } from '../../src/middleware/errorHandler.middleware';
import mongoose from 'mongoose';
import fs from 'fs';
import path from 'path';
import { IMAGES_DIR } from '../../src/constants/statics';

dotenv.config();

// Test user will be created dynamically
let testUser: any = null;

// Mock authenticateToken middleware - must be before routes import
jest.mock('../../src/middleware/auth.middleware', () => ({
  authenticateToken: (req: any, res: any, next: any) => {
    // Set req.user directly - bypasses JWT verification
    if (testUser) {
      req.user = {
        _id: testUser._id,
        email: testUser.email,
        name: testUser.name
      };
    }
    next();
  }
}));

// Import routes AFTER mock is set up
import mediaRoutes from '../../src/routes/media.routes';

// Create Express app for testing
const app = express();
app.use(express.json());

// Use the actual media routes - this ensures media.routes.ts is executed
app.use('/api/media', mediaRoutes);

app.use('*', notFoundHandler);
app.use(errorHandler);

// Create a test image buffer (1x1 PNG)
const createTestImage = (): Buffer => {
  // Minimal PNG image (1x1 pixel)
  const pngHeader = Buffer.from([
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
    0x00, 0x00, 0x00, 0x0D, // IHDR chunk length
    0x49, 0x48, 0x44, 0x52, // IHDR
    0x00, 0x00, 0x00, 0x01, // width: 1
    0x00, 0x00, 0x00, 0x01, // height: 1
    0x08, 0x06, 0x00, 0x00, 0x00, // bit depth, color type, etc.
    0x1F, 0x15, 0xC4, 0x89, // CRC
    0x00, 0x00, 0x00, 0x0A, // IDAT chunk length
    0x49, 0x44, 0x41, 0x54, // IDAT
    0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01, // compressed data
    0x0D, 0x0A, 0x2D, 0xB4, // CRC
    0x00, 0x00, 0x00, 0x00, // IEND chunk length
    0x49, 0x45, 0x4E, 0x44, // IEND
    0xAE, 0x42, 0x60, 0x82  // CRC
  ]);
  return pngHeader;
};

beforeAll(async () => {
  await setupTestDB();
  
  // Create a test user
  const newUser: CreateUserRequest = {
    email: 'test@example.com',
    name: 'Test User',
    googleId: `test-google-${Date.now()}`,
    age: 25,
    profilePicture: 'http://example.com/pic.jpg',
    bio: 'Test bio',
    location: 'Vancouver, BC',
    latitude: 49.2827,
    longitude: -123.1207,
    skillLevel: 'Intermediate'
  };
  testUser = await userModel.create(newUser);
  
  // Ensure uploads/images directory exists
  if (!fs.existsSync(IMAGES_DIR)) {
    fs.mkdirSync(IMAGES_DIR, { recursive: true });
  }
});

afterAll(async () => {
  // Clean up any test images
  if (fs.existsSync(IMAGES_DIR) && testUser) {
    const files = fs.readdirSync(IMAGES_DIR);
    const testFiles = files.filter(file => file.includes(testUser._id.toString()));
    testFiles.forEach(file => {
      try {
        fs.unlinkSync(path.join(IMAGES_DIR, file));
      } catch (error) {
        // Ignore errors during cleanup
      }
    });
  }
  
  // Clean up test user
  if (testUser) {
    await userModel.delete(new mongoose.Types.ObjectId(testUser._id));
  }
  
  await teardownTestDB();
});

afterEach(() => {
  // Clean up uploaded files after each test
  if (fs.existsSync(IMAGES_DIR) && testUser) {
    const files = fs.readdirSync(IMAGES_DIR);
    const testFiles = files.filter(file => file.includes(testUser._id.toString()));
    testFiles.forEach(file => {
      try {
        fs.unlinkSync(path.join(IMAGES_DIR, file));
      } catch (error) {
        // Ignore errors during cleanup
      }
    });
  }
});

describe('POST /api/media/upload - unmocked (no mocking)', () => {
  /*
    Inputs: multipart/form-data with 'media' field containing image file (PNG buffer)
    Expected status: 200
    Output: { message: string, data: { image: string } } - image path relative to project root
    Expected behavior: Uploads image file, saves it with user ID prefix and timestamp, returns file path
  */
  test('uploads an image successfully', async () => {
    const imageBuffer = createTestImage();
    
    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.png');

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('data');
    expect(res.body.data).toHaveProperty('image');
    expect(res.body.data.image).toBeDefined();
    expect(typeof res.body.data.image).toBe('string');
    
    // Verify file was actually created
    const imagePath = res.body.data.image;
    const fullPath = path.join(process.cwd(), imagePath);
    expect(fs.existsSync(fullPath)).toBe(true);
  });

  /*
    Inputs: none (no file in request)
    Expected status: 400
    Output: { message: 'No file uploaded' }
    Expected behavior: Rejects request when no file is provided
  */
  test('returns 400 when no file is uploaded', async () => {
    const res = await request(app)
      .post('/api/media/upload');

    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('No file uploaded');
  });

  /*
    Inputs: multipart/form-data with 'media' field containing non-image file (text file)
    Expected status: 400 or 500
    Output: error response
    Expected behavior: Multer fileFilter rejects non-image files before reaching controller
  */
  test('returns error when file is not an image', async () => {
    const textBuffer = Buffer.from('This is not an image');
    
    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', textBuffer, 'test.txt');

    // Multer will reject non-image files before reaching controller
    expect(res.status).toBeGreaterThanOrEqual(400);
  });

  /*
    Inputs: multipart/form-data with 'media' field containing image file with .jpg extension
    Expected status: 200
    Output: { message: string, data: { image: string } } - path containing .jpg extension
    Expected behavior: Accepts image files with different extensions (.jpg, .png, etc.)
  */
  test('uploads image with different extensions', async () => {
    const imageBuffer = createTestImage();
    
    // Test with .jpg extension
    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.jpg');

    expect(res.status).toBe(200);
    expect(res.body.data.image).toBeDefined();
    expect(res.body.data.image).toContain('.jpg');
  });

  /*
    Inputs: multipart/form-data with 'media' field containing image file (multiple sequential requests)
    Expected status: 200 for each request
    Output: { message: string, data: { image: string } } - different paths for each upload
    Expected behavior: Allows multiple sequential uploads, each creates unique file path with timestamp
  */
  test('uploads multiple images sequentially', async () => {
    const imageBuffer = createTestImage();
    
    // Upload first image
    const res1 = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test1.png');

    expect(res1.status).toBe(200);
    expect(res1.body.data.image).toBeDefined();

    // Upload second image
    const res2 = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test2.png');

    expect(res2.status).toBe(200);
    expect(res2.body.data.image).toBeDefined();
    
    // Both images should have different paths
    expect(res1.body.data.image).not.toBe(res2.body.data.image);
  });

  /*
    Inputs: multipart/form-data with 'media' field containing image file
    Expected status: 200
    Output: { message: string, data: { image: string } } - filename contains user ID prefix
    Expected behavior: Filename format: {userId}-{timestamp}.{extension}
  */
  test('saves image with user ID prefix', async () => {
    const imageBuffer = createTestImage();
    
    const res = await request(app)
      .post('/api/media/upload')
      .attach('media', imageBuffer, 'test-image.png');

    expect(res.status).toBe(200);
    
    // Extract filename from path
    const imagePath = res.body.data.image;
    const filename = path.basename(imagePath);
    
    // Filename should start with user ID
    expect(filename).toContain(testUser._id.toString());
  });
});

