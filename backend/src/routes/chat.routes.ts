import { Router } from "express";
import { ChatController } from "../controllers/chat.controller";

const router = Router();
const chatController = new ChatController();

router.get("/", (req, res) => chatController.listChats(req, res));
router.post("/", (req, res) => chatController.createChat(req, res));
router.get("/:chatId", (req, res) => chatController.getChat(req, res));
router.get("/messages/:chatId", (req, res) => chatController.getMessages(req, res));

export default router;
