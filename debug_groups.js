// Debug script with authentication to check Firebase groups
// Run with: node debug_groups.js

const { initializeApp } = require('firebase/app');
const { getAuth, signInAnonymously } = require('firebase/auth');
const { getDatabase, ref, get, child } = require('firebase/database');

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyA-04oS-c5FmfZ-DekJqdBl_GTnW4b_yAo",
  authDomain: "akahidegn-79376.firebaseapp.com",
  databaseURL: "https://akahidegn-79376-default-rtdb.europe-west1.firebasedatabase.app",
  projectId: "akahidegn-79376",
  storageBucket: "akahidegn-79376.firebasestorage.app",
  messagingSenderId: "81721365267",
  appId: "1:81721365267:android:dcceea55dbf8b542adc0e6"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const database = getDatabase(app);

async function debugGroups() {
  try {
    console.log("🔐 Attempting to authenticate...");

    // Try anonymous authentication (if enabled) or you'll need to implement proper auth
    try {
      await signInAnonymously(auth);
      console.log("✅ Anonymous authentication successful");
    } catch (authError) {
      console.log("❌ Anonymous auth failed, trying without auth:", authError.message);
    }

    console.log("📡 Fetching groups data...");
    const dbRef = ref(database);
    const snapshot = await get(child(dbRef, "groups"));

    if (snapshot.exists()) {
      const groups = snapshot.val();
      const groupEntries = Object.entries(groups);

      console.log(`\n🎯 FOUND ${groupEntries.length} GROUPS IN FIREBASE:`);
      console.log("=" .repeat(50));

      groupEntries.forEach(([id, group], index) => {
        console.log(`\n📍 Group ${index + 1}: ${id.substring(0, 8)}...`);
        console.log(`   Destination: ${group.destinationName || group.to || 'N/A'}`);
        console.log(`   Creator: ${group.creatorName || group.creatorId || 'N/A'}`);
        console.log(`   Status: ${group.status || 'N/A'}`);
        console.log(`   Members: ${group.memberCount || 0}/${group.maxMembers || 4}`);
        console.log(`   Coordinates: ${group.pickupLat || 'N/A'}, ${group.pickupLng || 'N/A'}`);

        const timestamp = group.timestamp || group.createdAt || 0;
        const age = timestamp ? Math.round((Date.now() - timestamp) / (1000 * 60)) : 'Unknown';
        console.log(`   Age: ${age} minutes ago`);
        console.log(`   Expired: ${age > 30 ? '❌ YES' : '✅ NO'}`);
      });

      // Analysis
      console.log("\n📊 ANALYSIS:");
      console.log("=" .repeat(30));

      const now = Date.now();
      const validGroups = groupEntries.filter(([_, group]) => {
        const timestamp = group.timestamp || group.createdAt || 0;
        return (now - timestamp) <= (30 * 60 * 1000); // Within 30 minutes
      });

      const groupsWithLocation = groupEntries.filter(([_, group]) =>
        group.pickupLat && group.pickupLng
      );

      console.log(`📈 Total groups: ${groupEntries.length}`);
      console.log(`⏰ Active (not expired): ${validGroups.length}`);
      console.log(`📍 With location data: ${groupsWithLocation.length}`);

      // Check default location usage
      const defaultLocationGroups = groupEntries.filter(([_, group]) =>
        group.pickupLat === 8.9806 && group.pickupLng === 38.7578
      );
      console.log(`🎯 Using default location (8.9806, 38.7578): ${defaultLocationGroups.length}`);

      // Recent groups (last 2 hours)
      const recentGroups = groupEntries.filter(([_, group]) => {
        const timestamp = group.timestamp || group.createdAt || 0;
        return (now - timestamp) <= (2 * 60 * 60 * 1000);
      });
      console.log(`🕐 Created in last 2 hours: ${recentGroups.length}`);

    } else {
      console.log("❌ No groups found in Firebase");
    }

  } catch (error) {
    console.error("💥 Error:", error.message);

    if (error.message.includes("Permission denied")) {
      console.log("\n🔒 PERMISSION ISSUE DETECTED:");
      console.log("- Firebase database rules require authentication");
      console.log("- Anonymous authentication might be disabled");
      console.log("- Try enabling anonymous auth in Firebase Console");
    }
  }
}

debugGroups();
