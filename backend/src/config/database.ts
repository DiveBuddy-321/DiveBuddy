import mongoose from 'mongoose';
import logger from '../utils/logger.util';

let handlersAttached = false;

function attachConnectionEventHandlers(): void {
  if (handlersAttached) return;
  handlersAttached = true;

  mongoose.connection.on('error', onMongoError);
  mongoose.connection.on('disconnected', onMongoDisconnected);
}

function onMongoError(error: unknown): void {
  const message = error instanceof Error
    ? error.message
    : typeof error === 'string'
      ? error
      : (() => {
          try { return JSON.stringify(error); } catch { return 'Unknown error'; }
        })();
  logger.error('MongoDB connection error:', message);
}

function onMongoDisconnected(): void {
  logger.info('MongoDB disconnected');
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

