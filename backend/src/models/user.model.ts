import mongoose, { Schema } from 'mongoose';
import { z } from 'zod';

import { HOBBIES } from '../constants/hobbies';
import {
  createUserSchema,
  GoogleUserInfo,
  IUser,
  updateProfileSchema,
  } from '../types/user.types';
import logger from '../utils/logger.util';
import { getLocationFromCoordinates } from '../utils/geoCoding.util';

const userSchema = new Schema<IUser>(
  {
    googleId: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    email: {
      type: String,
      required: true,
      unique: true,
      lowercase: true,
      trim: true,
    },
    name: {
      type: String,
      required: true,
      trim: true,
    },
    age: {
      type: Number,
      required: false,
    },
    level: {
      type: Number,
      required: false,
    },
    long: {
      type: Number,
      required: false,
    },
    lat: {
      type: Number,
      required: false,
    },
    city: {
      type: String,
      required: false,
      trim: true,
    },
    province: {
      type: String,
      required: false,
      trim: true,
    },
    country: {
      type: String,
      required: false,
      trim: true,
    },
    profilePicture: {
      type: String,
      required: false,
      trim: true,
    },
    bio: {
      type: String,
      required: false,
      trim: true,
      maxlength: 500,
    },
    hobbies: {
      type: [String],
      default: [],
      validate: {
        validator: function (hobbies: string[]) {
          return (
            hobbies.length === 0 ||
            hobbies.every(hobby => HOBBIES.includes(hobby))
          );
        },
        message:
          'Hobbies must be non-empty strings and must be in the available hobbies list',
      },
    },
  },
  {
    timestamps: true,
  }
);

export class UserModel {
  private user: mongoose.Model<IUser>;

  constructor() {
    this.user = mongoose.model<IUser>('User', userSchema);
  }

  async create(userInfo: GoogleUserInfo): Promise<IUser> {
    try {
      const validatedData = createUserSchema.parse(userInfo);

      // Geocode if coordinates are provided
      if (validatedData.lat !== undefined && validatedData.long !== undefined) {
        try {
          const locationInfo = await getLocationFromCoordinates(
            validatedData.lat,
            validatedData.long
          );
          validatedData.city = locationInfo.city;
          validatedData.province = locationInfo.province;
          validatedData.country = locationInfo.country;
          logger.info(`Geocoded new user location: ${locationInfo.city}, ${locationInfo.province}, ${locationInfo.country}`);
        } catch (error) {
          logger.error('Geocoding failed during user creation:', error);
          // Continue with user creation even if geocoding fails
        }
      }

      return await this.user.create(validatedData);
    } catch (error) {
      if (error instanceof z.ZodError) {
        console.error('Validation error:', error.issues);
        throw new Error('Invalid update data');
      }
      console.error('Error updating user:', error);
      throw new Error('Failed to update user');
    }
  }

  async update(
    userId: mongoose.Types.ObjectId,
    user: Partial<IUser>
  ): Promise<IUser | null> {
    try {
      const validatedData = updateProfileSchema.parse(user);

      const updatedUser = await this.user.findByIdAndUpdate(
        userId,
        validatedData,
        {
          new: true,
        }
      );
      return updatedUser;
    } catch (error) {
      logger.error('Error updating user:', error);
      throw new Error('Failed to update user');
    }
  }

  async delete(userId: mongoose.Types.ObjectId): Promise<void> {
    try {
      await this.user.findByIdAndDelete(userId);
    } catch (error) {
      logger.error('Error deleting user:', error);
      throw new Error('Failed to delete user');
    }
  }

  async findById(_id: mongoose.Types.ObjectId): Promise<IUser | null> {
    try {
      const user = await this.user.findOne({ _id });

      if (!user) {
        return null;
      }

      return user;
    } catch (error) {
      console.error('Error finding user by Google ID:', error);
      throw new Error('Failed to find user');
    }
  }

  async findByGoogleId(googleId: string): Promise<IUser | null> {
    try {
      const user = await this.user.findOne({ googleId });

      if (!user) {
        return null;
      }

      return user;
    } catch (error) {
      console.error('Error finding user by Google ID:', error);
      throw new Error('Failed to find user');
    }
  }

  async findAll(): Promise<IUser[]> {
    try {
      const users = await this.user.find({});
      return users;
    } catch (error) {
      logger.error('Error finding all users:', error);
      throw new Error('Failed to find users');
    }
  }
}

export const userModel = new UserModel();
