const admin = require('firebase-admin');
const fetch = require('node-fetch');

// Initialize Firebase Admin SDK with service account
// For debug purposes only - use environment variables in production
const serviceAccount = {
  "type": "service_account",
  "project_id": "akahiyaj-79376",
  "private_key_id": process.env.FIREBASE_PRIVATE_KEY_ID,
  "private_key": process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
  "client_email": process.env.FIREBASE_CLIENT_EMAIL,
  "client_id": process.env.FIREBASE_CLIENT_ID,
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": process.env.FIREBASE_CLIENT_CERT_URL
};

// Fallback to debug authentication if service account not available
const useDebugAuth = !serviceAccount.private_key;

if (useDebugAuth) {
  console.log('🔧 Using debug authentication mode');
} else {
  console.log('🔐 Using service account authentication');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://akahiyaj-79376-default-rtdb.europe-west1.firebasedatabase.app"
  });
}

const DEBUG_KEY = 'akahidegn_debug_2025';
const CLOUD_FUNCTION_URL = 'https://your-region-your-project.cloudfunctions.net/debugGroupsAccess';

async function checkGroupsWithDebugAuth() {
  console.log('📡 Checking groups via debug endpoint...');

  try {
    const response = await fetch(`${CLOUD_FUNCTION_URL}?action=list&debug_key=${DEBUG_KEY}`);

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const data = await response.json();

    console.log('📊 Groups Status:');
    console.log(`  Total Groups: ${data.count}`);
    console.log(`  Active Groups: ${data.activeCount}`);
    console.log(`  Expired Groups: ${data.expiredCount}`);

    if (data.groups.length > 0) {
      console.log('\n📋 Group Details:');
      data.groups.forEach((group, index) => {
        const status = group.isExpired ? '❌ EXPIRED' : '✅ ACTIVE';
        const distance = group.location.lat && group.location.lng ?
          `(${group.location.lat.toFixed(4)}, ${group.location.lng.toFixed(4)})` :
          '(No location)';

        console.log(`  ${index + 1}. ${status} ${group.destinationName || 'Unnamed'}`);
        console.log(`     Members: ${group.memberCount} | ${distance}`);
        console.log(`     Created: ${new Date(group.createdAt).toLocaleString()}`);
        console.log(`     Expires: ${new Date(group.expiresAt).toLocaleString()}`);
        console.log('');
      });
    }

    return data;
  } catch (error) {
    console.error('❌ Debug endpoint failed:', error.message);
    throw error;
  }
}

async function checkGroupsWithServiceAccount() {
  console.log('📡 Checking groups via service account...');

  try {
    const database = admin.database();
    const groupsRef = database.ref('groups');
    const snapshot = await groupsRef.once('value');

    if (!snapshot.exists()) {
      console.log('📭 No groups found in database');
      return { groups: [], count: 0, activeCount: 0, expiredCount: 0 };
    }

    const groups = snapshot.val();
    const now = Date.now();
    const groupList = [];
    let activeCount = 0;
    let expiredCount = 0;

    Object.keys(groups).forEach(groupId => {
      const group = groups[groupId];
      const expiresAt = group.expiresAt || (group.timestamp + (30 * 60 * 1000));
      const isExpired = now > expiresAt;

      if (isExpired) {
        expiredCount++;
      } else {
        activeCount++;
      }

      groupList.push({
        id: groupId,
        destinationName: group.destinationName,
        memberCount: group.memberCount || 0,
        createdAt: group.timestamp,
        expiresAt: expiresAt,
        isExpired: isExpired,
        location: {
          lat: group.pickupLat,
          lng: group.pickupLng
        }
      });
    });

    console.log('📊 Groups Status:');
    console.log(`  Total Groups: ${groupList.length}`);
    console.log(`  Active Groups: ${activeCount}`);
    console.log(`  Expired Groups: ${expiredCount}`);

    if (groupList.length > 0) {
      console.log('\n📋 Group Details:');
      groupList.forEach((group, index) => {
        const status = group.isExpired ? '❌ EXPIRED' : '✅ ACTIVE';
        const distance = group.location.lat && group.location.lng ?
          `(${group.location.lat.toFixed(4)}, ${group.location.lng.toFixed(4)})` :
          '(No location)';

        console.log(`  ${index + 1}. ${status} ${group.destinationName || 'Unnamed'}`);
        console.log(`     Members: ${group.memberCount} | ${distance}`);
        console.log(`     Created: ${new Date(group.createdAt).toLocaleString()}`);
        console.log(`     Expires: ${new Date(group.expiresAt).toLocaleString()}`);
        console.log('');
      });
    }

    return { groups: groupList, count: groupList.length, activeCount, expiredCount };
  } catch (error) {
    console.error('❌ Service account access failed:', error.message);
    throw error;
  }
}

async function cleanupExpiredGroups() {
  console.log('🧹 Cleaning up expired groups...');

  if (useDebugAuth) {
    try {
      const response = await fetch(`${CLOUD_FUNCTION_URL}?action=cleanup&debug_key=${DEBUG_KEY}`, {
        method: 'POST'
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      console.log(`✅ ${data.message}`);

      if (data.deletedGroups.length > 0) {
        console.log('🗑️ Deleted groups:');
        data.deletedGroups.forEach(groupId => {
          console.log(`  - ${groupId}`);
        });
      }

      return data;
    } catch (error) {
      console.error('❌ Cleanup via debug endpoint failed:', error.message);
      throw error;
    }
  } else {
    try {
      const database = admin.database();
      const groupsRef = database.ref('groups');
      const snapshot = await groupsRef.once('value');

      if (!snapshot.exists()) {
        console.log('📭 No groups to clean up');
        return { deletedCount: 0, deletedGroups: [] };
      }

      const groups = snapshot.val();
      const now = Date.now();
      const expiredGroupIds = [];

      Object.keys(groups).forEach(groupId => {
        const group = groups[groupId];
        const expiresAt = group.expiresAt || (group.timestamp + (30 * 60 * 1000));

        if (now > expiresAt) {
          expiredGroupIds.push(groupId);
        }
      });

      if (expiredGroupIds.length > 0) {
        const updates = {};
        expiredGroupIds.forEach(groupId => {
          updates[groupId] = null;
        });

        await groupsRef.update(updates);
        console.log(`✅ Cleaned up ${expiredGroupIds.length} expired groups`);

        if (expiredGroupIds.length > 0) {
          console.log('🗑️ Deleted groups:');
          expiredGroupIds.forEach(groupId => {
            console.log(`  - ${groupId}`);
          });
        }
      } else {
        console.log('✨ No expired groups found');
      }

      return { deletedCount: expiredGroupIds.length, deletedGroups: expiredGroupIds };
    } catch (error) {
      console.error('❌ Cleanup via service account failed:', error.message);
      throw error;
    }
  }
}

async function testLocationFiltering() {
  console.log('📍 Testing location-based filtering...');

  // Test coordinates (Addis Ababa area)
  const testLocations = [
    { name: 'Bole', lat: 8.9806, lng: 38.7578 },
    { name: 'Merkato', lat: 9.0054, lng: 38.7469 },
    { name: 'Piazza', lat: 9.0238, lng: 38.7469 }
  ];

  const userLocation = { lat: 8.9806, lng: 38.7578 }; // User at Bole

  console.log(`👤 User location: ${userLocation.lat}, ${userLocation.lng}`);
  console.log('🎯 Testing 500m proximity rule...\n');

  testLocations.forEach(location => {
    const distance = calculateDistance(
      userLocation.lat, userLocation.lng,
      location.lat, location.lng
    );

    const isWithin500m = distance <= 500;
    const status = isWithin500m ? '✅ VISIBLE' : '❌ FILTERED OUT';

    console.log(`📍 ${location.name}: ${distance.toFixed(0)}m away - ${status}`);
  });
}

function calculateDistance(lat1, lng1, lat2, lng2) {
  const earthRadius = 6371000; // Earth radius in meters
  const dLat = toRadians(lat2 - lat1);
  const dLng = toRadians(lng2 - lng1);
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadius * c;
}

function toRadians(degrees) {
  return degrees * (Math.PI / 180);
}

async function main() {
  console.log('🚀 Akahidegn Debug Tool v2.0');
  console.log('================================\n');

  const action = process.argv[2] || 'check';

  try {
    switch (action) {
      case 'check':
        if (useDebugAuth) {
          await checkGroupsWithDebugAuth();
        } else {
          await checkGroupsWithServiceAccount();
        }
        break;

      case 'cleanup':
        await cleanupExpiredGroups();
        break;

      case 'test-location':
        await testLocationFiltering();
        break;

      case 'stats':
        if (useDebugAuth) {
          const response = await fetch(`${CLOUD_FUNCTION_URL}?action=stats&debug_key=${DEBUG_KEY}`);
          const data = await response.json();
          console.log('📊 Statistics:', data);
        } else {
          await checkGroupsWithServiceAccount();
        }
        break;

      default:
        console.log('Usage:');
        console.log('  node check_groups_enhanced.js check       - Check all groups');
        console.log('  node check_groups_enhanced.js cleanup     - Clean up expired groups');
        console.log('  node check_groups_enhanced.js test-location - Test location filtering');
        console.log('  node check_groups_enhanced.js stats       - Show statistics');
    }
  } catch (error) {
    console.error('💥 Error:', error.message);
    process.exit(1);
  }

  console.log('\n✨ Debug session complete');
}

// Run the script
if (require.main === module) {
  main();
}

module.exports = {
  checkGroupsWithDebugAuth,
  checkGroupsWithServiceAccount,
  cleanupExpiredGroups,
  testLocationFiltering
};
