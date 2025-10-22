package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.dto.Buddy

interface BuddyRepository {
    suspend fun getBuddies(
        targetMinLevel: Int? = null,
        targetMaxLevel: Int? = null,
        targetMinAge: Int? = null,
        targetMaxAge: Int? = null
    ): Result<List<Buddy>>
}

