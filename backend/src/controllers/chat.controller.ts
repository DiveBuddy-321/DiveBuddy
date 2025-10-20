// Class controller matched to your routes (no `next` param in your router)
import { Request, Response } from "express";
import mongoose from "mongoose";
import { Chat } from "../models/chat.model";
import type { Id } from "../types/chat.types";
import type { IUser } from "../types/user.types";

type AuthedRequest = Request & { user?: IUser };

const asObjectId = (v: Id) =>
  typeof v === "string" ? new mongoose.Types.ObjectId(v) : v;

const isValidId = (v: any): v is string =>
  typeof v === "string" && mongoose.isValidObjectId(v);

export class ChatController {
  /** GET /rooms */
  async listChats(req: AuthedRequest, res: Response) {
    try {
      const user = req.user;
      if (!user?._id) return res.status(401).json({ error: "Unauthorized" });
      const rooms = await Chat.listForUser(asObjectId(user._id));
      return res.json(rooms);
    } catch (err: any) {
      return res.status(500).json({ error: err?.message ?? "Failed to list chats" });
    }
  }

  /** GET /:chatId */
  async getChat(req: AuthedRequest, res: Response) {
    try {
      const user = req.user;
      if (!user?._id) return res.status(401).json({ error: "Unauthorized" });
      const { chatId } = req.params;
      if (!isValidId(chatId)) return res.status(400).json({ error: "Invalid chatId" });

      const chat = await Chat.getForUser(chatId, asObjectId(user._id));
      if (!chat) return res.status(404).json({ error: "Chat not found" });
      return res.json(chat);
    } catch (err: any) {
      return res.status(500).json({ error: err?.message ?? "Failed to fetch chat" });
    }
  }

  /** POST /newChat  Body: { peerId: Id, name?: string } */
  async createChat(req: AuthedRequest, res: Response) {
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

      const chat = await Chat.createPair(asObjectId(user._id), asObjectId(peerId), name ?? null);
      return res.status(201).json(chat);
    } catch (err: any) {
      return res.status(500).json({ error: err?.message ?? "Failed to create chat" });
    }
  }

  /** DELETE /:chatId */

  /** GET /:chatId/messages?limit=50 */
  
}

// If you prefer a singleton in the router: export default new ChatController();
