import { connectDB, disconnectDB } from '../src/config/database';

export const setupTestDB = async (): Promise<void> => {
  await connectDB();
};

export const teardownTestDB = async (): Promise<void> => {
  await disconnectDB();
};