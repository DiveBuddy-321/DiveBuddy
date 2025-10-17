import { NextFunction, Request, Response } from 'express';

import logger from '../utils/logger.util';
import { GetAllBuddiesResponse } from '../types/buddy.types';
import { buddyAlgorithm } from '../utils/buddyAlgorithm.util';
import { isUserReadyForBuddyMatching } from '../types/user.types';
import { userModel } from '../models/user.model';

export class BuddyController {
  async getAllBuddies(
    req: Request,
    res: Response<GetAllBuddiesResponse>,
    next: NextFunction
  ) {
    try {
      const currentUser = req.user!;

      //age and level filters
      const targetMinLevel = req.query.targetMinLevel as number | undefined;
      const targetMaxLevel = req.query.targetMaxLevel as number | undefined;
      const targetMinAge = req.query.targetMinAge as number | undefined;
      const targetMaxAge = req.query.targetMaxAge as number | undefined;

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
      const sortedBuddies = buddyAlgorithm(
        currentUser.long!,
        currentUser.lat!,
        currentUser.level!,
        currentUser.age!,
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
