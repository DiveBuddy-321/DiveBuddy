import { IUser, isUserReadyForBuddyMatching } from '../types/user.types';
import { MAX_AGE, 
    MIN_AGE, 
    MAX_LEVEL, 
    MIN_LEVEL, 
    SKILL_LEVELS, 
    EARTH_RADIUS, 
    AGE_DIFF_WEIGHT, 
    SKILL_DIFF_WEIGHT, 
    SMALL_USER_COUNT_THRESHOLD, 
    LARGE_USER_COUNT_THRESHOLD, 
    MAX_USERS_TO_RETURN } from '../constants/statics';

export const buddyAlgorithm = (
    long: number,
    lat: number,
    level: number,
    age: number,
    targetMinLevel: number | undefined,
    targetMaxLevel: number | undefined,
    targetMinAge: number | undefined,
    targetMaxAge: number | undefined,
    users: IUser[]
) => {
    // Filter users who have completed their profile for buddy matching
    let eligibleUsers = users.filter(user => isUserReadyForBuddyMatching(user));

    //if user has specified a minimum and maximum level, limit users to those within the range
    if (targetMinLevel !== undefined && targetMaxLevel !== undefined) {
        const filteredUsers = eligibleUsers.filter(user => {
            const numericLevel = getNumericLevel(user);
            return numericLevel !== undefined && (numericLevel) >= targetMinLevel && (numericLevel) <= targetMaxLevel;
        });
        eligibleUsers = filteredUsers;
    }
    //if user has specified a minimum and maximum age, filter eligible users to those within the range
    if (targetMinAge !== undefined && targetMaxAge !== undefined) {
        const filteredUsers = eligibleUsers.filter(user => user.age !== undefined && user.age >= targetMinAge && user.age <= targetMaxAge);
        eligibleUsers = filteredUsers;
    }

    const distanceUsers = new Map<IUser, number>();
    for (const user of eligibleUsers) {
        const uLong = user.longitude;
        const uLat = user.latitude;
        const uLevel = getNumericLevel(user);
        const distance = calculateDistance(uLong, uLat, uLevel, user.age, long, lat, level, age);
        distanceUsers.set(user, distance);
    }
    const filteredUserCount = distanceUsers.size;
    //sort results in order of closest to furthest distance
    const sortedDistanceUsers = Array.from(distanceUsers.entries()).sort((a, b) => a[1] - b[1]);

    //threshold for number of users to return based on filtered user counts
    if (filteredUserCount < SMALL_USER_COUNT_THRESHOLD) {
        return sortedDistanceUsers
    } else {
        if (filteredUserCount < LARGE_USER_COUNT_THRESHOLD) {
            const threshold = Math.min(Math.floor(filteredUserCount * 0.5), SMALL_USER_COUNT_THRESHOLD);
            return sortedDistanceUsers.slice(0, threshold);
        } else {
            return sortedDistanceUsers.slice(0, MAX_USERS_TO_RETURN);
        }
    }

    
    
}

function calculateDistance(
    long1: number | undefined,
    lat1: number | undefined,
    level1: number | undefined,
    age1: number | undefined,
    long2: number | undefined,
    lat2: number | undefined,
    level2: number | undefined,
    age2: number | undefined
) {

    //do not consider users with missing data in calculations
    if (long1 === undefined || lat1 === undefined || level1 === undefined || age1 === undefined || long2 === undefined || lat2 === undefined || level2 === undefined || age2 === undefined) {
        return Infinity;
    }
        // Calculate geographic distance in kilometers using Haversine formula
        const geoDistance = haversineDistance(lat1, long1, lat2, long2); // in km
        // Normalize age difference (assuming age range 13-100, so max diff = 87)
        const ageDiff = Math.abs(age1 - age2) / (MAX_AGE - MIN_AGE);
        // Normalize skill level difference (beginner = 1, intermediate = 2, expert = 3, so max diff = 2)
        const skillDiff = Math.abs(level1 - level2) / (MAX_LEVEL - MIN_LEVEL);
        // weighted sum of differences for age and skill level, adjust parameter in constants.ts as needed
        return geoDistance + ageDiff * AGE_DIFF_WEIGHT + skillDiff * SKILL_DIFF_WEIGHT;
    }
    // Haversine formula to calculate distance between two lat/long points in km
    function haversineDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
        const toRad = (value: number) => value * Math.PI / 180;
        const R = EARTH_RADIUS; // Earth radius in km
        const dLat = toRad(lat2 - lat1);
        const dLon = toRad(lon2 - lon1);
        const a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

function getNumericLevel(user: IUser): number | undefined {
    if (user.skillLevel !== undefined) {
        // Map skill levels to numeric scale 1..3
        const index = SKILL_LEVELS.indexOf(user.skillLevel);
        return index === -1 ? undefined : index + 1;
    }
    return undefined;
}