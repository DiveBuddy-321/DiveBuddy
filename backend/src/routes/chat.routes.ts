import { Router } from "express";
import { ChatController } from "../controllers/chat.controller";
import { asyncHandler } from "../utils/asyncHandler.util";

const router = Router();
const chatController = new ChatController();

router.get("/", asyncHandler(async (req, res) => {
  await chatController.listChats(req, res);
}));

router.post("/", asyncHandler(async (req, res) => {
  await chatController.createChat(req, res);
}));

router.get("/:chatId", asyncHandler(async (req, res) => {
  await chatController.getChat(req, res);
}));

router.get("/messages/:chatId", asyncHandler(async (req, res) => {
  await chatController.getMessages(req, res);
}));

router.post("/:chatId/messages", asyncHandler(async (req, res) => {
  await chatController.sendMessage(req, res);
}));

export default router;
