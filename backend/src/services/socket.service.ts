import { Server, Socket } from "socket.io";
import jwt from "jsonwebtoken";
import mongoose from "mongoose";
import { Message } from "../models/message.model";
import { Chat } from "../models/chat.model";
import { userModel } from "../models/user.model";
import type { IUser } from "../types/user.types";

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
        const token = socket.handshake.auth.token || socket.handshake.headers.authorization?.split(" ")[1];
        
        if (!token) {
          return next(new Error("Authentication token required"));
        }

        const secret = process.env.JWT_SECRET;
        if (!secret) {
          return next(new Error("JWT secret not configured"));
        }

        // Match the auth middleware format - uses 'id' not 'userId'
        const decoded = jwt.verify(token, secret) as { id: mongoose.Types.ObjectId; userId?: string };
        const userId = decoded.id || decoded.userId; // Support both formats
        
        if (!userId) {
          return next(new Error("Invalid token format"));
        }

        const user = await userModel.findById(userId);

        if (!user) {
          console.error(`WebSocket auth: User not found for ID: ${userId}`);
          return next(new Error("User not found"));
        }

        socket.user = user;
        next();
      } catch (error) {
        console.error('WebSocket authentication error:', error);
        next(new Error("Invalid authentication token"));
      }
    });
  }

  private setupEventHandlers() {
    this.io.on("connection", (socket: AuthSocket) => {
      console.log(`User connected: ${socket.user?._id}`);

      // Join user to their personal room for notifications
      if (socket.user?._id) {
        socket.join(`user:${socket.user._id}`);
      }

      // Join a chat room
      socket.on("join_room", (data: JoinRoomData) => this.handleJoinRoom(socket, data));

      // Leave a chat room
      socket.on("leave_room", (data: JoinRoomData) => this.handleLeaveRoom(socket, data));

      // Send a message
      socket.on("send_message", (data: SendMessageData) => this.handleSendMessage(socket, data));

      // Disconnect
      socket.on("disconnect", () => this.handleDisconnect(socket));
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
      const chat = await Chat.getForUser(chatId, socket.user?._id as mongoose.Types.ObjectId);
      
      if (!chat) {
        socket.emit("error", { message: "Chat not found or access denied" });
        return;
      }

      socket.join(`chat:${chatId}`);
      socket.emit("joined_room", { chatId, chat });
      
      console.log(`User ${socket.user?._id} joined chat ${chatId}`);
    } catch (error: any) {
      console.error("Error joining room:", error);
      socket.emit("error", { message: error.message || "Failed to join room" });
    }
  }

  private async handleLeaveRoom(socket: AuthSocket, data: JoinRoomData) {
    try {
      const { chatId } = data;
      socket.leave(`chat:${chatId}`);
      socket.emit("left_room", { chatId });
      console.log(`User ${socket.user?._id} left chat ${chatId}`);
    } catch (error: any) {
      console.error("Error leaving room:", error);
      socket.emit("error", { message: error.message || "Failed to leave room" });
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
      const chat = await Chat.getForUser(chatId, socket.user?._id as mongoose.Types.ObjectId);
      
      if (!chat) {
        socket.emit("error", { message: "Chat not found or access denied" });
        return;
      }

      // Create the message
      const message = await Message.createMessage(
        chatId,
        socket.user?._id as mongoose.Types.ObjectId,
        content.trim()
      );

      // Populate sender info
      const populatedMessage = await Message.findById(message._id)
        .populate("sender", "name avatar email")
        .lean();

      // Emit to all users in the chat room
      this.io.to(`chat:${chatId}`).emit("new_message", {
        chatId,
        message: populatedMessage,
      });

      // Notify other participants (who might not be in the room)
      const otherParticipants = chat.participants.filter(
        (p) => String(p) !== String(socket.user?._id)
      );

      for (const participantId of otherParticipants) {
        this.io.to(`user:${participantId}`).emit("chat_updated", {
          chatId,
          lastMessage: populatedMessage,
        });
      }

      console.log(`Message sent in chat ${chatId} by user ${socket.user?._id}`);
    } catch (error: any) {
      console.error("Error sending message:", error);
      socket.emit("error", { message: error.message || "Failed to send message" });
    }
  }

  private handleDisconnect(socket: AuthSocket) {
    console.log(`User disconnected: ${socket.user?._id}`);
  }

  // Public method to emit events from outside the socket context (e.g., from REST endpoints)
  public emitToUser(userId: string, event: string, data: any) {
    this.io.to(`user:${userId}`).emit(event, data);
  }

  public emitToChat(chatId: string, event: string, data: any) {
    this.io.to(`chat:${chatId}`).emit(event, data);
  }
}
