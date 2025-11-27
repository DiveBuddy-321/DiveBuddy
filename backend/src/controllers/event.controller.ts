import { NextFunction, Request, Response } from 'express';
import { GetEventResponse, UpdateEventRequest, CreateEventRequest, IEvent } from '../types/event.types';
import logger from '../utils/logger.util';
import { MediaService } from '../services/media.service';
import { eventModel } from '../models/event.model';
import { userModel } from '../models/user.model';
import { Chat } from '../models/chat.model';
import mongoose from 'mongoose';
import { IUser } from '../types/user.types';

export class EventController {
  async getAllEvents(req: Request, res: Response<{ message: string; data?: { events: IEvent[] } }>, next: NextFunction) {
    try {
      const events = await eventModel.findAll();

      res.status(200).json({
        message: 'Events fetched successfully',
        data: { events },
      });
    } catch (error) {
      logger.error('Failed to fetch events:', error);
      next(error);
    }
  }

  async getEventById(req: Request, res: Response<GetEventResponse>, next: NextFunction) {
    try {
      const { id } = req.params as { id: string };
      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid event id' });
      }

      const event = await eventModel.findById(new mongoose.Types.ObjectId(id));
      if (!event) {
        return res.status(404).json({ message: 'Event not found' });
      }

      res.status(200).json({ message: 'Event fetched successfully', data: { event } });
    } catch (error) {
      logger.error('Failed to fetch event by id:', error);
      next(error);
    }
  }

  async createEvent(req: Request<unknown, unknown, CreateEventRequest>, res: Response<GetEventResponse>, next: NextFunction) {
    try {
      const requester = req.user;
      if (!requester?._id) {
        return res.status(401).json({ message: 'Unauthorized' });
      }

      // Add creator to attendees list
      const attendees = req.body.attendees || [];
      if (!attendees.includes(requester._id.toString())) {
        attendees.push(requester._id.toString());
      }

      const payload = {
        ...req.body,
        createdBy: requester._id.toString(),
        attendees: attendees,
      };

      const created = await eventModel.create(payload as CreateEventRequest);

      // Create event group chat and add creator
      try {
        await Chat.findOrCreateEventChat(
          created._id,
          created.title,
          requester._id
        );
        logger.info(`Event group chat created for event: ${created._id}`);
      } catch (chatError) {
        logger.error(`Failed to create event group chat for event ${created._id}:`, chatError);
        // Don't fail event creation if chat creation fails
      }

      // update user's eventsCreated and eventsJoined lists with this event
      const user = await userModel.findById(requester._id);
      if (user) {
        user.eventsCreated = user.eventsCreated || [];
        user.eventsJoined = user.eventsJoined || [];
        
        // Add to eventsCreated if not already there
        if (!user.eventsCreated.some((eId) => eId.equals(created._id))) {
          user.eventsCreated.push(created._id);
        }
        
        // Add to eventsJoined since creator is automatically an attendee
        if (!user.eventsJoined.some((eId) => eId.equals(created._id))) {
          user.eventsJoined.push(created._id);
        }
        
        const userObject = user.toObject() as IUser & { __v?: number };
        const { ...rest } = userObject;
        
        const updateBody = {
          ...rest,
          eventsCreated: user.eventsCreated.map((eId) => eId.toString()),
          eventsJoined: user.eventsJoined.map((eId) => eId.toString()),
        };

        await userModel.update(user._id, updateBody as unknown as Partial<IUser >);
      }

      res.status(201).json({ message: 'Event created successfully', data: { event: created } });
    } catch (error) {
      logger.error('Failed to create event:', error);
      next(error);
    }
  }

  async updateEvent(
    req: Request<unknown, unknown, UpdateEventRequest>,
    res: Response<GetEventResponse>,
    next: NextFunction
  ) {
    try {
      const { id } = req.params as { id: string };

      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid event id' });
      }

      const eventId = new mongoose.Types.ObjectId(id);

      const existing = await eventModel.findById(eventId);
      if (!existing) {
        return res.status(404).json({ message: 'Event not found' });
      }

      const updated = await eventModel.update(eventId, req.body as unknown as Partial<IEvent>);
      if (!updated) {
        return res.status(500).json({ message: 'Failed to update event' });
      }

      res.status(200).json({ message: 'Event updated successfully', data: { event: updated } });
    } catch (error) {
      logger.error('Failed to update event info:', error);
      next(error);
    }
  }

  async joinEvent(req: Request, res: Response, next: NextFunction) {
    try {
      const { id } = req.params as { id: string };
      const requester = req.user;
      if (!requester?._id) {
        return res.status(401).json({ message: 'Unauthorized' });
      }
      
      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid event id' });
      }

      const eventId = new mongoose.Types.ObjectId(id);

      const existing = await eventModel.findById(eventId);
      if (!existing) {
        return res.status(404).json({ message: 'Event not found' });
      }

      if (existing.attendees.includes(requester._id)) {
        return res.status(400).json({ message: 'User already joined the event' });
      }

      if (existing.attendees.length >= existing.capacity) {
        return res.status(400).json({ message: 'Event is at full capacity' });
      }

      existing.attendees.push(requester._id);

      const eventObject = existing.toObject() as IEvent & { __v?: number };
      const { ...rest } = eventObject;

      const updateBody = {
        ...rest,
        attendees: existing.attendees.map((attendeeId) => attendeeId.toString()),
      };

      // update user's eventsJoined list with this event
      const user = await userModel.findById(requester._id);
      if (user) {
        user.eventsJoined = user.eventsJoined || [];

        user.eventsJoined.push(eventId);
        const userObject = user.toObject() as IUser & { __v?: number };
        const { ...rest } = userObject;

        const updateBody = {
          ...rest,
          eventsJoined: user.eventsJoined.map((eId) => eId.toString()),
          eventsCreated: (user.eventsCreated || []).map((eId) => eId.toString()),
        };

        await userModel.update(user._id, updateBody as unknown as Partial<IUser >);
      }

      const updated = await eventModel.update(eventId, updateBody as unknown as Partial<IEvent>);

      if (!updated) {
        return res.status(500).json({ message: 'Failed to update event' });
      }

      // Add user to event group chat
      try {
        const eventChat = await Chat.findOne({ eventId: eventId });
        if (eventChat) {
          await Chat.addParticipant(String(eventChat._id), requester._id);
          logger.info(`User ${requester._id} added to event group chat for event: ${eventId}`);
        }
      } catch (chatError) {
        logger.error(`Failed to add user to event group chat for event ${eventId}:`, chatError);
        // Don't fail join if chat update fails
      }

      res.status(200).json({ message: 'Joined event successfully', data: { event: updated } });

    } catch (error) {
      logger.error('Failed to join event:', error);
      next(error);
    }
  }

  async leaveEvent(req: Request, res: Response, next: NextFunction) {
    try {
      const { id } = req.params as { id: string };
      const requester = req.user;
      if (!requester?._id) {
        return res.status(401).json({ message: 'Unauthorized' });
      }

      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid event id' });
      }

      const eventId = new mongoose.Types.ObjectId(id);

      const existing = await eventModel.findById(eventId);
      if (!existing) {
        return res.status(404).json({ message: 'Event not found' });
      }

      if (!existing.attendees.includes(requester._id)) {
        return res.status(400).json({ message: 'User is not an attendee of the event' });
      }

      // Prevent event creator from leaving the event
      if (existing.createdBy.equals(requester._id)) {
        return res.status(400).json({ message: 'Event creator cannot leave the event' });
      }

      existing.attendees = existing.attendees.filter((attendeeId) => !attendeeId.equals(requester._id));

      const eventObject = existing.toObject() as IEvent & { __v?: number };
      const { ...rest } = eventObject;

      const updateBody = {
        ...rest,
        attendees: existing.attendees.map((attendeeId) => attendeeId.toString()),
      };

      // remove this event from user's eventsJoined list
      const user = await userModel.findById(requester._id);
      if (user) {
        user.eventsJoined = user.eventsJoined || [];

        const userObject = user.toObject() as IUser & { __v?: number };
        const { ...rest } = userObject;

        const updateBody = {
          ...rest,
          eventsJoined: user.eventsJoined.filter((eId) => !eId.equals(eventId)).map((eId) => eId.toString()),
          eventsCreated: (user.eventsCreated || []).map((eId) => eId.toString()),
        };

        await userModel.update(user._id, updateBody as unknown as Partial<IUser >);
      }

      const updated = await eventModel.update(eventId, updateBody as unknown as Partial<IEvent>);
      if (!updated) {
        return res.status(500).json({ message: 'Failed to update event' });
      }

      // Remove user from event group chat
      try {
        const eventChat = await Chat.findOne({ eventId: eventId });
        if (eventChat) {
          await Chat.removeParticipant(String(eventChat._id), requester._id);
          logger.info(`User ${requester._id} removed from event group chat for event: ${eventId}`);
        }
      } catch (chatError) {
        logger.error(`Failed to remove user from event group chat for event ${eventId}:`, chatError);
        // Don't fail leave if chat update fails
      }

      res.status(200).json({ message: 'Left event successfully', data: { event: updated } });
    } catch (error) {
      logger.error('Failed to leave event:', error);
      next(error);
    }
  }

  async deleteEvent(req: Request, res: Response, next: NextFunction) {
    try {
      const { id } = req.params as { id: string };

      if (!mongoose.Types.ObjectId.isValid(id)) {
        return res.status(400).json({ message: 'Invalid event id' });
      }

      const eventId = new mongoose.Types.ObjectId(id);
      const existing = await eventModel.findById(eventId);
      if (!existing) {
        return res.status(404).json({ message: 'Event not found' });
      }

      // delete related media if any
      if (existing.photo) {
        MediaService.deleteImage(existing.photo);
      }

      // Delete event group chat (this removes all participants including creator)
      try {
        const eventChat = await Chat.findOne({ eventId: eventId });
        if (eventChat) {
          await Chat.deleteOne({ _id: eventChat._id });
          logger.info(`Event group chat deleted for event: ${eventId} (owner and all participants removed)`);
        }
      } catch (chatError) {
        logger.error(`Failed to delete event group chat for event ${eventId}:`, chatError);
        // Don't fail event deletion if chat cleanup fails
      }

      // bulk remove the event from all attendees' eventsJoined lists
      if (existing?.attendees && existing.attendees.length > 0) {
        await userModel.updateMany(
          { _id: { $in: existing.attendees } },
          { $pull: { eventsJoined: existing._id } }
        );
      }

      const createdByUser = await userModel.findById(existing.createdBy);
      if (createdByUser) {
        const userObject = createdByUser.toObject() as IUser & { __v?: number };
        const { ...rest } = userObject;

        const updateBody = {
          ...rest,
          eventsCreated: createdByUser.eventsCreated.filter((eventId) => !eventId.equals(existing._id)).map((eId) => eId.toString()),
          eventsJoined: (createdByUser.eventsJoined || []).map((eId) => eId.toString()),
        };

        await userModel.update(createdByUser._id, updateBody as unknown as Partial<IUser>);
      }

      await eventModel.delete(eventId);

      res.status(200).json({ message: 'Event deleted successfully' });
    } catch (error) {
      logger.error('Failed to delete event:', error);
      next(error);
    }
  }
} 
