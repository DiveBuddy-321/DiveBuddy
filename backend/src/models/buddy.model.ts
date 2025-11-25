import mongoose from 'mongoose';
import { FilterQuery } from 'mongoose';
import { IUser } from '../types/user.types';
import logger from '../utils/logger.util';
import { userModel } from './user.model';
import { Chat } from './chat.model';

export class BuddyModel {
  async getUsers(query: Record<string, unknown>): Promise<IUser[]> {
    try {
      const result = await userModel.findByQuery(query as unknown as FilterQuery<IUser>, 1000);
      return result;
    } catch (error) {
      logger.error('Failed to fetch buddies:', error);
      throw new Error('Failed to fetch buddies');
    }
  }

  buildMongoQuery(
    currentUser: IUser,
    filters: {
      allowedSkillLevels: string[];
      ageFilter: Record<string, number>;
      locationFilter: { latitude: Record<string, number>; longitude: Record<string, number> };
    },
    excludedUserIds?: mongoose.Types.ObjectId[]
  ): Record<string, unknown> {
    const { allowedSkillLevels, ageFilter, locationFilter } = filters;

    const idFilter: { $ne: mongoose.Types.ObjectId; $nin?: mongoose.Types.ObjectId[] } = { $ne: currentUser._id };
    if (Array.isArray(excludedUserIds) && excludedUserIds.length > 0) {
      idFilter.$nin = excludedUserIds;
    }

    return {
      _id: idFilter,
      ...locationFilter,
      ...(Object.keys(ageFilter).length ? { age: ageFilter } : {}),
      ...(allowedSkillLevels.length ? { skillLevel: { $in: allowedSkillLevels } } : {}),
    } as Record<string, unknown>;
  }

  async getDirectPartnerIds(
    userId: mongoose.Types.ObjectId
  ): Promise<mongoose.Types.ObjectId[]> {
    const chats = await Chat.find(
      {
        participants: userId,
        $expr: { $eq: [{ $size: "$participants" }, 2] }
      },
      { participants: 1 }
    )
      .lean()
      .exec();

    const partnerIdStrings = new Set<string>();
    for (const chat of chats as Array<{ participants: Array<mongoose.Types.ObjectId | string> }>) {
      const other = chat.participants.find((p) => String(p) !== String(userId));
      if (other) {
        partnerIdStrings.add(String(other));
      }
    }

    return Array.from(partnerIdStrings).map((id) => new mongoose.Types.ObjectId(id));
  }
}

export const buddyModel = new BuddyModel();


