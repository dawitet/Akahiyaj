package com.dawitf.akahidegn.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.dawitf.akahidegn.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    
    @Query("SELECT * FROM groups WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllGroupsPaged(): PagingSource<Int, GroupEntity>
    
    @Query("SELECT * FROM groups WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>
    
    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntity?
    
    @Query("""
        SELECT * FROM groups 
        WHERE isActive = 1 
        AND ((latitude - :latitude) * (latitude - :latitude) + 
             (longitude - :longitude) * (longitude - :longitude)) <= :radiusSquared
        ORDER BY createdAt DESC
    """)
    fun getNearbyGroups(latitude: Double, longitude: Double, radiusSquared: Double): Flow<List<GroupEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)
    
    @Update
    suspend fun updateGroup(group: GroupEntity)
    
    @Delete
    suspend fun deleteGroup(group: GroupEntity)
    
    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: String)
    
    @Query("DELETE FROM groups WHERE updatedAt < :timestamp")
    suspend fun deleteOldGroups(timestamp: Long)
    
    @Query("UPDATE groups SET memberCount = :memberCount WHERE id = :groupId")
    suspend fun updateMemberCount(groupId: String, memberCount: Int)
}
