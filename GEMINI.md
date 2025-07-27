# Akahidegn Android Application

## Overview
Akahidegn is a real-time, location-based carpooling/ride-sharing Android application. It allows users to create and join temporary groups with others traveling to the same destination. The app is primarily targeted at users in Ethiopia, specifically Addis Ababa, as indicated by the use of Amharic strings and default coordinates.

## Key Features
*   **Group Management:** Create and join temporary ride-sharing groups. Groups expire after 30 minutes to maintain active listings.
*   **Real-time Location:** Uses device GPS to display nearby groups (within a 500m radius) and updates user location in real-time.
*   **User Profiles:** Users have profiles with name, phone number, and avatar. This information is shared within groups for communication.
*   **In-App Communication:** Facilitates communication between group members by displaying contact information and enabling direct calls.
*   **Notifications:** Utilizes Firebase for real-time notifications for group events (member joins/leaves, group disbanded).
*   **Monetization:** Integrates Google AdMob for displaying ads (interstitial and rewarded) at key interaction points (e.g., before creating/joining a group).
*   **Technology Stack:** Built with Kotlin and Jetpack Compose for UI, Firebase (Authentication, Realtime Database) for backend services.

## Firebase Configuration
*   **Project ID:** `akahidegn-79376`
*   **Realtime Database URL:** `https://akahiyaj-79376-default-rtdb.europe-west1.firebasedatabase.app`
*   **`google-services.json`:** Located at `app/google-services.json`. **Confirmed to match Firebase Console settings.**
*   **Firebase CLI:** The Firebase CLI should be configured to use the `akahidegn-79376` project. If not, navigate to the project root (`/Users/dawitsahle/AndroidStudioProjects/Akahidegn`) and run `firebase use akahidegn-79376`. If `firebase init` is needed, ensure only "Realtime Database" is selected.
*   **Realtime Database Rules:** The rules are defined in `database.rules.json` at the project root.
    *   **Current State (Debugging):** Rules are temporarily set to allow authenticated users to read and write to the `groups` node (`.read": "auth != null", ".write": "auth != null"`) for debugging purposes.
    *   **Original Rules:** The original rules were complex and included extensive `.validate` rules, which were suspected of causing silent write failures.

## Known Issues & Debugging Notes

### Current Critical Issue: Firebase `PERMISSION_DENIED` for Installations Service
*   **Problem:** The app is consistently receiving `403 Forbidden: PERMISSION_DENIED` errors from the Firebase Installations Service (FIS). This prevents the app from properly authenticating with Firebase, leading to silent failures for Realtime Database write operations (data is not reaching the Firebase Console).
*   **Symptoms:**
    *   `E FirebaseMessaging: Failed to get FIS auth token`
    *   `E FirebaseMessaging: Firebase Installations Service is unavailable.`
    *   `W Firebase-Installations: Error when communicating with the Firebase Installations server API. HTTP response: [403 Forbidden: {"error": {"code": 403, "message": "The caller does not have permission", "status": "PERMISSION_DENIED"}}]`
    *   `W PersistentConnection: pc_0 - Firebase Database connection was forcefully killed by the server.`
*   **Troubleshooting Steps Taken:**
    *   Verified `google-services.json` contents against Firebase Console settings (all match).
    *   Updated Google Play Services on the emulator (now up to date and signed in).
    *   Enabled Firebase Realtime Database debug logging in `AkahidegnApplication.kt`.
    *   Hardcoded group creation location to Addis Ababa in `MainActivity.kt` (`9.005401, 38.763611`).
    *   Hardcoded user's current location to Addis Ababa in `MainActivity.kt` (`9.005401, 38.763611`) to bypass emulator GPS issues and ensure groups pass the 500m filter.
    *   Temporarily bypassed user registration dialog for debugging.
    *   Simplified Realtime Database rules to allow authenticated reads/writes for `groups` and `users`.
    *   Enabled open read/write rules for Cloud Firestore.
    *   Explicitly set Realtime Database URL in `FirebaseDatabase.getInstance()` calls in `AkahidegnApplication.kt` and `MainActivity.kt`.
    *   Cleared app data on the emulator.
*   **Current Hypothesis:** The `PERMISSION_DENIED` error is most likely due to **API Key restrictions** in the Google Cloud Project, where the API key is not explicitly allowed to access the Firebase Installations API or other necessary Firebase APIs.

### Other Issues:
*   **Registration Issue:** Initially, registration was failing. This was temporarily bypassed. The root cause was likely related to the Firebase `PERMISSION_DENIED` error, as user profile saving also involves Firebase.
*   **Group Visibility:** Groups were not appearing due to a combination of Firebase project mismatch, incorrect database rules, and emulator location filtering. These have been addressed by previous steps, but the core Firebase connectivity issue (FIS error) is still blocking full functionality.
*   **Deprecation Warnings:** `vibrate` deprecation warning in `NotificationManagerService.kt` (line 188). (Lower priority)

## Remaining Steps (High Priority)

1.  **Check API Key Restrictions in Google Cloud Console:**
    *   **Goal:** Identify and enable any missing API permissions for the project's API key.
    *   **Action:**
        1.  Go to Google Cloud Console: [https://console.cloud.google.com/](https://console.cloud.google.com/)
        2.  Select project: `akahidegn-79376`.
        3.  Navigate to **"APIs & Services" > "Credentials"**.
        4.  Find the API key matching `AIzaSyA-04oS-c5FmfZ-DekJqdBl_GTnW4b_yAo`.
        5.  Click on the API key name to edit its settings.
        6.  Under **"API restrictions"**, ensure the following APIs are explicitly listed and enabled:
            *   **Firebase Installations API**
            *   **Identity Toolkit API**
            *   **Firebase Realtime Database API**
        7.  Add any missing APIs and save changes.
    *   **Expected Outcome:** The `PERMISSION_DENIED` error for FIS should disappear from logs, and Firebase Realtime Database writes should succeed.

2.  **Verify Data Upload (App to Firebase) after API Key Fix:**
    *   **Goal:** Confirm that group creation attempts are now successfully reaching the Firebase Realtime Database.
    *   **Action:** After fixing API key restrictions, create a group in the emulator and then check the Firebase Console directly at `https://console.firebase.google.com/project/akahidegn-79376/database/akahidegn-79376-default-rtdb/data/~2F`.
    *   **Expected Outcome:** New group entries appear in the Firebase Realtime Database.

3.  **Verify Data Retrieval and Display:**
    *   **Goal:** Confirm groups are fetched and displayed correctly in the app.
    *   **Action:** If groups appear in the Firebase Console, check the app's UI. Monitor `adb logcat` for `MainViewModel` and `GROUP_FILTER` logs to ensure groups are being processed and not filtered out.

## CLI Usage Notes
*   **Use Google Cloud CLI and Firebase CLI:** When debugging Firebase issues, use the `gcloud` and `firebase` command-line tools to inspect project configurations, API key restrictions, and database rules.
*   **Avoid Long-Running Tasks:** Do not run commands that monitor or ping services for extended periods, as they can block further interaction.

## Troubleshooting Guide for Future Interactions

### Getting Unstuck During `adb shell ping` or Network Tests
If `adb shell ping` or other network tests get stuck or fail without clear errors, it indicates a deeper network connectivity issue on the emulator or host machine.

*   **Check Emulator Network Settings:**
    *   Ensure the emulator's Wi-Fi is enabled (if applicable).
    *   Check proxy settings if you are behind a corporate proxy.
*   **Check Host Machine Firewall/Antivirus:**
    *   Temporarily disable your computer's firewall or antivirus to see if it's blocking the emulator's network traffic.
*   **Try a Different Emulator/Device:**
    *   If possible, try running the app on a different emulator (e.g., a different API level or device type) or a physical Android device to rule out emulator-specific network issues.
*   **Restart ADB Server:**
    *   Sometimes, restarting the ADB server can resolve connectivity glitches:
        ```bash
        adb kill-server
        adb start-server
        ```
*   **Reboot Emulator:**
    *   A full reboot of the emulator can often clear transient network problems.
*   **Check Host Machine Internet Connection:**
    *   Ensure your development machine has a stable internet connection.

### General Debugging Strategy
*   **Start with Logs:** Always check `adb logcat` first. Use specific `grep` filters to narrow down relevant messages.
*   **Isolate the Problem:** Break down the issue into smaller, testable components (e.g., Firebase write vs. Firebase read vs. UI display).
*   **Simplify Temporarily:** For complex issues, temporarily simplify parts of the code (like security rules or hardcoding values) to isolate the problem area. **Remember to revert these changes later.**
*   **Verify External Services:** For cloud services like Firebase, always verify the state directly in the console (e.g., database content, API enablement, billing status).
*   **Communicate Clearly:** Provide detailed observations and the exact output of commands.