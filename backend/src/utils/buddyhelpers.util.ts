import { Request } from 'express';
import { IUser } from '../types/user.types';
import { SKILL_LEVELS } from '../constants/statics';

export function parseBuddyFilters(
  req: Request,
  currentUser: IUser
): {
  targetMinLevel?: number;
  targetMaxLevel?: number;
  targetMinAge?: number;
  targetMaxAge?: number;
  allowedSkillLevels: string[];
  ageFilter: Record<string, number>;
  locationFilter: { latitude: Record<string, number>; longitude: Record<string, number> };
} {
  const targetMinLevel = req.query.targetMinLevel !== undefined ? Number(req.query.targetMinLevel) : undefined;
  const targetMaxLevel = req.query.targetMaxLevel !== undefined ? Number(req.query.targetMaxLevel) : undefined;
  const targetMinAge = req.query.targetMinAge !== undefined ? Number(req.query.targetMinAge) : undefined;
  const targetMaxAge = req.query.targetMaxAge !== undefined ? Number(req.query.targetMaxAge) : undefined;

  const levelMin = targetMinLevel ?? 1;
  const levelMax = targetMaxLevel ?? SKILL_LEVELS.length;
  const allowedSkillLevels = SKILL_LEVELS.slice(
    Math.max(0, levelMin - 1),
    Math.min(SKILL_LEVELS.length, levelMax)
  );

  const ageFilter: Record<string, number> = {};
  if (targetMinAge !== undefined) ageFilter.$gte = targetMinAge;
  if (targetMaxAge !== undefined) ageFilter.$lte = targetMaxAge;

  const locationFilter = {
    latitude: { 
      $gte: (currentUser.latitude ?? 0) - 5, 
      $lte: (currentUser.latitude ?? 0) + 5 
    },
    longitude: { 
      $gte: (currentUser.longitude ?? 0) - 5, 
      $lte: (currentUser.longitude ?? 0) + 5 
    },
  };

  return {
    targetMinLevel,
    targetMaxLevel,
    targetMinAge,
    targetMaxAge,
    allowedSkillLevels,
    ageFilter,
    locationFilter,
  };
}

export function toNumericLevelFromSkill(skillLevel: unknown): number | undefined {
  if (typeof skillLevel !== 'string') return undefined;
  if (!SKILL_LEVELS.includes(skillLevel as (typeof SKILL_LEVELS)[number])) return undefined;
  const idx = SKILL_LEVELS.indexOf(skillLevel as (typeof SKILL_LEVELS)[number]);
  return idx === -1 ? undefined : idx + 1;
}

export function toBuddyResponse(
  sortedBuddies: Array<[IUser, number]>
): Array<{ user: IUser; distance: number }> {
  return sortedBuddies.map(([user, distance]) => ({ user, distance }));
}
