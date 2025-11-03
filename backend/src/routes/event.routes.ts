import { Router } from 'express';

import { EventController } from '../controllers/event.controller';
import { CreateEventRequest, UpdateEventRequest, createEventSchema, updateEventSchema } from '../types/event.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const eventController = new EventController();

router.get('/', (req, res, next) => { eventController.getAllEvents(req, res, next).catch(next); });

router.get('/:id', (req, res, next) => { eventController.getEventById(req, res, next).catch(next); });

router.post('/', validateBody<CreateEventRequest>(createEventSchema), (req, res, next) => { eventController.createEvent(req, res, next).catch(next); });

router.put('/join/:id', (req, res, next) => { eventController.joinEvent(req, res, next).catch(next); });

router.put('/leave/:id', (req, res, next) => { eventController.leaveEvent(req, res, next).catch(next); });

router.put('/:id', validateBody<UpdateEventRequest>(updateEventSchema), (req, res, next) => { eventController.updateEvent(req, res, next).catch(next); });

router.delete('/:id', (req, res, next) => { eventController.deleteEvent(req, res, next).catch(next); });

export default router;
