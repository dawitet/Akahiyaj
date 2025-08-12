package com.dawitf.akahidegn.data.remote.service.impl

import android.util.Log
import com.dawitf.akahidegn.core.error.AppError

import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.data.remote.service.GroupService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupServiceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : GroupService {

    private val groupsRef: DatabaseReference = database.getReference("groups")

    override fun getAllGroups(): Flow<Result<List<Group>>> = callbackFlow {
        Log.d("GroupService", "Setting up real-time listener for all groups")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val groups = snapshot.children.mapNotNull { childSnapshot ->
                        try {
                            val group = childSnapshot.getValue(Group::class.java)
                            if (group != null) {
                                // Ensure groupId is populated from the snapshot key
                                if (group.groupId == null) {
                                    group.groupId = childSnapshot.key
                                }
                                Log.d("GroupService", "Loaded group: ${group.groupId} - ${group.destinationName}")
                                group
                            } else {
                                Log.w("GroupService", "Failed to deserialize group from snapshot: ${childSnapshot.key}")
                                null
                            }
                        } catch (e: Exception) {
                            Log.e("GroupService", "Error deserializing group ${childSnapshot.key}", e)
                            null
                        }
                    }
                    Log.d("GroupService", "Successfully loaded ${groups.size} groups from Firebase")
                    trySend(Result.success(groups))
                } catch (e: Exception) {
                    Log.e("GroupService", "Error processing groups data", e)
                    trySend(Result.failure(AppError.UnknownError(e.message ?: "Failed to process groups data")))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupService", "Firebase listener cancelled: ${error.message}")
                trySend(Result.failure(AppError.NetworkError.FirebaseError(error.message)))
            }
        }

        // Enable Firebase offline persistence to improve reliability
        database.setPersistenceEnabled(true)
        groupsRef.keepSynced(true) // Keep groups data synced even when offline
        groupsRef.addValueEventListener(listener)

        awaitClose {
            Log.d("GroupService", "Removing Firebase listener")
            groupsRef.removeEventListener(listener)
        }
    }

    override suspend fun getGroupById(groupId: String): Result<Group> {
        return try {
            val snapshot = groupsRef.child(groupId).get().await()
            val group = snapshot.getValue(Group::class.java)
            if (group != null) {
                Result.success(group)
            } else {
                Result.failure(AppError.ValidationError.NotFound("Group not found"))
            }
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Unknown error occurred"))
        }
    }



    override suspend fun createGroup(group: Group): Result<Group> {
        return try {
            val newGroupRef = groupsRef.push()
            val newGroupId = newGroupRef.key ?: throw Exception("Failed to generate group ID")
            val groupWithId = group.copy(groupId = newGroupId)
            newGroupRef.setValue(groupWithId).await()
            Result.success(groupWithId)
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun updateGroup(group: Group): Result<Group> {
        return try {
            val groupId = group.groupId ?: throw Exception("Group ID is null")
            groupsRef.child(groupId).setValue(group).await()
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            groupsRef.child(groupId).removeValue().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun joinGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupRef = groupsRef.child(groupId)
            groupRef.child("members").child(userId).setValue(true).await()
            groupRef.child("memberCount").runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val currentCount = mutableData.getValue(Int::class.java) ?: 0
                    mutableData.value = currentCount + 1
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    // Handle completion, if needed
                }
            })

            val userSnapshot = database.getReference("users").child(userId).get().await()
            val userName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown User"

            val notification = mapOf(
                "type" to "group_update",
                "title" to "New Member",
                "body" to "$userName has joined the group."
            )
            sendNotificationToGroupMembers(groupId, notification)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupRef = groupsRef.child(groupId)
            groupRef.child("members").child(userId).removeValue().await()
            groupRef.child("memberCount").runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val currentCount = mutableData.getValue(Int::class.java) ?: 0
                    mutableData.value = maxOf(0, currentCount - 1)
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    // Handle completion, if needed
                }
            })

            val userSnapshot = database.getReference("users").child(userId).get().await()
            val userName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown User"

            val notification = mapOf(
                "type" to "group_update",
                "title" to "Member Left",
                "body" to "$userName has left the group."
            )
            sendNotificationToGroupMembers(groupId, notification)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun getExpiredGroups(thresholdTimestamp: Long): Result<List<Group>> {
        return try {
            val snapshot = groupsRef.orderByChild("timestamp").endAt(thresholdTimestamp.toDouble()).get().await()
            val expiredGroups = snapshot.children.mapNotNull { it.getValue(Group::class.java) }
            Result.success(expiredGroups)
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Unknown error occurred"))
        }
    }

    

    private suspend fun sendNotificationToGroupMembers(groupId: String, notification: Map<String, String>) {
        try {
            val groupSnapshot = groupsRef.child(groupId).get().await()
            val group = groupSnapshot.getValue(Group::class.java)
            if (group != null) {
                val tokens = group.memberDetails.map { it.value.phone }.filterNotNull()
                val data = notification + mapOf("groupId" to groupId)
                // In a real app, you would use a server to send notifications to a list of tokens.
                // For this example, we'll just log the tokens and data.
                Log.d("NOTIFICATION", "Sending notification to tokens: $tokens with data: $data")
            }
        } catch (e: Exception) {
            Log.e("NOTIFICATION", "Failed to send notification", e)
        }
    }
}