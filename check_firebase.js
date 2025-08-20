// This script checks Firebase Realtime Database directly

const { initializeApp } = require('firebase/app');
const { getDatabase, ref, get, child } = require('firebase/database');

// Firebase config from your project
const firebaseConfig = {
  apiKey: "AIzaSyA-04oS-c5FmfZ-DekJqdBl_GTnW4b_yAo",
  authDomain: "akahiyaj-79376.firebaseapp.com",
  databaseURL: "https://akahiyaj-79376-default-rtdb.europe-west1.firebasedatabase.app",
  projectId: "akahiyaj-79376",
  storageBucket: "akahiyaj-79376.firebasestorage.app",
  messagingSenderId: "81721365267",
  appId: "1:81721365267:android:c9d4be50e95b8d8c0d0c77"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getDatabase(app);
const dbRef = ref(db);

console.log("Checking Firebase database...");

// Check for groups
get(child(dbRef, 'groups')).then((snapshot) => {
  if (snapshot.exists()) {
    console.log("GROUPS FOUND! Count:", Object.keys(snapshot.val()).length);
    
    // Print first group details
    const groups = snapshot.val();
    const firstGroupKey = Object.keys(groups)[0];
    console.log("\nSample group key:", firstGroupKey);
    console.log("Sample group data:", JSON.stringify(groups[firstGroupKey], null, 2));
    
    // Check which fields exist in each group
    console.log("\nChecking fields across all groups:");
    let fieldCounts = {};
    Object.values(groups).forEach(group => {
      Object.keys(group).forEach(field => {
        fieldCounts[field] = (fieldCounts[field] || 0) + 1;
      });
    });
    
    console.log("Field presence across groups:");
    Object.entries(fieldCounts).sort((a,b) => b[1]-a[1]).forEach(([field, count]) => {
      console.log(`- ${field}: ${count}/${Object.keys(groups).length} groups`);
    });
  } else {
    console.log("No groups found in the database!");
  }
}).catch((error) => {
  console.error("Error reading database:", error);
});
