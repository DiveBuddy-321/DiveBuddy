import { Types } from "mongoose";
export type Id = string | Types.ObjectId;

export interface IChat {
  _id?: Id;
  isGroup: boolean;
  name?: string | null;
  participants: Id[];                 // keep ids here; populate type separately if needed
  eventId?: Id | null;                // reference to event for event group chats
  lastMessage?: Id | null;            // unpopulated - just the message ID
  lastMessageAt?: Date | null;        // include to match model & sorting
  createdAt?: Date;
  updatedAt?: Date;
}

// Chat with populated lastMessage
export interface IChatWithLastMessage extends Omit<IChat, 'lastMessage'> {
  lastMessage?: {
    _id: Id;
    content: string;
    sender: {
      _id: Id;
      name: string;
      avatar?: string;
    };
    createdAt: Date;
  } | null;
}

// Message types for chat functionality (MVP - text only)
export interface IMessage {
  _id?: Id;
  chat: Id;                           // reference to chat
  sender: Id;                         // reference to user who sent the message
  content: string;                    // message content (text only for MVP)
  createdAt?: Date;
  updatedAt?: Date;
}

// Populated message with sender details
export interface IMessageWithSender extends Omit<IMessage, 'sender'> {
  sender: {
    _id: Id;
    name: string;
    avatar?: string;
  };
}

// Message creation request (MVP - text only)
export interface ICreateMessageRequest {
  content: string;
}
