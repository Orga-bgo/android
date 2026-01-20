# Supabase Integration Guide

## Overview
This guide explains the Supabase integration for the MonopolyGo Android App. The integration provides cloud-based account management, multi-device sync, and device ID tracking while maintaining 100% compatibility with existing root-based operations.

## Architecture

```
┌─────────────────────────────────────────┐
│         Android UI Layer                │
│  (Activities, Fragments, Adapters)      │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│       NEW: Business Logic Layer         │
│  ┌───────────────────────────────────┐  │
│  │ AccountRepository                 │  │
│  │ - CRUD Operations                 │  │
│  │ - Async CompletableFuture         │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │ DeviceIdExtractor                 │  │
│  │ - SSAID (via Root)                │  │
│  │ - GAID (via Play Services)        │  │
│  │ - Device ID (Android ID)          │  │
│  └───────────────────────────────────┘  │
└─────────┬────────────────────┬──────────┘
          │                    │
┌─────────▼─────────┐   ┌─────▼──────────────┐
│  SupabaseManager  │   │  EXISTING Root Ops │
│  (HTTP/REST API)  │   │  RootManager.java  │
│  OkHttp Client    │   │  AccountManager    │
└─────────┬─────────┘   │  DataExtractor     │
          │             └────────────────────┘
┌─────────▼─────────┐
│  Supabase DB      │
│  PostgreSQL       │
│  (Cloud Sync)     │
└───────────────────┘
```

## Setup Instructions

### 1. Create Supabase Project

1. Go to https://supabase.com
2. Create a new project
3. Wait for database to initialize
4. Go to Settings → API
5. Copy:
   - Project URL (e.g., `https://xxxxx.supabase.co`)
   - Anon/Public Key (starts with `eyJ...`)

### 2. Configure Gradle Properties

Edit `gradle.properties`:

```properties
# Supabase Configuration
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

⚠️ **NEVER commit actual credentials to Git!** Add `gradle.properties` to `.gitignore` if it contains real credentials.

### 3. Create Database Schema

Run this SQL in Supabase SQL Editor:

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Accounts table
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    user_id TEXT,
    short_link TEXT,
    friend_link TEXT,
    friend_code TEXT,
    account_status TEXT DEFAULT 'active' CHECK (account_status IN ('active', 'suspended', 'banned', 'inactive')),
    
    -- Suspension tracking
    suspension_0_days INTEGER DEFAULT 0,
    suspension_3_days INTEGER DEFAULT 0,
    suspension_7_days INTEGER DEFAULT 0,
    suspension_permanent BOOLEAN DEFAULT FALSE,
    suspension_count INTEGER DEFAULT 0,
    
    -- Device IDs
    ssaid TEXT,
    gaid TEXT,
    device_id TEXT,
    
    -- Flags
    is_suspended BOOLEAN DEFAULT FALSE,
    has_error BOOLEAN DEFAULT FALSE,
    note TEXT,
    
    -- Timestamps
    last_played TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_accounts_name ON accounts(name);
CREATE INDEX idx_accounts_status ON accounts(account_status);
CREATE INDEX idx_accounts_deleted ON accounts(deleted_at);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);

-- Enable Row Level Security (RLS)
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;

-- Allow anonymous access for now (adjust for production!)
CREATE POLICY "Allow anonymous access" ON accounts FOR ALL USING (true);

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

## Usage Examples

### 1. Using SupabaseManager Directly

```java
import de.babixgo.monopolygo.database.SupabaseManager;

// Get singleton instance
SupabaseManager supabase = SupabaseManager.getInstance();

// Check if configured
if (!supabase.isConfigured()) {
    Log.e(TAG, "Supabase not configured!");
    return;
}

// Example: Fetch all accounts
try {
    List<Account> accounts = supabase.select("accounts", Account.class, "deleted_at=is.null");
    for (Account acc : accounts) {
        Log.d(TAG, "Account: " + acc.getName());
    }
} catch (IOException e) {
    Log.e(TAG, "Failed to fetch accounts", e);
}
```

### 2. Using AccountRepository (Recommended)

```java
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.models.Account;

// Create repository
AccountRepository repo = new AccountRepository();

// Create new account
Account newAccount = new Account("TestAccount", "user_123456");
newAccount.setShortLink("https://short.link/abc");
newAccount.setFriendCode("ABC123");

repo.createAccount(newAccount)
    .thenAccept(account -> {
        Log.d(TAG, "Account created with ID: " + account.getId());
    })
    .exceptionally(throwable -> {
        Log.e(TAG, "Failed to create account", throwable);
        return null;
    });

// Load all accounts
repo.getAllAccounts()
    .thenAccept(accounts -> {
        Log.d(TAG, "Loaded " + accounts.size() + " accounts");
        for (Account acc : accounts) {
            Log.d(TAG, "  - " + acc.getName() + " (" + acc.getAccountStatus() + ")");
        }
    })
    .exceptionally(throwable -> {
        Log.e(TAG, "Failed to load accounts", throwable);
        return null;
    });

// Update account
Account account = // ... load from somewhere
account.setNote("Updated note");
account.setSuspension3Days(1);

repo.updateAccount(account)
    .thenAccept(updated -> {
        Log.d(TAG, "Account updated successfully");
    })
    .exceptionally(throwable -> {
        Log.e(TAG, "Failed to update account", throwable);
        return null;
    });

// Update last played timestamp
repo.updateLastPlayed(accountId)
    .thenRun(() -> {
        Log.d(TAG, "Last played timestamp updated");
    })
    .exceptionally(throwable -> {
        Log.e(TAG, "Failed to update last played", throwable);
        return null;
    });

// Soft delete
repo.deleteAccount(accountId)
    .thenRun(() -> {
        Log.d(TAG, "Account deleted (soft delete)");
    })
    .exceptionally(throwable -> {
        Log.e(TAG, "Failed to delete account", throwable);
        return null;
    });
```

### 3. Using DeviceIdExtractor

```java
import de.babixgo.monopolygo.utils.DeviceIdExtractor;

// Extract all device IDs (async)
DeviceIdExtractor.extractAllIds(context)
    .thenAccept(deviceIds -> {
        Log.d(TAG, "SSAID: " + deviceIds.ssaid);
        Log.d(TAG, "GAID: " + deviceIds.gaid);
        Log.d(TAG, "Device ID: " + deviceIds.deviceId);
        
        if (deviceIds.isComplete()) {
            Log.d(TAG, "All device IDs extracted successfully!");
            
            // Save to account
            repo.updateDeviceIds(accountId, 
                deviceIds.ssaid, 
                deviceIds.gaid, 
                deviceIds.deviceId
            ).thenRun(() -> {
                Log.d(TAG, "Device IDs saved to account");
            });
        }
    })
    .exceptionally(throwable -> {
        Log.e(TAG, "Failed to extract device IDs", throwable);
        return null;
    });

// Extract individual IDs
String ssaid = DeviceIdExtractor.extractSSAID(); // Requires root
String deviceId = DeviceIdExtractor.extractDeviceId(context);

DeviceIdExtractor.extractGAID(context)
    .thenAccept(gaid -> {
        Log.d(TAG, "GAID: " + gaid);
    });
```

### 4. Complete Example: Account Backup with Device IDs

```java
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.utils.DeviceIdExtractor;
import de.babixgo.monopolygo.models.Account;

public void backupAccountWithDeviceIds(String accountName) {
    AccountRepository repo = new AccountRepository();
    
    // Step 1: Check if Supabase is configured
    if (!repo.isSupabaseConfigured()) {
        Log.w(TAG, "Supabase not configured, skipping cloud backup");
        // Continue with local root-based backup using existing AccountManager
        return;
    }
    
    // Step 2: Extract device IDs
    DeviceIdExtractor.extractAllIds(this)
        .thenCompose(deviceIds -> {
            // Step 3: Check if account exists
            return repo.getAccountByName(accountName)
                .thenCompose(existingAccount -> {
                    if (existingAccount != null) {
                        // Update existing account
                        existingAccount.setSsaid(deviceIds.ssaid);
                        existingAccount.setGaid(deviceIds.gaid);
                        existingAccount.setDeviceId(deviceIds.deviceId);
                        return repo.updateAccount(existingAccount);
                    } else {
                        // Create new account
                        Account newAccount = new Account(accountName, "user_" + System.currentTimeMillis());
                        newAccount.setSsaid(deviceIds.ssaid);
                        newAccount.setGaid(deviceIds.gaid);
                        newAccount.setDeviceId(deviceIds.deviceId);
                        return repo.createAccount(newAccount);
                    }
                });
        })
        .thenAccept(account -> {
            Log.d(TAG, "Account synced to cloud: " + account.getName());
            runOnUiThread(() -> {
                Toast.makeText(this, "Account backed up successfully!", Toast.LENGTH_SHORT).show();
            });
        })
        .exceptionally(throwable -> {
            Log.e(TAG, "Failed to backup account to cloud", throwable);
            runOnUiThread(() -> {
                Toast.makeText(this, "Cloud backup failed: " + throwable.getMessage(), 
                    Toast.LENGTH_LONG).show();
            });
            return null;
        });
}
```

## Design System Usage

### Colors

```xml
<!-- Primary Colors -->
<color name="primary_blue">#3B82F6</color>
<color name="primary_blue_dark">#1976D2</color>

<!-- Backgrounds -->
<color name="background_light">#E9EEF2</color>
<color name="card_background">#FFFFFF</color>

<!-- Text -->
<color name="text_dark">#1E252B</color>
<color name="text_gray">#64748B</color>

<!-- Status -->
<color name="error_red">#EF4444</color>
<color name="success_green">#10B981</color>
<color name="warning_yellow">#F59E0B</color>
```

### Button Styles

```xml
<!-- Blue Button -->
<Button
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Speichern"
    style="@style/BabixButton.Blue" />

<!-- Gray Button -->
<Button
    android:text="Abbrechen"
    style="@style/BabixButton.Gray" />

<!-- Red Button -->
<Button
    android:text="Löschen"
    style="@style/BabixButton.Red" />

<!-- Green Button -->
<Button
    android:text="Bestätigen"
    style="@style/BabixButton.Green" />
```

### Card Style

```xml
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    style="@style/BabixCard">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        
        <TextView
            android:text="Account Name"
            style="@style/SubHeaderText" />
            
        <TextView
            android:text="Details"
            style="@style/BodyText" />
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

## Account Model Fields

```java
// Basic Info
account.getId()              // Database ID (long)
account.getName()            // Account name (unique)
account.getUserId()          // MonopolyGo user ID

// Links
account.getShortLink()       // Short.io link
account.getFriendLink()      // Friend referral link
account.getFriendCode()      // Friend code

// Status
account.getAccountStatus()   // 'active', 'suspended', 'banned', 'inactive'
account.isSuspended()        // Boolean flag

// Suspension Tracking
account.getSuspension0Days()     // Count of 0-day suspensions
account.getSuspension3Days()     // Count of 3-day suspensions
account.getSuspension7Days()     // Count of 7-day suspensions
account.isSuspensionPermanent()  // Permanent ban flag
account.getSuspensionCount()     // Total suspensions
account.getSuspensionSummary()   // "0 3 7 X" format for UI

// Device IDs
account.getSsaid()           // Android SSAID (from MonopolyGo)
account.getGaid()            // Google Advertising ID
account.getDeviceId()        // Android Device ID

// Flags
account.isHasError()         // Error flag
account.getNote()            // Notes/comments

// Timestamps
account.getLastPlayed()              // Last played timestamp
account.getFormattedLastPlayed()     // "dd.MM.yyyy, HH:mm"
account.getCreatedAt()               // Creation timestamp
account.getUpdatedAt()               // Last update timestamp
account.getDeletedAt()               // Soft delete timestamp
```

## Error Handling

All async operations use `CompletableFuture` with proper exception handling:

```java
repo.getAllAccounts()
    .thenAccept(accounts -> {
        // Success handling
    })
    .exceptionally(throwable -> {
        // Error handling
        Log.e(TAG, "Operation failed", throwable);
        
        // Show user-friendly error
        if (throwable.getCause() instanceof IOException) {
            showError("Network error. Check internet connection.");
        } else {
            showError("Database error: " + throwable.getMessage());
        }
        
        return null; // Required for exceptionally()
    });
```

## Security Notes

⚠️ **Important Security Considerations:**

1. **API Keys**: Never commit real Supabase credentials to Git
2. **Row Level Security**: The example SQL uses `USING (true)` for development. For production:
   ```sql
   -- Example production RLS policy
   CREATE POLICY "Users can only see their own accounts"
       ON accounts FOR SELECT
       USING (auth.uid() = user_id::uuid);
   ```
3. **HTTPS**: Supabase uses HTTPS by default - never disable SSL
4. **Input Validation**: Always validate user input before sending to database
5. **Root Access**: Device ID extraction requires root - handle root permission denials gracefully

## Troubleshooting

### Build Errors

**Error: "compileSdk = 34 requires AGP 8.2+"**
- Solution: Already fixed - using compileSdk 34 with warning suppression

**Error: "Supabase URL not configured"**
- Check `gradle.properties` has valid SUPABASE_URL and SUPABASE_ANON_KEY
- Rebuild project after changing gradle.properties

### Runtime Errors

**Error: "Unexpected response 401"**
- Check Supabase anon key is correct
- Verify RLS policies allow anonymous access (development) or proper authentication (production)

**Error: "Network error"**
- Check internet connection
- Verify Supabase project is active (not paused)

**SSAID extraction returns null**
- Root access required
- MonopolyGo must be installed
- Check shared_prefs permissions

**GAID extraction fails**
- Requires Google Play Services
- User may have disabled ad tracking
- Handle null GAID gracefully

## Next Steps

This implementation provides the foundation for:

1. **Account List UI** (Teil 2) - RecyclerView with account cards
2. **Account Detail UI** - Edit account info, view device IDs
3. **Tycoon Racers Event Management** - Track event participation
4. **Real-time Sync** - Supabase Realtime for multi-device updates
5. **Offline Mode** - Local cache with background sync

## Summary

✅ **Completed:**
- Supabase REST API integration via OkHttp
- Account model with complete field mapping
- Device ID extraction (SSAID, GAID, Device ID)
- Repository pattern for clean architecture
- Async operations with CompletableFuture
- Material Design 3 styling
- Full backward compatibility with existing root operations

🔒 **No Changes to Existing Code:**
- RootManager.java - Unchanged
- AccountManager.java - Unchanged
- DataExtractor.java - Unchanged

All new functionality is layered on top via new packages:
- `de.babixgo.monopolygo.models` - Data models
- `de.babixgo.monopolygo.database` - Supabase integration
- `de.babixgo.monopolygo.utils` - Helper utilities
