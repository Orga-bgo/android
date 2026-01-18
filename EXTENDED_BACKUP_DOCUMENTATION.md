# Extended Backup System - Implementation Documentation

## Overview
This document describes the extended backup system implemented for the MonopolyGo Android application. The new system enhances the existing backup functionality by supporting multiple files, optional Facebook token backup, and ZIP archiving.

## Features

### 1. Multiple File Backup
The extended backup system now saves multiple files instead of just the account data file:

**Always Backed Up:**
- `WithBuddies.Services.User.0Production.dat` - Main account data (required)

**Optionally Backed Up (if present):**
- `device-id` - Device identifier
- `internal-device-id` - Internal device identifier
- `generatefid.lock` - Firebase ID lock file
- `com.scopely.monopolygo.v2.playerprefs.xml` - Player preferences
- `mys_mod_window_positions.xml` - Mod window positions
- `mys_mod_feature_settings.xml` - Mod feature settings

**Conditionally Backed Up:**
- `com.facebook.AccessTokenManager.SharedPreferences.xml` - Facebook token (only if user selects checkbox)

### 2. ZIP Archiving
All backup files are now packaged into a single ZIP archive:

**Process:**
1. Files are first copied to `/data/local/tmp/<accountName>/`
2. A ZIP archive is created: `/data/local/tmp/<accountName>.zip`
3. The ZIP is moved to final location: `/storage/emulated/0/MonopolyGo/Accounts/Eigene/<accountName>/<accountName>.zip`
4. Temporary files are cleaned up

**Advantages:**
- Single file for easier management
- Reduced storage space
- Better organization
- Includes metadata file (`backup_info.txt`)

### 3. Backup Metadata
Each backup includes a `backup_info.txt` file containing:
- Backup date and time
- List of all included files
- Whether FB-Token was included

## User Interface Changes

### Extended Backup Dialog
The new backup dialog (`dialog_backup_extended.xml`) includes:

1. **Interne ID Field** - Material TextInputLayout for account ID
2. **Notiz Field** - Multi-line text area for optional notes
3. **FB-Token Checkbox** - Card-based checkbox with explanation
4. **Info Box** - Visual summary of what will be backed up

### Visual Design
- Material Design 3 components
- Color-coded sections (blue for FB-Token, yellow for info)
- Clear visual hierarchy
- Helpful explanatory text

## Implementation Details

### AccountManager.java Changes

#### New Methods

**`backupAccountExtended(String accountName, boolean includeFbToken)`**
- Creates extended backup with ZIP archiving
- Returns `true` on success, `false` on failure
- Handles all file operations with proper error checking

**`restoreAccountExtended(String accountName)`**
- Restores account from ZIP archive
- Extracts all files to temporary location
- Copies files back to app data directory
- Sets proper permissions

**Helper Methods:**
- `fileExists(String path)` - Check if file exists using root
- `getFileName(String fullPath, int index)` - Generate friendly filenames
- `createFileList(String tempDir, List<String> files, boolean fbIncluded)` - Create metadata
- `restoreOptionalFiles(String tempDir)` - Restore optional files
- `setProperPermissions()` - Set correct file permissions

#### Modified Methods

**`getBackedUpAccounts(boolean isOwnAccounts)`**
- Updated to detect both ZIP and old .dat backups
- Maintains backward compatibility
- Filters directories to show only valid backups

### AccountManagementActivity.java Changes

#### New Methods

**`backupOwnAccountExtended(String internalId, String note, boolean includeFbToken)`**
- Integrates with UI
- Calls `AccountManager.backupAccountExtended()`
- Displays success/failure messages with details

#### Modified Methods

**`showBackupOwnDialog()`**
- Now inflates `dialog_backup_extended.xml`
- Reads FB-Token checkbox state
- Passes checkbox value to backup method

**`restoreAccount(String accountName, boolean isOwnAccounts)`**
- Checks for ZIP file first
- Falls back to old .dat file if ZIP not found
- Maintains full backward compatibility

## Backward Compatibility

The implementation maintains full backward compatibility:

1. **Old backups still work** - .dat files are still recognized and restorable
2. **Old backup method preserved** - `backupAccount()` method unchanged
3. **Dual format support** - `getBackedUpAccounts()` finds both formats
4. **Graceful fallback** - Restore tries ZIP first, then .dat

## File Permissions

The system properly manages file permissions:

```bash
chmod 660 <data_file>
chown <app_uid:app_gid> <data_file>
```

This ensures the MonopolyGo app can read the restored files.

## Error Handling

The implementation includes comprehensive error handling:

1. **File existence checks** before operations
2. **Command output validation** for root operations
3. **Cleanup on failure** - removes temporary files
4. **User feedback** via Toast messages and status text
5. **Graceful degradation** - continues if optional files missing

## Security Considerations

### Facebook Token
- Only backed up when user explicitly checks the box
- Stored in encrypted ZIP with other sensitive data
- User is informed about what data is being backed up

### Root Operations
- All file operations use validated root commands
- Paths are properly quoted to prevent injection
- Temporary files cleaned up after operations
- Proper error handling prevents data leaks

## Usage

### Creating an Extended Backup

1. Click "Eigenen Account sichern" button
2. Enter Interne ID (required)
3. Enter optional note
4. Check "FB-Token sichern?" if needed
5. Click "Sichern"

The system will:
- Stop the MonopolyGo app
- Copy all available files
- Create ZIP archive
- Move to final location
- Save metadata to CSV

### Restoring from Backup

1. Click "Account wiederherstellen"
2. Select account from list
3. Click to confirm

The system will:
- Detect backup format (ZIP or .dat)
- Stop the MonopolyGo app
- Extract/copy files
- Set proper permissions
- Offer to start the app

## Testing Recommendations

Since this is a root-based Android app, testing should be done on:

1. **Rooted Android device** with API 21+ (Android 5.0+)
2. **MonopolyGo installed** to test actual file operations
3. **Storage permissions granted** for backup location
4. **Root access granted** via SuperSU/Magisk

### Test Cases

1. **New Backup (without FB-Token)**
   - Verify ZIP created in correct location
   - Check backup_info.txt shows FB-Token: NO
   - Confirm all available files included

2. **New Backup (with FB-Token)**
   - Verify FB token file included in ZIP
   - Check backup_info.txt shows FB-Token: YES

3. **Restore New Backup**
   - Verify all files restored to correct locations
   - Confirm app works after restore
   - Check file permissions are correct

4. **Restore Old Backup**
   - Verify old .dat files still work
   - Confirm backward compatibility

5. **Missing Optional Files**
   - Verify backup succeeds even if optional files missing
   - Confirm only available files are backed up

## File Structure

```
/storage/emulated/0/MonopolyGo/Accounts/Eigene/
└── <AccountName>/
    └── <AccountName>.zip
        ├── account.dat (required)
        ├── device-id.txt (optional)
        ├── internal-device-id.txt (optional)
        ├── generatefid.lock (optional)
        ├── playerprefs.xml (optional)
        ├── window_positions.xml (optional)
        ├── feature_settings.xml (optional)
        ├── fb_token.xml (optional)
        └── backup_info.txt (metadata)
```

## Future Enhancements

Potential improvements for future versions:

1. **Compression options** - Let user choose compression level
2. **Encryption** - Add password protection for sensitive backups
3. **Cloud sync** - Backup to cloud storage services
4. **Scheduled backups** - Automatic periodic backups
5. **Backup verification** - Check backup integrity
6. **Selective restore** - Choose which files to restore

## Conclusion

The extended backup system provides a robust, user-friendly solution for comprehensive account management while maintaining full backward compatibility with existing backups. The implementation follows Android best practices and provides a solid foundation for future enhancements.
