import { describe, test, expect, beforeAll, afterAll, afterEach, jest, beforeEach } from '@jest/globals';
import { Server } from 'socket.io';
import { createServer } from 'http';
import { io as Client, Socket as ClientSocket } from 'socket.io-client';
import jwt from 'jsonwebtoken';
import mongoose from 'mongoose';
import dotenv from 'dotenv';
import { setupTestDB, teardownTestDB } from '../tests.setup';
import { SocketService } from '../../src/services/socket.service';
import { Chat } from '../../src/models/chat.model';
import { Message } from '../../src/models/message.model';
import { userModel } from '../../src/models/user.model';
import { CreateUserRequest } from '../../src/types/user.types';

dotenv.config();

// Test users will be created dynamically
let testUser: any = null;
let otherTestUser: any = null;

const JWT_SECRET = process.env.JWT_SECRET || 'test-secret';

describe('Socket.IO - mocked', () => {
  let httpServer: any;
  let io: Server;
  let socketService: SocketService;
  let clientSocket: ClientSocket;
  let userToken: string;

  beforeAll(async () => {
    await setupTestDB();
    
    // Create test users
    const newUser: CreateUserRequest = {
      email: 'test@example.com',
      name: 'Test User',
      googleId: `test-google-${Date.now()}`,
      age: 25,
      profilePicture: 'http://example.com/pic.jpg',
      bio: 'Test bio',
      location: 'Vancouver, BC',
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: 'Intermediate'
    };
    testUser = await userModel.create(newUser);
    
    const newOtherUser: CreateUserRequest = {
      email: 'other@example.com',
      name: 'Other Test User',
      googleId: `test-google-other-${Date.now()}`,
      age: 30,
      profilePicture: 'http://example.com/other-pic.jpg',
      bio: 'Other test bio',
      location: 'Vancouver, BC',
      latitude: 49.2827,
      longitude: -123.1207,
      skillLevel: 'Expert'
    };
    otherTestUser = await userModel.create(newOtherUser);
    
    // Create HTTP server and Socket.IO instance
    httpServer = createServer();
    io = new Server(httpServer, {
      cors: {
        origin: '*',
        methods: ['GET', 'POST'],
        credentials: true,
      },
    });

    // Initialize SocketService
    socketService = new SocketService(io);

    // Start server
    await new Promise<void>((resolve) => {
      httpServer.listen(0, () => {
        resolve();
      });
    });

    // Wait a bit for server to be fully ready
    await new Promise(resolve => setTimeout(resolve, 100));

    // Generate JWT token for user
    userToken = jwt.sign({ id: testUser._id.toString() }, JWT_SECRET, { expiresIn: '1h' });
  });

  afterAll(async () => {
    // Cleanup all sockets first
    if (clientSocket) {
      if (clientSocket.connected) {
        clientSocket.disconnect();
      }
      clientSocket.removeAllListeners();
    }
    
    // Disconnect all sockets from server
    if (io) {
      io.sockets.sockets.forEach((socket) => {
        socket.disconnect(true);
      });
      io.close();
    }
    
    // Close HTTP server
    if (httpServer) {
      await new Promise<void>((resolve) => {
        httpServer.close(() => resolve());
      });
    }
    
    // Give time for cleanup
    await new Promise(resolve => setTimeout(resolve, 200));
    
    // Clean up test users
    if (testUser) {
      await userModel.delete(new mongoose.Types.ObjectId(testUser._id));
    }
    if (otherTestUser) {
      await userModel.delete(new mongoose.Types.ObjectId(otherTestUser._id));
    }
    
    await teardownTestDB();
  });

  beforeEach(() => {
    // Reset client socket before each test
    if (clientSocket) {
      if (clientSocket.connected) {
        clientSocket.disconnect();
      }
      clientSocket.removeAllListeners();
      clientSocket = null as any;
    }
  });

  afterEach(async () => {
    jest.restoreAllMocks();
    if (clientSocket) {
      if (clientSocket.connected) {
        clientSocket.disconnect();
      }
      clientSocket.removeAllListeners();
      clientSocket = null as any;
    }
    // Give time for cleanup
    await new Promise(resolve => setTimeout(resolve, 100));
  });

  const getPort = () => (httpServer.address() as any)?.port;
  const getBaseURL = () => `http://localhost:${getPort()}`;

  const connectClient = (token: string, timeout: number = 3000): Promise<ClientSocket> => {
    return new Promise((resolve, reject) => {
      let timeoutId: NodeJS.Timeout;
      let resolved = false;

      const cleanup = () => {
        if (timeoutId) {
          clearTimeout(timeoutId);
        }
      };

      const socket = Client(getBaseURL(), {
        auth: { token },
        transports: ['websocket'],
        autoConnect: true,
      });

      const onConnect = () => {
        if (resolved) return;
        resolved = true;
        cleanup();
        socket.removeListener('connect', onConnect);
        socket.removeListener('connect_error', onError);
        resolve(socket);
      };

      const onError = (error: Error) => {
        if (resolved) return;
        resolved = true;
        cleanup();
        socket.removeListener('connect', onConnect);
        socket.removeListener('connect_error', onError);
        socket.disconnect();
        reject(error);
      };

      socket.on('connect', onConnect);
      socket.on('connect_error', onError);

      timeoutId = setTimeout(() => {
        if (!resolved && !socket.connected) {
          resolved = true;
          socket.removeListener('connect', onConnect);
          socket.removeListener('connect_error', onError);
          socket.disconnect();
          reject(new Error('Connection timeout'));
        }
      }, timeout);
    });
  };

  describe('Authentication errors', () => {
    test('returns error when userModel.findById fails', async () => {
      // Mock userModel.findById to throw an error
      jest.spyOn(userModel, 'findById').mockRejectedValue(new Error('Database error'));

      // Try to connect
      await expect(connectClient(userToken)).rejects.toBeDefined();
    });

    test('returns error when userModel.findById returns null', async () => {
      // Mock userModel.findById to return null
      jest.spyOn(userModel, 'findById').mockResolvedValue(null);

      // Try to connect
      await expect(connectClient(userToken)).rejects.toBeDefined();
    });
  });

  describe('join_room event - mocked', () => {
    test('returns error when Chat.getForUser fails', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      clientSocket = await connectClient(userToken);

      // Mock Chat.getForUser to throw an error
      jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Database connection failed'));

      const errorPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('error event timeout'));
        }, 5000);

        const onError = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        clientSocket.on('error', onError);
      });

      clientSocket.emit('join_room', { chatId: mockChatId });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Database connection failed');
      expect(Chat.getForUser).toHaveBeenCalledTimes(1);
    });

    test('returns error when database timeout occurs', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      clientSocket = await connectClient(userToken);

      // Mock Chat.getForUser to throw timeout error
      jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Connection timeout'));

      const errorPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('error event timeout'));
        }, 5000);

        const onError = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        clientSocket.on('error', onError);
      });

      clientSocket.emit('join_room', { chatId: mockChatId });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Connection timeout');
      expect(Chat.getForUser).toHaveBeenCalledTimes(1);
    });

    test('returns error when unexpected error occurs', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      clientSocket = await connectClient(userToken);

      // Mock Chat.getForUser to throw unexpected error
      jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Unexpected error'));

      const errorPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('error event timeout'));
        }, 5000);

        const onError = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        clientSocket.on('error', onError);
      });

      clientSocket.emit('join_room', { chatId: mockChatId });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Unexpected error');
      expect(Chat.getForUser).toHaveBeenCalledTimes(1);
    });
  });

  describe('send_message event - mocked', () => {
    test('returns error when Chat.getForUser fails', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      clientSocket = await connectClient(userToken);

      // Mock Chat.getForUser to throw an error
      jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Database error'));

      const errorPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('error event timeout'));
        }, 5000);

        const onError = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        clientSocket.on('error', onError);
      });

      clientSocket.emit('send_message', { chatId: mockChatId, content: 'Test message' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Database error');
      expect(Chat.getForUser).toHaveBeenCalledTimes(1);
    });

    test('returns error when Message.createMessage fails', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      clientSocket = await connectClient(userToken);

      // Mock Chat.getForUser to return a valid chat
      const mockChat = { 
        _id: new mongoose.Types.ObjectId(mockChatId), 
        participants: [testUser._id] 
      };
      jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);
      // Mock Message.createMessage to throw an error
      jest.spyOn(Message, 'createMessage').mockRejectedValue(new Error('Failed to create message'));

      const errorPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('error event timeout'));
        }, 5000);

        const onError = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        clientSocket.on('error', onError);
      });

      clientSocket.emit('send_message', { chatId: mockChatId, content: 'Test message' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Failed to create message');
      expect(Chat.getForUser).toHaveBeenCalledTimes(1);
      expect(Message.createMessage).toHaveBeenCalledTimes(1);
    });

    test('returns error when Message.getMessageById fails', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      const mockMessageId = new mongoose.Types.ObjectId();
      clientSocket = await connectClient(userToken);

      // Mock Chat.getForUser to return a valid chat
      const mockChat = { 
        _id: new mongoose.Types.ObjectId(mockChatId), 
        participants: [testUser._id] 
      };
      jest.spyOn(Chat, 'getForUser').mockResolvedValue(mockChat as any);
      // Mock Message.createMessage to return a message
      const mockMessage = { _id: mockMessageId, content: 'Test message', sender: testUser._id.toString() };
      jest.spyOn(Message, 'createMessage').mockResolvedValue(mockMessage as any);
      // Mock Message.getMessageById to throw an error
      jest.spyOn(Message, 'getMessageById').mockRejectedValue(new Error('Failed to fetch message'));

      const errorPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('error event timeout'));
        }, 5000);

        const onError = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        clientSocket.on('error', onError);
      });

      clientSocket.emit('send_message', { chatId: mockChatId, content: 'Test message' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Failed to fetch message');
      expect(Message.createMessage).toHaveBeenCalledTimes(1);
      expect(Message.getMessageById).toHaveBeenCalledTimes(1);
    });

    test('returns error when database connection fails', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      clientSocket = await connectClient(userToken);

      // Mock Chat.getForUser to throw connection error
      jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Database connection failed'));

      const errorPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('error event timeout'));
        }, 5000);

        const onError = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        clientSocket.on('error', onError);
      });

      clientSocket.emit('send_message', { chatId: mockChatId, content: 'Test message' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Database connection failed');
      expect(Chat.getForUser).toHaveBeenCalledTimes(1);
    });

    test('returns error when network timeout occurs', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      clientSocket = await connectClient(userToken);

      // Mock Chat.getForUser to throw timeout error
      jest.spyOn(Chat, 'getForUser').mockRejectedValue(new Error('Connection timeout'));

      const errorPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('error event timeout'));
        }, 5000);

        const onError = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        clientSocket.on('error', onError);
      });

      clientSocket.emit('send_message', { chatId: mockChatId, content: 'Test message' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Connection timeout');
      expect(Chat.getForUser).toHaveBeenCalledTimes(1);
    });
  });

  describe('leave_room event - mocked', () => {
    test('handles leave room without errors', async () => {
      const mockChatId = new mongoose.Types.ObjectId().toString();
      clientSocket = await connectClient(userToken);

      // Leave room should work without database calls
      const leavePromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('left_room timeout'));
        }, 5000);

        const onLeft = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('left_room', onLeft);
          resolve(data);
        };

        clientSocket.on('left_room', onLeft);
      });

      clientSocket.emit('leave_room', { chatId: mockChatId });
      
      const result: any = await leavePromise;
      expect(result.chatId).toBe(mockChatId);
    });
  });
});
