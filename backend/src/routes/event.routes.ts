import { Router } from 'express';

import { EventController } from '../controllers/event.controller';
import { CreateEventRequest, UpdateEventRequest, GetEventResponse, IEvent, createEventSchema, updateEventSchema } from '../types/event.types';
import { validateBody } from '../middleware/validation.middleware';
import { asyncHandler } from '../utils/asyncHandler.util';
import type { ParamsDictionary } from 'express-serve-static-core';

const router = Router();
const eventController = new EventController();

router.get('/', asyncHandler<ParamsDictionary, { message: string; data?: { events: IEvent[] } }>(
  async (req, res, next) => {
    await eventController.getAllEvents(req, res, next);
  }
));

router.get('/:id', asyncHandler<ParamsDictionary, GetEventResponse>(
  async (req, res, next) => {
    await eventController.getEventById(req, res, next);
  }
));

router.post('/', 
  validateBody<CreateEventRequest>(createEventSchema), 
  asyncHandler<unknown, GetEventResponse, CreateEventRequest>(
    async (req, res, next) => {
      await eventController.createEvent(req, res, next);
    }
  )
);

router.put('/join/:id', asyncHandler<ParamsDictionary, unknown>(
  async (req, res, next) => {
    await eventController.joinEvent(req, res, next);
  }
));

router.put('/leave/:id', asyncHandler<ParamsDictionary, unknown>(
  async (req, res, next) => {
    await eventController.leaveEvent(req, res, next);
  }
));

router.put('/:id', 
  validateBody<UpdateEventRequest>(updateEventSchema), 
  asyncHandler<unknown, GetEventResponse, UpdateEventRequest>(
    async (req, res, next) => {
      await eventController.updateEvent(req, res, next);
    }
  )
);

router.delete('/:id', asyncHandler<ParamsDictionary, unknown>(
  async (req, res, next) => {
    await eventController.deleteEvent(req, res, next);
  }
));

export default router;
