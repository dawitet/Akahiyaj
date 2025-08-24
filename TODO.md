# Todo: Hybrid Map & List Group Discovery Implementation

## Phase 0: Project Setup & Cleanup

*   [X] **Project Dependencies:**
    *   [X] Add OSMdroid dependency (`org.osmdroid:osmdroid-android:6.1.18` or latest stable).
        *   *File to modify:* `app/build.gradle.kts`
*   [X] **OSMdroid Configuration:**
    *   [X] Initialize OSMdroid configuration (User Agent) in `Application.onCreate()`.
        *   *File to modify:* `app/src/main/java/com/dawitf/akahidegn/AkahidegnApplication.kt`
*   [X] **Permissions & Feature Declaration:**
    *   [X] Verify `INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_NETWORK_STATE` permissions are present.
        *   *File to modify:* `app/src/main/AndroidManifest.xml` (No changes were needed)
*   [X] **Remove Chat Functionality:**
    *   [X] Identified and removed chat-related string resources from `values/strings.xml` and `values-am/strings.xml`.
    *   [X] Updated `PerformanceValidationTest.kt` to remove references to `ChatRepository`.
    *   [X] Removed chat notification settings from `ThemeManager.kt`.
    *   [X] Confirmed no `ChatRepository` interface/implementation or dedicated Chat UI files exist in the main source.
        *   *Files modified:* `app/src/main/res/values/strings.xml`, `app/src/main/res/values-am/strings.xml`, `app/src/test/java/com/dawitf/akahidegn/PerformanceValidationTest.kt`, `app/src/main/java/com/dawitf/akahidegn/ui/theme/ThemeManager.kt`.
*   [X] **Remove App-Level Offline Functionality (Data Sync):**
    *   [X] Explicitly disabled Firebase Realtime Database persistence in `AkahidegnApplication.kt`.
    *   [X] Reviewed `OptimisticOperationsManager.kt`; it manages in-memory optimistic state, not persistent offline caching.
    *   [X] Reviewed `JoinGroupWorker` and its usage; it doesn't seem to implement aggressive offline queueing (fails on error).
    *   [X] No other app-level data offline caching mechanisms (e.g., Room for group data) were identified.
        *   *Files modified:* `app/src/main/java/com/dawitf/akahidegn/AkahidegnApplication.kt`.

## Phase 1: Mandatory Location & Core UI Structure

*   [X] **Mandatory Location Permission Handling:**
    *   [X] Implement a blocking UI/dialog if location permission is denied, explaining its necessity.
    *   [X] Prevent app usage until permission is granted.
    *   [X] Update `MainActivity` or the entry point Composable to handle this logic.
        *   *Files to modify:* `MainActivity.kt` (or main Composable hosting screen)
*   [X] **Main UI Structure (Bottom Navigation or Similar):**
    *   [X] Define the main navigation structure (e.g., Bottom Navigation Bar with "Map", "Groups", "Settings" tabs).
        *   *Reference:* `jetpack_compose_guidelines_for_ai_agent.md` (Advanced Scaffold Techniques, Navigation)
    *   [X] Create placeholder Composables for each tab/screen.
        *   *Files to create/modify:* `MainScreen.kt` (or similar top-level UI host), new Composables for each tab.
*   [X] **ViewModel Adjustments (Initial):**
    *   [X] In `MainViewModel`, adapt existing group fetching logic. Remove explicit distance sorting if it's currently done before map display. Groups will be filtered primarily by expiry.
    *   *File to modify:* `app/src/main/java/com/dawitf/akahidegn/viewmodel/MainViewModel.kt`

## Phase 2: Map Screen Implementation (Main Page)

*   [X] **OSMdroid MapView Integration:**
    *   [X] Create a Composable (`MapScreen.kt` or within the main tab Composable) using `AndroidView` to host the OSMdroid `MapView`.
        *   *Reference:* `jetpack_compose_guidelines_for_ai_agent.md` (Using AndroidView with Compose)
    *   [X] Set default map tile source (e.g., `TileSourceFactory.MAPNIK`), multi-touch controls, and initial zoom/center.
*   [X] **User Location on Map:**
    *   User's current location is displayed on the map using OSMdroid's MyLocationNewOverlay with GpsMyLocationProvider. The map centers on the user's location when first available, and follows the user. Implementation verified in `MapScreen.kt`.
*   [ ] **Display Group Markers:**
    *   [ ] Fetch non-expired groups from `MainViewModel`.
    *   [ ] For each group with valid coordinates (`pickupLat`, `pickupLng`):
        *   [ ] Create a custom marker using a Material 3 pin icon.
            *   *Files to create:* Potentially a new Composable for the marker icon.
        *   [ ] Display "to: [Destination Name]" text above or near the pin (styled like a Material 3 button/chip).
            *   *Reference:* `jetpack_compose_guidelines_for_ai_agent.md` (Custom drawing or layout for markers if needed)
        *   [ ] Add markers to `MapView.overlays`.
    *   [ ] Optimize marker updates (e.g., clear and redraw only when group list or map view changes significantly).
*   [ ] **Group Info Overlay/Dialog:**
    *   [ ] On marker/destination name tap:
        *   [ ] Display an overlay or dialog (e.g., `AlertDialog`, custom Composable) showing group details: destination, current member count (e.g., "2/4 members").
        *   [ ] Include a "Join Group" button.
            *   *Reference:* `PHASE_3_UI_INTEGRATION_COMPLETE.md` (GroupMembersDialog can be a reference for UI structure)
*   [ ] **Join Group Functionality (Map):**
    *   [ ] Wire the "Join Group" button to `MainViewModel.joinGroup()`.
    *   [ ] Ensure optimistic UI updates reflect on the map overlay/dialog (e.g., member count changes, join button state).
        *   *Reference:* `PHASE_2_OPTIMISTIC_UI_COMPLETE.md`, `PHASE_3_UI_INTEGRATION_COMPLETE.md`
*   [ ] **Display Joined Group Member Details (Post-Join):**
    *   [ ] After joining, if the user re-opens the group info overlay, or via a separate "View Members" action:
        *   [ ] List members.
        *   [ ] Display a star next to the group creator.
        *   [ ] Allow viewing/dialing member phone numbers (requires `CALL_PHONE` permission, already in Manifest).
*   [ ] **Create Group FAB:**
    *   [ ] Add a Floating Action Button (FAB) on the Map screen for creating a new group.
    *   [ ] Link FAB to the existing group creation flow/screen.
        *   *Reference:* `jetpack_compose_guidelines_for_ai_agent.md` (Advanced FAB Animations if desired)

## Phase 3: "Groups" Tab Implementation (List & Search)

*   [ ] **Groups List UI:**
    *   [ ] Create a Composable (`GroupsListScreen.kt` or within the "Groups" tab Composable) to display groups as a scrollable list (`LazyColumn`).
    *   [ ] Each item should show destination, member count, distance (re-calculate on demand for display).
*   [ ] **Search Bar UI & Animation:**
    *   [ ] Add a search bar UI element near the bottom, above the History FAB.
    *   [ ] On click:
        *   [ ] Animate the search bar to move up using `SharedTransitionLayout` (or a simpler custom animation if `SharedTransitionLayout` is too complex for this specific case).
            *   *Reference:* `jetpack_compose_guidelines_for_ai_agent.md` (Shared Element Transitions)
        *   [ ] Display the soft keyboard.
*   [ ] **Search Functionality:**
    *   [ ] Implement search logic in `MainViewModel` (or a new ViewModel for this tab if separation is preferred).
    *   [ ] Input: Destination name.
    *   [ ] Output: Filtered list of groups matching the destination name.
    *   [ ] Sort results by distance from the user's current location.
*   [ ] **History FAB:**
    *   [ ] Ensure the History FAB is present on this screen, positioned appropriately.
*   [ ] **Join Group Functionality (List):**
    *   [ ] Allow users to join groups directly from the search results list (similar to the map overlay's join button).

## Phase 4: Advanced Features & Refinements

*   [X] **Custom Map Markers & Clustering (Optional but Recommended):**
    *   [X] Used custom icons for group markers on the map (`R.drawable.ic_custom_group_marker`).
    *   [X] Implemented marker clustering using `RadiusMarkerClusterer` for improved performance and map readability.
        *   *Files modified:* `ui/screens/MapScreen.kt`, `app/build.gradle.kts`.
*   [X] **Search & Filtering Enhancements:**
    *   [X] On `GroupsListScreen.kt`, added a `TextField` for search.
    *   [X] Added filter chips ("Show only groups with free spots", "Sort by Distance").
        *   *Files to modify:* `ui/screens/GroupsListScreen.kt`, `viewmodel/MainViewModel.kt`.
*   [X] **User Profile Screen:**
    *   [X] Created `UserProfileScreen.kt` integrating `ProfileFeatureViewModel`.
    *   [X] Displays user info (avatar, name, email, bio), preferences, activity history.
    *   [X] Includes "Update Bio" and "Sign Out" functionality.
    *   [X] Added navigation from `SettingsScreen` to `UserProfileScreen`.
        *   *Files created:* `ui/screens/UserProfileScreen.kt`
        *   *Files modified:* `ui/navigation/Screen.kt`, `ui/screens/SettingsScreen.kt`, `ui/MainScreen.kt`.
*   [ ] **Refine UI/UX & Add Animations:**
    *   [ ] Review animations in `ExploreScreen` (view toggle), `GroupDetailScreen` (entry/exit if any), `UserProfileScreen`.
    *   [ ] Ensure consistent Material 3 styling and responsive layouts.
    *   [ ] Polish interaction feedback (e.g., button press states, loading indicators).
*   [ ] **Map State Preservation & Enhancements:**
    *   [ ] Persist map's last known center and zoom level (e.g., using `DataStore` or `ViewModel` state saved across configuration changes) and restore it when `MapScreen` is reopened.
    *   [ ] Consider adding a "center on my location" button if aggressive auto-centering is disabled.
*   [ ] **Error Handling & Edge Cases:**
    *   [ ] Test all operations with network off/on.
    *   [ ] Review error messages for clarity and user-friendliness.
    *   [ ] Ensure graceful handling of missing data (e.g., group details, user profile).

## Phase 5: Testing & Production Readiness

*   [X] **Unit Tests for ViewModels & Repositories.**
    *   [X] Set up test environment for `AuthViewModel`.
    *   [X] Added comprehensive test cases for `AuthViewModel`.
    *   [X] Set up test environment for `GroupRepositoryImpl`.
    *   [X] Added initial test cases for `GroupRepositoryImpl`.
    *   [ ] Add comprehensive test cases for `GroupRepositoryImpl`.
    *   [ ] Write unit tests for `MainViewModel`.
    *   [ ] Write unit tests for `ProfileFeatureViewModel`.
    *   [ ] Write unit tests for `AnimationViewModel`.
*   [X] **UI Tests for critical user flows.**
    *   [X] Set up UI test environment.
    *   [X] Created `LoginScreenTest.kt` with an initial test case for successful login.
    *   [X] Replaced `Thread.sleep` with `waitForIdle` and `waitUntil` for more robust synchronization.
    *   [ ] Add UI tests for registration flow.
    *   [ ] Add UI tests for group creation flow.
    *   [ ] Add UI tests for joining a group.
    *   [ ] (Optional) Further refine UI tests with custom Idling Resources.
*   [X] **Generate Release APK with Proguard/R8.**
    *   [X] Enabled `isMinifyEnabled` and `isShrinkResources` for release builds.
    *   [X] Reviewed `proguard-rules.pro`.
    *   [X] Resolved `osmdroid-bonuspack` dependency issue by using direct GitHub path for JitPack.
    *   [X] Successfully built release APK with `assembleRelease`.
*   [ ] **Final review and documentation cleanup.**
    *   [ ] Review all `// TODO:` comments in the codebase.
    *   [ ] Ensure `README.md` and `GEMINI.md` are up-to-date.

## Future Considerations (Post-MVP)

*   Group creator tools (edit, disband group).
*   Push notifications for group updates (new members, messages if chat were added).
*   More advanced filtering options.
*   User ratings and reviews.
*   Dark Theme support improvements.
