import { NextFunction, Request, Response } from 'express';

import { GetProfileResponse, UpdateProfileRequest } from '../types/user.types';
import logger from '../utils/logger.util';
import { MediaService } from '../services/media.service';
import { userModel } from '../models/user.model';
import { getLocationFromCoordinates } from '../utils/geoCoding.util';

export class UserController {
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

      // If location coordinates are being updated, geocode them
      if (updateData.lat !== undefined && updateData.long !== undefined) {
        try {
          logger.info(`Geocoding location for user ${user._id}: (${updateData.lat}, ${updateData.long})`);
          const locationInfo = await getLocationFromCoordinates(
            updateData.lat,
            updateData.long
          );
          
          updateData.city = locationInfo.city;
          updateData.province = locationInfo.province;
          updateData.country = locationInfo.country;
          
          logger.info(`Geocoded to: ${locationInfo.city}, ${locationInfo.province}, ${locationInfo.country}`);
        } catch (error) {
          logger.error('Geocoding failed:', error);
          // Continue with update even if geocoding fails
        }
      }

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
