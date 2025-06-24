// Quick script to check if groups exist in Firebase and what their structure is
// Run with: node check_groups.js

const { initializeApp } = require('firebase/app');
const { getDatabase, ref, get, child } = require('firebase/database');

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyAmqtN3ceDYpkR8o5bCdO1G7Wd0x3vYHj8",
  authDomain: "akahidegn-79376.firebaseapp.com",
  databaseURL: "https://akahidegn-79376-default-rtdb.europe-west1.firebasedatabase.app",
  projectId: "akahidegn-79376",
  storageBucket: "akahidegn-79376.firebasestorage.app",
  messagingSenderId: "81721365267",
  appId: "1:81721365267:android:c9d4be50e95b8d8c0d0c77"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const database = getDatabase(app);
const dbRef = ref(database);

// Get the data at /groups
get(child(dbRef, "groups")).then((snapshot) => {
  if (snapshot.exists()) {
    console.log("GROUPS FOUND:");
    const groups = snapshot.val();
    console.log(`Total groups: ${Object.keys(groups).length}`);
    
    // Print the first group as an example
    const firstGroupId = Object.keys(groups)[0];
    const firstGroup = groups[firstGroupId];
    console.log("\nExample group structure:", firstGroupId);
    console.log(JSON.stringify(firstGroup, null, 2));
    
    // Check if the expected fields are present
    const keys = new Set();
    Object.values(groups).forEach(group => {
      if (group) {
        Object.keys(group).forEach(key => keys.add(key));
      }
    });
    
    console.log("\nAll possible keys across groups:");
    console.log(Array.from(keys).join(', '));
    
    // Check if there are any groups with timestamp vs createdAt
    const timestampGroups = Object.entries(groups).filter(([_, group]) => group.timestamp !== undefined);
    const createdAtGroups = Object.entries(groups).filter(([_, group]) => group.createdAt !== undefined);
    
    console.log(`\nGroups with 'timestamp': ${timestampGroups.length}`);
    console.log(`Groups with 'createdAt': ${createdAtGroups.length}`);
    
    // Check for a specific recently created group
    const recentGroups = Object.entries(groups)
      .filter(([_, group]) => {
        const time = group.createdAt || group.timestamp || 0;
        // Check for groups in last 2 hours
        return (Date.now() - time) < 2 * 60 * 60 * 1000;
      })
      .map(([id, group]) => {
        return {
          id,
          time: new Date(group.createdAt || group.timestamp || 0).toLocaleTimeString(),
          destination: group.to || group.destinationName || "Unknown"
        };
      });
    
    console.log("\nRecent groups (last 2 hours):");
    console.log(recentGroups);
    
  } else {
    console.log("No data available at /groups");
  }
}).catch((error) => {
  console.error("Error fetching data:", error);
});
