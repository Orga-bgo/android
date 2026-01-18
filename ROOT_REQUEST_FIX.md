# Fix: Root Permission Dialog Not Appearing

## Problem Statement

**Original Issue (German):** "Keine Rootanfrage der App. Nur Benachrichtigungen dass sie root braucht."

**Translation:** "No root request from the app. Only notifications that it needs root."

The application was showing Toast notifications that root access is required, but the actual root permission dialog (from Magisk/SuperSU/KernelSU) was never displayed to the user.

## Root Cause

The issue was in the `RootManager.requestRoot()` method. The code was calling `Shell.isAppGrantedRoot()`, which according to the [libsu documentation](https://topjohnwu.github.io/libsu/com/topjohnwu/superuser/Shell.html#isAppGrantedRoot()):

> This method returns `null` when it is currently unable to determine whether root access has been granted to the application. **This method will not block the calling thread; results will be returned immediately.**

**Key insight:** `Shell.isAppGrantedRoot()` is a **passive check** - it only checks if root has already been granted. It does **NOT** trigger the root permission dialog.

### Old (Incorrect) Code

```java
public static boolean requestRoot() {
    if (rootChecked && hasRootAccess) {
        return true;
    }
    
    try {
        // ❌ This only CHECKS status, doesn't REQUEST root!
        Boolean granted = Shell.isAppGrantedRoot();
        
        if (granted == null) {
            hasRootAccess = false;
        } else {
            hasRootAccess = granted;
        }
        
        rootChecked = true;
        return hasRootAccess;
    } catch (Exception e) {
        hasRootAccess = false;
        rootChecked = true;
        return false;
    }
}
```

## Solution

To actually trigger the root permission dialog, the app needs to **create a Shell instance** using `Shell.getShell()`. According to the libsu documentation and examples, creating a shell is what triggers the permission request.

### New (Correct) Code

```java
public static boolean requestRoot() {
    if (rootChecked && hasRootAccess) {
        return true;
    }
    
    try {
        // ✅ Creating a shell TRIGGERS the root permission dialog
        Shell shell = Shell.getShell();
        
        // Now check if root was granted
        hasRootAccess = shell.isRoot();
        rootChecked = true;
        
        android.util.Log.d("BabixGO", "Root access: " + (hasRootAccess ? "granted" : "denied"));
        return hasRootAccess;
    } catch (Exception e) {
        android.util.Log.e("BabixGO", "Root request error: " + e.getMessage());
        hasRootAccess = false;
        rootChecked = true;
        return false;
    }
}
```

## Additional Changes

### Updated `isRooted()` Method

```java
public static boolean isRooted() {
    Boolean granted = Shell.isAppGrantedRoot();
    if (granted == null) {
        // Root status is undetermined - assume device might be rooted
        // Return true so we attempt to request root
        return true;  // Changed from false
    }
    return granted;
}
```

**Reason:** When `Shell.isAppGrantedRoot()` returns `null` (undetermined status), we now return `true` instead of `false`. This ensures that the app will attempt to request root instead of immediately showing a "device not rooted" error.

## How It Works Now

1. **App starts** → `MainActivity.onCreate()` → `checkRootAccess()`
2. **Check if rooted:** `RootManager.isRooted()`
   - If `Shell.isAppGrantedRoot()` returns `null` → returns `true` (undetermined, but try anyway)
   - If returns `false` → shows warning dialog (device not rooted)
   - If returns `true` → continues (root already granted)
3. **Request root:** `RootManager.requestRoot()` in background thread
   - Calls `Shell.getShell()` → **THIS TRIGGERS THE PERMISSION DIALOG** 🎯
   - User sees Magisk/SuperSU/KernelSU dialog
   - User grants or denies permission
4. **Update UI** based on result:
   - If granted → Shows "✅ Root-Zugriff gewährt" toast
   - If denied → Shows warning dialog

## Testing Recommendations

### Test Scenario 1: Fresh Install (First Launch)
1. Install the app on a rooted device
2. Launch the app
3. **Expected:** Root permission dialog appears (Magisk/SuperSU/KernelSU)
4. Grant permission
5. **Expected:** "✅ Root-Zugriff gewährt" toast appears

### Test Scenario 2: Permission Already Granted
1. App already has root access
2. Launch the app
3. **Expected:** No dialog (already granted), just shows success toast

### Test Scenario 3: Permission Denied
1. Install the app on a rooted device
2. Launch the app
3. Root permission dialog appears
4. Deny permission
5. **Expected:** Warning dialog explaining root is required

### Test Scenario 4: Non-Rooted Device
1. Install on non-rooted device
2. Launch the app
3. **Expected:** Warning dialog immediately (no permission dialog)

## Technical Details

### libsu Library Behavior

From the libsu documentation and README:

```java
// Example from libsu README - Preload root shell
Shell.getShell(shell -> {
    // The main shell is now constructed and cached
    // Root permission dialog was shown during construction
});
```

**Key Point:** The act of constructing/getting a shell is what triggers the root permission request. `Shell.isAppGrantedRoot()` is meant for checking status after a shell has been created.

### Compatibility

This fix maintains compatibility with all supported root solutions:
- ✅ **Magisk 24+** (primary target)
- ✅ **KernelSU** (modern alternative)
- ✅ **SuperSU** (legacy, still supported)

Works on all Android versions (5.0 - 14+) as libsu handles the platform differences internally.

## Related Documentation

- [libsu GitHub](https://github.com/topjohnwu/libsu)
- [libsu Javadoc - Shell.isAppGrantedRoot()](https://topjohnwu.github.io/libsu/com/topjohnwu/superuser/Shell.html#isAppGrantedRoot())
- [libsu Javadoc - Shell.getShell()](https://topjohnwu.github.io/libsu/com/topjohnwu/superuser/Shell.html#getShell())

## Files Modified

- `app/src/main/java/de/babixgo/monopolygo/RootManager.java`
  - `requestRoot()` - Changed to call `Shell.getShell()` instead of `Shell.isAppGrantedRoot()`
  - `isRooted()` - Changed to return `true` when status is undetermined
  - Added detailed comments explaining the fix

## Build Status

✅ **Build successful:** `./gradlew assembleDebug`  
✅ **APK created:** `app/build/outputs/apk/debug/app-debug.apk`  
✅ **No compilation errors**

## Summary

**Before:** App only checked root status → No permission dialog → User confused  
**After:** App creates shell to request root → Permission dialog appears → User can grant access ✅
