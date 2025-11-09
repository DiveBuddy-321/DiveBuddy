import { connectDB, disconnectDB } from '../src/config/database';
import mongoose from 'mongoose';

export const setupTestDB = async (): Promise<void> => {
  await connectDB();
  // Ensure no leftover test users from previous runs
  const User = mongoose.model('User');
  await User.deleteMany({ email: 'test@example.com' }); 
  await User.deleteMany({ email: 'other@example.com' });
};

export const teardownTestDB = async (): Promise<void> => {
  await disconnectDB();
};