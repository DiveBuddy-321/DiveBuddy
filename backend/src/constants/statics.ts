export const IMAGES_DIR = 'uploads/images';

export const SKILL_LEVELS = ['Beginner', 'Intermediate', 'Expert'] as const;

export type SkillLevel = typeof SKILL_LEVELS[number];