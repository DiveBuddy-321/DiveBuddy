import mongoose, { Document } from 'mongoose';
import z from 'zod';
import { SKILL_LEVELS, SkillLevel } from '../constants/statics';

// User model
// ------------------------------------------------------------
export interface IUser extends Document {
  _id: mongoose.Types.ObjectId;
  googleId: string;
  email: string;
  name: string;
  age?: number;
  
  profilePicture?: string;
  bio?: string;
  location?: string;
  latitude?: number;
  longitude?: number;
  skillLevel?: SkillLevel;
  eventsJoined: mongoose.Types.ObjectId[];
  eventsCreated: mongoose.Types.ObjectId[];
  createdAt: Date;
  updatedAt: Date;
}

// Zod schemas
// ------------------------------------------------------------
export const createUserSchema = z.object({
  email: z.string().email(),
  name: z.string().min(1),
  googleId: z.string().min(1),
  age: z.number().int().positive().optional(),
  
  profilePicture: z.string().optional(),
  bio: z.string().max(500).optional(),
  location: z.string().optional(),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  skillLevel: z.enum(SKILL_LEVELS).optional(),
  eventsJoined: z.array(z.string()).optional(),
  eventsCreated: z.array(z.string()).optional(),
});

export const updateProfileSchema = z.object({
  name: z.string().min(1).optional(),
  age: z.number().int().positive().optional(),
  
  bio: z.string().max(500).optional(),
  location: z.string().optional(),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  profilePicture: z.string().optional(),
  skillLevel: z.enum(SKILL_LEVELS).optional(),
  eventsJoined: z.array(z.string()).optional(),
  eventsCreated: z.array(z.string()).optional(),
});

// Request types
// ------------------------------------------------------------
export interface GetProfileResponse {
  message: string;
  data?: {
    user: IUser;
  };
}

export type UpdateProfileRequest = z.infer<typeof updateProfileSchema>;
export type CreateUserRequest = z.infer<typeof createUserSchema>;

// Generic types
// ------------------------------------------------------------
export interface GoogleUserInfo {
  googleId: string;
  email: string;
  name: string;
  profilePicture?: string;
}

// Helper functions
// ------------------------------------------------------------
export const isUserReadyForBuddyMatching = (user: IUser): boolean => {
  return (
    user.age !== undefined &&
    user.skillLevel !== undefined &&
    user.longitude !== undefined &&
    user.latitude !== undefined
  );
};
