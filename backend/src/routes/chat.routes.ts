import { Router } from "express";
import type { Request, Response, NextFunction } from "express";
import { ChatController } from "../controllers/chat.controller";

const router = Router();
const chatController = new ChatController();

router.get("/", async (req: Request, res: Response, next: NextFunction) => {
  try { await chatController.listChats(req, res); }
  catch (err: unknown) { next(err); }
});
router.post("/", async (req: Request, res: Response, next: NextFunction) => {
  try { await chatController.createChat(req, res); }
  catch (err: unknown) { next(err); }
});
router.get("/:chatId", async (req: Request, res: Response, next: NextFunction) => {
  try { await chatController.getChat(req, res); }
  catch (err: unknown) { next(err); }
});
router.get("/messages/:chatId", async (req: Request, res: Response, next: NextFunction) => {
  try { await chatController.getMessages(req, res); }
  catch (err: unknown) { next(err); }
});
router.post("/:chatId/messages", async (req: Request, res: Response, next: NextFunction) => {
  try { await chatController.sendMessage(req, res); }
  catch (err: unknown) { next(err); }
});

export default router;
