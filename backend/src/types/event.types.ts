import mongoose, { Document } from 'mongoose';
import z from 'zod';
import { SKILL_LEVELS, SkillLevel } from '../constants/statics';

// Event model
// ------------------------------------------------------------
export interface IEvent extends Document {
  _id: mongoose.Types.ObjectId;
  title: string;
  description: string;
  date: Date;
  capacity: number;
  skillLevel?: SkillLevel;
  location?: string;
  latitude?: number;
  longitude?: number;
  createdBy: mongoose.Types.ObjectId;
  attendees: mongoose.Types.ObjectId[];
  photo?: string;
  createdAt: Date;
  updatedAt: Date;
}

// Zod schemas
// ------------------------------------------------------------
export const createEventSchema = z.object({
  title: z.string().min(1).max(100),
  description: z.string().min(1),
  date: z.coerce.date(),
  capacity: z.number().min(1),
  skillLevel: z.enum(SKILL_LEVELS).optional(),
  location: z.string().optional(),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  createdBy: z.string().optional(),
  attendees: z.array(z.string()),
  photo: z.string().optional(),
});

export const updateEventSchema = z.object({
  title: z.string().min(1).max(100),
  description: z.string().min(1),
  date: z.coerce.date(),
  capacity: z.number().min(1),
  skillLevel: z.enum(SKILL_LEVELS).optional(),
  location: z.string().optional(),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  attendees: z.array(z.string()),
  photo: z.string().optional()
});

// Request types
// ------------------------------------------------------------
export interface GetEventResponse {
  message: string;
  data?: {
    event: IEvent;
  };
};

//these must remain types because they are zod infererred types
export type UpdateEventRequest = z.infer<typeof updateEventSchema>;
export type CreateEventRequest = z.infer<typeof createEventSchema>;
