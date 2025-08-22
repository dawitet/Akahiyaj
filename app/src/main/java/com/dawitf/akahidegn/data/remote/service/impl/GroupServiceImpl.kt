package com.dawitf.akahidegn.data.remote.service.impl

import android.util.Log
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.core.result.success
import com.dawitf.akahidegn.core.result.failure
import com.dawitf.akahidegn.data.remote.service.GroupService
import com.dawitf.akahidegn.domain.model.MemberInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupServiceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
                    trySend(success(groups))
                } catch (e: Exception) {
                    Log.e("GroupService", "Error processing groups data", e)
                    trySend(failure(AppError.UnknownError(e.message ?: "Failed to process groups data").message))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupService", "Firebase listener cancelled: ${error.message}")
                trySend(failure(AppError.NetworkError.FirebaseError(error.message).message))
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
                success(group)
            } else {
                failure(AppError.ValidationError.NotFound("Group not found").message)
            }
        } catch (e: Exception) {
            failure(AppError.UnknownError(e.message ?: "Unknown error occurred").message)
        }
    }



    override suspend fun createGroup(group: Group): Result<Group> {
        return try {
            val newGroupRef = groupsRef.push()
            val newGroupId = newGroupRef.key ?: throw Exception("Failed to generate group ID")
            val now = System.currentTimeMillis()
            val ensured = group.copy(
                groupId = newGroupId,
                timestamp = group.timestamp ?: now,
                expiresAt = group.expiresAt ?: (group.timestamp ?: now) + (30 * 60 * 1000)
            )
            newGroupRef.setValue(ensured).await()
            success(ensured)
        } catch (e: Exception) {
            failure(AppError.UnknownError(e.message ?: "Unknown error occurred").message)
        }
    }

    override suspend fun updateGroup(group: Group): Result<Group> {
        return try {
            val groupId = group.groupId ?: throw Exception("Group ID is null")
            groupsRef.child(groupId).setValue(group).await()
            success(group)
        } catch (e: Exception) {
            failure(AppError.UnknownError(e.message ?: "Unknown error occurred").message)
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            groupsRef.child(groupId).removeValue().await()

            success(Unit)
        } catch (e: Exception) {
            failure(AppError.UnknownError(e.message ?: "Unknown error occurred").message)
        }
    }

    override suspend fun joinGroup(groupId: String, userId: String, userInfo: MemberInfo): Result<Unit> {
        return try {
            val groupRef = groupsRef.child(groupId)

            // Create a map of all the updates we want to perform
            val updates = mapOf(
                "members/$userId" to true,
                "memberDetails/$userId" to userInfo // Save the complete MemberInfo object
            )
            // Atomically apply all updates
            groupRef.updateChildren(updates).await()

            // Update the member count in a transaction
            groupRef.child("memberCount").runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val currentCount = mutableData.getValue(Int::class.java) ?: 0
                    mutableData.value = currentCount + 1
                    return Transaction.success(mutableData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    // Completion is handled by the main task
                }
            })

            // Fetch user display name from Firestore (permanent users)
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userName = userDoc.getString("name") ?: "Unknown User"

            val notification = mapOf(
                "type" to "group_update",
                "title" to "New Member",
                "body" to "$userName has joined the group."
            )
            sendNotificationToGroupMembers(groupId, notification)

            success(Unit)
        } catch (e: Exception) {
            failure(AppError.UnknownError(e.message ?: "Unknown error occurred").message)
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

            val userDoc = firestore.collection("users").document(userId).get().await()
            val userName = userDoc.getString("name") ?: "Unknown User"

            val notification = mapOf(
                "type" to "group_update",
                "title" to "Member Left",
                "body" to "$userName has left the group."
            )
            sendNotificationToGroupMembers(groupId, notification)

            success(Unit)
        } catch (e: Exception) {
            failure(AppError.UnknownError(e.message ?: "Unknown error occurred").message)
        }
    }

    override suspend fun getExpiredGroups(thresholdTimestamp: Long): Result<List<Group>> {
        return try {
            val snapshot = groupsRef.orderByChild("expiresAt").endAt(thresholdTimestamp.toDouble()).get().await()
            val expiredGroups = snapshot.children.mapNotNull { it.getValue(Group::class.java) }
            success(expiredGroups)
        } catch (e: Exception) {
            failure(AppError.UnknownError(e.message ?: "Unknown error occurred").message)
        }
    }

    private suspend fun sendNotificationToGroupMembers(groupId: String, notification: Map<String, String>) {
        try {
            val groupSnapshot = groupsRef.child(groupId).get().await()
            val group = groupSnapshot.getValue(Group::class.java)
            if (group != null) {
                val tokens = group.memberDetails.values.mapNotNull { it.phone }
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