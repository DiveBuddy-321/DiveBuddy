import { Types } from "mongoose";
export type Id = string | Types.ObjectId;

export interface IChat {
  _id?: Id;
  isGroup: boolean;
  name?: string | null;
  participants: Id[];                 // keep ids here; populate type separately if needed
  createdBy?: Id | null;              // optional/nullable to match model default
  lastMessage?: Id | null;
  lastMessageAt?: Date | null;        // include to match model & sorting
  createdAt?: Date;
  updatedAt?: Date;
}
