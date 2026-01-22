# GitHub Copilot Agent Prompt: MonopolyGo Backup/Restore Android App

## Ziel
Erstelle eine native Android-App, die die exakte Backup- und Restore-Logik aus den vorhandenen Shell-Skripten (`backup_account.sh`, `restore_account.sh`, `list_accounts.sh`) implementiert. Die App soll CSV-basiert arbeiten und Root-Zugriff nutzen.

## Basis-Referenzen
Analysiere zuerst diese Dateien als Referenz für die Implementierung:
- `backup_account.sh` - Backup-Logik mit ID-Extraktion
- `restore_account.sh` - Restore-Logik mit Berechtigungen
- `list_accounts.sh` - Account-Listung
- `export_account_csv.sh` - CSV-Export
- `IMPLEMENTATION_BACKUP_RESTORE.md` - Technische Details
- Vorhandene App-Struktur in `app/src/main/java/de/babixgo/monopolygo/`

## Technische Anforderungen

### 1. Projekt-Setup
- **Sprache**: Kotlin (bevorzugt) oder Java
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle
- **Paketname**: `de.babixgo.monopolygo`
- **Root Library**: Nutze vorhandenes `RootManager.java` oder erweitere es

### 2. App-Architektur
```
app/src/main/java/de/babixgo/monopolygo/
├── activities/
│   └── MainActivity.kt
├── fragments/
│   ├── BackupFragment.kt
│   ├── RestoreFragment.kt
│   └── AccountListFragment.kt
├── models/
│   └── BackupAccount.kt
├── utils/
│   ├── RootManager.kt
│   ├── CsvManager.kt
│   ├── BackupManager.kt
│   └── IdExtractor.kt
└── adapters/
    └── AccountAdapter.kt
```

### 3. Datenmodell

#### BackupAccount.kt
```kotlin
data class BackupAccount(
    val accountName: String,
    val userId: String,
    val gaid: String,
    val deviceToken: String,
    val appSetId: String,
    val ssaid: String,
    val backupPath: String,
    val datum: String,
    val zuletztGespielt: String,
    val notiz: String
)
```

#### CSV-Struktur (genau wie in Shell-Skripten)
```
AccountName,UserID,GAID,DeviceToken,AppSetID,SSAID,BackupPfad,Datum,ZuletztGespielt,Notiz
```

**CSV-Pfad**: `/storage/emulated/0/MonopolyGo/Backups/accounts.csv`

## Kernfunktionen (exakte Shell-Skript-Logik)

### Funktion 1: Backup erstellen

#### Workflow (aus `backup_account.sh`)
1. **Eingabe-Dialog**: Account-Name (Pflicht), Notiz (optional)
2. **Root-Prüfung**: Prüfe Root-Zugriff, breche ab falls nicht vorhanden
3. **App stoppen**: `am force-stop com.scopely.monopolygo`
4. **Warte**: 1 Sekunde Pause
5. **Backup-Verzeichnis erstellen**:
   - Format: `/storage/emulated/0/MonopolyGo/Backups/{accountName}_{timestamp}/`
   - Timestamp: `yyyyMMdd_HHmmss`
6. **Dateien kopieren** (mit Root):
   ```
   /data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory/
   → {backup_dir}/DiskBasedCacheDirectory/
   
   /data/data/com.scopely.monopolygo/shared_prefs/
   → {backup_dir}/shared_prefs/
   
   /data/system/users/0/settings_ssaid.xml
   → {backup_dir}/settings_ssaid.xml
   ```

7. **IDs extrahieren** aus `shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml`:

   **User ID** (probiere diese Keys in Reihenfolge):
   ```xml
   <string name="Scopely.Attribution.UserId">...</string>
   <string name="ScopelyProfile.UserId">...</string>
   <string name="Scopely.UserId">...</string>
   <string name="UserId">...</string>
   <int name="UserId" value="..."/>
   ```

   **GAID**:
   ```xml
   <string name="Scopely.Attribution.GoogleAdvertisingId">...</string>
   ```

   **Device Token**:
   ```xml
   <string name="Scopely.DeviceToken">...</string>
   ```

   **App Set ID**:
   ```xml
   <string name="Scopely.AppSetId">...</string>
   ```

   **SSAID** aus `settings_ssaid.xml`:
   - Regex: `com\.scopely\.monopolygo[^/]*/[^/]*/[^/]*/([0-9a-f]{16})`
   - Extrahiere ersten Match (16-stelliger Hex-Wert)

8. **Duplikat-Prüfung**: 
   - Prüfe ob User ID bereits in CSV existiert
   - Bei Duplikat: Bestätigungsdialog anzeigen

9. **CSV-Zeile hinzufügen**:
   ```
   "AccountName","UserID","GAID","DeviceToken","AppSetID","SSAID","BackupPath","Datum","ZuletztGespielt","Notiz"
   ```
   - Datum: `yyyy-MM-dd`
   - ZuletztGespielt: `yyyy-MM-dd HH:mm:ss`

10. **Success-Dialog**: Zeige Zusammenfassung mit User ID und Pfad

### Funktion 2: Restore durchführen

#### Workflow (aus `restore_account.sh`)
1. **CSV lesen**: Lade alle Accounts aus `accounts.csv`
2. **Account-Liste anzeigen**: RecyclerView mit:
   - Account-Name
   - User ID
   - Datum
   - Zuletzt gespielt
   - Status-Icon (✓ wenn Backup-Pfad existiert, ✗ wenn nicht)
   - Notiz (optional)

3. **Account auswählen**: User klickt auf Account
4. **Backup-Validierung**:
   - Prüfe ob Backup-Verzeichnis existiert
   - Prüfe ob `shared_prefs/` und `DiskBasedCacheDirectory/` vorhanden
   - Bei Fehler: Fehlermeldung und Abbruch

5. **Sicherheits-Dialog**:
   ```
   WARNUNG: Dieser Vorgang überschreibt den aktuellen Spielstand!
   Möchten Sie fortfahren?
   [Abbrechen] [Fortfahren]
   ```

6. **App stoppen**: `am force-stop com.scopely.monopolygo`
7. **Warte**: 1 Sekunde Pause

8. **Dateien wiederherstellen** (mit Root):
   ```bash
   # Alte Daten löschen
   rm -rf /data/data/com.scopely.monopolygo/shared_prefs/*
   rm -rf /data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory/*
   
   # Backup zurückkopieren
   cp -r {backup_dir}/shared_prefs/* /data/data/com.scopely.monopolygo/shared_prefs/
   cp -r {backup_dir}/DiskBasedCacheDirectory/* /data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory/
   ```

9. **Berechtigungen setzen** (kritisch!):
   ```bash
   chmod 660 /data/data/com.scopely.monopolygo/shared_prefs/*
   chmod -R 771 /data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory
   ```

10. **Owner setzen**:
    ```bash
    # Ermittle App UID (z.B. u0_a123)
    stat -c '%U' /data/data/com.scopely.monopolygo
    
    # Setze Owner
    chown -R {app_uid}:{app_uid} /data/data/com.scopely.monopolygo/shared_prefs
    chown -R {app_uid}:{app_uid} /data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory
    ```

11. **CSV aktualisieren**:
    - Setze `ZuletztGespielt` auf aktuelle Zeit (`yyyy-MM-dd HH:mm:ss`)

12. **App-Start-Dialog**:
    ```
    Restore abgeschlossen!
    MonopolyGo jetzt starten?
    [Nein] [Ja]
    ```
    - Bei Ja: `am start -n com.scopely.monopolygo/com.scopely.monopolygo.MainActivity`

### Funktion 3: Account-Liste anzeigen

#### UI-Komponenten (aus `list_accounts.sh`)
- **RecyclerView** mit CardView-Items
- **Jedes Item zeigt**:
  - Nummer (fortlaufend)
  - Account-Name (fett)
  - User ID (klein, grau)
  - Datum (erstellt)
  - Zuletzt gespielt (klein)
  - Status-Icon: ✓ (grün) oder ✗ (rot)
  - Notiz (falls vorhanden, kursiv)

- **Long-Click Menü**:
  - Restore
  - Details anzeigen
  - Löschen (mit Bestätigung)
  - In Zwischenablage kopieren (User ID)

- **Floating Action Button**: "+" für neues Backup

### Funktion 4: CSV-Export

#### Export-Funktion (aus `export_account_csv.sh`)
- **Export-Pfad**: `/storage/emulated/0/MonopolyGo/Backups/accounts_export_{timestamp}.txt`
- **Format**:
  ```
  =========================================
  MonopolyGo Accounts Export
  Datum: yyyy-MM-dd HH:mm:ss
  =========================================
  
  [1] AccountName
    User ID:          12345
    GAID:             xxxx-xxxx-xxxx
    Device Token:     xxxxx
    App Set ID:       xxxxx
    SSAID:            xxxxxxxxxxxxxxxx
    Backup-Pfad:      /path/to/backup
    Erstellt am:      2026-01-22
    Zuletzt gespielt: 2026-01-22 23:00:00
    Notiz:            Meine Notiz
  
  [2] ...
  ```

- **Share-Funktion**: Nach Export Sharing-Dialog anzeigen
- **Toast**: "Export abgeschlossen: {filename}"

## UI/UX-Anforderungen

### MainActivity
- **Navigation**: Bottom Navigation oder Tabs
  - Tab 1: "Backups" (BackupFragment)
  - Tab 2: "Restore" (RestoreFragment) 
  - Tab 3: "Liste" (AccountListFragment)

- **Toolbar**: 
  - Titel: "MonopolyGo Manager"
  - Menü-Items:
    - "Export" (CSV-Export)
    - "Einstellungen"
    - "Über"

### BackupFragment
```
┌─────────────────────────────────────┐
│ Neues Backup erstellen              │
├─────────────────────────────────────┤
│                                     │
│ Account-Name *                      │
│ ┌─────────────────────────────────┐ │
│ │ [Input-Feld]                    │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Notiz (optional)                    │
│ ┌─────────────────────────────────┐ │
│ │ [Mehrzeiliges Textfeld]         │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ℹ️ Root-Zugriff erforderlich        │
│                                     │
│ [        Backup erstellen        ]  │
│                                     │
└─────────────────────────────────────┘
```

### RestoreFragment / AccountListFragment
```
┌─────────────────────────────────────┐
│ 🔍 [Suche...]                       │
├─────────────────────────────────────┤
│ ╔═════════════════════════════════╗ │
│ ║ TestAccount ✓                   ║ │
│ ║ User ID: 123456789              ║ │
│ ║ 2026-01-20 | Zuletzt: 23:00     ║ │
│ ╚═════════════════════════════════╝ │
│                                     │
│ ╔═════════════════════════════════╗ │
│ ║ MeinAccount ✗                   ║ │
│ ║ User ID: 987654321              ║ │
│ ║ 2026-01-15 | Zuletzt: 12:30     ║ │
│ ║ Notiz: Hauptaccount             ║ │
│ ╚═════════════════════════════════╝ │
│                                     │
│                              [+]    │
└─────────────────────────────────────┘
```

### Dialoge

#### Backup-Fortschritt
```
Backup wird erstellt...

[███████░░░] 70%

Kopiere Dateien...
```

#### Restore-Warnung
```
⚠️ WARNUNG

Dieser Vorgang überschreibt den 
aktuellen Spielstand!

Account: TestAccount
User ID: 123456789

[Abbrechen]  [Fortfahren]
```

#### Erfolgs-Dialog (Backup)
```
✓ Backup erfolgreich!

Account: TestAccount
User ID: 123456789
Pfad: /storage/.../TestAccount_20260122_230000

[Details]  [OK]
```

## Technische Implementierungsdetails

### RootManager-Erweiterungen
Implementiere diese Methoden in `RootManager.kt`:

```kotlin
object RootManager {
    // Prüfe Root-Zugriff
    fun hasRootAccess(): Boolean
    
    // Stoppe App
    fun forceStopApp(packageName: String): Boolean
    
    // Kopiere Verzeichnis rekursiv
    fun copyDirectory(source: String, dest: String): Boolean
    
    // Kopiere Datei
    fun copyFile(source: String, dest: String): Boolean
    
    // Lese Datei-Inhalt
    fun readFile(filePath: String): String?
    
    // Setze Berechtigungen
    fun setPermissions(path: String, permissions: String): Boolean
    
    // Setze Owner
    fun setOwner(path: String, owner: String): Boolean
    
    // Ermittle App-UID
    fun getAppUid(packageName: String): String?
    
    // Lösche Verzeichnis
    fun deleteDirectory(path: String): Boolean
}
```

### IdExtractor-Implementierung

```kotlin
object IdExtractor {
    private val USER_ID_KEYS = listOf(
        "Scopely.Attribution.UserId",
        "ScopelyProfile.UserId",
        "Scopely.UserId",
        "UserId",
        "user_id",
        "PlayerId"
    )
    
    fun extractUserId(xmlContent: String): String? {
        // Probiere String-Format
        for (key in USER_ID_KEYS) {
            val regex = """<string name="$key">([^<]+)</string>""".toRegex()
            val match = regex.find(xmlContent)
            if (match != null) return match.groupValues[1]
        }
        
        // Probiere Int-Format
        for (key in USER_ID_KEYS) {
            val regex = """<int name="$key" value="([^"]+)"""".toRegex()
            val match = regex.find(xmlContent)
            if (match != null) return match.groupValues[1]
        }
        
        return null
    }
    
    fun extractGaid(xmlContent: String): String? {
        val regex = """<string name="Scopely\.Attribution\.GoogleAdvertisingId">([^<]+)</string>""".toRegex()
        return regex.find(xmlContent)?.groupValues?.get(1)
    }
    
    fun extractDeviceToken(xmlContent: String): String? {
        val regex = """<string name="Scopely\.DeviceToken">([^<]+)</string>""".toRegex()
        return regex.find(xmlContent)?.groupValues?.get(1)
    }
    
    fun extractAppSetId(xmlContent: String): String? {
        val regex = """<string name="Scopely\.AppSetId">([^<]+)</string>""".toRegex()
        return regex.find(xmlContent)?.groupValues?.get(1)
    }
    
    fun extractSsaid(xmlContent: String): String? {
        val regex = """com\.scopely\.monopolygo[^/]*/[^/]*/[^/]*/([0-9a-f]{16})""".toRegex()
        return regex.find(xmlContent)?.groupValues?.get(1)
    }
}
```

### CsvManager-Implementierung

```kotlin
object CsvManager {
    private const val CSV_HEADER = "AccountName,UserID,GAID,DeviceToken,AppSetID,SSAID,BackupPfad,Datum,ZuletztGespielt,Notiz"
    private const val CSV_PATH = "/storage/emulated/0/MonopolyGo/Backups/accounts.csv"
    
    fun initCsv() {
        val file = File(CSV_PATH)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText("$CSV_HEADER\n")
        }
    }
    
    fun addAccount(account: BackupAccount) {
        val line = buildCsvLine(account)
        File(CSV_PATH).appendText("$line\n")
    }
    
    fun getAllAccounts(): List<BackupAccount> {
        val file = File(CSV_PATH)
        if (!file.exists()) return emptyList()
        
        return file.readLines()
            .drop(1) // Skip header
            .filter { it.isNotBlank() }
            .map { parseCsvLine(it) }
    }
    
    fun updateAccount(oldAccount: BackupAccount, newAccount: BackupAccount) {
        val accounts = getAllAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.userId == oldAccount.userId }
        if (index >= 0) {
            accounts[index] = newAccount
            saveAllAccounts(accounts)
        }
    }
    
    fun deleteAccount(account: BackupAccount) {
        val accounts = getAllAccounts().filter { it.userId != account.userId }
        saveAllAccounts(accounts)
    }
    
    private fun buildCsvLine(account: BackupAccount): String {
        return listOf(
            account.accountName,
            account.userId,
            account.gaid,
            account.deviceToken,
            account.appSetId,
            account.ssaid,
            account.backupPath,
            account.datum,
            account.zuletztGespielt,
            account.notiz
        ).joinToString(",") { "\"$it\"" }
    }
    
    private fun parseCsvLine(line: String): BackupAccount {
        // CSV-Parsing mit Berücksichtigung von Anführungszeichen
        val values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            .map { it.trim().removeSurrounding("\"") }
        
        return BackupAccount(
            accountName = values[0],
            userId = values[1],
            gaid = values[2],
            deviceToken = values[3],
            appSetId = values[4],
            ssaid = values[5],
            backupPath = values[6],
            datum = values[7],
            zuletztGespielt = values[8],
            notiz = values.getOrElse(9) { "" }
        )
    }
    
    private fun saveAllAccounts(accounts: List<BackupAccount>) {
        val file = File(CSV_PATH)
        file.writeText("$CSV_HEADER\n")
        accounts.forEach { account ->
            file.appendText("${buildCsvLine(account)}\n")
        }
    }
}
```

### BackupManager-Implementierung

```kotlin
class BackupManager(private val context: Context) {
    private val monopolygoPackage = "com.scopely.monopolygo"
    private val monopolygoData = "/data/data/$monopolygoPackage"
    private val backupBase = "/storage/emulated/0/MonopolyGo/Backups"
    
    suspend fun createBackup(
        accountName: String,
        notiz: String,
        progressCallback: (String, Int) -> Unit
    ): Result<BackupAccount> = withContext(Dispatchers.IO) {
        try {
            // 1. Root-Prüfung
            if (!RootManager.hasRootAccess()) {
                return@withContext Result.failure(Exception("Root-Zugriff erforderlich"))
            }
            
            progressCallback("Stoppe MonopolyGo...", 10)
            
            // 2. App stoppen
            if (!RootManager.forceStopApp(monopolygoPackage)) {
                return@withContext Result.failure(Exception("Konnte App nicht stoppen"))
            }
            delay(1000)
            
            progressCallback("Erstelle Backup-Verzeichnis...", 20)
            
            // 3. Backup-Verzeichnis
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupDir = "$backupBase/${accountName}_$timestamp"
            File(backupDir).mkdirs()
            
            progressCallback("Kopiere Spielstände...", 30)
            
            // 4. Dateien kopieren
            val success1 = RootManager.copyDirectory(
                "$monopolygoData/files/DiskBasedCacheDirectory",
                "$backupDir/DiskBasedCacheDirectory"
            )
            
            progressCallback("Kopiere Einstellungen...", 50)
            
            val success2 = RootManager.copyDirectory(
                "$monopolygoData/shared_prefs",
                "$backupDir/shared_prefs"
            )
            
            progressCallback("Kopiere System-Dateien...", 70)
            
            val success3 = RootManager.copyFile(
                "/data/system/users/0/settings_ssaid.xml",
                "$backupDir/settings_ssaid.xml"
            )
            
            if (!success1 || !success2) {
                return@withContext Result.failure(Exception("Dateien kopieren fehlgeschlagen"))
            }
            
            progressCallback("Extrahiere IDs...", 80)
            
            // 5. IDs extrahieren
            val prefsFile = "$backupDir/shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml"
            val xmlContent = File(prefsFile).readText()
            
            val userId = IdExtractor.extractUserId(xmlContent) ?: "N/A"
            val gaid = IdExtractor.extractGaid(xmlContent) ?: "N/A"
            val deviceToken = IdExtractor.extractDeviceToken(xmlContent) ?: "N/A"
            val appSetId = IdExtractor.extractAppSetId(xmlContent) ?: "N/A"
            
            val ssaid = if (success3) {
                val ssaidContent = File("$backupDir/settings_ssaid.xml").readText()
                IdExtractor.extractSsaid(ssaidContent) ?: "N/A"
            } else "N/A"
            
            progressCallback("Speichere in Datenbank...", 90)
            
            // 6. CSV speichern
            val datum = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val zuletztGespielt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            val account = BackupAccount(
                accountName = accountName,
                userId = userId,
                gaid = gaid,
                deviceToken = deviceToken,
                appSetId = appSetId,
                ssaid = ssaid,
                backupPath = backupDir,
                datum = datum,
                zuletztGespielt = zuletztGespielt,
                notiz = notiz
            )
            
            // Duplikat-Prüfung
            if (userId != "N/A") {
                val existing = CsvManager.getAllAccounts()
                if (existing.any { it.userId == userId }) {
                    return@withContext Result.failure(Exception("Account mit User ID $userId existiert bereits"))
                }
            }
            
            CsvManager.addAccount(account)
            
            progressCallback("Fertig!", 100)
            
            Result.success(account)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun restoreBackup(
        account: BackupAccount,
        progressCallback: (String, Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Validierung
            val backupDir = File(account.backupPath)
            if (!backupDir.exists()) {
                return@withContext Result.failure(Exception("Backup-Verzeichnis nicht gefunden"))
            }
            
            if (!File("$backupDir/shared_prefs").exists() || 
                !File("$backupDir/DiskBasedCacheDirectory").exists()) {
                return@withContext Result.failure(Exception("Backup ist unvollständig"))
            }
            
            progressCallback("Stoppe MonopolyGo...", 10)
            
            // 2. App stoppen
            if (!RootManager.forceStopApp(monopolygoPackage)) {
                return@withContext Result.failure(Exception("Konnte App nicht stoppen"))
            }
            delay(1000)
            
            progressCallback("Lösche alte Daten...", 20)
            
            // 3. Alte Daten löschen
            RootManager.deleteDirectory("$monopolygoData/shared_prefs")
            RootManager.deleteDirectory("$monopolygoData/files/DiskBasedCacheDirectory")
            
            progressCallback("Stelle Einstellungen wieder her...", 40)
            
            // 4. Dateien zurückkopieren
            val success1 = RootManager.copyDirectory(
                "$backupDir/shared_prefs",
                "$monopolygoData/shared_prefs"
            )
            
            progressCallback("Stelle Spielstände wieder her...", 60)
            
            val success2 = RootManager.copyDirectory(
                "$backupDir/DiskBasedCacheDirectory",
                "$monopolygoData/files/DiskBasedCacheDirectory"
            )
            
            if (!success1 || !success2) {
                return@withContext Result.failure(Exception("Dateien kopieren fehlgeschlagen"))
            }
            
            progressCallback("Setze Berechtigungen...", 80)
            
            // 5. Berechtigungen setzen
            RootManager.setPermissions("$monopolygoData/shared_prefs", "660")
            RootManager.setPermissions("$monopolygoData/files/DiskBasedCacheDirectory", "771")
            
            // 6. Owner setzen
            val appUid = RootManager.getAppUid(monopolygoPackage)
            if (appUid != null) {
                RootManager.setOwner("$monopolygoData/shared_prefs", appUid)
                RootManager.setOwner("$monopolygoData/files/DiskBasedCacheDirectory", appUid)
            }
            
            progressCallback("Aktualisiere Datenbank...", 90)
            
            // 7. CSV aktualisieren
            val updatedAccount = account.copy(
                zuletztGespielt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            CsvManager.updateAccount(account, updatedAccount)
            
            progressCallback("Fertig!", 100)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Berechtigungen (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

## Gradle Dependencies

```gradle
dependencies {
    // Kotlin
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.9.0"
    
    // AndroidX
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.appcompat:appcompat:1.6.1"
    implementation "com.google.android.material:material:1.11.0"
    implementation "androidx.constraintlayout:constraintlayout:2.1.4"
    
    // Lifecycle & ViewModel
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    
    // RecyclerView
    implementation "androidx.recyclerview:recyclerview:1.3.2"
    
    // Root Access (optional - falls externe Library gewünscht)
    // implementation "eu.chainfire:libsuperuser:1.1.0"
}
```

## Acceptance Criteria

Die App ist fertig, wenn folgende Punkte erfüllt sind:

### Backup-Funktion
- [ ] Root-Zugriff wird korrekt geprüft
- [ ] MonopolyGo wird vor Backup gestoppt
- [ ] Alle 3 Dateien/Verzeichnisse werden kopiert
- [ ] User ID wird korrekt extrahiert (alle Fallback-Keys probiert)
- [ ] GAID wird korrekt extrahiert
- [ ] Device Token wird korrekt extrahiert
- [ ] App Set ID wird korrekt extrahiert
- [ ] SSAID wird mit Regex korrekt extrahiert (16-stelliger Hex)
- [ ] Alle IDs werden in CSV gespeichert
- [ ] Duplikat-Prüfung funktioniert (User ID)
- [ ] Progress-Dialog zeigt Fortschritt an
- [ ] Success-Dialog zeigt Zusammenfassung
- [ ] Toast bei Erfolg/Fehler

### Restore-Funktion
- [ ] Account-Liste zeigt alle CSV-Einträge
- [ ] Status-Icon zeigt an ob Backup existiert
- [ ] Backup-Validierung prüft Vollständigkeit
- [ ] Sicherheits-Dialog vor Überschreiben
- [ ] MonopolyGo wird gestoppt
- [ ] Alte Daten werden gelöscht
- [ ] Backup-Dateien werden korrekt zurückkopiert
- [ ] Berechtigungen 660 und 771 werden gesetzt
- [ ] Owner wird korrekt gesetzt
- [ ] CSV wird mit neuer Zeit aktualisiert
- [ ] Progress-Dialog zeigt Fortschritt
- [ ] App-Start-Dialog funktioniert
- [ ] MonopolyGo startet korrekt (bei Ja)

### Account-Liste
- [ ] Alle Accounts aus CSV werden angezeigt
- [ ] Status-Icon (✓/✗) funktioniert
- [ ] Notizen werden angezeigt
- [ ] Search-Funktion filtert Accounts
- [ ] Long-Click-Menü funktioniert
- [ ] User ID kann kopiert werden
- [ ] Löschen mit Bestätigung funktioniert

### CSV-Export
- [ ] Export erstellt Textdatei
- [ ] Alle IDs werden exportiert
- [ ] Format ist lesbar
- [ ] Share-Dialog funktioniert
- [ ] Toast zeigt Dateinamen

### Allgemein
- [ ] App läuft stabil ohne Crashes
- [ ] Error-Handling für alle Root-Operationen
- [ ] Material Design Guidelines eingehalten
- [ ] Dark Mode Support
- [ ] Alle Strings in strings.xml (Mehrsprachigkeit)
- [ ] Code ist kommentiert
- [ ] Keine Firebase-Abhängigkeit (nur CSV)

## Testing-Hinweise

### Test-Szenarien
1. **Backup erstellen**:
   - Frischen MonopolyGo Account
   - Account mit allen IDs
   - Account ohne SSAID
   - Duplikat-Account

2. **Restore testen**:
   - Existierendes Backup
   - Nicht-existierendes Backup
   - Unvollständiges Backup
   - Restore und dann MonopolyGo starten

3. **CSV-Management**:
   - Leere CSV
   - CSV mit 10+ Einträgen
   - Kaputte CSV-Zeile
   - Fehlende CSV-Datei

4. **Root-Szenarien**:
   - Mit Root
   - Ohne Root (Fehlermeldung)
   - Root-Zugriff während Restore verloren

## Zusätzliche Features (Optional)

Falls Zeit bleibt, implementiere:
- [ ] **Backup-Kompression**: ZIP-Archive statt Ordner
- [ ] **Auto-Backup**: Tägliches automatisches Backup
- [ ] **Backup-Verifizierung**: Checksum-Prüfung
- [ ] **Cloud-Sync**: Optional Backup auf Server
- [ ] **Backup-Verschlüsselung**: AES-256 Encryption
- [ ] **Multi-Account-Restore**: Mehrere Accounts parallel verwalten
- [ ] **Statistiken**: Backup-Größe, Anzahl, Trends
- [ ] **Import/Export**: CSV-Datei teilen
- [ ] **Backup-Schedule**: Zeitgesteuerte Backups

## Wichtige Hinweise

1. **KEIN Friend Link Auto-Generation**: Die Shell-Skripte generieren bewusst KEINE Friend Links. Diese Funktionalität NICHT implementieren.

2. **CSV ist Single Source of Truth**: Keine zusätzliche Datenbank (SQLite, Room) verwenden. Nur CSV.

3. **Root-Befehle ausführen**: Nutze `Runtime.getRuntime().exec()` oder Process Builder mit `su -c`.

4. **Thread-Safety**: Alle Root-Operationen auf Background-Thread (Coroutines).

5. **Error-Logging**: Jeder Root-Befehl sollte stdout/stderr loggen.

6. **Permissions-Handling**: Android 11+ benötigt MANAGE_EXTERNAL_STORAGE.

## Fragen zum Klären

Falls unklar, frage nach:
- Soll Firebase-Integration entfernt werden?
- Brauchen wir mehrsprachige UI (Deutsch/Englisch)?
- Welches Min SDK (21 oder höher)?
- Root-Library verwenden oder selbst implementieren?
- Design-Vorgaben (Material 2 oder Material 3)?

## Los geht's!

Analysiere die vorhandenen Shell-Skripte und das bestehende Repository. Erstelle dann:

1. **Schritt 1**: `models/BackupAccount.kt` - Datenmodell
2. **Schritt 2**: `utils/RootManager.kt` - Root-Operationen
3. **Schritt 3**: `utils/IdExtractor.kt` - ID-Extraktion
4. **Schritt 4**: `utils/CsvManager.kt` - CSV-Handling
5. **Schritt 5**: `utils/BackupManager.kt` - Backup/Restore-Logik
6. **Schritt 6**: `fragments/BackupFragment.kt` - Backup-UI
7. **Schritt 7**: `fragments/RestoreFragment.kt` - Restore-UI
8. **Schritt 8**: `fragments/AccountListFragment.kt` - Listen-UI
9. **Schritt 9**: `adapters/AccountAdapter.kt` - RecyclerView-Adapter
10. **Schritt 10**: `activities/MainActivity.kt` - Hauptactivity

Viel Erfolg! 🚀
