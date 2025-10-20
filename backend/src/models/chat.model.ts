import mongoose, { Schema, Model, Document } from "mongoose";
import type { IChat } from "../types/chat.types";

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
  listForUser(userId: mongoose.Types.ObjectId): Promise<IChat[]>; // lean objects
  getForUser(chatId: string, userId?: mongoose.Types.ObjectId): Promise<IChatDocument | null>; // document
  createPair(a: mongoose.Types.ObjectId, b: mongoose.Types.ObjectId, name?: string | null): Promise<IChatDocument>;
  getLastConversation(chatId: string, limit?: number): Promise<any[]>; // messages (lean)
  leave(chatId: string, userId: mongoose.Types.ObjectId): Promise<"left" | "deleted" | "noop">;
}

const chatSchema = new Schema<IChatDocument, IChatModel>(
  {
    // We DO NOT persist isGroup; it's derived from participants.length via virtuals
    name: { type: String, default: null, trim: true },
    participants: [{ type: Schema.Types.ObjectId, ref: "User", required: true}],
    createdBy: { type: Schema.Types.ObjectId, ref: "User", default: null },
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
chatSchema.statics.listForUser = function (userId: mongoose.Types.ObjectId) {
  return this.find({ participants: userId })
    .sort({ updatedAt: -1 })
    .select("_id name participants createdBy lastMessage lastMessageAt createdAt updatedAt")
    .lean({ virtuals: true })
    .exec();
};

// 2) One room by id; optionally enforce membership (document)
chatSchema.statics.getForUser = function (chatId: string, userId?: mongoose.Types.ObjectId) {
  const query: any = { _id: chatId };
  if (userId) query.participants = userId;
  return this.findOne(query).exec();
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
  } as any);
};

// 4) Latest N messages (newest→oldest); assumes Message model exists
chatSchema.statics.getLastConversation = function (chatId: string, limit: number = 50) {
  const Message: any = mongoose.model("Message");
  return Message.find({ chat: chatId })
    .sort({ createdAt: -1 })
    .limit(Math.max(1, Math.min(200, limit)))
    .populate("sender", "name avatar")
    .lean()
    .exec();
};

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
