package com.dawitf.akahidegn.data.remote.service.impl

import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.core.retry.RetryMechanism
import com.dawitf.akahidegn.data.remote.service.FirebaseGroupService
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class FirebaseGroupServiceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val retryMechanism: RetryMechanism
) : FirebaseGroupService {
    
    private val groupsRef = database.reference.child("groups")
    
    override fun getAllGroups(): Flow<Result<List<Group>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val groups = snapshot.children.mapNotNull { child ->
                        child.getValue(Group::class.java)?.copy(groupId = child.key ?: "")
                    }
                    trySend(Result.Success(groups))
                } catch (e: Exception) {
                    trySend(Result.Error(AppError.NetworkError.DataParsingError(e.message ?: "Failed to parse groups")))
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.Error(AppError.NetworkError.FirebaseError(error.message)))
            }
        }
        
        groupsRef.addValueEventListener(listener)
        awaitClose { groupsRef.removeEventListener(listener) }
    }
    
    override suspend fun getGroupById(groupId: String): Result<Group> {
        return try {
            retryMechanism.withRetry {
                val snapshot = groupsRef.child(groupId).get().await()
                val group = snapshot.getValue(Group::class.java)
                if (group != null) {
                    Result.Success(group.copy(groupId = groupId))
                } else {
                    Result.Error(AppError.ValidationError.NotFound("Group not found"))
                }
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to get group"))
        }
    }
    
    override fun getNearbyGroups(latitude: Double, longitude: Double, radiusKm: Double): Flow<Result<List<Group>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val allGroups = snapshot.children.mapNotNull { child ->
                        child.getValue(Group::class.java)?.copy(groupId = child.key ?: "")
                    }
                    
                    val nearbyGroups = allGroups.filter { group ->
                        val distance = calculateDistance(latitude, longitude, group.pickupLat ?: 0.0, group.pickupLng ?: 0.0)
                        distance <= radiusKm
                    }
                    
                    trySend(Result.Success(nearbyGroups))
                } catch (e: Exception) {
                    trySend(Result.Error(AppError.NetworkError.DataParsingError(e.message ?: "Failed to parse nearby groups")))
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.Error(AppError.NetworkError.FirebaseError(error.message)))
            }
        }
        
        groupsRef.addValueEventListener(listener)
        awaitClose { groupsRef.removeEventListener(listener) }
    }
    
    override suspend fun createGroup(group: Group): Result<Group> {
        return try {
            retryMechanism.withRetry {
                val groupRef = if (group.groupId?.isNotEmpty() == true) {
                    groupsRef.child(group.groupId!!)
                } else {
                    groupsRef.push()
                }
                
                val groupWithId = group.copy(groupId = groupRef.key ?: "")
                groupRef.setValue(groupWithId).await()
                Result.Success(groupWithId)
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to create group"))
        }
    }
    
    override suspend fun updateGroup(group: Group): Result<Group> {
        return try {
            retryMechanism.withRetry {
                groupsRef.child(group.groupId ?: "").setValue(group).await()
                Result.Success(group)
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to update group"))
        }
    }
    
    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            retryMechanism.withRetry {
                groupsRef.child(groupId).removeValue().await()
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to delete group"))
        }
    }
    
    override suspend fun joinGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            retryMechanism.withRetry {
                val groupRef = groupsRef.child(groupId)
                val snapshot = groupRef.get().await()
                val group = snapshot.getValue(Group::class.java)
                
                if (group != null) {
                    val updatedGroup = group.copy(memberCount = group.memberCount + 1)
                    groupRef.setValue(updatedGroup).await()
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.ValidationError.NotFound("Group not found"))
                }
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to join group"))
        }
    }
    
    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            retryMechanism.withRetry {
                val groupRef = groupsRef.child(groupId)
                val snapshot = groupRef.get().await()
                val group = snapshot.getValue(Group::class.java)
                
                if (group != null) {
                    val updatedGroup = group.copy(memberCount = maxOf(0, group.memberCount - 1))
                    groupRef.setValue(updatedGroup).await()
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.ValidationError.NotFound("Group not found"))
                }
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to leave group"))
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
