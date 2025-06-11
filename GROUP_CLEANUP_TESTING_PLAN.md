# Group Cleanup Testing Plan

## 🎯 Objective
Clean up all existing groups from Firebase and test the 30-minute group cleanup functionality from a fresh slate.

## 📋 Step-by-Step Cleanup & Testing Process

### Phase 1: Manual Cleanup (Choose ONE method)

#### Method 1: Firebase Console (Easiest) ⭐
1. **Go to Firebase Console**:
   - Visit: https://console.firebase.google.com/
   - Select your `Akahidegn` project

2. **Navigate to Realtime Database**:
   - Click "Realtime Database" in the left sidebar
   - Find the `groups` node in your database

3. **Delete All Groups**:
   - Click the "⋮" (three dots) next to `groups`
   - Select "Delete" to remove all groups at once
   - Confirm deletion

#### Method 2: App Debug Menu (Recommended for Testing)
1. **Build and Install App**:
   ```bash
   cd /Users/dawitsahle/AndroidStudioProjects/Akahidegn
   ./gradlew assembleDebug
   /Users/dawitsahle/Android/Sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Open Debug Menu**:
   - Open the app
   - Navigate to Debug Menu (if available in main menu)
   - Or add debug functionality to settings

3. **Mass Delete Groups**:
   - Click "🔥 MASS DELETE ALL GROUPS" button
   - Monitor logs: `adb logcat | grep GroupCleanupDebug`
   - Verify deletion completion in logs

#### Method 3: Firebase CLI
1. **Install Firebase CLI** (if not installed):
   ```bash
   npm install -g firebase-tools
   firebase login
   ```

2. **Run Cleanup Script**:
   ```bash
   ./firebase_cli_cleanup.sh
   ```
   - Choose option 1 to view current groups
   - Choose option 3 to backup groups first
   - Choose option 2 to delete all groups

### Phase 2: Verify Cleanup Complete

1. **Check Firebase Console**:
   - Verify `groups` node is empty or doesn't exist

2. **Check App**:
   - Open app and verify no groups are displayed
   - Use debug menu "Log All Groups with Timestamps"

### Phase 3: Test Group Cleanup Functionality

#### 3.1 Create Test Groups
1. **Create Multiple Test Groups**:
   - Use debug menu "Create Test Group" button (create 3-5 groups)
   - Or create groups manually through the app

2. **Verify Groups Created**:
   - Check Firebase Console to see new groups
   - Note their timestamps
   - Use "Log All Groups with Timestamps" in debug menu

#### 3.2 Test Immediate Cleanup (Simulated)
1. **Create Old Test Group**:
   - Temporarily modify `createTestGroup()` to use old timestamp:
   ```kotlin
   val oldTimestamp = System.currentTimeMillis() - (35 * 60 * 1000) // 35 minutes ago
   ```

2. **Test Cleanup Detection**:
   - Use "🧹 Delete Old Groups (30+ min)" button
   - Check logs to verify old groups are identified and deleted

#### 3.3 Test Automatic Cleanup
1. **Create Fresh Groups**:
   - Create 2-3 new groups with current timestamps

2. **Wait for Natural Expiration**:
   - Option A: Wait 30+ minutes (slow but real test)
   - Option B: Use "🧹 Delete Old Groups (1+ min)" for faster testing

3. **Trigger Cleanup**:
   - Use "Trigger Immediate Cleanup" button
   - Check logs for cleanup results

### Phase 4: Monitor & Verify

#### 4.1 Check Logs
Monitor these log tags:
```bash
# Main cleanup logs
adb logcat | grep -E "(GroupCleanupDebug|GroupCleanupWorker|FirebaseGroupService)"

# Specific cleanup events
adb logcat | grep "MASS DELETION\|cleanup\|expired"
```

#### 4.2 Expected Log Output
```
GroupCleanupDebug: Found X groups to delete
GroupCleanupDebug: ✅ Successfully deleted: [group info]
GroupCleanupDebug: 🔥 MASS DELETION COMPLETED!
GroupCleanupWorker: Successfully cleaned up X expired groups
```

#### 4.3 Verify in Firebase
- Check Firebase Console to confirm groups are deleted
- Verify only active (recent) groups remain

### Phase 5: Long-term Testing

#### 5.1 Test Automatic Scheduling
1. **Create Groups**: Add 2-3 groups through normal app usage
2. **Monitor Over Time**: Check if groups are automatically cleaned after 30 minutes
3. **Check WorkManager**: Verify background cleanup is working

#### 5.2 Test Edge Cases
- Groups with missing timestamps
- Groups created by different users
- App backgrounded/killed scenarios

## 🚨 Important Notes

### Security Considerations
- Mass deletion is **PERMANENT** - no undo
- Backup groups before deletion if needed
- Test on development environment first

### Expected Behavior
- ✅ Groups older than 30 minutes should be deleted
- ✅ Active groups (< 30 minutes) should remain
- ✅ Cleanup should run automatically every 30 minutes
- ✅ Manual cleanup should work immediately

### Troubleshooting
If cleanup doesn't work:
1. Check Firebase security rules (users can only delete their own groups)
2. Verify WorkManager is scheduled properly
3. Check network connectivity requirements
4. Monitor for Firebase authentication issues

## 📊 Success Criteria

✅ All old groups successfully deleted manually  
✅ New test groups created and visible  
✅ Old test groups identified correctly  
✅ Cleanup deletes expired groups only  
✅ Recent groups remain untouched  
✅ Automatic cleanup scheduling works  
✅ Debug tools provide clear feedback  

## 🔧 Debug Tools Available

1. **🔥 MASS DELETE ALL GROUPS** - Nuclear option, deletes everything
2. **🧹 Delete Old Groups (1+ min)** - Quick test with 1-minute threshold
3. **🧹 Delete Old Groups (30+ min)** - Normal 30-minute cleanup
4. **Trigger Immediate Cleanup** - Test WorkManager cleanup
5. **Log All Groups with Timestamps** - View current state
6. **Create Test Group** - Add test data

Ready to start the cleanup and testing process!
