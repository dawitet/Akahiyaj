package com.dawitf.akahidegn.util

import com.dawitf.akahidegn.R

/**
 * Avatar utility for providing fallback avatars for users and groups
 */
object AvatarUtil {
    
    // Curated user avatar drawables
    private val userAvatars = listOf(
        R.drawable.user_avatar_1,
        R.drawable.user_avatar_2,
        R.drawable.user_avatar_3,
        R.drawable.user_avatar_4,
        R.drawable.user_avatar_5
    )
    
    // Curated group avatar drawables
    private val groupAvatars = listOf(
        R.drawable.group_avatar_1,
        R.drawable.group_avatar_2,
        R.drawable.group_avatar_3,
        R.drawable.group_avatar_4,
        R.drawable.group_avatar_5
    )
    
    /**
     * Get a random user avatar based on user ID or name for consistency
     */
    fun getUserAvatar(userId: String? = null): Int {
        val index = if (userId != null) {
            userId.hashCode().let { if (it < 0) -it else it } % userAvatars.size
        } else {
            (0 until userAvatars.size).random()
        }
        return userAvatars[index]
    }
    
    /**
     * Get a random group avatar based on group ID or name for consistency
     */
    fun getGroupAvatar(groupId: String? = null): Int {
        val index = if (groupId != null) {
            groupId.hashCode().let { if (it < 0) -it else it } % groupAvatars.size
        } else {
            (0 until groupAvatars.size).random()
        }
        return groupAvatars[index]
    }
    
    /**
     * Get a default user avatar (first in the list)
     */
    fun getDefaultUserAvatar(): Int = userAvatars.first()
    
    /**
     * Get a default group avatar (first in the list)
     */
    fun getDefaultGroupAvatar(): Int = groupAvatars.first()
}
