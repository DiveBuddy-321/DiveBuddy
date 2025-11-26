import dotenv from 'dotenv';
import mongoose from 'mongoose';

import { connectDB, disconnectDB } from '../config/database';
import logger from '../utils/logger.util';
import { Chat } from '../models/chat.model';
import { Message } from '../models/message.model';
import { userModel } from '../models/user.model';

dotenv.config();

const sampleMessages = [
  'Hey! How’s it going?',
  'Doing great! You?',
  'Pretty good. Ready to play today?',
  'Absolutely. What time works for you?',
  'How about 6 PM at the usual spot?',
  'Perfect. See you then!',
  'Did you check out the new courts?',
  'Not yet—are they any good?',
  'Yeah, they’re awesome. Lots of space.',
  'Nice! Let’s try them next week.',
  'Sounds like a plan.',
  'GGs last time!',
  'We should run it back soon.',
  'Totally. I’ll bring a friend next time.',
  'Cool—more the merrier.',
  'I’m working on my serves lately.',
  'Same, trying to improve consistency.',
  'Want to drill before the game?',
  'Sure, 30 mins earlier?',
  'Works for me.',
] as const;

function pickRandom<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)];
}

function shuffle<T>(arr: T[]): T[] {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

async function seedChatsAndMessages(): Promise<void> {
  try {
    logger.info('🔌 Connecting to MongoDB...');
    await connectDB();
    logger.info('✅ Connected to MongoDB');

    logger.info('🗑️  Clearing existing chat data (messages then chats)...');
    await mongoose.connection.collection('messages').deleteMany({});
    await mongoose.connection.collection('chats').deleteMany({});
    logger.info('✅ Cleared messages and chats');

    logger.info('👥 Fetching users to create conversations...');
    const users = await userModel.findAll();
    if (!users || users.length < 2) {
      logger.info('Not enough users to create chats. Seed users first.');
      return;
    }

    const userIds = users.map((u) => u._id);
    shuffle(userIds);

    // Create up to 50 direct chats or as many pairs as available
    const directChatsToCreate = Math.min(50, Math.floor(userIds.length / 2));
    let createdChats = 0;
    let createdMessages = 0;

    logger.info(`💬 Creating ${String(directChatsToCreate)} direct chats...`);
    for (let i = 0; i < directChatsToCreate; i++) {
      const a = userIds[i * 2];
      const b = userIds[i * 2 + 1];

      const chat = await Chat.createPair(a, b, null);

      // Seed 8-20 messages alternating between the two users
      const messageCount = 8 + Math.floor(Math.random() * 13);
      let senderToggle = Math.random() < 0.5 ? 0 : 1;
      for (let m = 0; m < messageCount; m++) {
        const sender = senderToggle === 0 ? a : b;
        senderToggle = 1 - senderToggle;
        const content = pickRandom(sampleMessages);
        await Message.createMessage(String(chat._id), String(sender), content);
        createdMessages++;
      }

      createdChats++;
    }

    // Summaries
    const chatsCount = await mongoose.connection.collection('chats').countDocuments();
    const messagesCount = await mongoose.connection.collection('messages').countDocuments();
    logger.info('✅ Seeding complete!');
    logger.info(`📊 Chats created: ${String(createdChats)} (stored: ${String(chatsCount)})`);
    logger.info(`📨 Messages created: ${String(createdMessages)} (stored: ${String(messagesCount)})`);
  } catch (error: unknown) {
    logger.error('❌ Error seeding chats/messages:', error);
    process.exitCode = 1;
  } finally {
    await disconnectDB();
    logger.info('👋 Disconnected from MongoDB');
  }
}

// Run the seed script
void seedChatsAndMessages();

