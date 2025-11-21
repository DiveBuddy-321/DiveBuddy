import { Router } from "express";
import { ChatController } from "../controllers/chat.controller";
import { asyncHandler } from "../utils/asyncHandler.util";

const router = Router();
const chatController = new ChatController();

router.get("/", asyncHandler(async (req, res, next) => {
  await chatController.listChats(req, res, next);
}));

router.post("/", asyncHandler(async (req, res, next) => {
  await chatController.createChat(req, res, next);
}));

router.get("/:chatId", asyncHandler(async (req, res, next) => {
  await chatController.getChat(req, res, next);
}));

router.get("/messages/:chatId", asyncHandler(async (req, res, next) => {
  await chatController.getMessages(req, res, next);
}));

router.post("/:chatId/messages", asyncHandler(async (req, res, next) => {
  await chatController.sendMessage(req, res, next);
}));

export default router;
