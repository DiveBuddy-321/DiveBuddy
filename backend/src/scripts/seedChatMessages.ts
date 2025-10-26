import mongoose from 'mongoose';
import { config } from 'dotenv';
import { Chat } from '../models/chat.model';
import { Message } from '../models/message.model';

config();

const HEAVY_CHAT_COUNT = 400;
const LIGHT_MIN = 1;
const LIGHT_MAX = 10;

const SAMPLE_TEXTS = [
  'Tanish',
  'Alex',
  'Albert',
  'Chau',
  'Matthew',
  'Derek',
  'Lia',
  'Kevin',
  'Maya',
  'Erin',
  'Clara',
  'Cindy',
  'Jasmine',
  'Jessica',
  'Jade',
  'Kate',
  'Katie',
  'Arevik',
  'Carter',
  'Jackson',
  'Catherine',
  'Caitlin',
  'Dionne',
  'Colleen',
  'Josh',
  'John',
  'Taylor',
  'Samantha',
  'Michelle',
  'Kyra',
  'Kathy',
  'Brenda',
  'Bobby',
  'Sarah',
  'Sam',
  'Ryan',
  'Tyler',
  'Melissa',
  'Christine'
];

function randomInt(min: number, max: number) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pick<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)];
}

async function seed() {
  if (!process.env.MONGODB_URI) {
    throw new Error('MONGODB_URI is not set');
  }

  console.log('🔌 Connecting to MongoDB...');
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('✅ Connected');

  try {
    const chats = await Chat.find()
      .select('_id participants')
      .lean()
      .exec();

    if (!chats.length) {
      console.log('ℹ️ No chats found. Nothing to seed.');
      return;
    }

    // Choose a chat to receive heavy volume (must have at least 2 participants)
    const eligibleForHeavy = chats.filter(c => Array.isArray(c.participants) && c.participants.length >= 2);
    const heavyChat = eligibleForHeavy.length ? eligibleForHeavy[0] : null;

    let totalCreated = 0;

    for (const chat of chats) {
      const participants = (chat.participants || []).map((p: any) => String(p));
      if (participants.length < 2) {
        console.log(`⚠️ Skipping chat ${chat._id}: not enough participants (${participants.length}).`);
        continue;
      }

      const isHeavy = heavyChat && String(heavyChat._id) === String(chat._id);
      const targetCount = isHeavy ? HEAVY_CHAT_COUNT : randomInt(LIGHT_MIN, LIGHT_MAX);

      console.log(`✍️  Seeding ${targetCount} message(s) for chat ${chat._id}${isHeavy ? ' (heavy)' : ''}...`);

      for (let i = 0; i < targetCount; i++) {
        const senderId = pick(participants);
        const content = `${pick(SAMPLE_TEXTS)} (${i + 1}/${targetCount})`;
        await Message.createMessage(String(chat._id), senderId, content);
        totalCreated++;
        // Optional: throttle a bit for very heavy inserts. Space each message by 100ms to enforce message order
        if (isHeavy && i > 0) {
          await new Promise(r => setTimeout(r, 100));
        }
      }
    }

    console.log(`✅ Done. Created ${totalCreated} messages across ${chats.length} chats.`);
  } finally {
    await mongoose.disconnect();
    console.log('🔌 Disconnected');
  }
}

seed().catch(err => {
  console.error('❌ Seed failed:', err);
  process.exit(1);
});



