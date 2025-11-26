import { NextFunction, Request, Response } from 'express';
import mongoose from 'mongoose';

import { blockModel } from '../models/block.model';
import type { BlockResponse, BlockUserRequest } from '../types/block.types';
import logger from '../utils/logger.util';

export class BlockController {
  async blockUser(req: Request<unknown, unknown, BlockUserRequest>, res: Response<BlockResponse>, next: NextFunction) {
    try {
      const authUser = req.user;
      if (!authUser?._id) {
        return res.status(401).json({ message: 'Unauthorized' });
      }

      const { targetUserId } = req.body;
      if (!mongoose.Types.ObjectId.isValid(targetUserId)) {
        return res.status(400).json({ message: 'Invalid target user id' });
      }

      const blockerId = new mongoose.Types.ObjectId(authUser._id);
      const blockedId = new mongoose.Types.ObjectId(targetUserId);

      await blockModel.blockUser(blockerId, blockedId);
      console.log('User blocked successfully');

      return res.status(201).json({ message: 'User blocked successfully' });
    } catch (error) {
      if (error instanceof Error && error.message.includes('Cannot block yourself')) {
        return res.status(400).json({ message: error.message });
      }
      logger.error('Failed to block user:', error);
      next(error);
    }
  }

  async unblockUser(req: Request, res: Response<BlockResponse>, next: NextFunction) {
    try {
      const authUser = req.user;
      if (!authUser?._id) {
        return res.status(401).json({ message: 'Unauthorized' });
      }

      const { targetUserId } = req.params as { targetUserId: string };
      if (!mongoose.Types.ObjectId.isValid(targetUserId)) {
        return res.status(400).json({ message: 'Invalid target user id' });
      }

      const blockerId = new mongoose.Types.ObjectId(authUser._id);
      const blockedId = new mongoose.Types.ObjectId(targetUserId);
      await blockModel.unblockUser(blockerId, blockedId);
      console.log('User unblocked successfully');

      return res.status(200).json({ message: 'User unblocked successfully' });
      
    } catch (error) {
      logger.error('Failed to unblock user:', error);
      next(error);
    }
  }

  async getBlockedUsers(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const authUser = req.user;
      if (!authUser?._id) {
        res.status(401).json({ message: 'Unauthorized' });
        return;
      }

      const blockerId = new mongoose.Types.ObjectId(authUser._id);
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-call
      const blockedUserObjectIds = await blockModel.getBlockedUsers(blockerId);
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-call, @typescript-eslint/no-unsafe-member-access, @typescript-eslint/no-unsafe-return
      const blockedUserIds = blockedUserObjectIds.map((id) => id.toString());

      res.status(200).json({ 
        message: 'Blocked users fetched successfully',
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
        data: { blockedUserIds }
      });
    } catch (error) {
      logger.error('Failed to fetch blocked users:', error);
      next(error);
    }
  }
}


