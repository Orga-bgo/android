# Copilot Onboarding Instructions

## High-Level Overview
This repository hosts a native Android application for managing MonopolyGo accounts, designed for rooted Android devices. The app provides a GUI, simplifying and replacing Termux-based bash scripts. It supports account recovery, backups, data extraction, and automated operations.

Key details:
- **Languages:** Java (50.8%), Shell (49.2%)
- **Size:** Medium-sized codebase with clear modular structure.
- **Frameworks/Libraries:** Android SDK, AndroidX, RootManager, libsu (Root Access), Gson, OkHttp, OpenCSV.
- **Build System:** Gradle with Android Gradle Plugin 8.1.0.
- **Target Runtime:** Android 5.0+ (API 21), with Target SDK 33 (Android 13).

## Build and Validation Instructions

### Environment Setup
1. Install **Android Studio (latest)**.
2. Install Android SDK tools and dependencies for API levels 21-33.
3. Ensure Gradle 8.0 or later is installed.
4. Rooted Android device or emulator with SU or Magisk for testing.
5. Grant necessary storage and network permissions.

### Build Process
1. Clone the repository:
   ```bash
   git clone https://github.com/Orga-bgo/android.git
   ```
2. Navigate to the project directory and sync Gradle:
   ```bash
   cd android
   ./gradlew tasks
   ```
3. Ensure all plugins and dependencies are synced.
4. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`

### Running the Application
1. Transfer the APK to a rooted Android device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
2. Grant root access upon launch.
3. Validate core functionalities (account management, event handling).

### Testing
1. Unit Tests:
   ```bash
   ./gradlew testDebug
   ```
2. Instrumentation Tests (requires connected device):
   ```bash
   ./gradlew connectedDebugAndroidTest
   ```

### Linting
Run static analysis to capture code issues:
```bash
./gradlew lint
```

## Project Layout
- **app/src/main/java/de/babixgo/monopolygo/MainActivity.java**: Entry point; main menu.
- **RootManager.java**: Functions for root access operations.
- **AccountManager.java**: Core account management functions.
- **build.gradle**: Gradle dependencies and build configuration.
- **ANDROID_README.md**: Comprehensive project description and guide.
- **QUICK_INSTALL.md**: Documentations for binary setup.

### Folder Structure
```
app/
├── src/main/java/de/babixgo/monopolygo/
│   ├── MainActivity.java                    # Main menu
│   ├── AccountManagementActivity.java       # Account management
│   ├── RootManager.java                     # Root manager
│   ├── DataExtractor.java                   # Data operations
│   ├── FriendshipActivity.java              # Friendship management (WIP)
│   ├── build.gradle                         # Dependencies
│   └── AndroidManifest.xml                  # Configuration
├── res/
│   ├── layout/                             # XML UI definitions
│   ├── values/                             # Global resources (e.g., strings)
│   └── drawable/                           # Icons
└── build.gradle
```

## Validation Pipelines
The following actions are run on GitHub automatically:
- **Build**: Ensures the project compiles with Gradle.
- **Tests**: Executes unit and instrumentation tests.

To replicate:
1. Navigate to `.github/workflows/*.yml`.
2. Execute locally via `act`: 
   ```bash
   act -j build-apk.yml
   ```