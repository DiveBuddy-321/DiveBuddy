import mongoose, { Schema, Model, Document, FilterQuery } from "mongoose";
import type { IMessage } from "../types/chat.types";

/**
 * Message document interface extending the base IMessage
 */
export interface IMessageDocument extends IMessage, Document {
  _id: mongoose.Types.ObjectId;
}

export interface IMessageModel extends Model<IMessageDocument> {
  createMessage(chatId: string, senderId: string, content: string): Promise<IMessageDocument>;
  getMessagesForChat(chatId: string, limit?: number, before?: Date): Promise<IMessageDocument[]>;
  getMessageById(messageId: string): Promise<IMessageDocument | null>;
}

const messageSchema = new Schema<IMessageDocument, IMessageModel>(
  {
    chat: { 
      type: Schema.Types.ObjectId, 
      ref: "Chat", 
      required: true,
      index: true 
    },
    sender: { 
      type: Schema.Types.ObjectId, 
      ref: "User", 
      required: true,
      index: true 
    },
    content: { 
      type: String, 
      required: true, 
      trim: true,
      maxlength: 2000 // reasonable limit for text messages
    },
  },
  {
    timestamps: true,
    versionKey: false,
    toJSON: { virtuals: true },
    toObject: { virtuals: true },
  }
);

/* Indexes for performance */
messageSchema.index({ chat: 1, createdAt: -1 }); // For getting messages by chat, newest first
messageSchema.index({ sender: 1, createdAt: -1 }); // For getting messages by sender

/* Statics */

// Create a new message
messageSchema.statics.createMessage = async function (
  chatId: string, 
  senderId: string, 
  content: string
): Promise<IMessageDocument> {
  if (!content.trim()) {
    throw new Error("Message content cannot be empty");
  }
  
  // Create the message
  const message = await this.create({
    chat: new mongoose.Types.ObjectId(chatId),
    sender: new mongoose.Types.ObjectId(senderId),
    content: content.trim(),
  });
  
  // Update the chat's lastMessage and lastMessageAt fields
  const Chat = mongoose.model("Chat");
  await Chat.findByIdAndUpdate(chatId, {
    lastMessage: message._id,
    lastMessageAt: message.createdAt,
  });
  
  return message;
};

// Get messages for a chat with pagination
messageSchema.statics.getMessagesForChat = function (
  chatId: string, 
  limit: number = 50, 
  before?: Date
): Promise<IMessageDocument[]> {
  // Validate chatId format
  if (!mongoose.isValidObjectId(chatId)) {
    throw new Error("Invalid chatId format");
  }
  
  const query: FilterQuery<IMessageDocument> = { chat: new mongoose.Types.ObjectId(chatId) };
  
  if (before) {
    query.createdAt = { $lt: before };
  }
  
  return this.find(query)
    .sort({ createdAt: -1 })
    .limit(Math.max(1, Math.min(200, limit)))
    .populate("sender", "name avatar")
    .lean()
    .exec();
};

// Get a specific message by ID
messageSchema.statics.getMessageById = function (messageId: string): Promise<IMessageDocument | null> {
  return this.findById(messageId)
    .populate("sender", "name avatar")
    .lean()
    .exec();
};

export const Message = mongoose.model<IMessageDocument, IMessageModel>("Message", messageSchema);

