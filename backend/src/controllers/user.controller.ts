import { NextFunction, Request, Response } from 'express';

import { GetProfileResponse, UpdateProfileRequest } from '../types/user.types';
import logger from '../utils/logger.util';
import { MediaService } from '../services/media.service';
import { userModel } from '../models/user.model';
// geocoding removed; location is provided directly by client

export class UserController {
  async getAllProfiles(req: Request, res: Response<{ message: string; data?: { users: any[] } }>, next: NextFunction) {
    try {
      const users = await userModel.findAll();

      res.status(200).json({
        message: 'Profiles fetched successfully',
        data: { users },
      });
    } catch (error) {
      logger.error('Failed to fetch profiles:', error);
      next(error);
    }
  }

  async getProfileById(req: Request, res: Response<GetProfileResponse>, next: NextFunction) {
    try {
      const { id } = req.params as { id: string };

      const user = await userModel.findById(new (require('mongoose').Types.ObjectId)(id));

      if (!user) {
        return res.status(404).json({ message: 'User not found' });
      }

      res.status(200).json({ message: 'Profile fetched successfully', data: { user } });
    } catch (error) {
      logger.error('Failed to fetch profile by id:', error);
      next(error);
    }
  }

  async deleteProfileById(req: Request, res: Response, next: NextFunction) {
    try {
      const { id } = req.params as { id: string };

      const mongoose = require('mongoose');
      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid user id' });
      }

      const userId = new mongoose.Types.ObjectId(id);

      const user = await userModel.findById(userId);
      if (!user) {
        return res.status(404).json({ message: 'User not found' });
      }

      await MediaService.deleteAllUserImages(user._id.toString());
      await userModel.delete(user._id);

      res.status(200).json({ message: 'User deleted successfully' });
    } catch (error) {
      logger.error('Failed to delete user by id:', error);
      next(error);
    }
  }
  async updateProfileById(
    req: Request<unknown, unknown, UpdateProfileRequest>,
    res: Response<GetProfileResponse>,
    next: NextFunction
  ) {
    try {
      const { id } = req.params as { id: string };

      const mongoose = require('mongoose');
      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid user id' });
      }

      const targetId = new mongoose.Types.ObjectId(id);
      const targetUser = await userModel.findById(targetId);
      if (!targetUser) {
        return res.status(404).json({ message: 'User not found' });
      }

      const updated = await userModel.update(targetId, req.body);

      if (!updated) {
        return res.status(500).json({ message: 'Failed to update user' });
      }

      res.status(200).json({ message: 'User updated successfully', data: { user: updated } });
    } catch (error) {
      logger.error('Failed to update user by id:', error);
      next(error);
    }
  }
  getProfile(req: Request, res: Response<GetProfileResponse>) {
    const user = req.user!;

    res.status(200).json({
      message: 'Profile fetched successfully',
      data: { user },
    });
  }

  async updateProfile(
    req: Request<unknown, unknown, UpdateProfileRequest>,
    res: Response<GetProfileResponse>,
    next: NextFunction
  ) {
    try {
      const user = req.user!;
      const updateData = { ...req.body };


      const updatedUser = await userModel.update(user._id, updateData);

      if (!updatedUser) {
        return res.status(404).json({
          message: 'User not found',
        });
      }

      res.status(200).json({
        message: 'User info updated successfully',
        data: { user: updatedUser },
      });
    } catch (error) {
      logger.error('Failed to update user info:', error);

      if (error instanceof Error) {
        return res.status(500).json({
          message: error.message || 'Failed to update user info',
        });
      }

      next(error);
    }
  }

  async deleteProfile(req: Request, res: Response, next: NextFunction) {
    try {
      const user = req.user!;

      await MediaService.deleteAllUserImages(user._id.toString());

      await userModel.delete(user._id);

      res.status(200).json({
        message: 'User deleted successfully',
      });
    } catch (error) {
      logger.error('Failed to delete user:', error);

      if (error instanceof Error) {
        return res.status(500).json({
          message: error.message || 'Failed to delete user',
        });
      }

      next(error);
    }
  }
}
