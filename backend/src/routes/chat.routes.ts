import { Router } from "express";
import { authenticateToken } from "../middleware/auth.middleware";
import { ChatController } from "../controllers/chat.controller";

const router = Router();
const chatController = new ChatController();

router.get("/rooms", authenticateToken, (req, res) => chatController.listChats(req, res));
router.get("/:chatId", authenticateToken, (req, res) => chatController.getChat(req, res));
router.post("/newChat", authenticateToken, (req, res) => chatController.createChat(req, res));
export default router;
