# Akahidegn App Testing & Debugging Guide

This guide provides comprehensive information on how to build, deploy, test, and debug the Akahidegn Android application, with special focus on Firebase integration.

## 🛠️ Build Tools

The project includes several scripts that make it easy to build and test the application:

### Building the App

1. **Build App** (Debug or Release)
   ```bash
   ./build_app.sh [debug|release]
   ```
   - Default is debug if no argument is provided
   - Creates APK at `app/build/outputs/apk/debug/app-debug.apk` or `app/build/outputs/apk/release/app-release.apk`
   - Also copies APK to root directory for easy access

### Installing & Testing

1. **Uninstall and Install Fresh APK**
   ```bash
   ./uninstall_and_install_apk.sh
   ```
   - Uninstalls any existing app version to avoid signature conflicts
   - Installs the latest built APK
   - Launches the app and monitors logs

2. **Monitor App**
   ```bash
   ./monitor_app.sh
   ```
   - Check if the app is installed and running
   - Start the app if needed
   - Monitor app logs with various filtering options
   - Check Firebase connection status

### Firebase Testing

1. **Test Firebase Groups**
   ```bash
   ./test_firebase_groups.sh
   ```
   - Verify app and emulator status
   - Create test group JSON data
   - Test group creation through the app
   - Test Firebase CLI operations

2. **Create Test Group**
   ```bash
   ./create_test_group.sh
   ```
   - Creates a test group directly through the Firebase CLI

3. **Check Group Creation**
   ```bash
   ./check_group_creation.sh
   ```
   - Monitors logs specifically for group creation operations

4. **Apply Test Rules**
   ```bash
   ./apply_test_rules.sh
   ```
   - Applies testing Firebase rules (more permissive) for development

5. **Restore Rules**
   ```bash
   ./restore_rules.sh
   ```
   - Restores production Firebase rules

## 📱 Common Testing Workflows

### First-time Setup

1. Make sure Android Studio and Android Emulator are installed
2. Start an emulator
3. Build the debug app: `./build_app.sh`
4. Install the app: `./uninstall_and_install_apk.sh`

### Testing Group Creation

1. Make sure Firebase rules are properly set (see `FIREBASE_RULES_UPDATE.md`)
2. Start the emulator and install the latest app
3. Monitor group creation: `./check_group_creation.sh`
4. Launch the app and attempt to create a group
5. Check logs for any errors or success messages

### Testing Authentication

1. Install the app on the emulator
2. Use `./monitor_app.sh` and select option 2 to monitor Firebase-related logs
3. Launch the app and attempt to log in
4. Check logs for authentication success or failure

## 🔍 Troubleshooting

### Common Issues and Solutions

1. **APK Install Fails with INSTALL_FAILED_UPDATE_INCOMPATIBLE**
   - Use `./uninstall_and_install_apk.sh` to completely uninstall the app first

2. **Firebase "Permission denied" Errors**
   - Check Firebase rules in Firebase Console
   - Apply testing rules: `./apply_test_rules.sh`
   - Verify authentication is working properly
   - Ensure data structure matches the expected format in rules

3. **Group Creation Fails**
   - Monitor logs for specific error messages: `./check_group_creation.sh`
   - Verify Group.kt has the correct toMap() structure
   - Test Firebase connection directly with `./test_firebase_groups.sh`

4. **App Crashes**
   - Check logs for error message: `./monitor_app.sh` option 3
   - Look for syntax errors or other code issues
   - Run `./check_syntax.sh` to find potential code problems

## 📚 Additional Resources

- **Firebase Setup Guide**: See `FIREBASE_SETUP_GUIDE.md`
- **Firebase Troubleshooting**: See `FIREBASE_TROUBLESHOOTING.md`
- **Firebase Rules Update**: See `FIREBASE_RULES_UPDATE.md`
- **Quick Start Guide**: See `QUICK_START_GUIDE.md`

## 📋 Testing Completion Checklist

Before marking the app as production-ready, verify the following functionality works:

- [ ] Authentication (login and signup)
- [ ] FCM Token registration
- [ ] Group creation
- [ ] Group listing/reading
- [ ] User profile updates
- [ ] Location services
- [ ] All Firebase features (Authentication, Realtime Database, FCM)
