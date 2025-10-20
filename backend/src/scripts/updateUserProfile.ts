import mongoose from 'mongoose';
import { config } from 'dotenv';
import { userModel } from '../models/user.model';
import { isUserReadyForBuddyMatching } from '../types/user.types';

config();

async function updateUserProfile() {
  try {
    // Get email from command line arguments
    const email = process.argv[2];

    if (!email) {
      console.error('❌ Please provide an email address');
      console.log('Usage: npm run update-user <email> [age] [skillLevel] [latitude] [longitude]');
      console.log('Example: npm run update-user john.doe@example.com 25 Intermediate 49.2827 -123.1207');
      process.exit(1);
    }

    // Get optional parameters from command line
    const age = process.argv[3] ? parseInt(process.argv[3]) : undefined;
    const skillLevel = process.argv[4] ? String(process.argv[4]) : undefined;
    const latitude = process.argv[5] ? parseFloat(process.argv[5]) : undefined;
    const longitude = process.argv[6] ? parseFloat(process.argv[6]) : undefined;

    console.log('🔌 Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI!);
    console.log('✅ Connected to MongoDB\n');

    // Find user by email
    const usersCollection = mongoose.connection.collection('users');
    const user = await usersCollection.findOne({ email });

    if (!user) {
      console.error(`❌ User with email "${email}" not found`);
      process.exit(1);
    }

    console.log('👤 User found:');
    console.log(`   Name: ${user.name}`);
    console.log(`   Email: ${user.email}`);
    console.log(`   Current Age: ${user.age ?? 'Not set'}`);
    console.log(`   Current Skill Level: ${user.skillLevel ?? 'Not set'}`);
    console.log(`   Current Coordinates: ${user.latitude ?? 'Not set'}, ${user.longitude ?? 'Not set'}\n`);

    // Prepare update data
    const updateData: any = {};
    
    if (age !== undefined) {
      updateData.age = age;
      console.log(`   Setting age to: ${age}`);
    } else if (!user.age) {
      updateData.age = 25; // Default age
      console.log(`   Setting age to: 25 (default)`);
    }

    if (skillLevel !== undefined) {
      updateData.skillLevel = skillLevel;
      console.log(`   Setting skillLevel to: ${skillLevel}`);
    }

    if (latitude !== undefined) {
      updateData.latitude = latitude;
      console.log(`   Setting latitude to: ${latitude}`);
    }

    if (longitude !== undefined) {
      updateData.longitude = longitude;
      console.log(`   Setting longitude to: ${longitude}`);
    }

    if (Object.keys(updateData).length === 0) {
      console.log('\n✅ User already has all required fields set!');
      console.log(`   Ready for buddy matching: YES`);
      await mongoose.connection.close();
      return;
    }

    // Update user
    console.log('\n💾 Updating user...');
    await usersCollection.updateOne(
      { email },
      { $set: updateData }
    );

    // Fetch updated user
    const updatedUser = await usersCollection.findOne({ email });

    if (!updatedUser) {
      console.error(`❌ User with email "${email}" not found`);
      process.exit(1);
    }
    
    console.log('\n✅ User updated successfully!');
    console.log('─'.repeat(60));
    console.log(`👤 Name: ${updatedUser.name}`);
    console.log(`📧 Email: ${updatedUser.email}`);
    console.log(`🎂 Age: ${updatedUser.age}`);
    console.log(`🎯 Skill Level: ${updatedUser.skillLevel}`);
    console.log(`📍 Coordinates: (${updatedUser.latitude}, ${updatedUser.longitude})`);
    console.log(`🎨 Hobbies: ${updatedUser.hobbies?.join(', ') || 'None'}`);
    if (updatedUser.bio) {
      console.log(`📝 Bio: ${updatedUser.bio}`);
    }
    console.log('─'.repeat(60));
    
    const ready = updatedUser.age && updatedUser.skillLevel && updatedUser.latitude && updatedUser.longitude;
    console.log(`\n✅ Ready for buddy matching: ${ready ? 'YES' : 'NO'}`);
    
    if (ready) {
      console.log('\n🎉 This user can now use the buddy matching feature!');
    }

  } catch (error) {
    console.error('❌ Error updating user:', error);
    process.exit(1);
  } finally {
    await mongoose.connection.close();
    console.log('\n👋 Disconnected from MongoDB');
  }
}

// Run the script
updateUserProfile();

