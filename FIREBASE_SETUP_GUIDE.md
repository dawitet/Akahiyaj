# Firebase Setup Guide for Akahiyaj (አካሂያጅ) Ride-Sharing App

## ✅ Implementation Status

🎉 **COMPLETED**: All Firebase components have been successfully integrated:
- ✅ Firebase dependencies updated in build.gradle.kts with BoM 32.7.0+
- ✅ Firebase initialized in AkahidegnApplication.kt with Auth and Database persistence
- ✅ Group cleanup system fully implemented with WorkManager (30-minute automatic cleanup)
- ✅ Production-ready security rules documented below (ready for deployment)
- ✅ Comprehensive testing suite implemented
- ✅ Debug helpers available for testing cleanup functionality

🚀 **READY**: The app is fully integrated with Firebase and ready for production deployment!

## 🔥 Firebase Project Configuration

### Step 1: Firebase Console Setup

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Create or Select Project**: 
   - If creating new: Click "Create a project" → Name it "akahiyaj"
   - If existing: Select your current "akahidegn" project

3. **Enable Required Services**:
   - ✅ Authentication (Anonymous sign-in)
   - ✅ Realtime Database
   - ✅ Cloud Messaging (FCM) for notifications
   - ✅ Analytics (optional but recommended)

### Step 2: Android App Configuration

1. **Add Android App**:
   - Package name: `com.dawitf.akahidegn` (keep existing to avoid breaking code)
   - App nickname: "Akahiyaj Android"
   - SHA-1 key: (get from `keytool -list -v -keystore ~/.android/debug.keystore`)

2. **Download google-services.json**:
   - Place in `app/` directory
   - Replace existing file if updating

### Step 3: Authentication Setup

1. **Go to Authentication → Sign-in method**
2. **Enable Anonymous Authentication**:
   ```
   ✅ Anonymous: Enabled
   ```

### Step 4: Realtime Database Setup

1. **Go to Realtime Database → Create Database**
2. **Choose Location**: Select closest to your users (e.g., us-central1)
3. **Security Rules**: Start in test mode, then apply rules below

## 🔒 Firebase Security Rules

### Realtime Database Rules

```json
{
  "rules": {
    "groups": {
      // Indexing for performant queries on these fields
      ".indexOn": ["createdAt", "from", "to"],
      "$groupId": {
        // Anyone can read group data (e.g., for a public list of trips).
        // For more security, you could change this to "auth != null".
        ".read": true,

        // CRITICAL FIX: Write Rule
        // - A user must be authenticated (auth != null).
        // - On CREATION (!data.exists()), the 'createdBy' field in the new data must match the user's ID.
        // - On UPDATE (data.exists()), the 'createdBy' field in the existing data must match the user's ID.
        ".write": "auth != null && ((!data.exists() && newData.child('createdBy').val() == auth.uid) || (data.exists() && data.child('createdBy').val() == auth.uid))",

        // Top-level validation for a new or updated group
        ".validate": "newData.hasChildren(['id', 'from', 'to', 'departureTime', 'availableSeats', 'pricePerPerson', 'createdAt', 'createdBy'])",

        // ---- Individual Field Validations ----
        "id": {
          ".validate": "newData.isString() && newData.val() == $groupId"
        },
        "from": {
          ".validate": "newData.isString() && newData.val().length > 0"
        },
        "to": {
          ".validate": "newData.isString() && newData.val().length > 0"
        },
        "departureTime": {
          // It's often better to use a number (timestamp) for dates for easier querying and validation
          ".validate": "newData.isString() && newData.val().length > 0"
        },
        "availableSeats": {
          ".validate": "newData.isNumber() && newData.val() >= 1 && newData.val() <= 8"
        },
        "pricePerPerson": {
          ".validate": "newData.isNumber() && newData.val() >= 0"
        },
        "createdAt": {
          // Combined validation: Ensures timestamp is a number and is not set to a future time.
          ".validate": "newData.isNumber() && newData.val() <= now"
        },
        "createdBy": {
          // On creation, ensures the creator is the person writing the data.
          ".validate": "newData.isString() && newData.val() == auth.uid"
        },

        // ---- Sub-collections ----
        "members": {
          "$uid": {
            // A user can add themselves, OR the group creator can add anyone.
            ".write": "auth != null && ($uid == auth.uid || root.child('groups').child($groupId).child('createdBy').val() == auth.uid)",
            ".validate": "newData.hasChildren(['name', 'joinedAt'])"
          }
        },
        "chat": {
          "$messageId": {
            // Only a group member or the creator can write a message.
            ".write": "auth != null && (data.parent().parent().child('members').child(auth.uid).exists() || data.parent().parent().child('createdBy').val() == auth.uid)",
            // Validate the message structure and ensure the sender is the authenticated user.
            ".validate": "newData.hasChildren(['text', 'sender', 'timestamp']) && newData.child('sender').val() == auth.uid"
          }
        },
        // All other child nodes under a group are not allowed
        "$other": { ".validate": false }
      }
    },

    "users": {
      "$uid": {
        // A user can only read and write their own data. Perfect.
        ".read": "$uid == auth.uid",
        ".write": "$uid == auth.uid",

        // Assuming a user object can have 'profile' and 'settings'
        ".validate": "newData.hasChildren(['profile'])",

        "profile": {
          ".validate": "newData.hasChildren(['name', 'email'])" // Example with more fields
        },
        "settings": {
          // This allows settings to be an object with key-value pairs of primitives.
          "$settingKey": {
            ".validate": "newData.isString() || newData.isBoolean() || newData.isNumber()"
          }
        },
         // All other child nodes under a user are not allowed
        "$other": { ".validate": false }
      }
    },

    "cleanup": {
      // Read-only node, likely for a server process. This is fine.
      ".read": true,
      ".write": false
    }
  }
}
```

### Key Security Features:
- ✅ **CRITICAL SECURITY FIX**: Only group creators can modify their own groups - prevents unauthorized data tampering
- ✅ **Proper Write Permissions**: Users can only create groups as themselves and only modify groups they created
- ✅ **Member Management**: Users can add themselves OR group creators can add anyone to their groups
- ✅ **Chat Security**: Only group members and creators can send messages in group chats
- ✅ **Data Validation**: Strict validation for all group data with proper field requirements
- ✅ **User Data Protection**: Users can only access their own profile and settings data
- ✅ **Structured Validation**: Clear separation between profile and settings with proper validation
- ✅ **Unknown Field Prevention**: `$other` rules prevent unauthorized fields from being added
- ✅ **Authentication Required**: All write operations require proper authentication

## 🗑️ Automatic Group Cleanup Configuration

### Cloud Functions Setup (Recommended)

Create a Cloud Function to automatically delete old groups:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.cleanupOldGroups = functions.pubsub.schedule('every 5 minutes')
  .onRun(async (context) => {
    const db = admin.database();
    const now = Date.now();
    const thirtyMinutesAgo = now - (30 * 60 * 1000); // 30 minutes in milliseconds
    
    try {
      // Get all groups
      const groupsSnapshot = await db.ref('groups').once('value');
      const groups = groupsSnapshot.val();
      
      if (groups) {
        const deletionPromises = [];
        
        Object.keys(groups).forEach(groupId => {
          const group = groups[groupId];
          if (group.createdAt && group.createdAt < thirtyMinutesAgo) {
            console.log(`Deleting old group: ${groupId}, created at: ${new Date(group.createdAt)}`);
            deletionPromises.push(db.ref(`groups/${groupId}`).remove());
          }
        });
        
        await Promise.all(deletionPromises);
        console.log(`Cleanup completed. Deleted ${deletionPromises.length} old groups.`);
      }
    } catch (error) {
      console.error('Cleanup failed:', error);
    }
    
    return null;
  });
```

### Alternative: Client-Side Cleanup Logic

For simpler setups without Cloud Functions, you can implement cleanup logic in your Android app:

```kotlin
// In your MainViewModel or GroupRepository
private fun cleanupOldGroups() {
    val database = Firebase.database.reference.child("groups")
    val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000)
    
    database.orderByChild("createdAt")
        .endAt(thirtyMinutesAgo.toDouble())
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (groupSnapshot in snapshot.children) {
                    val createdBy = groupSnapshot.child("createdBy").value as? String
                    val currentUser = Firebase.auth.currentUser?.uid
                    
                    // Only delete groups created by the current user
                    if (createdBy == currentUser) {
                        groupSnapshot.ref.removeValue()
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("Cleanup", "Failed to cleanup old groups", error.toException())
            }
        })
}
```

**Note**: This approach requires each user to clean up only their own groups due to the security rules.

## 📱 Firebase Configuration in Android App

### Step 1: Update build.gradle (app level)

```kotlin
dependencies {
    // Firebase BoM
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    
    // Firebase services
    implementation 'com.google.firebase:firebase-auth-ktx'
    implementation 'com.google.firebase:firebase-database-ktx'
    implementation 'com.google.firebase:firebase-messaging-ktx'
    implementation 'com.google.firebase:firebase-analytics-ktx'
}
```

### Step 2: Initialize Firebase in Application Class

```kotlin
class AkahiyajApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Enable Firebase offline persistence
        if (!Firebase.database.reference.isReady) {
            Firebase.database.setPersistenceEnabled(true)
        }
        
        // Initialize Firebase Auth
        Firebase.auth.signInAnonymously()
    }
}
```

## 🔄 Data Synchronization Settings

### Realtime Database Configuration

1. **Go to Realtime Database → Rules**
2. **Apply the security rules above**
3. **Test the rules** using the Rules simulator

### Offline Persistence

Your app already has offline persistence enabled. This ensures:
- ✅ App works offline
- ✅ Data syncs when connection returns
- ✅ No data loss during network issues

## 🧪 Testing Firebase Setup

### Test 1: Authentication
```bash
# Check if anonymous auth is working
adb logcat | grep "Firebase Auth"
```

### Test 2: Database Connection
```bash
# Check database connection
adb logcat | grep "Firebase Database"
```

### Test 3: Group Cleanup
1. Create a test group
2. Wait 30+ minutes
3. Check if group is automatically deleted

## 🚨 Important Notes

### Security Checklist:
- ✅ **Never use test mode rules in production**
- ✅ **Always validate user input**
- ✅ **Monitor database usage and costs**
- ✅ **Set up Firebase Security Rules backup**

### 🔒 Security Improvements in These Rules:

#### Major Security Fixes:
1. **Group Write Protection**: 
   - ❌ **OLD**: `".write": "auth != null"` - ANY authenticated user could modify ANY group
   - ✅ **NEW**: Only group creators can modify their own groups
   
2. **Proper Member Management**:
   - Users can add themselves to groups
   - Group creators can add any user to their groups
   - Prevents unauthorized member additions

3. **Chat Message Security**:
   - Only group members and creators can send messages
   - Message sender validation prevents impersonation
   - Proper message structure validation

4. **Data Structure Validation**:
   - Clear separation between user profile and settings
   - Prevents unknown fields from being added (`$other` rules)
   - Proper field type and content validation

#### Why These Rules Are Better:
- **Prevents Data Tampering**: Users cannot modify other users' groups
- **Prevents Spam**: Users cannot add messages to groups they're not part of
- **Data Integrity**: Strict validation ensures clean, consistent data
- **Performance**: Proper indexing for efficient queries
- **Future-Proof**: Structured validation allows for easy feature additions

### Performance Optimization:
- ✅ **Enable database indexing** for queries
- ✅ **Use database listeners efficiently**
- ✅ **Implement proper error handling**
- ✅ **Monitor memory usage**

### Backup Strategy:
- ✅ **Export database regularly**
- ✅ **Set up automated backups**
- ✅ **Test restore procedures**

## 📊 Monitoring & Analytics

### Firebase Console Monitoring:
1. **Authentication**: Monitor sign-in methods and user counts
2. **Database**: Monitor read/write operations and data usage
3. **Performance**: Monitor app performance metrics
4. **Crashlytics**: Monitor app crashes and errors

### Recommended Alerts:
- Database usage > 80% of quota
- Authentication failures > 10% of attempts
- App crashes > 1% of sessions

---

## 🎯 Next Steps After Setup

1. **Test all functionality** in the Firebase console
2. **Verify group cleanup** is working correctly
3. **Monitor database usage** for the first week
4. **Set up backup procedures**
5. **Configure push notifications** (optional)

---

*Setup Guide Created: June 8, 2025*  
*Compatible with: Firebase SDK 32.7.0+*  
*App Version: 0.7*
