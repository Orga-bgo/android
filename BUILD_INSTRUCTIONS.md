# Build Instructions for MonopolyGo Manager Android APK

This document provides detailed instructions for building the MonopolyGo Manager Android APK.

## Prerequisites

### Required Software
1. **Android Studio** (Arctic Fox 2020.3.1 or newer)
   - Download from: https://developer.android.com/studio
   
2. **Java Development Kit (JDK) 8 or higher**
   - Check version: `java -version`
   - Download from: https://adoptium.net/ or https://www.oracle.com/java/

3. **Android SDK**
   - Installed automatically with Android Studio
   - Minimum API Level 21 (Android 5.0)
   - Target API Level 33 (Android 13)

4. **Firebase Project** (optional, for cloud features)
   - Create at: [Firebase Console](https://console.firebase.google.com)
   - Download `google-services.json`
   - Place in `app/` directory
   - See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for details

5. **Rooted Android Device** (for testing)
   - SuperSU or Magisk installed
   - Root access granted to app

### Optional Tools
- **Git** for version control
- **Gradle 8.0+** (included in project wrapper)

## Building with Android Studio

### Step 1: Clone the Repository
```bash
git clone https://github.com/babix555/Bgo.git
cd Bgo
```

### Step 2: Open Project in Android Studio
1. Launch Android Studio
2. Click **File → Open**
3. Navigate to the cloned `Bgo` directory
4. Click **OK**

### Step 3: Sync Gradle Files
Android Studio will automatically sync Gradle. If it doesn't:
1. Click **File → Sync Project with Gradle Files**
2. Wait for the sync to complete
3. Resolve any dependency issues that appear

### Step 4: Configure SDK
1. Go to **File → Project Structure**
2. Under **SDK Location**, ensure Android SDK is properly configured
3. Check that API Level 33 is installed
4. If not, go to **Tools → SDK Manager** and install:
   - Android SDK Platform 33
   - Android SDK Build-Tools 33.0.0+

### Step 5: Build the APK

#### For Debug Build:
1. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for build to complete
3. Click **locate** in the notification to find the APK
4. APK location: `app/build/outputs/apk/debug/app-debug.apk`

#### For Release Build:
1. Generate a signing key (first time only):
   - **Build → Generate Signed Bundle / APK**
   - Select **APK** and click **Next**
   - Click **Create new...** to create a keystore
   - Fill in keystore details and remember the passwords
   
2. Sign and build:
   - **Build → Generate Signed Bundle / APK**
   - Select **APK** and click **Next**
   - Choose your keystore file and enter passwords
   - Select **release** build variant
   - Click **Finish**
   - APK location: `app/build/outputs/apk/release/app-release.apk`

## Building from Command Line

### Step 1: Setup Environment
Ensure JAVA_HOME is set:
```bash
export JAVA_HOME=/path/to/java
export PATH=$JAVA_HOME/bin:$PATH
```

Verify Java installation:
```bash
java -version
javac -version
```

### Step 2: Initialize Gradle Wrapper
If gradle wrapper jar is missing, download it:
```bash
# Download gradle wrapper jar
mkdir -p gradle/wrapper
cd gradle/wrapper
wget https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar
cd ../..
```

Or use system Gradle to generate wrapper:
```bash
gradle wrapper --gradle-version 8.0
```

### Step 3: Build with Gradle

#### Debug Build:
```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Release Build (unsigned):
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

#### Release Build (signed):
First, create a keystore:
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

Then build with signing:
```bash
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=$(pwd)/my-release-key.jks \
  -Pandroid.injected.signing.store.password=your_store_password \
  -Pandroid.injected.signing.key.alias=my-key-alias \
  -Pandroid.injected.signing.key.password=your_key_password
```

### Step 4: Clean Build (if needed)
```bash
./gradlew clean
./gradlew assembleDebug
```

## Verifying the Build

### Check APK Info:
```bash
# Using aapt (Android Asset Packaging Tool)
aapt dump badging app/build/outputs/apk/debug/app-debug.apk

# Check minimum SDK version
aapt dump badging app/build/outputs/apk/debug/app-debug.apk | grep sdkVersion

# Check permissions
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

### Install APK:
```bash
# Using adb (Android Debug Bridge)
adb install app/build/outputs/apk/debug/app-debug.apk

# Force reinstall
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### Problem: Gradle sync fails
**Solution:**
1. Check internet connection
2. Clear Gradle cache: `rm -rf ~/.gradle/caches/`
3. Re-sync: `./gradlew --refresh-dependencies`

### Problem: SDK not found
**Solution:**
1. Open Android Studio
2. Go to **Tools → SDK Manager**
3. Install required SDK versions
4. Update `local.properties` with SDK path:
   ```
   sdk.dir=/path/to/Android/Sdk
   ```

### Problem: Build fails with "package does not exist"
**Solution:**
1. Ensure all dependencies are downloaded
2. Run `./gradlew clean`
3. Run `./gradlew build --refresh-dependencies`

### Problem: Out of memory during build
**Solution:**
Add to `gradle.properties`:
```
org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

### Problem: Missing gradle-wrapper.jar
**Solution:**
```bash
# Download from Gradle repository
cd gradle/wrapper
wget https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar

# Or regenerate wrapper
gradle wrapper --gradle-version 8.0
```

## Build Variants

### Debug Build
- Includes debug symbols
- Not optimized
- Can be debugged with Android Studio
- Larger APK size
- **Use for:** Development and testing

### Release Build
- Optimized with ProGuard/R8
- No debug symbols
- Smaller APK size
- Must be signed for distribution
- **Use for:** Production deployment

## Signing the APK

### Why Sign?
- Required for Google Play Store
- Required for installation on most devices
- Identifies the developer

### Keystore Security
⚠️ **Important:**
- Keep your keystore file secure
- Never commit keystore to version control
- Backup your keystore securely
- If you lose your keystore, you cannot update your app

### Signing Configuration
Add to `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("my-release-key.jks")
            storePassword "your_store_password"
            keyAlias "my-key-alias"
            keyPassword "your_key_password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## Size Optimization

### Enable ProGuard/R8:
In `app/build.gradle`:
```gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

### Check APK Size:
```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

## CI/CD Integration

### GitHub Actions Example:
```yaml
name: Android Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Gradle
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

## Additional Resources

- [Android Developer Documentation](https://developer.android.com/docs)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Android Studio User Guide](https://developer.android.com/studio/intro)
- [ProGuard Manual](https://www.guardsquare.com/manual/home)

## Support

If you encounter issues:
1. Check this guide first
2. Review error messages carefully
3. Search Android Studio documentation
4. Open an issue on GitHub with:
   - Error messages
   - Build logs
   - System information (OS, Java version, Android Studio version)
