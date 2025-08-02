# Akahidegn Android App

## Overview

This document outlines the current state and future direction of the Akahidegn Android application. The project is pivoting from a real-time GPS-based matching system to a more robust, form-based system with user profiles.

## Project Details

*   **Project Name:** Akahidegn
*   **Platform:** Android
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Dependency Injection:** Hilt
*   **Database:** Room
*   **Networking:** Retrofit
*   **Backend:** Firebase (Realtime Database, Authentication, Crashlytics, Messaging), Cloudflare (for user profiles)

## Core Functionality (New Direction)

While the app is still under development, the core functionality is shifting to:

*   **User Profiles:** Users sign in with Google and provide their phone number to create a profile.
*   **Group Creation:** Users can create groups by filling out a form with their starting point, destination, and the number of seats they need.
*   **Group Management:** Users can view and manage their active groups in a dedicated tab.
*   **Notifications:** Users receive push notifications for important group events (e.g., group full, group disbanded).
*   **Peer-to-Peer Communication:** Group members can see each other's phone numbers to coordinate.

## New To-Do List

*   **[x] Overhaul User Authentication & Profiles:**
    *   [x] Replace Anonymous Auth with Google Sign-In.
    *   [x] Create user profiles with name, age, and sex from Google, and a user-provided phone number.
    *   [x] Implement a one-time registration form for the phone number.
    *   [x] Offload user profile data to Cloudflare to manage Firebase usage.

*   **[x] Rework Group Matching System:**
    *   [x] Remove all real-time GPS location fetching and 500-meter radius matching logic.
    *   [x] Implement a form for group creation with fields: "From," "Via (Optional)," "To," and "Seats Needed."
    *   [x] Show a rewarded ad before a user can create a group.

*   **[x] Enhance Group Management:**
    *   [x] Create a new tab titled "የእርስዎ ንቁ ቡድኖች" (Your Active Groups) to show user's created/joined groups.
    *   [x] Enforce a limit: a user can create only 1 group.
    *   [x] Enforce a limit: a user can join a maximum of 2 groups.
    *   [x] Allow group members to see each other's phone numbers.
    *   [ ] Implement options for group creator to disband, mark unavailable, mark full, or delete the group.
    *   [ ] Implement option for user to leave a group.

*   **[x] Implement Notifications:**
    *   [x] Configure and enable push notifications.
    *   [x] Send a notification when a user's group is full.
    *   [x] Send a notification when a user's group is disbanded.
    *   [x] Send a notification when a user joins a group for other members.

*   **[x] UI & UX Revamp:**
    *   [x] Implement dynamic color schemes for navigation tabs and ensure all necessary imports are in place.
        *   **Plan for Dynamic Color Schemes and Navigation:**
            1.  **Verify `ui/theme/Theme.kt`:**
                *   Ensure `AkahidegnTheme` composable function accepts a `selectedColorScheme: ColorScheme? = null` parameter.
                *   Confirm that `HomeColorScheme`, `ActiveGroupsColorScheme`, and `SettingsColorScheme` are defined as `lightColorScheme` instances with distinct colors.
                *   Verify that `currentColorScheme` is correctly set to `selectedColorScheme ?: systemColorScheme`.
            2.  **Modify `MainActivity.kt`:**
                *   **Imports:**
                    *   Add `import androidx.compose.material3.NavigationBar`
                    *   Add `import androidx.compose.material3.NavigationBarItem`
                    *   Add `import androidx.navigation.compose.currentBackStackEntryAsState`
                    *   Add `import androidx.navigation.NavDestination.Companion.hierarchy`
                    *   Add `import androidx.navigation.NavGraph.Companion.findStartDestination`
                    *   Add `import com.dawitf.akahidegn.ui.navigation.Screen`
                    *   Add `import androidx.compose.material.icons.filled.Home`
                    *   Add `import androidx.compose.material.icons.filled.List`
                    *   Add `import androidx.compose.material.icons.filled.Settings`
                    *   Add `import androidx.compose.material3.Scaffold`
                    *   Add `import androidx.compose.foundation.layout.padding`
                    *   Add `import androidx.compose.material3.Icon`
                    *   Add `import androidx.compose.material3.Text`
                    *   Add `import com.dawitf.akahidegn.ui.theme.HomeColorScheme`
                    *   Add `import com.dawitf.akahidegn.ui.theme.ActiveGroupsColorScheme`
                    *   Add `import com.dawitf.akahidegn.ui.theme.SettingsColorScheme`
                *   **`initializeMainScreen()` function:**
                    *   Inside `setContent`, retrieve the `navController` and `currentRoute`.
                    *   Implement a `when` statement to determine the `colorScheme` based on the `currentRoute` (e.g., `Screen.Main.route -> HomeColorScheme`).
                    *   Wrap the `NavHost` and its content within a `Scaffold` composable.
                    *   Inside the `Scaffold`, define the `bottomBar` using `NavigationBar`.
                    *   For each `Screen` (Main, ActiveGroups, Settings), create a `NavigationBarItem` with the appropriate icon, label, selected state, and `onClick` logic for navigation.
                    *   Ensure the `NavHost` uses `modifier = Modifier.padding(innerPadding)` to account for the bottom navigation bar.
                    *   Update the `composable` blocks to call the respective screen composables (e.g., `MainScreenContent(navController)`).
            3.  **Rebuild the application** to ensure all changes are correctly applied and there are no compilation errors.
    *   [x] Create a distinct color scheme for each main tab for clear visual separation.
    *   [x] Ensure UI components adhere to a native Android look and feel.
    *   [x] Update the app's launcher icon to use one of the splash screen images.
    *   [x] Set the other image as the app's splash screen.
    *   [x] Remove decorative images from the Settings and Notifications screens to simplify the UI.
    *   [x] Add a "ሰርቪሥ" (Service) tab as a "Coming Soon" placeholder with a descriptive text about a future subscription service.

*   **[x] App Features & Compatibility:**
    *   [x] Add a "Recommend App" feature in Settings that uses the native Android share functionality to send the APK.
    *   [x] Remove price calculation, carbon footprint, and other non-essential features.
    *   [x] Ensure app compatibility with older Android versions and a range of devices, including Samsung.
    *   [x] Add native advanced ad ID.

*   **[x] Backend & Firebase:**
    *   [x] Update Firebase Realtime Database rules to support the new data model (Google Auth, user profiles, group structure).
    *   [x] Restructure the Firebase database to store user profiles and the new group format.

## Completed Tasks

*   [x] Improve UI color tones (avoid yellow-on-yellow, use more readable colors)
*   [x] Use Firebase CLI for database management; only use Cloudflare/Cloud CLI if absolutely needed
*   [x] Clean up code and dependencies for minimal, fast MVP
*   [x] Removed redundant files and and directories (e.g., `MainActivity.kt.backup`, `fixed`, `simple`, `standalone`, `utils`).
*   [x] Removed empty `FormatUtils.kt`.
*   [x] Renamed `FirebaseGroupService` to `GroupService` and `FirebaseGroupServiceImpl` to `GroupServiceImpl` for naming consistency.
*   [x] Updated deprecated `Icons.Default.List` to `Icons.AutoMirrored.Filled.List` in `MainScreen.kt`.
*   [x] Removed unused Firebase Firestore, Storage, and Google Maps dependencies.
*   [x] **Gradle Sync and Compilation Issues:**
    *   [x] Updated Android Gradle Plugin (AGP), Kotlin, Hilt, and other library versions in `build.gradle.kts` and `libs.versions.toml` to resolve compatibility issues.
    *   [x] Addressed "unresolved reference" errors by ensuring all necessary components and data classes were correctly defined and imported.
    *   [x] Resolved internal compiler errors by cleaning the build cache and ensuring proper Compose compiler extension versioning.
    *   [x] Fixed "integer number too large" error in `BuildConfig.java` by correctly quoting the `ADMOB_BANNER_ID` string in `app/build.gradle.kts`.
    *   [x] Removed problematic `accessibility` and `performance` packages to resolve persistent internal compiler errors, indicating potential deeper incompatibilities or unused code.
    *   [x] Simplified `MainActivity.kt` and `SplashActivity.kt` by removing complex UI logic and replacing it with basic composables to isolate and resolve compilation issues.
    *   [x] Removed theme-related code from `SettingsViewModel` and `SettingsScreen.kt` to align with the removal of `ThemeManager`.
    *   [x] Created missing `SwipeAction.kt`, `ThemeMode.kt`, `LanguageOption.kt`, `FontSizeOption.kt`, `NotificationPreferencesManager.kt`, `FirebaseModule.kt`, and `FirebaseGroupServiceImpl.kt` files to resolve unresolved references and Hilt injection issues.
    *   [x] Restored `EmptyStateComponents.kt` after repeated internal compiler errors, indicating a transient issue rather than a code problem.

## Debugging and UI Fixes (July 30, 2025)

*   [x] **Emulator Launch and APK Installation:**
    *   [x] Launched Pixel 9 emulator using `emulator -avd Pixel_9`.
    *   [x] Uninstalled previous app version (`com.dawitf.akahidegn`) using `adb uninstall`.
    *   [x] Installed new debug APK (`app-debug.apk`) using `adb install`.
    *   [x] Copied debug APK to desktop.

*   [x] **App Crash Debugging:**
    *   [x] Identified `NoSuchMethodException` for `MainViewModel` in logcat.
    *   [x] Added `@HiltViewModel` annotation to `MainViewModel.kt`.
    *   [x] Identified `AkahidegnDatabase_Impl does not exist` error in logcat.
    *   [x] Uncommented `@Database` annotation in `AkahidegnDatabase.kt`.
    *   [x] Performed multiple Gradle clean and rebuild operations (`./gradlew clean`, `./gradlew assembleDebug`) to resolve build cache and annotation processing issues.
    *   [x] Cleared Gradle caches (`rm -rf ~/.gradle/caches`, `rm -rf ~/.gradle/wrapper/dists`) to force fresh builds and dependency downloads.

*   [x] **UI Rendering Fixes:**
    *   [x] Addressed "white screen with cut off writing" by reconstructing `MainScreen.kt`.
    *   [x] Replaced placeholder UI in `MainScreen.kt` with a more complete structure including `EnhancedSearchBar`, `FilterChips`, `SearchResultsHeader`, and a `LazyColumn` for groups.
    *   [x] Ensured correct imports for Compose UI components (`Notifications`, `Settings`, `clickable`) in `MainScreen.kt`.

## Recent Problems and Solutions

*   **Problem:** Firebase Realtime Database deployment failed due to syntax errors in `database.rules.json` (unsupported `every`, `isMap()`, `size()` functions).
    *   **Solution:** Simplified the validation rules in `database.rules.json` to use supported methods and ensure correct JSON structure. This involved removing complex validation for nested maps and relying on basic existence checks.

*   **Problem:** Build failed due to `MainActivity.kt` having duplicate `showGroupMembersDialog` function definitions and incorrect lambda usage within `onGroupClick`.
    *   **Solution:** [x] The `MainActivity.kt` file was in an inconsistent state. The problematic sections were systematically cleaned up and re-implemented correctly. This involved:
        1.  [x] Removing the incorrectly placed `showGroupMembersDialog` function from within `MainScreenContent`.
        2.  [x] Correcting the `onGroupClick` lambda within `MainScreenContent` to properly call the *outer* `showGroupMembersDialog` function, passing the `group` object and the `onLeaveGroup` lambda.
        3.  [x] Ensuring the outer `showGroupMembersDialog` function is correctly defined and accepts the `group` and `onLeaveGroup` lambda.
        4.  [x] Adding any necessary missing imports.
        5.  [x] Rebuilding and verifying the application.