import { IUser, isUserReadyForBuddyMatching } from '../types/user.types';

export const buddyAlgorithm = (long: number, lat: number, level: number, age: number, targetMinLevel: number | undefined,
    targetMaxLevel: number | undefined, targetMinAge: number | undefined, targetMaxAge: number | undefined, users: IUser[]) => {
    // Filter users who have completed their profile for buddy matching
    let eligibleUsers = users.filter(user => isUserReadyForBuddyMatching(user));

    //if user has specified a minimum and maximum level, limit users to those within the range
    if (targetMinLevel !== undefined && targetMaxLevel !== undefined) {
        const filteredUsers = eligibleUsers.filter(user => user.level !== undefined && user.level >= targetMinLevel && user.level <= targetMaxLevel);
        eligibleUsers = filteredUsers;
    }
    //if user has specified a minimum and maximum age, filter eligible users to those within the range
    if (targetMinAge !== undefined && targetMaxAge !== undefined) {
        const filteredUsers = eligibleUsers.filter(user => user.age !== undefined && user.age >= targetMinAge && user.age <= targetMaxAge);
        eligibleUsers = filteredUsers;
    }

    const distanceUsers = new Map<IUser, number>();
    for (const user of eligibleUsers) {
        const distance = calculateDistance(user.long, user.lat, user.level, user.age, long, lat, level, age);
        distanceUsers.set(user, distance);
    }
    
    //sort results in order of closest to furthest distance
    const sortedDistanceUsers = Array.from(distanceUsers.entries()).sort((a, b) => a[1] - b[1]);
    return sortedDistanceUsers.slice(0,10);
}

function calculateDistance(long1: number | undefined, 
                            lat1: number | undefined, 
                            level1:number | undefined, 
                            age1:number | undefined, 
                            long2: number | undefined, 
                            lat2: number | undefined, 
                            level2:number | undefined, 
                            age2:number | undefined) {

    //do not consider users with missing data in calculations
    if (long1 === undefined || lat1 === undefined || level1 === undefined || age1 === undefined || long2 === undefined || lat2 === undefined || level2 === undefined || age2 === undefined) {
        return Infinity;
    }
    const adjustedAge1 = age1 / 5;
    const adjustedAge2 = age2 / 5;
    return Math.sqrt((Math.abs(long1 - long2)) ** 2 + (Math.abs(lat1 - lat2)) ** 2) + ((Math.abs(adjustedAge1 - adjustedAge2)) ** 2) + ((Math.abs(level1 - level2)) **2);
}