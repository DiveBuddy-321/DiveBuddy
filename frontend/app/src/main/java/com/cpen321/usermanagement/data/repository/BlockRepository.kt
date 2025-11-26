package com.cpen321.usermanagement.data.repository

interface BlockRepository {
    suspend fun blockUser(targetUserId: String): Result<Unit>
    suspend fun unblockUser(targetUserId: String): Result<Unit>
    suspend fun getBlockedUsers(): Result<List<String>>
    suspend fun checkIfBlockedBy(targetUserId: String): Result<Boolean>
}

