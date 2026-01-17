# Changelog

All notable changes to the MonopolyGo Manager Android App will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.3] - 2026-01-13

### Fixed
- **Account Backup File Copy Issue**
  - Fixed issue where "Account sichern" only created folder but didn't copy the account data file
  - MonopolyGo app is now force-stopped before backup (consistent with restore behavior)
  - Added 1-second wait time after stopping app to ensure proper shutdown
  - Account backup now reliably copies the `WithBuddies.Services.User.0Production.dat` file
  
- **AccountManager.java**
  - Added `forceStopApp()` call at the beginning of `backupAccount()` method
  - Added wait time with `Thread.sleep(1000)` for app to fully stop
  - Improved code comments to clarify backup process steps
  
- **Bash Scripts**
  - `2_Eigener_Account_sichern.sh` - Added `am force-stop` command before file copy
  - Added sleep time to ensure app has fully stopped before attempting file copy

### Documentation
- **USER_GUIDE.md**
  - Updated backup process documentation to mention app is stopped before backup

## [1.0.2] - 2026-01-12

### Changed
- **UserID Extraction Now Optional**
  - Account backups no longer fail if UserID extraction fails
  - Backup proceeds with "N/A" placeholder for UserID when extraction fails
  - Shortlink creation is conditional - only attempted if UserID is available
  - Users can now save accounts with just the provided internal ID/name
  - Fixed issue: "Weiterhin 'Keine UserID gefunden'" - backups no longer blocked by UserID extraction failures

### Fixed
- **AccountManagementActivity.java**
  - Removed early return when UserID extraction fails in `backupOwnAccount()`
  - Added warning message when UserID not found but backup continues
  - Display "Nicht gefunden" for UserID and "Nicht verfügbar" for Shortlink in success message when extraction fails
  - Backup operation now always proceeds regardless of UserID extraction result

- **Bash Scripts**
  - `2_Eigener_Account_sichern.sh` - Continues execution even if UserID not found, uses "N/A" placeholder
  - `2_Eigener_Account_sichern.sh` - Shortlink creation only attempted when UserID is available
  - `2_Eigener_Account_sichern.sh` - Duplicate checking adjusted to handle "N/A" UserID values
  - `2_Kunden_Account_sichern.sh` - Same improvements as own account script
  - Both scripts now show warnings instead of errors when UserID extraction fails

### Documentation
- **USER_GUIDE.md**
  - Updated backup section to reflect optional UserID extraction
  - Clarified that backups succeed even without UserID
  - Updated troubleshooting section to explain new behavior
  - Added note that accounts backed up without UserID can still be restored

## [1.0.1] - 2026-01-12

### Fixed
- **UserID Extraction Enhancement**
  - Added comprehensive fallback logic for UserID extraction
  - Now checks 11 different possible field names (up from 2)
  - Added support for integer XML element format (`<int name="..." value="..."/>`)
  - Refactored bash scripts to use array loops for better maintainability
  - Improved error messages to guide users
  - Fixed issue: "Immer noch - keine User ID gefunden"

### Changed
- **DataExtractor.java**
  - Enhanced `extractUserId()` method with additional field name fallbacks
  - Checks for: `Scopely.Attribution.UserId`, `ScopelyProfile.UserId`, `Scopely.UserId`, `UserId`, `user_id`, `userId`, `PlayerId`, `player_id`, `playerId`, `PlayerID`, `UserID`
  - Added support for both string and integer XML formats
  
- **Bash Scripts**
  - `2_Eigener_Account_sichern.sh` - Refactored with array-based field name checking
  - `2_Kunden_Account_sichern.sh` - Refactored with array-based field name checking
  
- **Documentation**
  - Updated USER_GUIDE.md with enhanced troubleshooting information

## [1.0.0] - 2026-01-12

### Added - Initial Release

#### Core Features
- **Root Permission Management**
  - Automatic root detection
  - Root permission request dialog
  - Root access verification
  - Persistent root session management

#### Account Management
- **Account Restoration**
  - Select from own or customer accounts
  - Force-stop MonopolyGo before restore
  - Copy account files with root privileges
  - Set proper file permissions
  - Optional app restart after restore

- **Own Account Backup**
  - Extract UserID from app preferences
  - Create Short.io links automatically
  - Save account files to organized directories
  - Store metadata (Internal ID, UserID, Date, Link, Note)
  - Duplicate detection

- **Customer Account Backup**
  - Save customer information
  - Extract UserID from friendship links
  - Store customer metadata

#### User Interface
- **Main Activity**
  - Module selection (Account Management, Partner Event, Friendship)
  - Root warning dialog
  - Permission management
  - Clean, intuitive layout

- **Account Management Activity**
  - Restore account with source selection
  - Backup own account with form dialog
  - Backup customer account with form dialog
  - Copy links (placeholder)
  - Status display area

- **Partner Event Activity** (In Development)
  - Add customer (placeholder)
  - Select accounts (placeholder)
  - Create assignment (placeholder)
  - Team setup (placeholder)

- **Friendship Activity** (In Development)
  - Download and installation (placeholder)

#### Technical Implementation
- **Root Manager**
  - Device root check
  - Root permission request
  - Root command execution
  - Multi-command support
  - Error handling

- **Account Manager**
  - Directory initialization
  - Force-stop app functionality
  - Start app functionality
  - Account restore with proper permissions
  - Account backup with metadata
  - List backed-up accounts
  - Open friend links

- **Data Extractor**
  - UserID extraction from SharedPreferences
  - XML parsing for app data
  - File existence checks

- **Short Link Manager**
  - Short.io API integration
  - OkHttp HTTP client
  - JSON request/response handling
  - Error handling

#### Dependencies
- libsu 5.0.1 - Root access management
- OkHttp 4.11.0 - HTTP client
- OpenCSV 5.7.1 - CSV processing
- Gson 2.10.1 - JSON processing
- AndroidX AppCompat 1.6.1
- Material Components 1.9.0
- ConstraintLayout 2.1.4

#### Build System
- Gradle 8.0 wrapper
- Android Gradle Plugin 8.1.0
- Target SDK 33 (Android 13)
- Minimum SDK 21 (Android 5.0)
- ProGuard configuration

#### Documentation
- **ANDROID_README.md** - Comprehensive app documentation
- **BUILD_INSTRUCTIONS.md** - Detailed build guide
- **SECURITY.md** - Security considerations and best practices
- **USER_GUIDE.md** - Complete user manual
- **MIGRATION_GUIDE.md** - Migration from bash scripts
- **README.md** - Updated project overview

#### Directory Structure
- Auto-creation of storage directories:
  - `/storage/emulated/0/MonopolyGo/Accounts/Eigene/`
  - `/storage/emulated/0/MonopolyGo/Accounts/Kunden/`
  - `/storage/emulated/0/MonopolyGo/Partnerevents/`
  - `/storage/emulated/0/MonopolyGo/Backups/`

#### Permissions
- WRITE_EXTERNAL_STORAGE - Save backups
- READ_EXTERNAL_STORAGE - Read backups
- INTERNET - API calls
- ACCESS_NETWORK_STATE - Network status

### Security
- Root access required notice
- User permission dialogs
- Input validation
- Secure API key storage (to be improved)
- File permission management

### Known Limitations
- Partner Event features not yet implemented
- Friendship features not yet implemented
- CSV data management not yet implemented
- Backup/restore as ZIP not yet implemented
- Edit account info not yet implemented
- Advanced link copying not yet implemented

### Technical Debt
- API key hardcoded (should use secure storage)
- No data encryption for backups
- No certificate pinning for API
- Limited input sanitization

## [Unreleased]

### Planned for 1.1.0
- Complete Partner Event implementation
- Complete Friendship Bar implementation
- CSV account data management
- Enhanced link copying functionality
- Edit account information
- Backup/restore as ZIP files

### Planned for 1.2.0
- Data encryption for backups
- Secure API key storage
- Certificate pinning
- Advanced error recovery
- Batch operations
- Account statistics

### Planned for 2.0.0
- Multi-device sync
- Cloud backup integration
- Advanced analytics
- Automated scheduling
- Plugin system

## Version History

- **1.0.0** (2026-01-12) - Initial release with core account management
- More versions to come...

---

## Contributing

See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for information about migrating from bash scripts.

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

## License

This project is for private use.
