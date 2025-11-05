import mongoose from 'mongoose';
import logger from '../utils/logger.util';

let handlersAttached = false;

function attachConnectionEventHandlers(): void {
  if (handlersAttached) return;
  handlersAttached = true;

  mongoose.connection.on('error', (error: Error) => {
    logger.error('MongoDB connection error:', error?.message ?? 'Unknown error');
  });

  mongoose.connection.on('disconnected', () => {
    logger.info('MongoDB disconnected');
  });

  // Important: non-async handler to satisfy "void expected"
  process.on('SIGINT', () => {
    // Do not return the promise; consume it here.
    mongoose.connection
      .close()
      .then(() => {
        logger.info('MongoDB connection closed through app termination');
        process.exit(0);
      })
      .catch((error) => {
        logger.error('Error closing MongoDB connection:', error);
        process.exit(1);
      });
  });
}

export const connectDB = async (): Promise<void> => {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    logger.error('❌ Failed to connect to MongoDB: MONGODB_URI is not set');
    process.exitCode = 1;
    return;
  }

  try {
    await mongoose.connect(uri);
    logger.info('MongoDB connected successfully');
    attachConnectionEventHandlers();
  } catch (error) {
    logger.error('❌ Failed to connect to MongoDB:', error);
    process.exitCode = 1;
  }
};

export const disconnectDB = async (): Promise<void> => {
  try {
    await mongoose.connection.close();
    logger.info('MongoDB disconnected successfully');
  } catch (error) {
    logger.error('❌ Error disconnecting from MongoDB:', error);
  }
};

