import { Router } from 'express';

import { authenticateToken } from './middleware/auth.middleware';
import authRoutes from './routes/auth.routes';
import buddyRoutes from './routes/buddy.routes';
import mediaRoutes from './routes/media.routes';
import usersRoutes from './routes/user.routes';
import eventsRoutes from './routes/event.routes';
import chatRoutes from "./routes/chat.routes";
import blockRoutes from './routes/block.routes';

const router = Router();

router.use('/auth', authRoutes);

router.use('/buddy', authenticateToken, buddyRoutes);

router.use('/users', authenticateToken, usersRoutes);

router.use('/media', authenticateToken, mediaRoutes);

router.use('/events', authenticateToken, eventsRoutes);

router.use("/chats", authenticateToken, chatRoutes);

router.use('/blocks', authenticateToken, blockRoutes);

export default router;
