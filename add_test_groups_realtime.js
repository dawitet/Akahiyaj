// Direct test data addition to Firebase Realtime Database
// This will add test groups near Addis Ababa for testing
// Run with: node add_test_groups_realtime.js

const { initializeApp } = require('firebase/app');
const { getDatabase, ref, push, set } = require('firebase/database');

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

// Test groups around Addis Ababa (Bole area)
const testGroups = [
  {
    groupId: 'test_group_1_' + Date.now(),
    destinationName: 'Bole International Airport',
    originalDestination: 'Bole International Airport',
    pickupLat: 9.005401,
    pickupLng: 38.763611,
    from: 'Bole',
    to: 'Bole International Airport',
    timestamp: Date.now(),
    createdAt: Date.now(),
    maxMembers: 4,
    memberCount: 1,
    availableSeats: 3,
    pricePerPerson: 50,
    creatorId: 'test_user_1',
    creatorName: 'Dawit Test',
    members: {
      'test_user_1': true
    },
    memberDetails: {
      'test_user_1': {
        name: 'Dawit Test',
        phone: '0911000927',
        avatar: 'avatar_1',
        joinedAt: Date.now()
      }
    }
  },
  {
    groupId: 'test_group_2_' + Date.now(),
    destinationName: 'Edna Mall',
    originalDestination: 'Edna Mall',
    pickupLat: 9.0065,
    pickupLng: 38.7619,
    from: 'Bole',
    to: 'Edna Mall',
    timestamp: Date.now(),
    createdAt: Date.now(),
    maxMembers: 4,
    memberCount: 2,
    availableSeats: 2,
    pricePerPerson: 30,
    creatorId: 'test_user_2',
    creatorName: 'Test User 2',
    members: {
      'test_user_2': true,
      'test_user_3': true
    },
    memberDetails: {
      'test_user_2': {
        name: 'Test User 2',
        phone: '0912000000',
        avatar: 'avatar_2',
        joinedAt: Date.now()
      },
      'test_user_3': {
        name: 'Test User 3',
        phone: '0913000000',
        avatar: 'avatar_3',
        joinedAt: Date.now() - 300000
      }
    }
  },
  {
    groupId: 'test_group_3_' + Date.now(),
    destinationName: 'Bole Medhanialem Church',
    originalDestination: 'Bole Medhanialem Church',
    pickupLat: 9.0075,
    pickupLng: 38.7625,
    from: 'Bole',
    to: 'Bole Medhanialem Church',
    timestamp: Date.now(),
    createdAt: Date.now(),
    maxMembers: 4,
    memberCount: 1,
    availableSeats: 3,
    pricePerPerson: 25,
    creatorId: 'test_user_4',
    creatorName: 'Test User 4',
    members: {
      'test_user_4': true
    },
    memberDetails: {
      'test_user_4': {
        name: 'Test User 4',
        phone: '0914000000',
        avatar: 'avatar_4',
        joinedAt: Date.now()
      }
    }
  }
];

async function addTestGroups() {
  console.log('🔄 Adding test groups to Firebase Realtime Database...');
  
  try {
    for (const group of testGroups) {
      // Add to realtime database using push to generate unique key
      const groupsRef = ref(database, 'groups');
      const newGroupRef = push(groupsRef);
      
      // Update the group ID to match the generated key
      group.groupId = newGroupRef.key;
      
      await set(newGroupRef, group);
      console.log(`✅ Added group: ${group.destinationName} with ID: ${group.groupId}`);
    }
    
    console.log('🎉 All test groups added successfully to Realtime Database!');
    console.log(`📍 Added ${testGroups.length} groups near Bole, Addis Ababa`);
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Error adding test groups:', error);
    process.exit(1);
  }
}

addTestGroups();
