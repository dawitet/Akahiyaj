const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const database = admin.database();

// Clean up expired groups every 5 minutes
exports.cleanupExpiredGroups = functions.pubsub.schedule("every 5 minutes").onRun(async (_context) => {
  console.log("Starting cleanup of expired groups...");

  try {
    const groupsRef = database.ref("groups");
    const snapshot = await groupsRef.once("value");

    if (!snapshot.exists()) {
      console.log("No groups found to clean up");
      return null;
    }

    const groups = snapshot.val();
    const now = Date.now();
    const expiredGroupIds = [];

    // Find expired groups (older than 30 minutes)
    Object.keys(groups).forEach((groupId) => {
      const group = groups[groupId];
      const expiresAt = group.expiresAt || (group.timestamp + (30 * 60 * 1000)); // 30 minutes

      if (now > expiresAt) {
        expiredGroupIds.push(groupId);
        console.log(`Group ${groupId} expired at ${new Date(expiresAt)}`);
      }
    });

    // Remove expired groups
    if (expiredGroupIds.length > 0) {
      const updates = {};
      expiredGroupIds.forEach((groupId) => {
        updates[groupId] = null; // Delete the group
      });

      await groupsRef.update(updates);
      console.log(`Cleaned up ${expiredGroupIds.length} expired groups:`, expiredGroupIds);
    } else {
      console.log("No expired groups found");
    }

    return null;
  } catch (error) {
    console.error("Error cleaning up expired groups:", error);
    throw error;
  }
});

// HTTP endpoint for debug access with limited authentication
exports.debugGroupsAccess = functions.https.onRequest(async (req, res) => {
  // Enable CORS
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");

  if (req.method === "OPTIONS") {
    res.status(200).send();
    return;
  }

  // Simple debug authentication - only allow during development
  const debugKey = req.query.debug_key || req.headers["x-debug-key"];
  const expectedKey = "akahidegn_debug_2025"; // Change this regularly

  if (debugKey !== expectedKey) {
    res.status(401).json({
      error: "Unauthorized",
      message: "Invalid debug key"
    });
    return;
  }

  try {
    const action = req.query.action || "list";

    switch (action) {
      case "list":
        await handleListGroups(req, res);
        break;
      case "stats":
        await handleGroupStats(req, res);
        break;
      case "cleanup":
        await handleManualCleanup(req, res);
        break;
      default:
        res.status(400).json({ error: "Invalid action" });
    }
  } catch (error) {
    console.error("Debug endpoint error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

async function handleListGroups(_req, res) {
  const groupsRef = database.ref("groups");
  const snapshot = await groupsRef.once("value");

  if (!snapshot.exists()) {
    res.json({ groups: [], count: 0 });
    return;
  }

  const groups = snapshot.val();
  const now = Date.now();
  const groupList = [];

  Object.keys(groups).forEach((groupId) => {
    const group = groups[groupId];
    const expiresAt = group.expiresAt || (group.timestamp + (30 * 60 * 1000));
    const isExpired = now > expiresAt;

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

  res.json({
    groups: groupList,
    count: groupList.length,
    activeCount: groupList.filter((g) => !g.isExpired).length,
    expiredCount: groupList.filter((g) => g.isExpired).length
  });
}

async function handleGroupStats(_req, res) {
  const groupsRef = database.ref("groups");
  const snapshot = await groupsRef.once("value");

  if (!snapshot.exists()) {
    res.json({
      totalGroups: 0,
      activeGroups: 0,
      expiredGroups: 0,
      averageMemberCount: 0
    });
    return;
  }

  const groups = snapshot.val();
  const now = Date.now();
  let totalGroups = 0;
  let activeGroups = 0;
  let expiredGroups = 0;
  let totalMembers = 0;

  Object.keys(groups).forEach((groupId) => {
    const group = groups[groupId];
    const expiresAt = group.expiresAt || (group.timestamp + (30 * 60 * 1000));
    const isExpired = now > expiresAt;

    totalGroups++;
    totalMembers += group.memberCount || 0;

    if (isExpired) {
      expiredGroups++;
    } else {
      activeGroups++;
    }
  });

  res.json({
    totalGroups,
    activeGroups,
    expiredGroups,
    averageMemberCount: totalGroups > 0 ? (totalMembers / totalGroups).toFixed(2) : 0,
    lastUpdated: now
  });
}

async function handleManualCleanup(_req, res) {
  // Trigger manual cleanup
  const groupsRef = database.ref("groups");
  const snapshot = await groupsRef.once("value");

  if (!snapshot.exists()) {
    res.json({ message: "No groups to clean up", deletedCount: 0 });
    return;
  }

  const groups = snapshot.val();
  const now = Date.now();
  const expiredGroupIds = [];

  Object.keys(groups).forEach((groupId) => {
    const group = groups[groupId];
    const expiresAt = group.expiresAt || (group.timestamp + (30 * 60 * 1000));

    if (now > expiresAt) {
      expiredGroupIds.push(groupId);
    }
  });

  if (expiredGroupIds.length > 0) {
    const updates = {};
    expiredGroupIds.forEach((groupId) => {
      updates[groupId] = null;
    });

    await groupsRef.update(updates);
  }

  res.json({
    message: `Cleaned up ${expiredGroupIds.length} expired groups`,
    deletedCount: expiredGroupIds.length,
    deletedGroups: expiredGroupIds
  });
}
