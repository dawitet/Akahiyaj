package com.dawitf.akahidegn

import android.util.Log
import com.google.firebase.database.DataSnapshot

/**
 * Helper class to read Group objects from Firebase
 */
object GroupReader {
    /**
     * Converts a Firebase DataSnapshot to a Group object
     */
    fun fromSnapshot(snapshot: DataSnapshot): Group? {
        try {
            val group = Group()
            group.groupId = snapshot.key
            val data = snapshot.value as? Map<*, *> ?: run {
                Log.e("GROUP_READER", "Snapshot value is not a Map or is null for key: ${snapshot.key}")
                return null
            }

            // Map fields directly corresponding to Group.kt properties
            group.creatorId = data["creatorId"] as? String
            group.creatorName = data["creatorName"] as? String
            group.creatorCloudflareId = data["creatorCloudflareId"] as? String // Keep if used
            group.destinationName = data["destinationName"] as? String
            group.originalDestination = data["originalDestination"] as? String
            group.from = data["from"] as? String
            group.to = data["to"] as? String
            group.status = data["status"] as? String
            group.pickupLat = data["pickupLat"] as? Double
            group.pickupLng = data["pickupLng"] as? Double
            group.timestamp = data["timestamp"] as? Long
            group.maxMembers = (data["maxMembers"] as? Long)?.toInt() ?: (data["maxMembers"] as? Int ?: 4)
            group.memberCount = (data["memberCount"] as? Long)?.toInt() ?: (data["memberCount"] as? Int ?: 0)
            group.imageUrl = data["imageUrl"] as? String

            // Convert members map
            (data["members"] as? Map<*, *>)?.let { membersMap ->
                group.members = HashMap()
                membersMap.forEach { (key, value) ->
                    if (key is String && value is Boolean) {
                        group.members[key] = value
                    }
                }
            }

            // Convert memberDetails map
            (data["memberDetails"] as? Map<*, *>)?.let { memberDetailsMap ->
                group.memberDetails = HashMap()
                memberDetailsMap.forEach { (key, value) ->
                    if (key is String && value is Map<*, *>) {
                        val name = value["name"] as? String ?: ""
                        val phone = value["phone"] as? String ?: ""
                        val avatar = value["avatar"] as? String ?: "avatar_1" // Default avatar
                        val joinedAt = value["joinedAt"] as? Long ?: System.currentTimeMillis()
                        group.memberDetails[key] = MemberInfo(name, phone, avatar, joinedAt)
                    }
                }
            }
            
            // Defaulting timestamp if null after attempting to read, to prevent issues with non-null expectations later
            if (group.timestamp == null) {
                group.timestamp = System.currentTimeMillis()
                Log.w("GROUP_READER", "Group ${group.groupId} missing timestamp, defaulted to current time.")
            }
            if (group.pickupLat == null) group.pickupLat = 0.0
            if (group.pickupLng == null) group.pickupLng = 0.0


            Log.d("GROUP_READER", "Successfully parsed group: ID=${group.groupId}, Dest=${group.destinationName}")
            return group
        } catch (e: Exception) {
            Log.e("GROUP_READER", "Failed to parse group from Firebase snapshot key: ${snapshot.key}, Error: ${e.message}", e)
            val rawData = try { snapshot.value.toString() } catch (ex: Exception) { "Could not get raw data." }
            Log.e("GROUP_READER", "Raw data for key ${snapshot.key}: $rawData")
            return null
        }
    }
}