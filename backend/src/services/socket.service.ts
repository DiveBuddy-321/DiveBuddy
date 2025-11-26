import { ExtendedError, Server, Socket } from "socket.io";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import { Message } from "../models/message.model";
import { Chat } from "../models/chat.model";
import { userModel } from "../models/user.model";
import { blockModel } from "../models/block.model";
import type { IUser } from "../types/user.types";
import logger from "../utils/logger.util";

interface AuthSocket extends Socket {
  user?: IUser;
}

interface JoinRoomData {
  chatId: string;
}

interface SendMessageData {
  chatId: string;
  content: string;
}

export class SocketService {
  private io: Server;

  constructor(io: Server) {
    this.io = io;
    this.setupMiddleware();
    this.setupEventHandlers();
  }

  private setupMiddleware() {
    // Authentication middleware for socket connections
    this.io.use(async (socket: AuthSocket, next) => {
      try {
        const rawAuth = (socket.handshake.auth as Record<string, unknown> | undefined)?.token;
        const headerAuth = socket.handshake.headers.authorization;
        const headerToken = typeof headerAuth === "string" ? headerAuth.split(" ")[1] : undefined;
        const token = typeof rawAuth === "string" ? rawAuth : headerToken;
        
        if (!token) {
          next(new Error("Authentication token required") as ExtendedError);
          return;
        }

        const secret = process.env.JWT_SECRET;
        if (!secret) {
          next(new Error("JWT secret not configured") as ExtendedError);
          return;
        }

        // Match the auth middleware format - uses 'id' not 'userId'
        const decoded = jwt.verify(token, secret) as { id?: mongoose.Types.ObjectId | string; userId?: string };
        const userId = decoded.id ?? decoded.userId; // Support both formats
        
        if (!userId) {
          next(new Error("Invalid token format") as ExtendedError);
          return;
        }

        const user = await userModel.findById(new mongoose.Types.ObjectId(String(userId)));

        if (!user) {
          logger.error("WebSocket auth: User not found for ID:", userId);
          next(new Error("User not found") as ExtendedError);
          return;
        }

        socket.user = user;
        next();
      } catch (error: unknown) {
        logger.error('WebSocket authentication error:', error);
        next(new Error(error instanceof Error ? error.message : "Invalid authentication token"));
      }
    });
  }

  private setupEventHandlers() {
    this.io.on("connection", (socket: AuthSocket) => {

      // Join user to their personal room for notifications
      Promise
        .resolve(socket.join(`user:${String(socket.user?._id)}`))
        .catch((err: unknown) => {
          logger.error("Failed to join user room:", err as Error);
        });

      // Join a chat room
      socket.on("join_room", (data: JoinRoomData) => this.handleJoinRoom(socket, data));

      // Leave a chat room
      socket.on("leave_room", (data: JoinRoomData) => this.handleLeaveRoom(socket, data));

      // Send a message
      socket.on("send_message", (data: SendMessageData) => this.handleSendMessage(socket, data));

      // Disconnect
      socket.on("disconnect", () => {this.handleDisconnect(socket)});
    });
  }

  private async handleJoinRoom(socket: AuthSocket, data: JoinRoomData) {
    try {
      const { chatId } = data;

      if (!mongoose.isValidObjectId(chatId)) {
        socket.emit("error", { message: "Invalid chat ID" });
        return;
      }

      // Verify user is a participant in this chat
      const currentUserId = socket.user?._id;
      if (!currentUserId) {
        socket.emit("error", { message: "User not authenticated" });
        return;
      }
      const chat = await Chat.getForUser(chatId, String(currentUserId));
      
      if (!chat) {
        socket.emit("error", { message: "Chat not found or access denied" });
        return;
      }

      await socket.join(`chat:${chatId}`);
      socket.emit("joined_room", { chatId, chat });
      
    } catch (error: unknown) {
      logger.error("Error joining room:", error);
      socket.emit("error", { message: error instanceof Error ? error.message : "Failed to join room" });
    }
  }

  private async handleLeaveRoom(socket: AuthSocket, data: JoinRoomData) {
    try {
      const { chatId } = data;
      await socket.leave(`chat:${chatId}`);
      socket.emit("left_room", { chatId });
    } catch (error: unknown) {
      logger.error("Error leaving room:", error);
      socket.emit("error", { message: error instanceof Error ? error.message : "Failed to leave room" });
    }
  }

  private async handleSendMessage(socket: AuthSocket, data: SendMessageData) {
    try {
      const { chatId, content } = data;

      if (!mongoose.isValidObjectId(chatId)) {
        socket.emit("error", { message: "Invalid chat ID" });
        return;
      }

      if (!content || content.trim().length === 0) {
        socket.emit("error", { message: "Message content is required" });
        return;
      }

      // Verify user is a participant
      const currentUserId = socket.user?._id;
      if (!currentUserId) {
        socket.emit("error", { message: "User not authenticated" });
        return;
      }
      const userIdStr = String(currentUserId);
      const chat = await Chat.getForUser(chatId, userIdStr);
      
      if (!chat) {
        logger.error("Chat not found or user not a participant:", { userId: userIdStr, chatId });
        socket.emit("error", { message: "Chat not found or access denied" });
        return;
      }
      
      // Check if the user is blocked by any of the other participants
      const otherParticipants = chat.participants.filter(
        (participantId) => String(participantId) !== String(currentUserId)
      );

      for (const participantId of otherParticipants) {
        const isBlocked = await blockModel.isBlockedBy(
          new mongoose.Types.ObjectId(currentUserId),
          new mongoose.Types.ObjectId(String(participantId))
        );
        if (isBlocked) {
          socket.emit("error", { 
            message: "You cannot send messages to this user. You have been blocked."
          });
          return;
        }

        // Check if the user has blocked the other participant
        const hasBlocked = await blockModel.hasBlocked(
          new mongoose.Types.ObjectId(currentUserId),
          new mongoose.Types.ObjectId(String(participantId))
        );
        if (hasBlocked) {
          socket.emit("error", { 
            message: "You cannot send messages to this user. You have blocked this user."
          });
          return;
        }
      }

      // Create the message - pass string IDs as expected by the new model
      const message = await Message.createMessage(
        chatId,
        String(socket.user?._id),
        content.trim()
      );

      // Get populated message using the new getMessageById method
      const populatedMessage = await Message.getMessageById(String(message._id));

      if (!populatedMessage) {
        throw new Error("Failed to retrieve created message");
      }

      // Emit to all users in the chat room
      this.io.to(`chat:${chatId}`).emit("new_message", {
        chatId,
        message: populatedMessage,
      });

      // Notify other participants who are NOT currently in the room
      // Get all sockets in the chat room
      const socketsInRoom = await this.io.in(`chat:${chatId}`).allSockets();
      const userIdsInRoom = new Set<string>();
      
      // Get user IDs of all connected sockets in the room
      for (const socketId of socketsInRoom) {
        const s = this.io.sockets.sockets.get(socketId) as AuthSocket | undefined;
        if (s?.user?._id) {
          userIdsInRoom.add(String(s.user._id));
        }
      }

      // Send chat_updated only to participants NOT in the room
      const notificationParticipants = chat.participants.filter(
        (p) => String(p) !== String(socket.user?._id)
      );

      for (const participantId of notificationParticipants) {
        // Only send if user is not in the chat room
        if (!userIdsInRoom.has(String(participantId))) {
          this.io.to(`user:${String(participantId)}`).emit("chat_updated", {
            chatId,
            lastMessage: populatedMessage,
          });
        }
      }

    } catch (error: unknown) {
      logger.error("Error sending message:", error);
      socket.emit("error", { message: error instanceof Error ? error.message : "Failed to send message" });
    }
  }

  private handleDisconnect(socket: AuthSocket) {
    logger.info("User disconnected from server:", socket.user?._id);
  }

  // Public method to emit events from outside the socket context (e.g., from REST endpoints)
  public emitToUser<T>(userId: string, event: string, data: T) {
    this.io.to(`user:${userId}`).emit(event, data);
  }

  public emitToChat<T>(chatId: string, event: string, data: T) {
    this.io.to(`chat:${chatId}`).emit(event, data);
  }
} 
