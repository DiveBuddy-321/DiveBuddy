import { Router } from "express";
import { ChatController } from "../controllers/chat.controller";

const router = Router();
const chatController = new ChatController();

router.get("/", (req, res) => { void chatController.listChats(req, res); });
router.post("/", (req, res) => { void chatController.createChat(req, res); });
router.get("/:chatId", (req, res) => { void chatController.getChat(req, res); });
router.get("/messages/:chatId", (req, res) => { void chatController.getMessages(req, res); });
router.post("/:chatId/messages", (req, res) => { void chatController.sendMessage(req, res); });

export default router;
