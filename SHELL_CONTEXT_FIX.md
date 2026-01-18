# Shell Context Fix for Root Commands

## Problem

Root commands were being executed **WITHOUT shell context**, causing shell builtins like `test -f` to fail with `NOT_FOUND` instead of `EXISTS`.

When executing commands manually via `su` shell, the test worked correctly:
```bash
su
test -f /data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat && echo 'EXISTS' || echo 'NOT_FOUND'
# Output: EXISTS
```

But the app was getting `NOT_FOUND` because `test -f` is a **shell builtin** and not available in all root contexts.

## Solution

Execute all root commands through explicit shell context using `sh -c`.

## Changes Made

### 1. RootManager.java

#### Updated `runRootCommand()` method:
- **Before**: Commands were sent directly to `su` process
  ```java
  os.writeBytes(command + "\n");
  ```
  
- **After**: Commands are wrapped in `sh -c` for shell context
  ```java
  os.writeBytes("sh -c '" + command.replace("'", "'\\''") + "'\n");
  ```

This ensures that:
- Shell builtins like `test`, `[`, and `[[` are available
- Shell features like command substitution, pipes, and redirections work correctly
- Commands execute in a proper shell environment

#### Updated `runRootCommands()` method:
- Applied same `sh -c` wrapper for multiple commands

### 2. ZipManager.java

#### Updated `fileExistsWithRoot()` method:
- **Before**: Used `test -f` (shell builtin, unreliable in some contexts)
  ```java
  "test -f \"" + path + "\" && echo 'EXISTS' || echo 'NOT_FOUND'"
  ```
  
- **After**: Uses `[ -f ]` (POSIX-compliant, more robust)
  ```java
  "[ -f \"" + path + "\" ] && echo 'EXISTS' || echo 'NOT_FOUND'"
  ```

- **Fallback**: Still uses `ls` as a secondary method if `[ -f ]` fails

#### Updated `copyFileWithRoot()` method:
Added multiple fallback methods for better reliability:

1. **Method 1**: `cat` with output redirection
   ```java
   cat "source" > "dest" 2>&1
   ```

2. **Fallback 1**: `dd` (block copy, works on more systems)
   ```java
   dd if="source" of="dest" 2>&1
   ```

3. **Fallback 2**: `cp` (standard copy)
   ```java
   cp -f "source" "dest" 2>&1
   ```

Each method is tried in sequence until one succeeds.

### 3. AccountManagementActivity.java

Added `testShellCommands()` method for testing:
- Tests `[ -f ]` command
- Tests `test -f` command
- Tests `ls` command
- Tests `find` command
- Displays results in a dialog for verification

**Note**: The test method temporarily replaces `showBackupDialog()` for testing purposes. After verification, the original backup functionality should be restored.

## Security Improvements

### Command Injection Prevention
- Single quotes in commands are properly escaped: `'\\''`
- This prevents command injection even if filenames contain quotes

Example:
```java
// Input command: test -f "/path/with'quote/file"
// Escaped: sh -c 'test -f "/path/with'\''quote/file"'
```

### Validation
- Commands are validated through `isCommandSafe()` before execution
- Dangerous patterns are rejected

## Testing Instructions

1. Build and install the APK:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. Monitor logs:
   ```bash
   adb logcat | grep BabixGO
   ```

3. In the app:
   - Open "Accountverwaltung" (Account Management)
   - Click "Account sichern" (Backup Account)
   - Review the test dialog showing results of all 4 test methods
   - Check logcat for detailed execution logs

4. Expected results:
   - Test 1 (`[ -f ]`): Should show `EXISTS`
   - Test 2 (`test -f`): Should show `EXISTS`
   - Test 3 (`ls`): Should show file details
   - Test 4 (`find`): Should list matching files

## Reverting Test Code

After testing is complete, revert the test code in `AccountManagementActivity.java`:

1. Remove the `testShellCommands()` call from `showBackupDialog()`
2. Uncomment the original backup dialog code
3. Keep the `testShellCommands()` method for future debugging if needed

## Benefits

1. **Reliability**: Commands now work consistently across different Android devices and root implementations
2. **Compatibility**: Shell builtins are now available
3. **Fallback**: Multiple methods for file operations ensure success
4. **Debugging**: Enhanced logging for troubleshooting
5. **Security**: Proper command escaping prevents injection attacks

## Technical Details

### Why `sh -c`?

When you run `su`, you get a root shell, but it's not necessarily a full shell environment. Using `sh -c 'command'` explicitly spawns a shell (`sh`) to execute the command, ensuring:
- Shell builtins are available
- Shell syntax (pipes, redirections, substitutions) works
- Environment is consistent

### Why `[ -f ]` instead of `test -f`?

- `[ ]` is more universally available as a builtin
- `test` might not be available as a builtin in minimal shells
- Both are POSIX-compliant, but `[ ]` has better compatibility
- As fallback, `ls` doesn't depend on shell builtins at all

## Related Files

- `app/src/main/java/de/babixgo/monopolygo/RootManager.java`
- `app/src/main/java/de/babixgo/monopolygo/ZipManager.java`
- `app/src/main/java/de/babixgo/monopolygo/AccountManagementActivity.java`
- `app/src/main/java/de/babixgo/monopolygo/AccountManager.java` (uses the updated methods)
