package com.dawitf.akahidegn.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug helper to test Firebase Security Rules compliance.
 * This creates a minimal group object that strictly follows the security rules.
 */
public class FirebaseRulesTestReceiver extends BroadcastReceiver {
    private static final String TAG = "FIREBASE_RULES_TEST";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && 
            intent.getAction().equals("com.dawitf.akahidegn.DEBUG_TEST_FIREBASE_RULES")) {
            
            Log.d(TAG, "Starting Firebase Rules compliance test...");
            testFirebaseRules();
        }
    }
    
    private void testFirebaseRules() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "No authenticated user! Test cannot proceed.");
            return;
        }
        
        String uid = auth.getCurrentUser().getUid();
        Log.d(TAG, "Current user ID: " + uid);
        
        // Get reference to the database
        DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        String groupId = groupsRef.push().getKey();
        if (groupId == null) {
            Log.e(TAG, "Failed to create group ID");
            return;
        }
        
        Log.d(TAG, "Testing with new group ID: " + groupId);
        
        // Create a minimal group that follows the exact structure required by security rules
        Map<String, Object> minimalGroup = new HashMap<>();
        minimalGroup.put("id", groupId);
        minimalGroup.put("from", "Current Location");
        minimalGroup.put("to", "Test Destination");
        minimalGroup.put("departureTime", "1718545678"); // String timestamp
        minimalGroup.put("availableSeats", 4);
        minimalGroup.put("pricePerPerson", 0);
        minimalGroup.put("createdAt", System.currentTimeMillis());
        minimalGroup.put("createdBy", uid); // CRITICAL: must match auth.uid
        
        // Create a properly structured members object
        Map<String, Object> members = new HashMap<>();
        Map<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("name", "Test User");
        memberInfo.put("joinedAt", System.currentTimeMillis());
        members.put(uid, memberInfo);
        minimalGroup.put("members", members);
        
        Log.d(TAG, "Writing minimal group: " + minimalGroup);
        
        // Try to write the group
        groupsRef.child(groupId).setValue(minimalGroup)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "SUCCESS! Group written to database. Security rules passed.");
                
                // Now try to read it back
                groupsRef.child(groupId).get()
                    .addOnSuccessListener(snapshot -> {
                        Log.d(TAG, "Read test also successful! Data: " + snapshot.getValue());
                        
                        // Clean up - delete test group after success
                        groupsRef.child(groupId).removeValue();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Read test failed: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "FAILED! Security rules validation failed: " + e.getMessage());
                Log.e(TAG, "Validation error details:", e);
            });
    }
}
