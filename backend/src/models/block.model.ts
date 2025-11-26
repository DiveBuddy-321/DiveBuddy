import mongoose, { Schema } from 'mongoose';

export interface IBlock {
  blocker: mongoose.Types.ObjectId;
  blocked: mongoose.Types.ObjectId;
  createdAt?: Date;
  updatedAt?: Date;
}

const blockSchema = new Schema<IBlock>(
  {
    blocker: { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    blocked: { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
  },
  { timestamps: true, versionKey: false }
);

// Ensure a user cannot block the same target more than once
blockSchema.index({ blocker: 1, blocked: 1 }, { unique: true });

export const Block = mongoose.model<IBlock>('Block', blockSchema);

export class BlockModel {
  async blockUser(blockerId: mongoose.Types.ObjectId, blockedId: mongoose.Types.ObjectId): Promise<IBlock> {
    if (String(blockerId) === String(blockedId)) {
      throw new Error('Cannot block yourself');
    }
    // Upsert to be idempotent
    const result = await Block.findOneAndUpdate(
      { blocker: blockerId, blocked: blockedId },
      { $setOnInsert: { blocker: blockerId, blocked: blockedId } },
      { upsert: true, new: true }
    ).exec();
    if (!result) {
      // This should not happen due to upsert + new
      throw new Error('Failed to create block record');
    }
    return result;
  }

  async unblockUser(blockerId: mongoose.Types.ObjectId, blockedId: mongoose.Types.ObjectId): Promise<number> {
    const res = await Block.deleteOne({ blocker: blockerId, blocked: blockedId }).exec();
    return res.deletedCount ?? 0;
  }

  async getBlockedUsers(blockerId: mongoose.Types.ObjectId): Promise<mongoose.Types.ObjectId[]> {
    const blocks = await Block.find({ blocker: blockerId }).select('blocked').lean().exec();
    return blocks.map(block => block.blocked);
  }

  async isBlockedBy(userId: mongoose.Types.ObjectId, potentialBlockerId: mongoose.Types.ObjectId): Promise<boolean> {
    const block = await Block.findOne({ blocker: potentialBlockerId, blocked: userId }).lean().exec();
    return !!block;
  }

  async hasBlocked(blockerId: mongoose.Types.ObjectId, blockedId: mongoose.Types.ObjectId): Promise<boolean> {
    const block = await Block.findOne({ blocker: blockerId, blocked: blockedId }).lean().exec();
    return !!block;
  }
}

export const blockModel = new BlockModel();


