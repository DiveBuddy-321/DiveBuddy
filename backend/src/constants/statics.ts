export const IMAGES_DIR = 'uploads/images';

export const SKILL_LEVELS = ['Beginner', 'Intermediate', 'Expert'] as const;

export const MAX_AGE = 100;
export const MIN_AGE = 13;

export const MAX_LEVEL = 3;
export const MIN_LEVEL = 1;

export const AGE_DIFF_WEIGHT = 10; //factor to multiply age difference by when calculating closest matches in buddy matching algorithm
export const SKILL_DIFF_WEIGHT = 10; //factor to multiply skill level difference by when calculating closest matches in buddy matching algorithm

//BUDDY MATCHING: threshold for number of users to return based on filtered user counts
export const SMALL_USER_COUNT_THRESHOLD = 10; //if number of filtered users is less than this, return all users
export const LARGE_USER_COUNT_THRESHOLD = 200; //if number of filtered users is greater than the SMALL_USER_COUNT_THRESHOLD but less than this, return 50% of users
export const MAX_USERS_TO_RETURN = 100; //if number of returned users is greater than large threshold, return this number of users

export const EARTH_RADIUS = 6371; // in km

export type SkillLevel = typeof SKILL_LEVELS[number];