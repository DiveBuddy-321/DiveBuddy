import mongoose, { Schema, Model, Document } from "mongoose";
import type { FilterQuery } from "mongoose";
import type { IChat, IChatWithLastMessage } from "../types/chat.types";
import type { IMessage } from "../types/chat.types";

/**
 * Your IChat extends Document in chat.types.ts, which conflicts with Mongoose's Document _id.
 * To fix: for the DB document type we override _id as ObjectId and treat isGroup as a virtual.
 */
export interface IChatDocument extends Omit<IChat, "_id" | "isGroup">, Document {
  _id: mongoose.Types.ObjectId;
  isGroup: boolean; // virtual (participants.length >= 3)
  isDirect: boolean; // virtual (participants.length === 2)
}

export interface IChatModel extends Model<IChatDocument> {
  listForUser(userId: mongoose.Types.ObjectId): Promise<IChatWithLastMessage[]>; // populated objects
  getForUser(chatId: string, userId?: mongoose.Types.ObjectId | string): Promise<IChatDocument | null>; // document (will be populated)
  createPair(a: mongoose.Types.ObjectId, b: mongoose.Types.ObjectId, name?: string | null): Promise<IChatDocument>;
  findDirectPair(a: mongoose.Types.ObjectId, b: mongoose.Types.ObjectId): Promise<IChatDocument | null>;
  getLastConversation(chatId: string, limit?: number): Promise<IMessage[]>; // messages (lean)
  leave(chatId: string, userId: mongoose.Types.ObjectId): Promise<"left" | "deleted" | "noop">;
}

const chatSchema = new Schema<IChatDocument, IChatModel>(
  {
    // We DO NOT persist isGroup; it's derived from participants.length via virtuals
    name: { type: String, default: null, trim: true },
    participants: [{ type: Schema.Types.ObjectId, ref: "User", required: true}],
    lastMessage: { type: Schema.Types.ObjectId, ref: "Message", default: null },
    lastMessageAt: { type: Date, default: null, index: true },
  },
  {
    timestamps: true,
    versionKey: false,
    toJSON: { virtuals: true },
    toObject: { virtuals: true },
  }
);

/* Virtuals */
chatSchema.virtual("isDirect").get(function (this: IChatDocument) {
  return Array.isArray(this.participants) && this.participants.length === 2;
});

chatSchema.virtual("isGroup").get(function (this: IChatDocument) {
  return Array.isArray(this.participants) && this.participants.length >= 3;
});

/* Indexes */
chatSchema.index({ participants: 1 });
chatSchema.index({ updatedAt: -1 });

/* Statics */

// 1) All rooms for a user (lean + virtuals)
chatSchema.statics.listForUser = function (this: IChatModel, userId: mongoose.Types.ObjectId) {
  return this.find({ participants: userId })
    .sort({ updatedAt: -1 })
    .select("_id name participants lastMessage lastMessageAt createdAt updatedAt")
    .populate({
      path: 'lastMessage',
      select: 'content sender createdAt',
      populate: {
        path: 'sender',
        select: 'name profilePicture'
      }
    })
    .lean({ virtuals: true })
    .exec();
};

// 2) One room by id; optionally enforce membership (document)
chatSchema.statics.getForUser = function (this: IChatModel, chatId: string, userId?: mongoose.Types.ObjectId | string) {
  // Validate chatId format
  if (!mongoose.isValidObjectId(chatId)) {
    throw new Error("Invalid chat ID format");
  }
  
  const query: FilterQuery<IChatDocument> = { _id: new mongoose.Types.ObjectId(chatId) };
  
  // If userId is provided, ensure user is a participant
  // Convert to ObjectId for proper comparison
  if (userId) {
    const userObjectId = typeof userId === 'string' 
      ? new mongoose.Types.ObjectId(userId) 
      : userId;
    query.participants = userObjectId;
  }
  
  return this.findOne(query)
    .populate({
      path: 'lastMessage',
      select: 'content sender createdAt',
      populate: {
        path: 'sender',
        select: 'name profilePicture'
      }
    })
    .exec();
};

// 3) Create a 2-person (direct-like) room (document)
chatSchema.statics.createPair = function (
  a: mongoose.Types.ObjectId,
  b: mongoose.Types.ObjectId,
  name?: string | null
) {
  if (!a || !b) throw new Error("Both participant ids are required");
  const dedup = Array.from(new Set([String(a), String(b)])).map((id) => new mongoose.Types.ObjectId(id));
  if (dedup.length < 2) throw new Error("A direct chat requires two distinct participants");
  return this.create({
    name: name?.trim() || null,
    participants: dedup,
  } as unknown as IChatDocument);
};

// 4) Find existing direct chat between two users (participants exactly those two)
chatSchema.statics.findDirectPair = function (
  a: mongoose.Types.ObjectId,
  b: mongoose.Types.ObjectId
) {
  if (!a || !b) return Promise.resolve(null);
  const [aId, bId] = [new mongoose.Types.ObjectId(a), new mongoose.Types.ObjectId(b)];
  return this.findOne({
    participants: { $all: [aId, bId] },
    $expr: { $eq: [{ $size: "$participants" }, 2] },
  }).exec();
};

// Note: Message queries are handled by Message model for better separation of concerns

// 5) Leave a chat; delete if it becomes empty
chatSchema.statics.leave = async function (
  chatId: string,
  userId: mongoose.Types.ObjectId
): Promise<"left" | "deleted" | "noop"> {
  const chat = await this.findById(chatId).exec();
  if (!chat) return "noop";

  const before = chat.participants.length;
  chat.participants = chat.participants.filter((p) => String(p) !== String(userId));

  if (chat.participants.length === 0) {
    await this.deleteOne({ _id: chat._id });
    return "deleted";
  }

  if (chat.participants.length !== before) {
    chat.updatedAt = new Date();
    await chat.save();
    return "left";
  }
  return "noop";
};

export const Chat = mongoose.model<IChatDocument, IChatModel>("Chat", chatSchema);
