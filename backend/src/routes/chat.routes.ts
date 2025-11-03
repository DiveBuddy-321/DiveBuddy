import { Router } from "express";
import { ChatController } from "../controllers/chat.controller";

const router = Router();
const chatController = new ChatController();

router.get("/", (req, res, next) => { chatController.listChats(req, res).catch(next); });
router.post("/", (req, res, next) => { chatController.createChat(req, res).catch(next); });
router.get("/:chatId", (req, res, next) => { chatController.getChat(req, res).catch(next); });
router.get("/messages/:chatId", (req, res, next) => { chatController.getMessages(req, res).catch(next); });
router.post("/:chatId/messages", (req, res, next) => { chatController.sendMessage(req, res).catch(next); });

export default router;
