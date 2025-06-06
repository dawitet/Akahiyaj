# AKAHIYAJ (አካሂያጅ) - Ride Sharing App Documentation
*Complete App Architecture and Implementation Guide - UPDATED JUNE 2025*

## 📱 **APP OVERVIEW**

**Akahiyaj** (አካሂያጅ - meaning "Take me with you" in Amharic) is a comprehensive ride-sharing application built for Android using Kotlin and Jetpack Compose. The app features modern UI/UX design, advanced functionality, and complete Amharic localization for the Ethiopian market.

### **✨ Key Features (IMPLEMENTATION STATUS: 100% COMPLETE)**
- **🎨 Modern UI/UX**: ✅ Material 3 design with glassmorphism effects, animations, and transitions
- **🔍 Advanced Search**: ✅ Smart filtering, sorting, and real-time search capabilities  
- **📑 Bookmark System**: ✅ Save favorite groups with activity tracking
- **🎭 Theme Support**: ✅ Dynamic light/dark mode switching with smooth animations
- **♿ Accessibility**: ✅ Full screen reader support, high contrast, and 48dp touch targets
- **🔄 Real-time Updates**: ✅ Firebase Realtime Database with pull-to-refresh
- **⏰ Auto-Cleanup**: ✅ Groups automatically expire after 30 minutes
- **💰 Monetization**: ✅ Integrated AdMob banner and interstitial ads
- **🌍 Amharic Support**: ✅ Complete localization with Noto Sans Ethiopic typography
- **⚡ Performance**: ✅ Memory optimization, infinite scroll, and smooth animations
- **🚨 Error Handling**: ✅ Comprehensive error recovery with beautiful UI feedback
- **✅ Success Feedback**: ✅ Animated confirmations and haptic feedback throughout
- **🔧 Integration**: ✅ Complete component integration into main app flow

---

## 🏗️ **ARCHITECTURE OVERVIEW**

### **Tech Stack**
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Database**: Firebase Realtime Database
- **Authentication**: Firebase Auth
- **Ads**: Google AdMob
- **Dependency Injection**: Dagger Hilt
- **Background Tasks**: WorkManager
- **Image Loading**: Coil
- **Location Services**: Android Location API

### **Architecture Pattern**
The app follows **MVVM (Model-View-ViewModel)** architecture with:
- **Models**: Data classes (Group, User)
- **Views**: Composable functions
- **ViewModels**: MainViewModel for state management
- **Repository Pattern**: Firebase integration layer

### **Project Structure**
```
com.dawitf.akahidegn/
├── MainActivity.kt                 # Main entry point
├── Group.kt                       # Group data model
├── GroupCleanupWorker.kt          # Background cleanup service
├── GroupCleanupScheduler.kt       # Cleanup scheduling logic
├── SplashActivity.kt              # App launch screen
├── ui/
│   ├── components/
│   │   ├── BannerAdComponents.kt  # AdMob banner components
│   │   └── MainScreenComponents.kt # UI components
│   ├── screens/
│   │   ├── MainScreen.kt          # Primary app screen
│   │   ├── NameInputScreen.kt     # User registration
│   │   └── ChatScreen.kt          # Group communication
│   ├── social/
│   │   ├── SocialScreen.kt        # Social features
│   │   └── RideBuddyScreen.kt     # Ride buddy matching
│   └── theme/
│       └── Theme.kt               # Material 3 theming
└── viewmodel/
    └── MainViewModel.kt           # Central state management
```

---

## 🔧 **CORE FEATURES DETAILED**

### **1. Group Management System**

#### **Group Creation Flow**
1. User enters name (if first time)
2. Clicks "Create Group" FAB
3. Location permission check
4. Interstitial ad display
5. Group creation form
6. Firebase database update
7. Real-time sync with all users

#### **Group Data Model**
```kotlin
data class Group(
    val id: String = "",
    val creatorName: String = "",
    val destination: String = "",
    val currentPassengers: Int = 0,
    val maxPassengers: Int = 4,
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val location: String = ""
)
```

#### **Auto-Cleanup Mechanism**
- **Trigger**: Groups older than 30 minutes
- **Implementation**: WorkManager periodic task
- **Schedule**: Every 15 minutes check
- **Logic**: `GroupCleanupWorker.kt`

### **2. Real-time Updates**
- **Database**: Firebase Realtime Database
- **Path**: `/groups/{groupId}`
- **Listeners**: LiveData observers in ViewModel
- **Sync**: Automatic UI updates on data changes

### **3. Advertisement Integration**

#### **Banner Ads**
- **Placement**: Every 3rd item in carousel
- **Component**: `CarouselBannerAd`
- **Size**: Medium Rectangle (320x250)
- **Test Unit**: `ca-app-pub-3940256099942544/6300978111`

#### **Interstitial Ads**
- **Trigger**: Before group creation
- **Timing**: After 5-second delay
- **Fallback**: Skip if load fails

### **4. Material 3 Design Implementation**
- **Dynamic Colors**: Adapts to system theme
- **Components**: TopAppBar, BottomSheet, Cards, FAB
- **Typography**: Material 3 type scale
- **Color Scheme**: Primary, Secondary, Surface containers

---

## 🛠️ **SETUP AND BUILD INSTRUCTIONS**

### **Prerequisites**
- Android Studio Hedgehog+ (2023.1.1+)
- JDK 11 or higher
- Android SDK 35 (API level 35)
- Firebase project setup
- AdMob account

### **Firebase Configuration**
1. Create Firebase project
2. Add Android app with package `com.dawitf.akahidegn`
3. Download `google-services.json` to `app/` directory
4. Enable:
   - Realtime Database
   - Authentication
   - Crashlytics
   - Performance Monitoring

### **AdMob Setup**
1. Create AdMob account
2. Link to Firebase project
3. Create ad units:
   - Banner ads
   - Interstitial ads
4. Replace test IDs in code

### **Build Steps**
```bash
# Clone repository
git clone [repository-url]
cd Akahidegn

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

---

## 🧪 **TESTING GUIDE**

### **Unit Tests**
- **Location**: `app/src/test/`
- **Framework**: JUnit 4
- **Coverage**: Group creation, cleanup logic
- **Run**: `./gradlew test`

### **UI Testing Checklist**
1. **App Launch**: Splash screen displays with car image
2. **Name Input**: First-time user registration flow
3. **Main Screen**: Groups carousel with banner ads
4. **Group Creation**: Ad → Form → Database sync
5. **Real-time Updates**: Multi-device group sync
6. **Auto-Cleanup**: Groups disappear after 30 min
7. **Feedback**: Email intent opens correctly

### **Device Testing**
- **Minimum**: Android 8.0 (API 26)
- **Target**: Android 14 (API 35)
- **Resolution**: Phone and tablet layouts
- **Network**: Online/offline scenarios

---

## 🔍 **TROUBLESHOOTING**

### **Common Issues**

#### **Build Failures**
- **Cause**: Missing google-services.json
- **Solution**: Download from Firebase Console

#### **Groups Not Appearing**
- **Cause**: Network connectivity or Firebase rules
- **Solution**: Check internet and database permissions

#### **Ads Not Loading**
- **Cause**: Test device not configured
- **Solution**: Add device to AdMob test devices

#### **Location Permission Issues**
- **Cause**: Runtime permissions not granted
- **Solution**: Check manifest and request logic

### **Performance Optimization**
- **Database**: Use indexed queries
- **Images**: Implement image caching with Coil
- **Memory**: Proper Composable lifecycle management
- **Battery**: Optimize WorkManager intervals

---

## 📊 **ANALYTICS AND MONITORING**

### **Firebase Analytics Events**
- `group_created`: Track group creation success
- `ad_viewed`: Monitor ad performance
- `app_launch`: User engagement metrics

### **Crashlytics Integration**
- Automatic crash reporting
- Custom exception logging
- Performance monitoring

### **Performance Metrics**
- App startup time
- Database query latency
- UI rendering performance

---

## 🚀 **DEPLOYMENT**

### **Release Preparation**
1. Update version code/name in `build.gradle.kts`
2. Generate signed APK/AAB
3. Test on release configuration
4. Update Play Store listing

### **Play Store Requirements**
- **Target SDK**: Latest (API 35)
- **Privacy Policy**: Required for AdMob
- **App Signing**: Google Play App Signing
- **Content Rating**: Appropriate for ride-sharing

---

## 🔮 **FUTURE ENHANCEMENTS**

### **Planned Features**
- [ ] Push notifications for group updates
- [ ] Route optimization with Google Maps
- [ ] User ratings and reviews system
- [ ] Payment integration
- [ ] Multi-language support expansion
- [ ] Dark/Light theme toggle
- [ ] Offline mode capabilities

### **Technical Improvements**
- [ ] Migration to Kotlin Multiplatform
- [ ] GraphQL API implementation
- [ ] Advanced caching strategies
- [ ] Accessibility enhancements
- [ ] CI/CD pipeline setup

---

## 📞 **SUPPORT AND CONTACT**

- **Developer**: Dawit Fikadu
- **Email**: dawitfikadu3@gmail.com
- **Feedback**: Via in-app feedback option
- **Issues**: GitHub Issues (if applicable)

## 🆕 **RECENTLY IMPLEMENTED FEATURES**

### **1. Settings Screen with Theme Toggle**
- Complete settings screen with theme preferences (Light/Dark/System)
- Theme preferences are persisted using DataStore
- Navigation to settings from both MainScreen and UserProfileScreen
- Additional accessibility settings for text size and high contrast mode

### **2. Group Filtering & Searching**
- Advanced filtering by destination, departure time, and capacity
- Sort options by proximity, time, available seats, price, and rating
- Real-time filtering with smart algorithm implementation
- Filtering UI with animated transitions and haptic feedback

### **3. Bookmark & Recent Activity System**
- BookmarkManager for saving and retrieving favorite routes
- Recent activity tracking for user interactions
- Dedicated RecentActivityScreen with chronological activity display
- Persistent storage using DataStore with proper serialization

### **4. Optimized Image Loading**
- LazyImageLoader component for efficient image loading
- Image caching with memory and disk support
- Placeholder handling during loading
- Error states with appropriate fallback UI

### **5. Accessibility Improvements**
- AccessibilityUtils for screen reader support
- Minimum touch target size of 48dp for all interactive elements
- High contrast mode for visually impaired users
- Semantic properties for better screen reader experience

### **6. Performance Documentation**
- Comprehensive ACCESSIBILITY_AND_PERFORMANCE.md guide
- Memory management best practices
- State handling optimizations
- Component lifecycle management guidelines

---

*Last Updated: June 6, 2025*
*Version: 1.0.0*
