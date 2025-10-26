package com.cpen321.usermanagement.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatUtils {
    
    /**
     * Gets the other participant's ID from a chat with two participants
     * @param participants List of participant IDs
     * @param currentUserId Current user's ID
     * @return The other participant's ID, or null if not found
     */
    fun getOtherParticipantId(participants: List<String>, currentUserId: String): String? {
        return participants.find { it != currentUserId }
    }
    
    /**
     * Formats the last message time into a human-readable string
     * @param lastMessageAt Date of the last message
     * @return Formatted time string (e.g., "30m", "2h", "1d")
     */
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