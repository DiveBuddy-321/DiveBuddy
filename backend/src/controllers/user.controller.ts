import { NextFunction, Request, Response } from 'express';
import mongoose from 'mongoose';

import { GetProfileResponse, UpdateProfileRequest } from '../types/user.types';
import logger from '../utils/logger.util';
import { MediaService } from '../services/media.service';
import { userModel } from '../models/user.model';
import { eventModel } from '../models/event.model';
import type { IUser } from '../types/user.types';
import type { IEvent } from '../types/event.types';

export class UserController {
  async getAllProfiles(req: Request, res: Response<{ message: string; data?: { users: IUser[] } }>, next: NextFunction) {
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

      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid user id' });
      }

      const user = await userModel.findById(new mongoose.Types.ObjectId(id));

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

      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid user id' });
      }

      const userId = new mongoose.Types.ObjectId(id);

      const user = await userModel.findById(userId);
      if (!user) {
        return res.status(404).json({ message: 'User not found' });
      }

      MediaService.deleteAllUserImages(user._id.toString());

      for (const eventId of user?.eventsJoined || []) {
        // Remove user from all joined events
        const eventData = await eventModel.findById(eventId);

        if (eventData) {
          const eventObject = eventData.toObject() as IEvent & { __v?: number };
          const { ...rest } = eventObject;

          const updateBody = {
            ...rest,
            attendees: eventData.attendees.filter((attendeeId) => !attendeeId.equals(user._id)).map((attendeeId) => attendeeId.toString()),
          };

          await eventModel.update(eventId, updateBody as unknown as Partial<IEvent>);
        }
      }

      // delete all events created by the user
      for (const eventId of user?.eventsCreated || []) {
        const eventData = await eventModel.findById(eventId);

        if (eventData) {
          
          for (const attendeeId of eventData.attendees) {
            // Remove event from all attendees' eventsJoined list
            const attendeeData = await userModel.findById(attendeeId);
            if (attendeeData) {
              const attendeeObject = attendeeData.toObject() as IUser & { __v?: number };
              const { ...attendeeRest } = attendeeObject;

              const attendeeUpdateBody = {
                ...attendeeRest,
                eventsJoined: attendeeData.eventsJoined.filter((eId) => !eId.equals(eventId)).map((eId) => eId.toString()),
                eventsCreated: (attendeeData.eventsCreated || []).map((eId) => eId.toString()),
              };

              await userModel.update(attendeeId, attendeeUpdateBody as unknown as Partial<IUser>);
            }
          }
        }

        await eventModel.delete(eventId);
      }

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

      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid user id' });
      }

      const targetId = new mongoose.Types.ObjectId(id);
      const targetUser = await userModel.findById(targetId);
      if (!targetUser) {
        return res.status(404).json({ message: 'User not found' });
      }

      const updated = await userModel.update(targetId, req.body as Partial<IUser>);

      if (!updated) {
        return res.status(500).json({ message: 'Failed to update user' });
      }

      res.status(200).json({ message: 'User updated successfully', data: { user: updated } });
    } catch (error) {
      logger.error('Failed to update user by id:', error);
      next(error);
    }
  }
  getProfile(req: Request, res: Response<GetProfileResponse>, next: NextFunction) {
    try {
    const user = req.user;
    if (!user) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    res.status(200).json({
      message: 'Profile fetched successfully',
        data: { user },
      });
    } catch (error) {
      logger.error('Failed to fetch profile:', error);
      next(error);
    }
  }

  async updateProfile(
    req: Request<unknown, unknown, UpdateProfileRequest>,
    res: Response<GetProfileResponse>,
    next: NextFunction
  ) {
    try {
      const user = req.user;
      if (!user?._id) {
        return res.status(401).json({ message: 'Unauthorized' });
      }
      const updateData = { ...req.body };


      const updatedUser = await userModel.update(user._id, updateData as Partial<IUser>);

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
      next(error);
    }
  }

  async deleteProfile(req: Request, res: Response, next: NextFunction) {
    try {
      const user = req.user;
      if (!user?._id) {
        return res.status(401).json({ message: 'Unauthorized' });
      }

      MediaService.deleteAllUserImages(user._id.toString());

      const userId = new mongoose.Types.ObjectId(user._id);
      const userData = await userModel.findById(userId);

      for (const eventId of userData?.eventsJoined || []) {
        // Remove user from all joined events
        const eventData = await eventModel.findById(eventId);

        if (eventData) {
          const eventObject = eventData.toObject() as IEvent & { __v?: number };
          const { ...rest } = eventObject;

          const updateBody = {
            ...rest,
            attendees: eventData.attendees.filter((attendeeId) => !attendeeId.equals(userId)).map((attendeeId) => attendeeId.toString()),
          };
          await eventModel.update(eventId, updateBody as unknown as Partial<IEvent>);
        }
      }

      // delete all events created by the user
      for (const eventId of userData?.eventsCreated || []) {
        const eventData = await eventModel.findById(eventId);

        if (eventData) {

          for (const attendeeId of eventData.attendees) {
            // Remove event from all attendees' eventsJoined list
            const attendeeData = await userModel.findById(attendeeId);
            if (attendeeData) {
              const attendeeObject = attendeeData.toObject() as IUser & { __v?: number };
              const { ...attendeeRest } = attendeeObject;

              const attendeeUpdateBody = {
                ...attendeeRest,
                eventsJoined: attendeeData.eventsJoined.filter((eId) => !eId.equals(eventId)).map((eId) => eId.toString()),
                eventsCreated: (attendeeData.eventsCreated || []).map((eId) => eId.toString()),
              };

              await userModel.update(attendeeId, attendeeUpdateBody as unknown as Partial<IUser>);
            }
          }
        }

        await eventModel.delete(eventId);
      }

      await userModel.delete(user._id);

      res.status(200).json({
        message: 'User deleted successfully',
      });
    } catch (error) {
      logger.error('Failed to delete user:', error);
      next(error);
    }
  }
}
