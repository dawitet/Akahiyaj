/**
 * One-time mass deletion script for Firebase groups
 * This script will delete ALL groups from Firebase Realtime Database
 * 
 * USAGE:
 * 1. Add this to your app temporarily
 * 2. Call massDeleteAllGroups() from your debug helper
 * 3. Remove this code after cleanup
 */

// Add this to GroupCleanupDebugHelper.kt temporarily
fun massDeleteAllGroups() {
    CoroutineScope(Dispatchers.IO).launch {
        Log.d(TAG, "🔥 MASS DELETION: Starting to delete ALL groups from Firebase")
        
        try {
            val result = groupRepository.getAllGroups().first()
            
            when (result) {
                is Result.Success -> {
                    val allGroups = result.data
                    Log.d(TAG, "Found ${allGroups.size} groups to delete")
                    
                    allGroups.forEach { group ->
                        Log.d(TAG, "Deleting group: ${group.destinationName} (${group.groupId})")
                        
                        try {
                            groupRepository.deleteGroup(group.groupId ?: "")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to delete group ${group.groupId}: ${e.message}")
                        }
                    }
                    
                    Log.d(TAG, "🔥 MASS DELETION: Completed! Deleted ${allGroups.size} groups")
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to get groups for mass deletion: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mass deletion failed: ${e.message}", e)
        }
    }
}

// Alternative: Direct Firebase deletion (more aggressive)
fun directFirebaseMassDelete() {
    CoroutineScope(Dispatchers.IO).launch {
        Log.d(TAG, "🔥 DIRECT FIREBASE DELETION: Removing all groups")
        
        try {
            // This requires access to Firebase Database reference
            // You'd need to inject FirebaseDatabase into the debug helper
            val database = FirebaseDatabase.getInstance()
            val groupsRef = database.getReference("groups")
            
            groupsRef.removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "🔥 All groups deleted successfully from Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete all groups: ${e.message}", e)
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Direct Firebase deletion failed: ${e.message}", e)
        }
    }
}

/**
 * INSTRUCTIONS TO ADD THIS TO YOUR APP:
 * 
 * 1. Open GroupCleanupDebugHelper.kt
 * 2. Add the massDeleteAllGroups() function
 * 3. In MainActivity debug section, add a button to call this function
 * 4. Run the app and click the mass delete button
 * 5. Check logs to verify deletion
 * 6. Remove this code after cleanup
 */
