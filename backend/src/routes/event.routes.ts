import { Router } from 'express';

import { EventController } from '../controllers/event.controller';
import { CreateEventRequest, UpdateEventRequest, createEventSchema, updateEventSchema } from '../types/event.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const eventController = new EventController();

router.get('/', eventController.getAllEvents);

router.get('/:id', eventController.getEventById);

router.post('/', validateBody<CreateEventRequest>(createEventSchema), eventController.createEvent);

router.put('/:id', validateBody<UpdateEventRequest>(updateEventSchema), eventController.updateEvent);

router.delete('/:id', eventController.deleteEvent);

export default router;
