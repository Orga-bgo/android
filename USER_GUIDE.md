# User Guide - MonopolyGo Manager Android App

## Table of Contents
1. [Installation](#installation)
2. [First Launch](#first-launch)
3. [Root Permissions](#root-permissions)
4. [Main Features](#main-features)
5. [Account Management](#account-management)
6. [Partner Event](#partner-event)
7. [Friendship Bar](#friendship-bar)
8. [Troubleshooting](#troubleshooting)
9. [FAQ](#faq)

## Installation

### Prerequisites
- ✅ Rooted Android device (SuperSU or Magisk required)
- ✅ Android 5.0 (Lollipop) or higher
- ✅ ~10 MB free storage space
- ✅ Storage permissions

### Installation Steps

1. **Download the APK**
   - Download `app-debug.apk` or `app-release.apk` from GitHub releases
   - Transfer to your device if downloaded on PC

2. **Enable Unknown Sources**
   - Go to Settings → Security
   - Enable "Unknown Sources" or "Install Unknown Apps"
   - Select your file manager and allow installations

3. **Install the APK**
   - Open your file manager
   - Navigate to the downloaded APK
   - Tap to install
   - Grant storage permissions when prompted

4. **Grant Root Access**
   - Launch the app
   - SuperSU/Magisk will prompt for root access
   - **Tap "Grant"** to allow root permissions
   - App will verify root access

## First Launch

### Welcome Screen
When you first launch the app, you'll see:

```
┌─────────────────────────────┐
│   MonopolyGo Manager        │
├─────────────────────────────┤
│  Root-Zugriff erforderlich  │
│  für volle Funktionalität   │
├─────────────────────────────┤
│  [Accountverwaltung]        │
│  [Partnerevent]             │
│  [Freundschaftsbalken]      │
└─────────────────────────────┘
```

### Initial Setup
The app will automatically:
1. Request storage permissions
2. Check for root access
3. Request root permission from SuperSU/Magisk
4. Create necessary directories:
   - `/storage/emulated/0/MonopolyGo/Accounts/Eigene/`
   - `/storage/emulated/0/MonopolyGo/Accounts/Kunden/`
   - `/storage/emulated/0/MonopolyGo/Partnerevents/`
   - `/storage/emulated/0/MonopolyGo/Backups/`

## Root Permissions

### Why Root is Required
The app needs root access to:
- Read account data from `/data/data/com.scopely.monopolygo/`
- Copy account files for backup/restore
- Extract UserID from app preferences
- Force-stop and start the MonopolyGo app

### Root Warning Dialog
On first launch, you'll see:

```
⚠️ WARNUNG: Diese App benötigt Root-Zugriff

Die folgenden Funktionen erfordern Root-Rechte:
✓ Account-Wiederherstellung
✓ Account-Sicherung  
✓ Zugriff auf App-Daten
✓ Automatische Freundschaftsanfragen

Ohne Root sind nur Basis-Funktionen verfügbar.
```

### Granting Root Access
1. When prompted by SuperSU/Magisk:
   - Tap **"Grant"**
   - Optionally check "Remember choice"
   - Wait for confirmation

2. If denied:
   - App will show limited functionality
   - You can try again by restarting the app

## Main Features

### 1. Account Management (Accountverwaltung)
Manage your MonopolyGo accounts with ease.

**Features:**
- ✅ Restore backed-up accounts
- ✅ Backup your own accounts
- ✅ Backup customer accounts
- ✅ Copy friendship links

### 2. Partner Event (Partnerevent)
Manage partner events and team assignments.

**Features:**
- 🚧 Add customers
- 🚧 Select own accounts
- 🚧 Create assignments
- 🚧 Team setup

*Status: In Development*

### 3. Friendship Bar (Freundschaftsbalken)
Automate friendship bar events.

**Features:**
- 🚧 Download and installation

*Status: In Development*

## Account Management

### Restore Account (Account wiederherstellen)

**Purpose:** Load a previously backed-up account.

**Steps:**

1. **Open Account Management**
   - Tap "Accountverwaltung" on main screen

2. **Select Restore**
   - Tap "Account wiederherstellen"

3. **Choose Source**
   ```
   Quelle auswählen:
   • Eigene Accounts
   • Kunden Accounts
   • Abbrechen
   ```

4. **Select Account**
   - Choose from list of backed-up accounts
   - Confirm selection

5. **Restoration Process**
   - App force-stops MonopolyGo
   - Copies account file with root privileges
   - Sets proper permissions
   - Shows success message

6. **Start Game (Optional)**
   ```
   App starten?
   Möchten Sie MonopolyGo jetzt starten?
   [Ja]  [Nein]
   ```

**What Happens:**
- MonopolyGo is force-stopped
- Account file is copied to active location
- Permissions are set correctly
- You can optionally start the game

**Success Indicators:**
- ✅ "Account erfolgreich wiederhergestellt"
- ✅ Status shows account name
- ✅ Option to start app appears

**Error Messages:**
- ❌ "Fehler beim Wiederherstellen" - Check root access
- ❌ "Keine Ordner gefunden" - No backups available

### Backup Own Account (Eigenen Account sichern)

**Purpose:** Save your current account with metadata.

**Steps:**

1. **Open Backup Dialog**
   - Tap "Eigenen Account sichern"

2. **Enter Information**
   ```
   ┌─────────────────────────┐
   │ Interne ID:             │
   │ [Enter ID, e.g. ACC001] │
   │                         │
   │ Notiz (optional):       │
   │ [Optional note]         │
   └─────────────────────────┘
   ```

3. **Confirm**
   - Tap "Sichern" (Save)
   - Wait for processing

4. **Automatic Operations**
   - Stops MonopolyGo app (to ensure file consistency)
   - Attempts to extract UserID from game data (optional)
   - Creates short link via Short.io API (if UserID available)
   - Copies account file to backup location
   - Saves metadata

5. **Success**
   ```
   Account gesichert
   Interne ID: ACC001
   UserID: 123456789 (or "Nicht gefunden")
   Shortlink: https://go.babixgo.de/ACC001 (or "Nicht verfügbar")
   ```

**What Gets Saved:**
- Account file: `WithBuddies.Services.User.0Production.dat`
- Location: `/storage/emulated/0/MonopolyGo/Accounts/Eigene/[InternalID]/`
- Metadata: Internal ID, UserID (or "N/A" if extraction fails), Date, Shortlink (or "N/A" if UserID unavailable), Note

**Important Notes:**
- ✅ **Backup proceeds even if UserID extraction fails** - The account will be saved with the provided internal ID
- ⚠️ If UserID is not found, it will be stored as "N/A" and shortlink creation will be skipped
- ⚠️ You can still restore accounts that were backed up without UserID

**Warning Messages:**
- ⚠️ "UserID nicht gefunden - Backup wird trotzdem durchgeführt" - Account will be saved without UserID
- ⚠️ "Shortlink konnte nicht erstellt werden" - Check internet connection (backup still successful)
- ❌ "Fehler beim Sichern" - Check storage permissions and try again

### Backup Customer Account (Kunden Account sichern)

**Purpose:** Save customer account information.

**Steps:**

1. **Open Customer Backup**
   - Tap "Kunden Account sichern"

2. **Enter Information**
   ```
   ┌─────────────────────────────────┐
   │ Kundenname:                     │
   │ [Customer name]                 │
   │                                 │
   │ Freundschaftslink:              │
   │ [monopolygo://add-friend/...]   │
   └─────────────────────────────────┘
   ```

3. **Save**
   - Tap "Sichern"
   - UserID extracted from link
   - Information stored

**What Gets Saved:**
- Customer name
- UserID (extracted from link)
- Friendship link
- Date

**Note:** This doesn't copy game files, only metadata.

### Copy Links (Kopiere Links)

**Purpose:** Quick access to friendship links.

**Status:** 🚧 In Development

**Planned Features:**
- List all saved links
- Copy to clipboard
- Share links
- Open links directly

## Partner Event

**Status:** 🚧 In Development

**Planned Features:**

### Add Customer (Kunde hinzufügen)
- Add customer to event
- Specify booked slots
- Link to customer account

### Select Accounts (Eigene Accounts wählen)
- Multi-select from your accounts
- Assign to event
- Max 4 slots per account

### Create Assignment (Zuweisung erstellen)
- Distribute customers across accounts
- One customer per account max
- Balance slot distribution

### Team Setup (Team zusammenstellen)
- Automated friend request sequence
- Restore account → Open links → Next account
- 10-second delays between operations

## Friendship Bar

**Status:** 🚧 In Development

**Planned Features:**

### Download and Installation
- Download required files
- Automatic installation
- Event participation automation

## Troubleshooting

### App Won't Start

**Problem:** App crashes on launch

**Solutions:**
1. Check Android version (5.0+ required)
2. Grant storage permissions
3. Clear app data and cache
4. Reinstall the app

### Root Access Denied

**Problem:** "Root-Zugriff wurde verweigert"

**Solutions:**
1. Open SuperSU/Magisk
2. Check if app is in the list
3. Manually grant root permission
4. Restart the app
5. Ensure device is properly rooted

### Account Restore Fails

**Problem:** "Fehler beim Wiederherstellen"

**Solutions:**
1. Verify root access is granted
2. Check if MonopolyGo is installed
3. Verify backup file exists
4. Try manual restore via file manager
5. Check `/data/data/` permissions

### UserID Not Found

**Current Behavior:** As of v1.0.2, UserID extraction is **optional**. Account backups will proceed even if UserID cannot be found.

**What Happens:**
1. App attempts to extract UserID from game data
2. If extraction fails, a warning is shown but backup continues
3. Account is saved with "N/A" for UserID field
4. Shortlink creation is skipped (not possible without UserID)
5. Account can still be restored normally

**To Improve UserID Detection:**
1. Ensure MonopolyGo is installed
2. Open MonopolyGo at least once to initialize preferences
3. Verify root access is granted
4. Check if preferences file exists:
   `/data/data/com.scopely.monopolygo/shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml`
5. The app automatically checks multiple possible field names:
   - `Scopely.Attribution.UserId`
   - `ScopelyProfile.UserId`
   - `Scopely.UserId`
   - `UserId`, `user_id`, `userId`
   - `PlayerId`, `player_id`, `playerId`
   - Both string and integer XML formats

**Note:** Even if UserID extraction fails, your account backup is complete and can be restored. The UserID is only needed for creating shortlinks and tracking purposes.

### Shortlink Creation Fails

**Problem:** "Shortlink konnte nicht erstellt werden"

**Solutions:**
1. Check internet connection
2. Verify API is accessible
3. Try again later
4. Check if domain is reachable

### No Backups Found

**Problem:** "Keine Ordner gefunden"

**Solutions:**
1. Create a backup first
2. Check directory exists:
   `/storage/emulated/0/MonopolyGo/Accounts/`
3. Grant storage permissions
4. Check if files are accessible

## FAQ

### Q: Is root access really required?
**A:** Yes, for most features. MonopolyGo stores data in protected directories that require root access.

### Q: Is this safe?
**A:** The app only uses root for MonopolyGo data management. Review SECURITY.md for details.

### Q: Can I use without root?
**A:** Limited functionality. Some features work without root, but backup/restore requires it.

### Q: Will this violate MonopolyGo terms?
**A:** Review MonopolyGo's terms of service. This tool modifies game data files.

### Q: Can I lose my account?
**A:** Always backup before operations. The app creates backups to prevent data loss.

### Q: What's the difference from Termux scripts?
**A:** Same functionality with a user-friendly GUI and native Android integration.

### Q: How do I update the app?
**A:** Download new APK and install over existing. Data is preserved.

### Q: Can I transfer accounts between devices?
**A:** Yes, copy the `/storage/emulated/0/MonopolyGo/` directory to the new device.

### Q: What if I lose root access?
**A:** Backups remain accessible. You'll need root again for restore operations.

### Q: Is my data sent anywhere?
**A:** Only short link creation uses internet (Short.io API). No other data is transmitted.

## Support

### Getting Help
1. Review this guide thoroughly
2. Check [SECURITY.md](SECURITY.md) for security concerns
3. Check [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for build issues
4. Open a GitHub issue with details:
   - Device model and Android version
   - Root method (SuperSU/Magisk)
   - Error messages
   - Steps to reproduce

### Reporting Bugs
Include:
- ✅ App version
- ✅ Android version
- ✅ Root status
- ✅ Error message
- ✅ Screenshots
- ✅ Steps to reproduce

### Feature Requests
We welcome feature requests! Open an issue with:
- Clear description
- Use case
- Expected behavior
- Any relevant examples

## Tips & Best Practices

### Regular Backups
- ✅ Backup before major game updates
- ✅ Backup before trying new features
- ✅ Backup to external storage periodically
- ✅ Test restore on non-critical account first

### Security
- ✅ Only grant root when app is active
- ✅ Keep backups secure
- ✅ Don't share account files publicly
- ✅ Use strong device password

### Organization
- ✅ Use meaningful internal IDs (e.g., MAIN_ACC, FARM01)
- ✅ Add notes to backups
- ✅ Keep customer info updated
- ✅ Regular cleanup of old backups

### Performance
- ✅ Close MonopolyGo before operations
- ✅ Ensure stable internet for link creation
- ✅ Keep sufficient storage space
- ✅ Restart app if it becomes slow

---

**Need more help?** Open an issue on GitHub: https://github.com/babix555/Bgo/issues
