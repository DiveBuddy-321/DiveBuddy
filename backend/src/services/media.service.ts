import fs from 'fs';
import path from 'path';

import { IMAGES_DIR } from '../constants/statics';

export class MediaService {
  // Validate that a path is within the expected directory
  private static isPathSafe(filePath: string, baseDir: string): boolean {
    const resolvedPath = path.resolve(filePath);
    const resolvedBase = path.resolve(baseDir);
    return resolvedPath.startsWith(resolvedBase);
  }

  static saveImage(filePath: string, userId: string): string {
    try {
      // Validate input path is safe
      const uploadsDir = path.resolve('./uploads');
      if (!this.isPathSafe(filePath, uploadsDir)) {
        throw new Error('Invalid file path');
      }

      const fileExtension = path.extname(filePath);
      const fileName = `${userId}-${Date.now()}${fileExtension}`;
      const newPath = path.join(IMAGES_DIR, fileName);

      fs.renameSync(filePath, newPath);

      return newPath.split(path.sep).join('/');
    } catch (error) {
      // Clean up uploaded file on error
      try {
        const uploadsDir = path.resolve('./uploads');
        if (this.isPathSafe(filePath, uploadsDir)) {
          fs.unlinkSync(filePath);
        }
      } catch {
        // Ignore cleanup errors
      }
      throw new Error(`Failed to save profile picture: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  static deleteImage(url: string): void {
    try {
      if (url.startsWith(IMAGES_DIR)) {
        const filePath = path.join(process.cwd(), url.substring(1));
        const imagesDir = path.resolve(IMAGES_DIR);
        // Validate path is within IMAGES_DIR to prevent path traversal
        if (this.isPathSafe(filePath, imagesDir)) {
          try {
            fs.unlinkSync(filePath);
          } catch {
            // File doesn't exist or cannot be deleted
          }
        }
      }
    } catch (error) {
      console.error('Failed to delete old profile picture:', error);
    }
  }

  static deleteAllUserImages(userId: string): void {
    try {
      // Try to read directory; if it doesn't exist, readdirSync will throw
      const dirPath = path.resolve(IMAGES_DIR);
      if (!dirPath.startsWith(path.resolve(__dirname, '../uploads'))) {
        throw new Error('Unsafe image directory path');
      }
      const files = fs.readdirSync(dirPath);
      const userFiles = files.filter(file => file.startsWith(userId + '-'));

      userFiles.forEach((file) => {this.deleteImage(file);});
    } catch (error) {
      // Directory doesn't exist or cannot be read
      console.error('Failed to delete user images:', error);
    }
  }
} 
