# AGENTS.md - babixGO MonopolyGo Manager

> **Zweck:** Vollständige Projekt-Dokumentation für AI-Agents (GitHub Copilot, Claude, etc.)  
> **Letzte Aktualisierung:** 21. Januar 2026  
> **Version:** 2.0.0 (Navigation Restructure)

---

## 📋 INHALTSVERZEICHNIS

1. [Projekt-Übersicht](#projekt-übersicht)
2. [Kritische Architektur-Regeln](#kritische-architektur-regeln)
3. [App-Struktur & Navigation](#app-struktur--navigation)
4. [Technologie-Stack](#technologie-stack)
5. [Design-System](#design-system)
6. [Datenmodelle](#datenmodelle)
7. [Core-Funktionalitäten](#core-funktionalitäten)
8. [Code-Konventionen](#code-konventionen)
9. [Debugging & Logging](#debugging--logging)
10. [Deployment](#deployment)

---

## 🎯 PROJEKT-ÜBERSICHT

### Was ist babixGO?

**Native Android App** zur Verwaltung von MonopolyGo Accounts mit:
- **Multi-Account Management** via Root-Zugriff
- **Supabase PostgreSQL Backend** für Multi-Device Synchronisation
- **Tycoon Racers Event-Management** mit automatischer Ausführung
- **Device-ID Tracking** (SSAID, GAID, Android ID)
- **Kunden-Verwaltung** für Service-Dienstleistungen

### Kernfunktionen

1. **Account-Backup & Restore** via Root (BESTEHEND - NICHT ÄNDERN!)
2. **Multi-Device Sync** via Supabase (NEU - zusätzliche Schicht)
3. **Tycoon Racers Events** mit Team- und Slot-Management
4. **Automatische Event-Ausführung** (Restore → Start → Links öffnen)
5. **Suspension Tracking** (0-Tage, 3-Tage, 7-Tage, Permanent)
6. **Kunden-Management** mit Freundschaftslinks

---

## ⚠️ KRITISCHE ARCHITEKTUR-REGELN

### 🔴 REGEL #1: BESTEHENDE ROOT-IMPLEMENTIERUNG NIEMALS ÄNDERN

```java
// DIESE DATEIEN SIND SAKROSANKT - NIE ANFASSEN!
RootManager.java        // ← KOMPLETT UNVERÄNDERT
AccountManager.java     // ← KOMPLETT UNVERÄNDERT (nur Nutzung)
DataExtractor.java      // ← KOMPLETT UNVERÄNDERT
```

**Warum?**
- Diese Root-Implementierung funktioniert bereits perfekt
- Sie wurde mühsam durch Trial & Error entwickelt
- Änderungen führen zu Timing-Problemen und Root-Fehlern

**Stattdessen:**
- Supabase = **zusätzliche Schicht** für Metadaten
- Neue Features = **eigene Klassen** (z.B. `DeviceIdExtractor`, `EventExecutor`)
- Root-Layer wird **genutzt, aber nie modifiziert**

### 🔴 REGEL #2: APP-STRUKTUR (v2.0 - NEUE NAVIGATION)

```
MainActivity (DrawerLayout)
├── Fragment: AccountListFragment ← STARTSEITE
├── Fragment: TycoonRacersFragment
├── Fragment: PartnerEventFragment
├── Fragment: FriendshipBarFragment
├── Fragment: CustomerManagementFragment
└── Fragment: SettingsFragment

AccountDetailActivity (separate Activity)
└── Vollständige Account-Details + Edit-Dialog
```

**Wichtig:**
- **Accountliste = Startseite** (nicht mehr AccountManagementActivity)
- **Hamburger-Menü** enthält alle Hauptfunktionen
- **FAB** auf Accountliste = "Account sichern"
- **Accountverwaltung Activity wird ersetzt**

### 🔴 REGEL #3: SUPABASE ALS METADATEN-LAYER

```
┌─────────────────────────────────────┐
│      Android UI Layer               │
│   (Activities, Fragments)           │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│    Business Logic Layer             │
│  (Repositories, Managers)           │
└───┬─────────────────────┬───────────┘
    │                     │
┌───▼────────┐   ┌───────▼────────────┐
│ Supabase   │   │  Root Operations   │
│ (Metadata) │   │  (File Backup)     │
└────────────┘   └────────────────────┘
```

**Supabase speichert:**
- Account-Namen, UserIDs, Device-IDs
- Suspension-Status, Error-Flags
- Event-Zuweisungen, Team-Slots
- Kunden-Informationen

**Root-Operationen bleiben:**
- Account-Backup (Dateien kopieren)
- Account-Restore (Dateien zurückkopieren)
- UserID-Extraktion aus SharedPreferences
- Device-ID-Extraktion aus App-Daten

---

## 🏗️ APP-STRUKTUR & NAVIGATION

### MainActivity (DrawerLayout + NavigationView)

```xml
<!-- activity_main.xml -->
<androidx.drawerlayout.widget.DrawerLayout>
    
    <!-- Main Content -->
    <FrameLayout android:id="@+id/fragment_container" />
    
    <!-- Navigation Drawer -->
    <com.google.android.material.navigation.NavigationView
        android:id="@+id/nav_view"
        app:menu="@menu/drawer_menu" />
        
</androidx.drawerlayout.widget.DrawerLayout>
```

### Navigation-Menü

```xml
<!-- res/menu/drawer_menu.xml -->
<menu>
    <item android:id="@+id/nav_accounts"
          android:icon="@drawable/ic_accounts"
          android:title="Accountliste" />
          
    <item android:id="@+id/nav_tycoon_racers"
          android:icon="@drawable/ic_trophy"
          android:title="Tycoon Racers" />
          
    <item android:id="@+id/nav_partner_event"
          android:icon="@drawable/ic_handshake"
          android:title="Partnerevent" />
          
    <item android:id="@+id/nav_friendship_bar"
          android:icon="@drawable/ic_heart"
          android:title="Freundschaftsbalken" />
          
    <item android:id="@+id/nav_customers"
          android:icon="@drawable/ic_people"
          android:title="Kunden" />
          
    <item android:id="@+id/nav_settings"
          android:icon="@drawable/ic_settings"
          android:title="Einstellungen" />
</menu>
```

### AccountListFragment (Startseite)

```java
// AccountListFragment.java
public class AccountListFragment extends Fragment {
    
    // IDENTISCH zu bisheriger AccountListActivity
    // PLUS: FAB für "Account sichern"
    
    private RecyclerView rvAccounts;
    private AccountListAdapter adapter;
    private AccountRepository repository;
    private FloatingActionButton fabBackup;
    
    @Override
    public View onCreateView(...) {
        View view = inflater.inflate(R.layout.fragment_account_list, ...);
        
        // Setup RecyclerView
        rvAccounts = view.findViewById(R.id.rv_accounts);
        adapter = new AccountListAdapter(this::showAccountOptions);
        rvAccounts.setAdapter(adapter);
        
        // Setup FAB
        fabBackup = view.findViewById(R.id.fab_backup);
        fabBackup.setOnClickListener(v -> showBackupDialog());
        
        loadAccounts();
        return view;
    }
    
    private void showAccountOptions(Account account) {
        // Dialog: Wiederherstellen / Mehr anzeigen
        String[] options = {"Wiederherstellen", "Mehr anzeigen", "Abbrechen"};
        
        new AlertDialog.Builder(requireContext())
            .setTitle(account.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: restoreAccount(account); break;
                    case 1: openAccountDetail(account); break;
                }
            })
            .show();
    }
    
    private void showBackupDialog() {
        // IDENTISCH zu bisheriger AccountManagementActivity.showBackupOwnDialog()
        // → Dialog für Backup mit Interne ID + Notiz
        // → Führt Root-Backup + Supabase-Speicherung durch
    }
    
    private void restoreAccount(Account account) {
        // IDENTISCH zu bisheriger Restore-Logik
        // → Root-Restore + Last-Played Update
    }
    
    private void openAccountDetail(Account account) {
        // Navigate to AccountDetailActivity
        Intent intent = new Intent(requireContext(), AccountDetailActivity.class);
        intent.putExtra("account_id", account.getId());
        startActivity(intent);
    }
}
```

### Layout-Struktur

```
res/layout/
├── activity_main.xml              ← DrawerLayout + Toolbar + Fragment-Container
├── fragment_account_list.xml      ← RecyclerView + FAB (STARTSEITE)
├── fragment_tycoon_racers.xml     ← Event-Liste
├── fragment_customers.xml         ← Kunden-Liste
├── activity_account_detail.xml    ← Vollständige Account-Details (UNVERÄNDERT)
├── activity_event_detail.xml      ← Event mit Teams (UNVERÄNDERT)
└── item_account.xml               ← Account List Item (UNVERÄNDERT)
```

---

## 🛠️ TECHNOLOGIE-STACK

### Android
- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 33 (Android 13)
- **Language:** Java 8
- **Build System:** Gradle 8.x

### Core Dependencies

```gradle
dependencies {
    // Android Core
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.1'
    implementation 'androidx.drawerlayout:drawerlayout:1.1.1'
    
    // Root Access (BESTEHEND)
    implementation 'com.github.topjohnwu.libsu:core:5.0.1'
    
    // Network & JSON (BESTEHEND)
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'com.opencsv:opencsv:5.7.1'
    
    // Supabase (NEU)
    implementation 'io.github.jan-tennert.supabase:postgrest-kt:2.0.4'
    implementation 'io.github.jan-tennert.supabase:realtime-kt:2.0.4'
    implementation 'io.ktor:ktor-client-android:2.3.5'
    
    // Google Play Services (NEU - für GAID)
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
    
    // Coroutines (NEU - für Async)
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
}
```

### Root-Zugriff Voraussetzungen
- Device muss gerootet sein (Magisk oder SuperSU)
- MonopolyGo muss installiert sein
- Package: `com.scopely.monopolygo`
- Data Path: `/data/data/com.scopely.monopolygo/`

---

## 🎨 DESIGN-SYSTEM

### Farbpalette

```xml
<!-- res/values/colors.xml -->
<resources>
    <!-- Primary -->
    <color name="primary_blue">#3B82F6</color>
    <color name="primary_blue_dark">#1976D2</color>
    
    <!-- Background -->
    <color name="header_dark">#1E252B</color>
    <color name="background_light">#E9EEF2</color>
    <color name="card_background">#FFFFFF</color>
    <color name="card_blue_light">#DBEAFE</color>
    <color name="status_box_bg">#F8FAFC</color>
    
    <!-- Text -->
    <color name="text_dark">#1E252B</color>
    <color name="text_gray">#64748B</color>
    <color name="text_light_gray">#AAAAAA</color>
    <color name="text_white">#FFFFFF</color>
    
    <!-- Status -->
    <color name="error_red">#EF4444</color>
    <color name="success_green">#10B981</color>
    <color name="warning_yellow">#F59E0B</color>
    
    <!-- Border -->
    <color name="border_light">#E2E8F0</color>
    <color name="border_gray">#DDDDDD</color>
</resources>
```

### Typography

```xml
<!-- res/values/styles.xml -->
<resources>
    <!-- Headers -->
    <style name="HeaderText">
        <item name="android:textSize">24sp</item>
        <item name="android:textColor">@color/text_white</item>
        <item name="android:fontFamily">sans-serif-medium</item>
    </style>
    
    <style name="SubHeaderText">
        <item name="android:textSize">20sp</item>
        <item name="android:textColor">@color/text_dark</item>
        <item name="android:fontFamily">sans-serif-medium</item>
    </style>
    
    <!-- Body -->
    <style name="BodyText">
        <item name="android:textSize">14sp</item>
        <item name="android:textColor">@color/text_dark</item>
    </style>
    
    <!-- Labels -->
    <style name="LabelText">
        <item name="android:textSize">12sp</item>
        <item name="android:textColor">@color/text_gray</item>
        <item name="android:textAllCaps">true</item>
        <item name="android:letterSpacing">0.1</item>
    </style>
</resources>
```

### Button Styles

```xml
<style name="BabixButton" parent="Widget.MaterialComponents.Button">
    <item name="cornerRadius">12dp</item>
    <item name="android:paddingTop">14dp</item>
    <item name="android:paddingBottom">14dp</item>
    <item name="android:textSize">14sp</item>
    <item name="android:fontFamily">sans-serif-medium</item>
    <item name="android:elevation">4dp</item>
</style>

<style name="BabixButton.Blue">
    <item name="backgroundTint">@color/primary_blue</item>
    <item name="android:textColor">@color/text_white</item>
</style>

<style name="BabixButton.Gray">
    <item name="backgroundTint">@color/text_gray</item>
    <item name="android:textColor">@color/text_white</item>
</style>

<style name="BabixButton.Red">
    <item name="backgroundTint">@color/error_red</item>
    <item name="android:textColor">@color/text_white</item>
</style>

<style name="BabixButton.Green">
    <item name="backgroundTint">@color/success_green</item>
    <item name="android:textColor">@color/text_white</item>
</style>
```

### Card Style

```xml
<style name="BabixCard" parent="Widget.MaterialComponents.CardView">
    <item name="cardCornerRadius">15dp</item>
    <item name="cardElevation">4dp</item>
    <item name="cardBackgroundColor">@color/card_background</item>
    <item name="contentPadding">16dp</item>
</style>
```

---

## 💾 DATENMODELLE

### Account Model

```java
public class Account {
    // IDs
    private long id;
    private String name;              // Eindeutiger Account-Name
    private String userId;            // MonopolyGo User-ID (extrahiert)
    
    // Links
    private String shortLink;         // https://go.babixgo.de/xxx
    private String friendLink;        // monopolygo://add-friend/xxx
    private String friendCode;        // XXX-123-YYZ
    
    // Status
    private String accountStatus;     // 'active', 'suspended', 'banned', 'inactive'
    
    // Suspension Tracking
    private int suspension0Days;      // Anzahl 0-Tage Sperren
    private int suspension3Days;      // Anzahl 3-Tage Sperren
    private int suspension7Days;      // Anzahl 7-Tage Sperren
    private boolean suspensionPermanent; // Permanent gesperrt
    private int suspensionCount;      // Gesamt (berechnet)
    
    // Device IDs
    private String ssaid;             // Android SSAID (via Root)
    private String gaid;              // Google Advertising ID
    private String deviceId;          // Android Device ID
    
    // Flags
    private boolean isSuspended;      // Aktuell gesperrt (berechnet)
    private boolean hasError;         // Fehler-Flag
    
    // Metadata
    private String note;              // Notizen
    private String lastPlayed;        // Letztes Spielen (ISO DateTime)
    private String createdAt;
    private String updatedAt;
    private String deletedAt;         // Soft-Delete
    
    // Helper Methods
    public String getSuspensionSummary() {
        return suspension0Days + " " + suspension3Days + " " + 
               suspension7Days + " " + (suspensionPermanent ? "X" : "-");
    }
    
    public String getErrorStatusText() {
        return hasError ? "ja" : "nein";
    }
}
```

### Event Model (Tycoon Racers)

```java
public class Event {
    private long id;
    private String name;              // "TR-001"
    private String displayName;       // "Tycoon Racers Januar 2026"
    private String description;
    private String startDate;         // "2026-01-21" (ISO Date)
    private String endDate;           // "2026-01-25"
    private String eventStatus;       // 'planned', 'active', 'completed', 'cancelled'
    private int totalTeams;           // Berechnet
    private int totalCustomers;       // Berechnet
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
```

### Team Model

```java
public class Team {
    private long id;
    private long eventId;             // Zugehöriges Event
    private Long customerId;          // Zugewiesener Kunde (optional)
    private String name;              // "Team 1"
    
    // Slot Assignments (4 Accounts)
    private Long slot1AccountId;
    private Long slot2AccountId;
    private Long slot3AccountId;
    private Long slot4AccountId;
    
    // Slot Names (für schnelle Anzeige)
    private String slot1Name;
    private String slot2Name;
    private String slot3Name;
    private String slot4Name;
    
    // Completion Status
    private boolean isComplete;       // Alle 4 Slots belegt (berechnet)
    
    private String createdAt;
    private String updatedAt;
}
```

### Customer Model

```java
public class Customer {
    private long id;
    private String name;              // "Ines"
    private String accountName;       // MonopolyGo Account-Name
    private String username;          // Login-Name (optional)
    private String password;          // Passwort (verschlüsselt)
    private String autok;             // Autok-Daten
    private String userId;            // MonopolyGo User-ID
    private String friendLink;        // monopolygo://add-friend/xxx
    private String friendCode;        // XXX-123
    
    // Slot Management
    private int totalSlots;           // Standard: 4
    private int usedSlots;            // Aktuell belegt
    private int remainingSlots;       // Berechnet: total - used
    
    private String note;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
```

---

## 🔧 CORE-FUNKTIONALITÄTEN

### 1. Account-Backup (Root + Supabase)

```java
/**
 * WORKFLOW:
 * 1. Root-Backup: Dateien kopieren (BESTEHEND)
 * 2. UserID extrahieren (BESTEHEND)
 * 3. Device-IDs extrahieren (NEU)
 * 4. Supabase speichern (NEU)
 */
public void backupAccount(String accountName, String note) {
    new Thread(() -> {
        // 1. Root-Backup (UNVERÄNDERT)
        boolean fileBackupSuccess = AccountManager.backupAccount(
            AccountManager.getAccountsEigenePath(), 
            accountName
        );
        
        if (!fileBackupSuccess) {
            showError("File-Backup fehlgeschlagen");
            return;
        }
        
        // 2. UserID extrahieren (UNVERÄNDERT)
        String userId = DataExtractor.extractUserId();
        
        // 3. Device-IDs extrahieren (NEU)
        DeviceIdExtractor.extractAllIds(context)
            .thenAccept(deviceIds -> {
                // 4. Supabase speichern (NEU)
                Account account = new Account(accountName, userId);
                account.setSsaid(deviceIds.ssaid);
                account.setGaid(deviceIds.gaid);
                account.setDeviceId(deviceIds.deviceId);
                account.setNote(note);
                
                if (userId != null) {
                    String shortLink = ShortLinkManager.createShortLink(userId, accountName);
                    account.setShortLink(shortLink);
                    account.setFriendLink("monopolygo://add-friend/" + userId);
                }
                
                repository.createAccount(account)
                    .thenRun(() -> showSuccess("Backup komplett"))
                    .exceptionally(e -> {
                        showError("Supabase-Fehler: " + e.getMessage());
                        return null;
                    });
            });
    }).start();
}
```

### 2. Account-Restore (Root + Supabase Update)

```java
/**
 * WORKFLOW:
 * 1. MonopolyGo Force-Stop
 * 2. Root-Restore: Dateien zurückkopieren (BESTEHEND)
 * 3. Last-Played Update in Supabase (NEU)
 */
public void restoreAccount(Account account) {
    new Thread(() -> {
        // 1. Force Stop
        AccountManager.forceStopApp();
        Thread.sleep(1000);
        
        // 2. Root-Restore (UNVERÄNDERT)
        String sourceFile = AccountManager.getAccountsEigenePath() + 
                           account.getName() + "/WithBuddies.Services.User.0Production.dat";
        boolean success = AccountManager.restoreAccount(sourceFile);
        
        if (!success) {
            showError("Restore fehlgeschlagen");
            return;
        }
        
        // 3. Last-Played Update (NEU)
        repository.updateLastPlayed(account.getId())
            .thenRun(() -> showSuccess("Account wiederhergestellt"))
            .exceptionally(e -> {
                showError("Supabase-Update fehlgeschlagen: " + e.getMessage());
                return null;
            });
    }).start();
}
```

### 3. Device-ID Extraktion

```java
/**
 * Extrahiert alle Device-IDs für Account-Tracking
 */
public class DeviceIdExtractor {
    
    public static class DeviceIds {
        public String ssaid;  // Via Root aus MonopolyGo SharedPrefs
        public String gaid;   // Via Google Play Services
        public String deviceId; // Android Secure ID
    }
    
    /**
     * SSAID aus MonopolyGo App-Daten (Root erforderlich)
     */
    public static String extractSSAID() {
        String findCommand = "find /data/data/com.scopely.monopolygo/shared_prefs/ -name '*.xml' -type f";
        String files = RootManager.runRootCommand(findCommand);
        
        for (String file : files.split("\n")) {
            String content = RootManager.runRootCommand("cat \"" + file.trim() + "\"");
            String ssaid = extractValueFromXml(content, "android_id");
            if (ssaid != null) return ssaid;
        }
        return null;
    }
    
    /**
     * GAID via Google Play Services (async)
     */
    public static CompletableFuture<String> extractGAID(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AdvertisingIdClient.Info adInfo = 
                    AdvertisingIdClient.getAdvertisingIdInfo(context);
                return adInfo.getId();
            } catch (Exception e) {
                return null;
            }
        });
    }
    
    /**
     * Device ID via Android Settings
     */
    public static String extractDeviceId(Context context) {
        return Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ANDROID_ID
        );
    }
}
```

### 4. Event Execution (Automatisch)

```java
/**
 * Führt Tycoon Racers Event automatisch aus:
 * - Für jedes Team
 * - Für jeden belegten Slot (1-4)
 * - Restore → Start → Links öffnen
 */
public class EventExecutor {
    
    public void executeEvent(long eventId) {
        teamRepository.getTeamsByEventId(eventId)
            .thenAccept(teams -> {
                for (Team team : teams) {
                    executeTeam(team);
                }
            });
    }
    
    private void executeTeam(Team team) {
        // Get Customer
        Customer customer = customerRepository.getCustomerById(team.getCustomerId()).get();
        
        // Process each slot
        processSlot(team.getSlot1AccountId(), customer);
        processSlot(team.getSlot2AccountId(), customer);
        processSlot(team.getSlot3AccountId(), customer);
        processSlot(team.getSlot4AccountId(), customer);
    }
    
    private void processSlot(Long accountId, Customer customer) throws Exception {
        if (accountId == null) return;
        
        Account account = accountRepository.getAccountById(accountId).get();
        
        // 1. Force Stop
        AccountManager.forceStopApp();
        Thread.sleep(1000);
        
        // 2. Restore Account
        String sourceFile = AccountManager.getAccountsEigenePath() + 
                           account.getName() + "/WithBuddies.Services.User.0Production.dat";
        AccountManager.restoreAccount(sourceFile);
        
        // 3. Start MonopolyGo
        AccountManager.startApp();
        Thread.sleep(10000); // Wait 10 seconds
        
        // 4. Open Friend Link
        AccountManager.openFriendLink(customer.getUserId());
        Thread.sleep(2000);
    }
}
```

---

## 📝 CODE-KONVENTIONEN

### Package-Struktur

```
de.babixgo.monopolygo/
├── activities/           ← Activities (MainActivity, AccountDetailActivity)
├── fragments/           ← Fragments (AccountListFragment, etc.)
├── adapters/            ← RecyclerView Adapters
├── models/              ← Data Models (Account, Event, Team, Customer)
├── database/            ← Repositories (AccountRepository, EventRepository)
├── utils/               ← Utility Classes (DeviceIdExtractor, EventExecutor)
├── RootManager.java     ← Root-Zugriff (BESTEHEND - NICHT ÄNDERN)
├── AccountManager.java  ← Account Operations (BESTEHEND - NICHT ÄNDERN)
└── DataExtractor.java   ← Data Extraction (BESTEHEND - NICHT ÄNDERN)
```

### Naming Conventions

```java
// Classes
public class AccountListAdapter { }
public class AccountRepository { }

// Methods
private void loadAccounts() { }
private void showBackupDialog() { }
public CompletableFuture<Account> getAccountById(long id) { }

// Variables
private RecyclerView rvAccounts;
private AccountRepository repository;
private TextView tvAccountName;
private Button btnRestore;

// Constants
private static final String TAG = "AccountListFragment";
private static final int REQUEST_CODE_BACKUP = 1001;
```

### Error Handling Pattern

```java
repository.getAllAccounts()
    .thenAccept(accounts -> runOnUiThread(() -> {
        adapter.setAccounts(accounts);
    }))
    .exceptionally(throwable -> {
        runOnUiThread(() -> {
            Log.e(TAG, "Failed to load accounts", throwable);
            Toast.makeText(context, 
                "Fehler beim Laden: " + throwable.getMessage(), 
                Toast.LENGTH_LONG).show();
        });
        return null;
    });
```

### Threading Pattern

```java
// Background work
new Thread(() -> {
    // Root operations (blocking)
    boolean success = AccountManager.backupAccount(...);
    
    runOnUiThread(() -> {
        // UI updates
        if (success) {
            Toast.makeText(this, "Erfolg", Toast.LENGTH_SHORT).show();
        }
    });
}).start();

// Async operations
CompletableFuture.supplyAsync(() -> {
    // Background work
    return result;
}).thenAccept(result -> runOnUiThread(() -> {
    // UI updates
}));
```

---

## 🐛 DEBUGGING & LOGGING

### Logging Pattern

```java
public class AccountListFragment extends Fragment {
    private static final String TAG = "AccountListFragment";
    
    private void loadAccounts() {
        Log.d(TAG, "Loading accounts from Supabase");
        
        repository.getAllAccounts()
            .thenAccept(accounts -> {
                Log.d(TAG, "Loaded " + accounts.size() + " accounts");
                runOnUiThread(() -> adapter.setAccounts(accounts));
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Failed to load accounts", throwable);
                return null;
            });
    }
}
```

### Root-Operation Logging

```java
// In RootManager.java (BESTEHEND - nur als Referenz)
public static String runRootCommand(String command) {
    Log.d(TAG, "Executing root command: " + command);
    
    try {
        String result = Shell.cmd(command).exec().getOut().toString();
        Log.d(TAG, "Command result: " + result);
        return result;
    } catch (Exception e) {
        Log.e(TAG, "Root command failed", e);
        return "Error: " + e.getMessage();
    }
}
```

### Logcat Filter

```bash
# Filter für babixGO
adb logcat | grep -E "(AccountList|AccountDetail|EventExecutor|RootManager)"

# Nur Errors
adb logcat | grep -E "E/(AccountList|RootManager)"

# Supabase Requests
adb logcat | grep -E "Supabase|Repository"
```

---

## 🚀 DEPLOYMENT

### Build Variants

```gradle
android {
    buildTypes {
        debug {
            applicationIdSuffix ".debug"
            debuggable true
            minifyEnabled false
        }
        
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 
                         'proguard-rules.pro'
        }
    }
}
```

### ProGuard Rules

```proguard
# Keep Root Manager classes
-keep class com.topjohnwu.superuser.** { *; }

# Keep Supabase classes
-keep class io.github.jan.supabase.** { *; }

# Keep Models
-keep class de.babixgo.monopolygo.models.** { *; }

# Keep Root Implementation
-keep class de.babixgo.monopolygo.RootManager { *; }
-keep class de.babixgo.monopolygo.AccountManager { *; }
-keep class de.babixgo.monopolygo.DataExtractor { *; }
```

### APK Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (signiert)
./gradlew assembleRelease

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Version Management

```gradle
android {
    defaultConfig {
        versionCode 2
        versionName "2.0.0"  // Navigation Restructure
    }
}
```

**Version History:**
- **1.0.0** - Initial Release (AccountManagementActivity)
- **1.1.0** - Supabase Integration
- **2.0.0** - Navigation Restructure (AccountList als Startseite)

---

## ✅ QUICK REFERENCE

### Was Agents MÜSSEN wissen:

1. **Root-Code NIEMALS ändern** (`RootManager.java`, `AccountManager.java`, `DataExtractor.java`)
2. **Accountliste = Startseite** (nicht mehr AccountManagementActivity)
3. **DrawerLayout Navigation** mit Hamburger-Menü
4. **Supabase = Metadaten** (nicht File-Backup)
5. **Material Design** aus Screenshots

### Was Agents tun sollten:

- ✅ Neue Features als **eigene Klassen** implementieren
- ✅ Root-Code **nutzen, aber nicht ändern**
- ✅ Async/Await Pattern für Supabase
- ✅ Error Handling mit Exceptions
- ✅ Logging für Debugging
- ✅ UI Updates auf UI Thread

### Was Agents NICHT tun sollten:

- ❌ Root-Implementierung anfassen
- ❌ AccountManagementActivity als Hauptbildschirm
- ❌ Sync Blocking Operations auf UI Thread
- ❌ Hardcoded Credentials
- ❌ Fehlende Error Handling

---

## 📞 SUPPORT

Bei Problemen:
1. Prüfe Logcat: `adb logcat | grep babixgo`
2. Prüfe Root-Zugriff: `RootManager.checkRootAccess()`
3. Prüfe Supabase Connection: Test mit `SupabaseManager.getClient()`
4. Referenziere diese Dokumentation

**Letzte Aktualisierung:** 21. Januar 2026  
**Agent Version:** 2.0.0 (Navigation Restructure)