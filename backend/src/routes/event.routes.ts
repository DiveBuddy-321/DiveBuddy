import { Router } from 'express';

import { EventController } from '../controllers/event.controller';
import { CreateEventRequest, UpdateEventRequest, createEventSchema, updateEventSchema } from '../types/event.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const eventController = new EventController();

router.get('/', (req, res, next) => { void eventController.getAllEvents(req, res, next); });

router.get('/:id', (req, res, next) => { void eventController.getEventById(req, res, next); });

router.post('/', validateBody<CreateEventRequest>(createEventSchema), (req, res, next) => { void eventController.createEvent(req, res, next); });

router.put('/join/:id', (req, res, next) => { void eventController.joinEvent(req, res, next); });

router.put('/leave/:id', (req, res, next) => { void eventController.leaveEvent(req, res, next); });

router.put('/:id', validateBody<UpdateEventRequest>(updateEventSchema), (req, res, next) => { void eventController.updateEvent(req, res, next); });

router.delete('/:id', (req, res, next) => { void eventController.deleteEvent(req, res, next); });

export default router;
