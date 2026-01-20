# App Structure Restructuring

## Overview

The MonopolyGo Manager app has been restructured to improve user experience and navigation. The main changes include:

1. **New Main Screen**: The Account List is now the main entry point of the app
2. **Navigation Drawer**: A hamburger menu provides easy access to all modules
3. **Floating Action Button (FAB)**: Quick access to "Neuen Account sichern" function

## Architecture Changes

### MainActivity

**Before:**
- Simple button-based menu screen
- Separate activities for each function
- No unified navigation

**After:**
- Integrated account list display
- Navigation drawer with hamburger menu
- Direct access to all modules
- FAB for quick account backup

### New Structure

```
MainActivity (with DrawerLayout)
├── Account List (RecyclerView)
│   ├── Click account → Options Dialog
│   │   ├── Wiederherstellen
│   │   └── Mehr anzeigen
│   └── FAB → Neuen Account sichern
└── Navigation Drawer (Hamburger Menu)
    ├── 🏠 Accountliste (Home)
    ├── 🎯 Tycoon Racers
    ├── 🤝 Partnerevent
    ├── 💝 Freundschaftsbalken
    ├── 👥 Kunden
    └── ⚙️ Einstellungen
```

## Navigation Menu Items

### 🏠 Accountliste (Home)
- **Status**: ✅ Implemented
- **Description**: Main account list view with RecyclerView
- **Features**:
  - View all accounts
  - Click account for options (Restore/Show More)
  - FAB for "Neuen Account sichern"

### 🎯 Tycoon Racers
- **Status**: 🚧 Placeholder
- **Description**: Module for Tycoon Racer events
- **Future Features**:
  - Event management
  - Team composition
  - Progress tracking

### 🤝 Partnerevent
- **Status**: ✅ Implemented
- **Description**: Partner event management
- **Features**:
  - Event creation
  - Team management
  - Assignment tracking

### 💝 Freundschaftsbalken
- **Status**: ✅ Implemented
- **Description**: Friendship bar management
- **Features**:
  - Friend list management
  - Progress tracking

### 👥 Kunden
- **Status**: 🚧 Placeholder
- **Description**: Customer account management
- **Future Features**:
  - Customer database
  - Account assignments
  - Customer tracking

### ⚙️ Einstellungen
- **Status**: 🚧 Placeholder
- **Description**: App settings and configuration
- **Future Features**:
  - Backup paths configuration
  - App preferences
  - Account settings

## User Interface

### Main Screen Layout

The main screen now uses a `DrawerLayout` with:
- **Header**: Menu button, title, and column headers
- **Content**: RecyclerView showing all accounts
- **FAB**: Floating action button for "Neuen Account sichern"
- **Drawer**: Navigation menu with all modules

### Account List

Each account displays:
- **Name**: Account identifier
- **Zuletzt online**: Last played timestamp
- **Sus**: Suspension status
- **Error**: Error status (red if error exists)

### Interaction

1. **Click Account**: Shows options dialog
   - Wiederherstellen: Restore account to device
   - Mehr anzeigen: View account details
   - Abbrechen: Cancel

2. **FAB Click**: Opens account backup dialog

3. **Menu Button**: Opens navigation drawer

## Migration Notes

### Removed Components
- Old MainActivity with button-based menu
- Redundant AccountManagementActivity entry point (still exists for backup functionality)

### Preserved Components
- AccountManagementActivity (for backup/restore operations)
- AccountListActivity (now integrated into MainActivity)
- AccountDetailActivity (for viewing account details)
- All existing database and repository classes

### New Components
- Navigation drawer menu (`drawer_menu.xml`)
- Navigation header (`nav_header.xml`)
- TycoonRacersActivity (placeholder)
- CustomersActivity (placeholder)
- SettingsActivity (placeholder)

## Technical Details

### Dependencies Added
```gradle
implementation 'androidx.drawerlayout:drawerlayout:1.2.0'
implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'
```

### Layouts
- `activity_main.xml`: DrawerLayout with account list and FAB
- `nav_header.xml`: Navigation drawer header
- `drawer_menu.xml`: Navigation menu items

### Permissions
No changes to existing permissions:
- Root access required for backup/restore
- Storage permissions required for file operations
- Network access for Supabase integration

## Future Enhancements

1. **Tycoon Racers Module**: Full implementation
2. **Customers Module**: Customer account management
3. **Settings Module**: App configuration
4. **Enhanced Navigation**: Bottom navigation bar option
5. **Dark Theme**: Support for dark mode
6. **Search**: Search and filter accounts

## Testing

### Build Status
✅ App builds successfully without errors

### Manual Testing Required
- [ ] Navigation drawer opens/closes correctly
- [ ] All menu items navigate to correct activities
- [ ] Account list displays properly
- [ ] FAB opens backup dialog
- [ ] Click account shows options dialog
- [ ] Restore and detail view work correctly
- [ ] Back button behavior is correct
- [ ] Permissions are requested properly

## Backward Compatibility

The restructuring maintains backward compatibility:
- All existing data remains accessible
- Database schema unchanged
- File structure unchanged
- Existing backup files compatible

## Screenshots

(To be added after testing on device/emulator)

## Support

For issues or questions, please refer to:
- [Main README](README.md)
- [User Guide](USER_GUIDE.md)
- [Build Instructions](BUILD_INSTRUCTIONS.md)
