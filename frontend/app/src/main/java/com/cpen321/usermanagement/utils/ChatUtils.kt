package com.cpen321.usermanagement.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatUtils {
    fun formatLastMessageTime(lastMessageAt: Date?): String {
        if (lastMessageAt == null) return ""

        val now = Date()
        val diff = now.time - lastMessageAt.time

        return when {
            diff < 60 * 1000 -> "now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(lastMessageAt)
        }
    }
}