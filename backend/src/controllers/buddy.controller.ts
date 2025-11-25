import { NextFunction, Request, Response } from 'express';

import logger from '../utils/logger.util';
import { GetAllBuddiesResponse } from '../types/buddy.types';
import { buddyAlgorithm } from '../utils/buddyAlgorithm.util';
import { isUserReadyForBuddyMatching } from '../types/user.types';
import { buddyModel } from '../models/buddy.model';
import { parseBuddyFilters, toBuddyResponse, toNumericLevelFromSkill } from '../utils/buddyhelpers.util';

export class BuddyController {
  async getAllBuddies(
    req: Request,
    res: Response<GetAllBuddiesResponse>,
    next: NextFunction
  ) {
    try {
      const currentUser = req.user ?? null;
      if (!currentUser) {
        return res.status(401).json({ message: 'Unauthorized' });
      }

      if (!isUserReadyForBuddyMatching(currentUser)) {
        return res.status(400).json({
          message: 'Please complete your profile (age, level, location) before finding buddies',
        });
      }
      const filters = parseBuddyFilters(req, currentUser);
      const excludedUserIds = await buddyModel.getDirectPartnerIds(currentUser._id);
      const query = buddyModel.buildMongoQuery(currentUser, filters, excludedUserIds);
      const otherUsers = await buddyModel.getUsers(query);

      const currentLong = currentUser.longitude;
      const currentLat = currentUser.latitude;
      const currentLevel = toNumericLevelFromSkill(currentUser.skillLevel);

      if (
        currentLong === undefined ||
        currentLat === undefined ||
        currentLevel === undefined ||
        currentUser.age === undefined
      ) {
        return res.status(500).json({
          message: 'Missing required fields: longitude, latitude, level, age',
        });
      }

      const sortedBuddies = buddyAlgorithm(
        currentLong,
        currentLat,
        currentLevel,
        currentUser.age,
        filters.targetMinLevel,
        filters.targetMaxLevel,
        filters.targetMinAge,
        filters.targetMaxAge,
        otherUsers
      );
      const buddies = toBuddyResponse(sortedBuddies);

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
