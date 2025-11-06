import { IUser } from './user.types';


export interface GetAllBuddiesResponse {
  message: string;
  data?: {
    buddies: { user: IUser; distance: number }[];
  };
}