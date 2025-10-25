import { IUser } from './user.types';


export type GetAllBuddiesResponse = {
  message: string;
  data?: {
    buddies: { user: IUser; distance: number }[];
  };
}