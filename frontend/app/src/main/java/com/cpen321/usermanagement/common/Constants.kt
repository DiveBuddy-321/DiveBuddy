package com.cpen321.usermanagement.common

object Constants {
    // Network
    const val NETWORK_TIMEOUT_MS = 15000
    const val NETWORK_RETRY_COUNT = 2
    const val NETWORK_RETRY_DELAY_MS = 1000

    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val PREFETCH_DISTANCE = 2

    // Input / search
    const val DEFAULT_DEBOUNCE_MS = 300

    // UI animations
    const val ANIM_SHORT_MS = 150
    const val ANIM_MEDIUM_MS = 300
    const val ANIM_LONG_MS = 600

    // Map / location
    const val DEFAULT_MAP_ZOOM = 12f
    const val MAP_MAX_ZOOM = 20f
    const val MAP_MIN_ZOOM = 3f

    // Validation / limits
    const val MAX_BIO_LENGTH = 500
    const val MAX_NAME_LENGTH = 50
    const val MAX_IMAGE_SIZE_MB = 10

    // Caching
    const val MEMORY_CACHE_SIZE_MB = 32
    const val DISK_CACHE_SIZE_MB = 256

    // Time helpers
    const val ONE_SECOND_MS = 1000
    const val ONE_MINUTE_MS = 60 * ONE_SECOND_MS
    const val ONE_HOUR_MS = 60 * ONE_MINUTE_MS

    //skill levels
    const val BEGINNER_LEVEL = 1
    const val INTERMEDIATE_LEVEL = 2
    const val ADVANCED_LEVEL = 3

    //age range
    const val MIN_AGE = 13
    const val MAX_AGE = 100
}


