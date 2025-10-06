package com.cpen321.usermanagement.data.remote.dto

data class Buddy(
    val user: User,
    val distance: Double
)

data class BuddiesData(
    val buddies: List<Buddy>
)

