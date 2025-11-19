import { NextFunction, Request, Response } from 'express';

import logger from '../utils/logger.util';
import { GetAllBuddiesResponse } from '../types/buddy.types';
import { buddyAlgorithm } from '../utils/buddyAlgorithm.util';
import { isUserReadyForBuddyMatching, IUser } from '../types/user.types';
import { userModel } from '../models/user.model';
import { Chat } from '../models/chat.model';
import { SKILL_LEVELS } from '../constants/statics';
import { FilterQuery } from 'mongoose';

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
          message:
            'Please complete your profile (age, level, location) before finding buddies',
        });
      }

      const filters = this.parseFilters(req, currentUser);
      const query = this.buildMongoQuery(currentUser, filters);
      const otherUsers = await this.getUsers(query);

      // Get list of users who already have a direct chat with the current user
      const matchedUserIds = await this.getMatchedUserIds(currentUser._id);

      // Filter out users who already have a chat with the current user
      const unmatchedUsers = otherUsers.filter(
        user => !matchedUserIds.has(String(user._id))
      );

      const currentLong = currentUser.longitude;
      const currentLat = currentUser.latitude;
      const currentLevel = this.toNumericLevel(currentUser.skillLevel);

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
        unmatchedUsers
      );

      const buddies = this.toBuddyResponse(sortedBuddies);

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

  private async getUsers(query: Record<string, unknown>): Promise<IUser[]> {
    try {
      const result = await userModel.findByQuery(
        query as unknown as FilterQuery<IUser>,
        1000
      );
      return result;
    } catch (error) {
      logger.error('Failed to fetch buddies:', error);
      throw new Error('Failed to fetch buddies');
    }
  }

  private async getMatchedUserIds(userId: any): Promise<Set<string>> {
    try {
      // Find all direct chats (2 participants) where the current user is a participant
      const chats = await Chat.find({
        participants: userId,
        $expr: { $eq: [{ $size: '$participants' }, 2] },
      })
        .select('participants')
        .lean()
        .exec();

      // Extract the other participant's ID from each chat
      const matchedIds = new Set<string>();
      for (const chat of chats) {
        for (const participantId of chat.participants) {
          const idStr = String(participantId);
          if (idStr !== String(userId)) {
            matchedIds.add(idStr);
          }
        }
      }

      return matchedIds;
    } catch (error) {
      logger.error('Failed to fetch matched users:', error);
      // Return empty set on error to avoid breaking the buddy matching
      return new Set<string>();
    }
  }

  private parseFilters(
    req: Request,
    currentUser: IUser
  ): {
    targetMinLevel?: number;
    targetMaxLevel?: number;
    targetMinAge?: number;
    targetMaxAge?: number;
    allowedSkillLevels: string[];
    ageFilter: Record<string, number>;
    locationFilter: {
      latitude: Record<string, number>;
      longitude: Record<string, number>;
    };
  } {
    const targetMinLevel =
      req.query.targetMinLevel !== undefined
        ? Number(req.query.targetMinLevel)
        : undefined;
    const targetMaxLevel =
      req.query.targetMaxLevel !== undefined
        ? Number(req.query.targetMaxLevel)
        : undefined;
    const targetMinAge =
      req.query.targetMinAge !== undefined
        ? Number(req.query.targetMinAge)
        : undefined;
    const targetMaxAge =
      req.query.targetMaxAge !== undefined
        ? Number(req.query.targetMaxAge)
        : undefined;

    const levelMin = targetMinLevel ?? 1;
    const levelMax = targetMaxLevel ?? SKILL_LEVELS.length;
    const allowedSkillLevels = SKILL_LEVELS.slice(
      Math.max(0, levelMin - 1),
      Math.min(SKILL_LEVELS.length, levelMax)
    );

    const ageFilter: Record<string, number> = {};
    if (targetMinAge !== undefined) ageFilter.$gte = targetMinAge;
    if (targetMaxAge !== undefined) ageFilter.$lte = targetMaxAge;

    const locationFilter = {
      latitude: {
        $gte: (currentUser.latitude ?? 0) - 5,
        $lte: (currentUser.latitude ?? 0) + 5,
      },
      longitude: {
        $gte: (currentUser.longitude ?? 0) - 5,
        $lte: (currentUser.longitude ?? 0) + 5,
      },
    };

    return {
      targetMinLevel,
      targetMaxLevel,
      targetMinAge,
      targetMaxAge,
      allowedSkillLevels,
      ageFilter,
      locationFilter,
    };
  }

  private buildMongoQuery(
    currentUser: IUser,
    filters: {
      allowedSkillLevels: string[];
      ageFilter: Record<string, number>;
      locationFilter: {
        latitude: Record<string, number>;
        longitude: Record<string, number>;
      };
    }
  ): Record<string, unknown> {
    const { allowedSkillLevels, ageFilter, locationFilter } = filters;

    return {
      _id: { $ne: currentUser._id },
      ...locationFilter,
      ...(Object.keys(ageFilter).length ? { age: ageFilter } : {}),
      ...(allowedSkillLevels.length
        ? { skillLevel: { $in: allowedSkillLevels } }
        : {}),
    } as Record<string, unknown>;
  }

  private toNumericLevel(skillLevel: unknown): number | undefined {
    if (typeof skillLevel !== 'string') return undefined;
    if (!SKILL_LEVELS.includes(skillLevel as (typeof SKILL_LEVELS)[number]))
      return undefined;
    const idx = SKILL_LEVELS.indexOf(
      skillLevel as (typeof SKILL_LEVELS)[number]
    );
    return idx === -1 ? undefined : idx + 1;
  }

  private toBuddyResponse(
    sortedBuddies: Array<[IUser, number]>
  ): Array<{ user: IUser; distance: number }> {
    return sortedBuddies.map(([user, distance]) => ({ user, distance }));
  }
}
