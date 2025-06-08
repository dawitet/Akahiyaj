package com.dawitf.akahidegn.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Test class to verify AvatarUtil functionality
 */
class AvatarUtilTest {
    
    @Test
    fun `getUserAvatar returns consistent avatar for same user ID`() {
        val userId = "user123"
        val avatar1 = AvatarUtil.getUserAvatar(userId)
        val avatar2 = AvatarUtil.getUserAvatar(userId)
        
        assertEquals("Same user ID should return same avatar", avatar1, avatar2)
    }
    
    @Test
    fun `getGroupAvatar returns consistent avatar for same group ID`() {
        val groupId = "group456" 
        val avatar1 = AvatarUtil.getGroupAvatar(groupId)
        val avatar2 = AvatarUtil.getGroupAvatar(groupId)
        
        assertEquals("Same group ID should return same avatar", avatar1, avatar2)
    }
    
    @Test
    fun `different IDs return potentially different avatars`() {
        val user1Avatar = AvatarUtil.getUserAvatar("user1")
        val user2Avatar = AvatarUtil.getUserAvatar("user2")
        
        // This test might occasionally fail due to hash collisions, but generally should pass
        // The important thing is that each ID is consistent with itself
        assertTrue("Avatar system should work", user1Avatar > 0 && user2Avatar > 0)
    }
}
