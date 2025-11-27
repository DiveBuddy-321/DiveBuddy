import { z } from 'zod';

export const blockUserSchema = z.object({
  targetUserId: z.string().min(1, 'targetUserId is required'),
});

export type BlockUserRequest = z.infer<typeof blockUserSchema>;

export type BlockResponse = { message: string };


