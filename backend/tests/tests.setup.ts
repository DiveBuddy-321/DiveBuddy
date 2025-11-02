import { connectDB, disconnectDB } from '../src/config/database';

export const setupTestDB = async (): Promise<void> => {
  await connectDB();
};

export const teardownTestDB = async (): Promise<void> => {
  await disconnectDB();
};

// export const clearTestDB = async (): Promise<void> => {
//   const collections = mongoose.connection.collections;
//   for (const key in collections) {
//     await collections[key].deleteMany({});
//   }
// };