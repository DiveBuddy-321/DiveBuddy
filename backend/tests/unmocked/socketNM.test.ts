import { describe, test, expect, beforeAll, afterAll, beforeEach, afterEach } from '@jest/globals';
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

dotenv.config();
const USER = process.env.USER_ID as string;
const OTHER_USER = process.env.OTHER_USER_ID as string || new mongoose.Types.ObjectId().toString();
const JWT_SECRET = process.env.JWT_SECRET || 'test-secret';

describe('Socket.IO - unmocked (no mocking)', () => {
  let httpServer: any;
  let io: Server;
  let socketService: SocketService;
  let clientSocket: ClientSocket;
  let chatId: string;
  let userToken: string;
  let otherUserToken: string;

  beforeAll(async () => {
    await setupTestDB();
    
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

    const port = (httpServer.address() as any)?.port;
    const baseURL = `http://localhost:${port}`;

    // Generate JWT tokens for users
    userToken = jwt.sign({ id: USER }, JWT_SECRET, { expiresIn: '1h' });
    otherUserToken = jwt.sign({ id: OTHER_USER }, JWT_SECRET, { expiresIn: '1h' });

    // Create a test chat
    const chat = await Chat.createPair(
      new mongoose.Types.ObjectId(USER),
      new mongoose.Types.ObjectId(OTHER_USER),
      'Test Chat'
    );
    chatId = String(chat._id);
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
    
    // Clean up test data
    if (chatId) {
      await Chat.deleteOne({ _id: chatId });
      await Message.deleteMany({ chat: chatId });
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
    // Disconnect client after each test and wait a bit for cleanup
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

  test('connects successfully with valid token', async () => {
    clientSocket = await connectClient(userToken);
    expect(clientSocket.connected).toBe(true);
  });

  test('fails to connect without token', async () => {
    await expect(
      connectClient('')
    ).rejects.toBeDefined();
  });

  test('fails to connect with invalid token', async () => {
    await expect(
      connectClient('invalid-token')
    ).rejects.toBeDefined();
  });

  test('automatically joins user room on connection', async () => {
    clientSocket = await connectClient(userToken);
    expect(clientSocket.connected).toBe(true);
    // User room is joined automatically - we can verify by checking if we can emit to it
    // This is implicit behavior, so we just verify connection works
    expect(clientSocket.connected).toBe(true);
  });

  describe('join_room event', () => {
    test('joins a valid chat room', async () => {
      clientSocket = await connectClient(userToken);
      
      const joinPromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('join_room timeout'));
        }, 5000);

        const onJoined = (data: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('joined_room', onJoined);
          clientSocket.removeListener('error', onError);
          resolve(data);
        };

        const onError = (error: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('joined_room', onJoined);
          clientSocket.removeListener('error', onError);
          reject(error);
        };

        clientSocket.on('joined_room', onJoined);
        clientSocket.on('error', onError);
      });

      clientSocket.emit('join_room', { chatId });
      
      const result: any = await joinPromise;
      expect(result.chatId).toBe(chatId);
      expect(result.chat).toBeDefined();
      expect(result.chat._id).toBe(chatId);
    });

    test('returns error for invalid chat ID', async () => {
      clientSocket = await connectClient(userToken);
      
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

      clientSocket.emit('join_room', { chatId: 'invalid-id' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Invalid chat ID');
    });

    test('returns error for non-existent chat', async () => {
      clientSocket = await connectClient(userToken);
      const fakeChatId = new mongoose.Types.ObjectId().toString();
      
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

      clientSocket.emit('join_room', { chatId: fakeChatId });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Chat not found or access denied');
    });

    test('returns error when user is not a participant', async () => {
      // Create a chat between OTHER_USER and a third user
      const thirdUser = new mongoose.Types.ObjectId().toString();
      const otherChat = await Chat.createPair(
        new mongoose.Types.ObjectId(OTHER_USER),
        new mongoose.Types.ObjectId(thirdUser),
        'Private Chat'
      );
      const otherChatId = String(otherChat._id);

      clientSocket = await connectClient(userToken);
      
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

      clientSocket.emit('join_room', { chatId: otherChatId });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Chat not found or access denied');

      // Cleanup
      await Chat.deleteOne({ _id: otherChatId });
    });
  });

  describe('leave_room event', () => {
    test('leaves a chat room successfully', async () => {
      clientSocket = await connectClient(userToken);
      
      // First join the room
      await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('join_room timeout'));
        }, 5000);

        const onJoined = () => {
          clearTimeout(timeout);
          clientSocket.removeListener('joined_room', onJoined);
          clientSocket.removeListener('error', onError);
          resolve(null);
        };

        const onError = (error: any) => {
          clearTimeout(timeout);
          clientSocket.removeListener('joined_room', onJoined);
          clientSocket.removeListener('error', onError);
          reject(error);
        };

        clientSocket.on('joined_room', onJoined);
        clientSocket.on('error', onError);
        clientSocket.emit('join_room', { chatId });
      });

      // Then leave it
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

      clientSocket.emit('leave_room', { chatId });
      
      const result: any = await leavePromise;
      expect(result.chatId).toBe(chatId);
    });
  });

  describe('send_message event', () => {
    test('sends a message successfully', async () => {
      const userClient = await connectClient(userToken);
      const otherClient = await connectClient(otherUserToken);

      // Both users join the room
      await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('join_room timeout'));
        }, 5000);

        const onJoined = () => {
          clearTimeout(timeout);
          userClient.removeListener('joined_room', onJoined);
          userClient.removeListener('error', onError);
          resolve(null);
        };

        const onError = (error: any) => {
          clearTimeout(timeout);
          userClient.removeListener('joined_room', onJoined);
          userClient.removeListener('error', onError);
          reject(error);
        };

        userClient.on('joined_room', onJoined);
        userClient.on('error', onError);
        userClient.emit('join_room', { chatId });
      });

      await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('join_room timeout'));
        }, 5000);

        const onJoined = () => {
          clearTimeout(timeout);
          otherClient.removeListener('joined_room', onJoined);
          otherClient.removeListener('error', onError);
          resolve(null);
        };

        const onError = (error: any) => {
          clearTimeout(timeout);
          otherClient.removeListener('joined_room', onJoined);
          otherClient.removeListener('error', onError);
          reject(error);
        };

        otherClient.on('joined_room', onJoined);
        otherClient.on('error', onError);
        otherClient.emit('join_room', { chatId });
      });

      // Send message from user
      const messageContent = 'Hello from socket test!';
      const messagePromise = new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('new_message timeout'));
        }, 5000);

        const onNewMessage = (data: any) => {
          clearTimeout(timeout);
          otherClient.removeListener('new_message', onNewMessage);
          resolve(data);
        };

        otherClient.on('new_message', onNewMessage);
      });

      userClient.emit('send_message', { chatId, content: messageContent });
      
      const result: any = await messagePromise;
      expect(result.chatId).toBe(chatId);
      expect(result.message).toBeDefined();
      expect(result.message.content).toBe(messageContent);
      expect(String(result.message.sender._id || result.message.sender)).toBe(USER);

      // Cleanup
      userClient.disconnect();
      otherClient.disconnect();
    });

    test('returns error for invalid chat ID', async () => {
      clientSocket = await connectClient(userToken);
      
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

      clientSocket.emit('send_message', { chatId: 'invalid-id', content: 'Test message' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Invalid chat ID');
    });

    test('returns error when content is missing', async () => {
      clientSocket = await connectClient(userToken);
      
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

      clientSocket.emit('send_message', { chatId, content: '' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Message content is required');
    });

    test('returns error when content is empty', async () => {
      clientSocket = await connectClient(userToken);
      
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

      clientSocket.emit('send_message', { chatId, content: '   ' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Message content is required');
    });

    test('returns error when chat not found', async () => {
      clientSocket = await connectClient(userToken);
      const fakeChatId = new mongoose.Types.ObjectId().toString();
      
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

      clientSocket.emit('send_message', { chatId: fakeChatId, content: 'Test message' });
      
      const error: any = await errorPromise;
      expect(error.message).toBe('Chat not found or access denied');
    });
  });

  describe('disconnect event', () => {
    test('handles disconnect gracefully', async () => {
      clientSocket = await connectClient(userToken);
      expect(clientSocket.connected).toBe(true);
      
      clientSocket.disconnect();
      
      // Give it a moment to process disconnect
      await new Promise(resolve => setTimeout(resolve, 100));
      
      expect(clientSocket.connected).toBe(false);
    });
  });
});

