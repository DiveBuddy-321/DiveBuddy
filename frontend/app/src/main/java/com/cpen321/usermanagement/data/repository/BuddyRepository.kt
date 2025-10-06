package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.dto.Buddy

interface BuddyRepository {
    suspend fun getBuddies(): Result<List<Buddy>>
}

