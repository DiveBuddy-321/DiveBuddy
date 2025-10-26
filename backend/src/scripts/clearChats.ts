import mongoose from 'mongoose';
import { config } from 'dotenv';

config();

async function clearChats() {
  try {
    if (!process.env.MONGODB_URI) {
      throw new Error('MONGODB_URI is not set');
    }

    console.log('🔌 Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('✅ Connected to MongoDB');

    const chatsCol = mongoose.connection.collection('chats');
    const messagesCol = mongoose.connection.collection('messages');

    const [existingChats, existingMessages] = await Promise.all([
      chatsCol.countDocuments(),
      messagesCol.countDocuments(),
    ]);

    console.log(`🗄️ Existing -> chats: ${existingChats}, messages: ${existingMessages}`);

    // Delete messages first (they reference chats)
    const delMsgs = await messagesCol.deleteMany({});
    const delChats = await chatsCol.deleteMany({});

    console.log(`🧹 Deleted messages: ${delMsgs.deletedCount}`);
    console.log(`🧹 Deleted chats: ${delChats.deletedCount}`);

    await mongoose.disconnect();
    console.log('✅ Done.');
    process.exit(0);
  } catch (err) {
    console.error('❌ Error clearing chats:', err);
    process.exit(1);
  }
}

clearChats();


