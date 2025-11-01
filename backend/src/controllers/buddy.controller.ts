import { NextFunction, Request, Response } from 'express';

import logger from '../utils/logger.util';
import { GetAllBuddiesResponse } from '../types/buddy.types';
import { buddyAlgorithm } from '../utils/buddyAlgorithm.util';
import { isUserReadyForBuddyMatching } from '../types/user.types';
import { userModel } from '../models/user.model';
import { SKILL_LEVELS } from '../constants/statics';

export class BuddyController {
  async getAllBuddies(
    req: Request,
    res: Response<GetAllBuddiesResponse>,
    next: NextFunction
  ) {
    try {
      const currentUser = req.user ?? null;

      if (!currentUser) {
        return res.status(401).json({
          message: 'Unauthorized',
        });
      }

      //age and level filters
      const targetMinLevel = req.query.targetMinLevel !== undefined ? Number(req.query.targetMinLevel) : undefined;
      const targetMaxLevel = req.query.targetMaxLevel !== undefined ? Number(req.query.targetMaxLevel) : undefined;
      const targetMinAge = req.query.targetMinAge !== undefined ? Number(req.query.targetMinAge) : undefined;
      const targetMaxAge = req.query.targetMaxAge !== undefined ? Number(req.query.targetMaxAge) : undefined;

      // Check if current user has completed their profile
      if (!isUserReadyForBuddyMatching(currentUser)) {
        return res.status(400).json({
          message: 'Please complete your profile (age, level, location) before finding buddies',
        });
      }

      // Fetch all users from database except the current user
      const allUsers = await userModel.findAll();
      const otherUsers = allUsers.filter(
        user => user._id.toString() !== currentUser._id.toString()
      );

      // Run buddy matching algorithm
      const currentLong = currentUser.longitude;
      const currentLat = currentUser.latitude;
      const currentLevel = ((): number | undefined => {
        if (currentUser.skillLevel !== undefined) {
          const idx = SKILL_LEVELS.indexOf(currentUser.skillLevel);
          return idx === -1 ? undefined : idx + 1;
        }
        return undefined;
      })();

      if (!currentLong || !currentLat || !currentLevel || !currentUser.age) {
        return res.status(500).json({
          message: 'Missing required fields: longitude, latitude, level, age',
        });
      }

      const sortedBuddies = buddyAlgorithm(
        currentLong,
        currentLat,
        currentLevel,
        currentUser.age,
        targetMinLevel,
        targetMaxLevel,
        targetMinAge,
        targetMaxAge,
        otherUsers
      );

      // Transform results to match the response type
      const buddies = sortedBuddies.map(([user, distance]) => ({
        user,
        distance,
      }));

      return res.status(200).json({
        message: 'Buddies fetched successfully',
        data: { buddies },
      });
    } catch (error) {
      logger.error('Failed to fetch buddies:', error);

      if (error instanceof Error) {
        return res.status(500).json({
          message: error.message || 'Failed to fetch buddies',
        });
      }

      next(error);
    }
  }
}
