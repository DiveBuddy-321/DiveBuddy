// Class controller matched to your routes (no `next` param in your router)
import { NextFunction, Request, Response } from "express";
import mongoose from "mongoose";
import { Chat } from "../models/chat.model";
import { Message } from "../models/message.model";
import { blockModel } from "../models/block.model";
import type { Id } from "../types/chat.types";
import type { IUser } from "../types/user.types";
import logger from '../utils/logger.util';

type AuthedRequest = Request & { user?: IUser };

const asObjectId = (v: Id) =>
  typeof v === "string" ? new mongoose.Types.ObjectId(v) : v;

const isValidId = (v: unknown): v is string =>
  typeof v === "string" && mongoose.isValidObjectId(v);

export class ChatController {
  /** GET /rooms */
  async listChats(req: AuthedRequest, res: Response, next: NextFunction) {
    try {
      const user = req.user;
      if (!user?._id) return res.status(401).json({ error: "Unauthorized" });
      const rooms = await Chat.listForUser(asObjectId(user._id));
      return res.json(rooms);
    } catch (error) {
      logger.error('Failed to list chats:', error);
      next(error);
    }
  }

  /** GET /:chatId */
  async getChat(req: AuthedRequest, res: Response, next: NextFunction) {
    try {
      const user = req.user;
      if (!user?._id) return res.status(401).json({ error: "Unauthorized" });
      const { chatId } = req.params;
      if (!isValidId(chatId)) return res.status(400).json({ error: "Invalid chatId" });

      const chat = await Chat.getForUser(chatId, asObjectId(user._id));
      if (!chat) return res.status(404).json({ error: "Chat not found" });
      return res.json(chat);
    } catch (error) {
      logger.error('Failed to get chat:', error);
      next(error);
    }
  }

  /** POST /newChat  Body: { peerId: Id, name?: string } */
  async createChat(req: AuthedRequest, res: Response, next: NextFunction) {
    try {
      const user = req.user;
      if (!user?._id) return res.status(401).json({ error: "Unauthorized" });

      const { peerId, name } = (req.body ?? {}) as { peerId?: Id; name?: string | null };
      if (!peerId || (typeof peerId === "string" && !mongoose.isValidObjectId(peerId))) {
        return res.status(400).json({ error: "peerId is required and must be a valid ObjectId" });
      }
      if (String(peerId) === String(user._id)) {
        return res.status(400).json({ error: "Cannot create a direct chat with yourself" });
      }

      // If a direct chat between these two already exists, return it instead of creating a duplicate
      const existing = await Chat.findDirectPair(asObjectId(user._id), asObjectId(peerId));
      if (existing) {
        return res.status(200).json(existing);
      }

      const chat = await Chat.createPair(asObjectId(user._id), asObjectId(peerId), name ?? null);
      return res.status(201).json(chat);
    } catch (error) {
      logger.error('Failed to create chat:', error);
      next(error);
    }
  }

  /** DELETE /:chatId */

  /** GET /:chatId/messages?limit=20&before=<timestamp> */
  async getMessages(req: AuthedRequest, res: Response, next: NextFunction) {
    try {
      const user = req.user;
      if (!user?._id) return res.status(401).json({ error: "Unauthorized" });
      
      const { chatId } = req.params;

      if (!isValidId(chatId)) return res.status(400).json({ error: "Invalid chatId" });

      // Verify user is a participant in the chat
      const chat = await Chat.getForUser(chatId, asObjectId(user._id));
      if (!chat) return res.status(404).json({ error: "Chat not found or access denied" });

      // Parse query parameters
      const limit = parseInt(req.query.limit as string) || 20;
      const before = req.query.before as string;

      // Validate limit
      const validLimit = Math.max(1, Math.min(200, limit));

      // Get messages using the Message model
      let beforeDate: Date | undefined;
      if (before) {
        beforeDate = new Date(before);
        if (isNaN(beforeDate.getTime())) {
          return res.status(400).json({ error: "Invalid 'before' timestamp format" });
        }
      }

      const messages = await Message.getMessagesForChat(chatId, validLimit, beforeDate);

      return res.json({
        messages,
        chatId,
        limit: validLimit,
        count: messages.length,
        hasMore: messages.length === validLimit
      });
    } catch (error) {
      logger.error('Failed to fetch messages:', error);
      next(error);
    }
  }

  /** POST /:chatId/messages  Body: { content: string } */
  async sendMessage(req: AuthedRequest, res: Response, next: NextFunction) {
    try {
      const user = req.user;
      if (!user?._id) return res.status(401).json({ error: "Unauthorized" });
      
      const { chatId } = req.params;
      const { content } = req.body as { content?: string };

      if (!isValidId(chatId)) return res.status(400).json({ error: "Invalid chatId" });
      
      if (!content || typeof content !== "string" || !content.trim()) {
        return res.status(400).json({ error: "Message content is required" });
      }
      // Verify user is a participant in the chat
      const chat = await Chat.getForUser(chatId, asObjectId(user._id));
      if (!chat) return res.status(404).json({ error: "Chat not found or access denied" });

      // Check if the user is blocked by any of the other participants
      const otherParticipants = chat.participants.filter(
        (participantId) => String(participantId) !== String(user._id)
      );
      for (const participantId of otherParticipants) {
        const isBlocked = await blockModel.isBlockedBy(
          asObjectId(user._id),
          asObjectId(participantId)
        );
        if (isBlocked) {
          return res.status(403).json({ 
            error: "You cannot send messages to this user",
            message: "You have been blocked by this user"
          });
        }

        // Check if the user has blocked the other participant
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-call
        const hasBlocked = await blockModel.hasBlocked(
          asObjectId(user._id),
          asObjectId(participantId)
        );
        if (hasBlocked) {
          return res.status(403).json({ 
            error: "You cannot send messages to this user",
            message: "You have blocked this user"
          });
        }
      }

      // Create the message
      const message = await Message.createMessage(chatId, String(user._id), content.trim());

      // Return the populated message
      const populatedMessage = await Message.getMessageById(String(message._id));
      return res.status(201).json(populatedMessage);
    } catch (error) {
      logger.error('Failed to send message:', error);
      next(error);
    }
  }
}
