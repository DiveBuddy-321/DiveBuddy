import { NextFunction, Request, Response } from 'express';
import { GetEventResponse, UpdateEventRequest, CreateEventRequest, IEvent } from '../types/event.types';
import logger from '../utils/logger.util';
import { MediaService } from '../services/media.service';
import { eventModel } from '../models/event.model';
import mongoose from 'mongoose';

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

      const payload: CreateEventRequest & { createdBy?: string } = {
        ...req.body,
        createdBy: requester._id.toString(),
      };

      const created = await eventModel.create(payload);

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

      if (error instanceof Error) {
        return res.status(500).json({
          message: error.message || 'Failed to update event info',
        });
      }

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
      const { createdAt: _createdAt, updatedAt: _updatedAt, createdBy: _createdBy, _id, ...rest } = eventObject;

      const updateBody = {
        ...rest,
        attendees: existing.attendees.map((attendeeId) => attendeeId.toString()),
      };

      const updated = await eventModel.update(eventId, updateBody as unknown as Partial<IEvent>);
      if (!updated) {
        return res.status(500).json({ message: 'Failed to update event' });
      }
      res.status(200).json({ message: 'Joined event successfully', data: { event: updated } });

    } catch (error) {
      logger.error('Failed to join event:', error);

      if (error instanceof Error) {
        return res.status(500).json({
          message: 'Failed to join event',
        });
      }

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

      existing.attendees = existing.attendees.filter((attendeeId) => !attendeeId.equals(requester._id));

      const eventObject = existing.toObject() as IEvent & { __v?: number };
      const { createdAt: _createdAt, updatedAt: _updatedAt, createdBy: _createdBy, _id, ...rest } = eventObject;

      const updateBody = {
        ...rest,
        attendees: existing.attendees.map((attendeeId) => attendeeId.toString()),
      };

      const updated = await eventModel.update(eventId, updateBody as unknown as Partial<IEvent>);
      if (!updated) {
        return res.status(500).json({ message: 'Failed to update event' });
      }
      res.status(200).json({ message: 'Left event successfully', data: { event: updated } });
    } catch (error) {
      logger.error('Failed to leave event:', error);

      if (error instanceof Error) {
        return res.status(500).json({
          message: 'Failed to leave event',
        });
      }

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
        await MediaService.deleteImage(existing.photo);
      }

      await eventModel.delete(eventId);

      res.status(200).json({ message: 'Event deleted successfully' });
    } catch (error) {
      logger.error('Failed to delete event:', error);

      if (error instanceof Error) {
        return res.status(500).json({
          message: error.message || 'Failed to delete event',
        });
      }

      next(error);
    }
  }
}
