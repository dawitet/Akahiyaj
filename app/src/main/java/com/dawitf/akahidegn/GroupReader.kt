package com.dawitf.akahidegn

import android.util.Log
import com.google.firebase.database.DataSnapshot

/**
 * Helper class to read Group objects from Firebase that were written using toMap()
 */
object GroupReader {
    /**
     * Converts a Firebase DataSnapshot to a Group object
     * This handles the format difference between how we write (toMap) and read groups
     */
    fun fromSnapshot(snapshot: DataSnapshot): Group? {
        try {
            // Create an empty group
            val group = Group()
            
            // Set the group ID from the Firebase key
            group.groupId = snapshot.key
            
            // Get all the data as a map
            val data = snapshot.value as? Map<*, *> ?: return null
            
            // Map the fields from Firebase format to Group object
            // Handle the specific format we're using in toMap()
            group.creatorId = data["createdBy"] as? String
            group.creatorName = data["creatorName"] as? String
            group.creatorCloudflareId = data["creatorCloudflareId"] as? String
            group.destinationName = data["to"] as? String
            group.originalDestination = data["originalDestination"] as? String
            group.pickupLat = (data["pickupLat"] as? Double) ?: 0.0
            group.pickupLng = (data["pickupLng"] as? Double) ?: 0.0
            group.timestamp = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
            group.maxMembers = (data["maxMembers"] as? Long)?.toInt() ?: 4
            group.memberCount = (data["memberCount"] as? Long)?.toInt() ?: 0
            group.imageUrl = data["imageUrl"] as? String
            
            // Convert members map
            val membersMap = data["members"] as? Map<*, *>
            if (membersMap != null) {
                group.members = HashMap()
                for ((key, value) in membersMap) {
                    if (key is String && value is Boolean && value) {
                        group.members[key] = true
                    }
                }
            }
            
            // Convert member details map
            val memberDetailsMap = data["memberDetails"] as? Map<*, *>
            if (memberDetailsMap != null) {
                group.memberDetails = HashMap()
                for ((key, value) in memberDetailsMap) {
                    if (key is String && value is Map<*, *>) {
                        val name = value["name"] as? String ?: ""
                        val phone = value["phone"] as? String ?: ""
                        val avatar = value["avatar"] as? String ?: "avatar_1"
                        val joinedAt = (value["joinedAt"] as? Long) ?: System.currentTimeMillis()
                        
                        group.memberDetails[key] = MemberInfo(name, phone, avatar, joinedAt)
                    }
                }
            }
            
            Log.d("GROUP_READER", "Successfully read group: ${group.destinationName} with ID: ${group.groupId}")
            // Print detailed group info for debugging
            Log.d("GROUP_DATA", "Group details: creator=${group.creatorName}, " +
                    "destination=${group.destinationName}, members=${group.memberCount}")
            return group
        } catch (e: Exception) {
            Log.e("GROUP_READER", "Failed to parse group from Firebase: ${e.message}", e)
            // Log the raw data to help with debugging
            try {
                val rawData = snapshot.value
                Log.e("GROUP_READER", "Raw data: $rawData")
            } catch (ex: Exception) {
                Log.e("GROUP_READER", "Couldn't log raw data: ${ex.message}")
            }
            return null
        }
    }
}
