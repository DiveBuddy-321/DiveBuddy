import { Router } from 'express';

import { authenticateToken } from './middleware/auth.middleware';
import authRoutes from './routes/auth.routes';
import buddyRoutes from './routes/buddy.routes';
import mediaRoutes from './routes/media.routes';
import usersRoutes from './routes/user.routes';
<<<<<<< HEAD
import eventsRoutes from './routes/event.routes';
=======
>>>>>>> 85a61613683af7debc9b4b3ed16d6c1c85eff903
import chatRoutes from "./routes/chat.routes";

const router = Router();

router.use('/auth', authRoutes);

router.use('/buddy', authenticateToken, buddyRoutes);

router.use('/users', authenticateToken, usersRoutes);

router.use('/media', authenticateToken, mediaRoutes);

router.use("/chat", chatRoutes);

export default router;
