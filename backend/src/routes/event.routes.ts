import { Router } from 'express';
import type { Request, Response, NextFunction } from 'express';

import { EventController } from '../controllers/event.controller';
import { CreateEventRequest, UpdateEventRequest, GetEventResponse, IEvent, createEventSchema, updateEventSchema } from '../types/event.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const eventController = new EventController();

router.get('/', async (req: Request, res: Response<{ message: string; data?: { events: IEvent[] } }>, next: NextFunction) => {
  try { await eventController.getAllEvents(req, res, next); }
  catch (err: unknown) { next(err); }
});

router.get('/:id', async (req: Request, res: Response<GetEventResponse>, next: NextFunction) => {
  try { await eventController.getEventById(req, res, next); }
  catch (err: unknown) { next(err); }
});

router.post('/', validateBody<CreateEventRequest>(createEventSchema), async (req: Request<unknown, unknown, CreateEventRequest>, res: Response<GetEventResponse>, next: NextFunction) => {
  try { await eventController.createEvent(req, res, next); }
  catch (err: unknown) { next(err); }
});

router.put('/join/:id', async (req: Request, res: Response, next: NextFunction) => {
  try { await eventController.joinEvent(req, res, next); }
  catch (err: unknown) { next(err); }
});

router.put('/leave/:id', async (req: Request, res: Response, next: NextFunction) => {
  try { await eventController.leaveEvent(req, res, next); }
  catch (err: unknown) { next(err); }
});

router.put('/:id', validateBody<UpdateEventRequest>(updateEventSchema), async (req: Request<unknown, unknown, UpdateEventRequest>, res: Response<GetEventResponse>, next: NextFunction) => {
  try { await eventController.updateEvent(req, res, next); }
  catch (err: unknown) { next(err); }
});

router.delete('/:id', async (req: Request, res: Response, next: NextFunction) => {
  try { await eventController.deleteEvent(req, res, next); }
  catch (err: unknown) { next(err); }
});

export default router;
