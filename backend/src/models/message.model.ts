import mongoose, { Schema, Model, Document } from "mongoose";

export interface IMessage {
  _id?: mongoose.Types.ObjectId | string;
  chat: mongoose.Types.ObjectId | string;
  sender: mongoose.Types.ObjectId | string;
  content: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface IMessageDocument extends Omit<IMessage, "_id">, Document {
  _id: mongoose.Types.ObjectId;
}

export interface IMessageModel extends Model<IMessageDocument> {
  createMessage(
    chatId: string | mongoose.Types.ObjectId,
    senderId: string | mongoose.Types.ObjectId,
    content: string
  ): Promise<IMessageDocument>;
}

const messageSchema = new Schema<IMessageDocument, IMessageModel>(
  {
    chat: { type: Schema.Types.ObjectId, ref: "Chat", required: true, index: true },
    sender: { type: Schema.Types.ObjectId, ref: "User", required: true },
    content: { type: String, required: true, trim: true },
  },
  {
    timestamps: true,
    versionKey: false,
  }
);

// Index for efficient querying of messages by chat
messageSchema.index({ chat: 1, createdAt: -1 });

// Static method to create a message and update the chat
messageSchema.statics.createMessage = async function (
  chatId: string | mongoose.Types.ObjectId,
  senderId: string | mongoose.Types.ObjectId,
  content: string
) {
  const Chat = mongoose.model("Chat");
  
  // Create the message
  const message = await this.create({
    chat: chatId,
    sender: senderId,
    content,
  });

  // Update the chat's lastMessage and lastMessageAt
  await Chat.findByIdAndUpdate(chatId, {
    lastMessage: message._id,
    lastMessageAt: message.createdAt,
  });

  return message;
};

export const Message = mongoose.model<IMessageDocument, IMessageModel>("Message", messageSchema);
