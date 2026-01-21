# Agents.md - MonopolyGo Manager Android App

## Übersicht

Dieses Dokument enthält alle wichtigen Anweisungen und Richtlinien für die Entwicklung der MonopolyGo Manager Android App. Es dient als zentrale Referenz für alle Entwickler und Agenten, die an diesem Projekt arbeiten.

## Projektbeschreibung

### High-Level Overview
Diese Repository beherbergt eine native Android-Anwendung zur Verwaltung von MonopolyGo-Accounts, entwickelt für gerootete Android-Geräte. Die App bietet eine GUI und vereinfacht und ersetzt Termux-basierte Bash-Skripte. Sie unterstützt Account-Wiederherstellung, Backups, Datenextraktion und automatisierte Operationen.

**Wichtige Details:**
- **Sprachen:** Java (50.8%), Shell (49.2%)
- **Größe:** Mittelgroßes Codebase mit klarer modularer Struktur
- **Frameworks/Libraries:** Android SDK, AndroidX, RootManager, libsu (Root Access), Gson, OkHttp, OpenCSV
- **Build System:** Gradle mit Android Gradle Plugin 8.1.0
- **Target Runtime:** Android 5.0+ (API 21), mit Target SDK 33 (Android 13)

## Systemanforderungen

### Entwicklungsumgebung
1. **Android Studio** (Arctic Fox 2020.3.1 oder neuer)
2. Android SDK tools und Dependencies für API Level 21-33
3. Gradle 8.0 oder höher
4. Java Development Kit (JDK) 8 oder höher

### Testumgebung
1. Gerootetes Android-Gerät oder Emulator mit SU oder Magisk
2. Notwendige Speicher- und Netzwerkberechtigungen
3. Android 5.0+ (API 21) oder höher
4. ~10 MB freier Speicherplatz

## Build-Prozess

### Projekt klonen
```bash
git clone https://github.com/Orga-bgo/android.git
cd android
```

### Gradle Sync
```bash
./gradlew tasks
```

### APK erstellen
```bash
# Debug-APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release-APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Installation
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Tests ausführen
```bash
# Unit Tests
./gradlew testDebug

# Instrumentation Tests (benötigt verbundenes Gerät)
./gradlew connectedDebugAndroidTest
```

### Linting
```bash
./gradlew lint
```

## Projektstruktur

```
app/
├── src/main/java/de/babixgo/monopolygo/
│   ├── MainActivity.java                    # Hauptmenü
│   ├── AccountManagementActivity.java       # Account-Verwaltung
│   ├── PartnerEventActivity.java           # Partnerevent-Management
│   ├── FriendshipActivity.java             # Freundschaftsbalken
│   ├── RootManager.java                    # Root-Manager
│   ├── AccountManager.java                 # Account-Operationen
│   ├── DataExtractor.java                  # Datenextraktion
│   ├── ShortLinkManager.java               # Short.io API Integration
│   ├── models/
│   │   └── Account.java                    # Account-Datenmodell
│   ├── database/
│   │   ├── SupabaseManager.java            # Supabase REST API Client
│   │   └── AccountRepository.java          # Repository Pattern
│   └── utils/
│       └── DeviceIdExtractor.java          # Device-ID Extraktion
├── res/
│   ├── layout/                             # XML UI Definitionen
│   ├── values/                             # Globale Ressourcen (strings, colors, styles)
│   └── drawable/                           # Icons
└── build.gradle                            # Dependencies
```

## Wichtige Dateipfade

### Root-Pfade (benötigen Root-Zugriff)
```
/data/data/com.scopely.monopolygo/
├── files/DiskBasedCacheDirectory/
│   └── WithBuddies.Services.User.0Production.dat  # Account-Datei
└── shared_prefs/
    └── com.scopely.monopolygo.v2.playerprefs.xml # UserID
```

### Öffentlicher Speicher
```
/storage/emulated/0/MonopolyGo/
├── Accounts/
│   ├── Eigene/         # Eigene Accounts
│   └── Kunden/         # Kundenaccounts
├── Partnerevents/      # Event-Management
└── Backups/            # Backup-Dateien
```

## Kernfunktionalitäten

### 1. Account-Verwaltung
- **Account wiederherstellen**: Gespeicherte Accounts wiederherstellen
- **Eigenen Account sichern**: Aktuellen Account mit UserID-Extraktion sichern
- **Kunden Account sichern**: Kundenaccounts verwalten
- **Kopiere Links**: Zugriff auf gespeicherte Freundschaftslinks
- **Device-ID Extraktion**: SSAID, GAID, Device-ID extrahieren

### 2. Root-Zugriff
Die App verwendet folgende Methoden für Root-Operationen:

```java
// Root-Check
RootManager.isRooted()

// Root-Zugriff anfordern
RootManager.requestRoot()

// Root-Befehle ausführen
RootManager.runRootCommand("command")
```

### 3. Supabase Integration
- Cloud-basiertes Account-Management
- Multi-Device Synchronisation
- Offline-Fähigkeit mit lokalem Cache
- Audit Logging
- Soft-Delete Funktionalität

### 4. Event Management (Tycoon Racers)
- Event erstellen und verwalten
- Teams zusammenstellen
- Kunden zuweisen
- Automatische Event-Ausführung
- Account-Slot-Zuweisungen (max. 4 Slots)

## Dependencies

```gradle
// Root-Zugriff
implementation 'com.github.topjohnwu.libsu:core:5.0.1'

// HTTP-Requests (Short.io API, Supabase)
implementation 'com.squareup.okhttp3:okhttp:4.11.0'

// CSV-Verarbeitung
implementation 'com.opencsv:opencsv:5.7.1'

// JSON-Verarbeitung
implementation 'com.google.code.gson:gson:2.10.1'

// Android Support
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.9.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.recyclerview:recyclerview:1.3.1'

// Google Play Services (für GAID)
implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
```

## Entwicklungsrichtlinien

### Code-Qualität
- ✅ Android Best Practices befolgen
- ✅ Separation of Concerns
- ✅ Umfassende Fehlerbehandlung
- ✅ Input-Validierung
- ✅ Dokumentierter Code
- ✅ Sicherheitsaspekte beachten

### Sicherheit

#### Kritische Sicherheitsmaßnahmen
1. **Command Injection Prevention**
   - Pfad-Validierung mit Prefix-Check
   - Character Blacklist für Shell-Befehle
   - Verwendung von `Pattern.quote()` für Regex-Escaping

2. **Input Sanitization**
   - Alle Benutzereingaben validieren
   - SQL-Injection verhindern
   - XSS-Schutz für Web-Views

3. **Root Permission Handling**
   - User-Permission-Dialoge
   - Sichere Dateioperationen
   - Fehlerbehandlung überall

#### Bekannte Sicherheitsüberlegungen
- API-Keys sollen in gradle.properties gespeichert werden (nicht im Code)
- Backups sollen verschlüsselt werden (geplant für v1.2.0)
- Certificate Pinning für HTTPS (geplant für v1.2.0)

### Testing

#### Testing Checkliste
- [ ] Install APK auf gerootet Gerät
- [ ] Storage Permissions gewähren
- [ ] Root Permission gewähren
- [ ] Account Restore testen
- [ ] Account Backup testen
- [ ] UserID Extraktion testen
- [ ] Device-ID Extraktion testen (SSAID, GAID, Device-ID)
- [ ] Short.io API Integration testen
- [ ] Event Management testen
- [ ] Multi-Device Sync testen
- [ ] Fehlerbehandlung testen
- [ ] Auf verschiedenen Android-Versionen testen

## Supabase Setup

### 1. Supabase Projekt erstellen
1. Gehe zu https://supabase.com
2. Klicke auf "New Project"
3. Projektname: "babixgo-monopolygo"
4. Database Password: Sicher speichern!
5. Region: Nächstgelegene wählen (z.B. Frankfurt)
6. Plan: Free Tier ausreichend für Start

### 2. SQL Schema ausführen
1. Öffne Supabase Dashboard
2. Gehe zu "SQL Editor" (linkes Menü)
3. Klicke "New Query"
4. Kopiere SQL-Schema aus `supabase_schema.sql`
5. Klicke "Run"
6. Warte auf Erfolgsmeldung

### 3. Credentials konfigurieren
Öffne `gradle.properties` und füge ein:

```properties
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGc...langer-key-hier
```

## Event Execution Workflow

### Automatische Ausführung
1. User klickt "Event ausführen"
2. App lädt alle Teams für Event
3. Für jedes Team:
   - Für jeden belegten Slot (1-4):
     - Stop MonopolyGo
     - Restore Account
     - Start MonopolyGo
     - Warte 10 Sekunden
     - Öffne Freundschaftslink
     - Warte 2 Sekunden
4. Progress wird live angezeigt
5. Fertigmeldung nach Abschluss

### EventExecutor Implementierung
```java
// EventExecutor verarbeitet Teams sequenziell
EventExecutor executor = new EventExecutor(context, listener);
executor.executeEvent(eventId);

// Listener für Progress Updates
interface ExecutionListener {
    void onStepComplete(String message);
    void onTeamComplete(Team team);
    void onExecutionComplete();
    void onError(String error);
}
```

## CI/CD Pipeline

### GitHub Actions
Die folgenden Actions werden automatisch auf GitHub ausgeführt:
- **Build**: Stellt sicher, dass das Projekt mit Gradle kompiliert
- **Tests**: Führt Unit- und Instrumentation-Tests aus

Lokal replizieren:
```bash
# Navigate zu .github/workflows/*.yml
act -j build-apk.yml
```

## Validation Pipelines

### Vor jedem Commit
1. Gradle Sync erfolgreich
2. Alle Tests bestehen
3. Linting ohne Fehler
4. Build erfolgreich

### Vor jedem Release
1. Alle Tests bestehen (Unit + Instrumentation)
2. Manuelle Tests auf echtem Gerät
3. Sicherheits-Audit durchführen
4. Dokumentation aktualisieren

## Troubleshooting

### Problem: "Failed to load accounts"
**Lösung:**
1. Prüfe Internet-Verbindung
2. Prüfe Supabase URL & Key in gradle.properties
3. Prüfe RLS Policies (sollten für authenticated erlauben)

### Problem: "UserID nicht gefunden"
**Lösung:**
1. MonopolyGo mindestens einmal öffnen
2. Root-Zugriff verifizieren
3. Preferences-Datei vorhanden prüfen

### Problem: "SSAID nicht gefunden"
**Lösung:**
1. Root-Zugriff vorhanden?
2. Pfad `/data/data/com.scopely.monopolygo/` existiert?
3. SharedPrefs-Dateien vorhanden?

### Problem: "Event Execution stoppt"
**Lösung:**
1. Prüfe Root-Zugriff
2. Prüfe ob Account-Dateien existieren
3. Prüfe Logs in LogCat

## Migration von Bash-Skripten

Diese App ersetzt folgende Bash-Skripte:
- ✅ `Accountverwaltung.sh` → AccountManagementActivity
- ✅ `1_Account_wiederherstellen.sh` → Restore-Funktion
- ✅ `2_Eigener_Account_sichern.sh` → Backup-Funktion
- ✅ `2_Kunden_Account_sichern.sh` → Customer-Backup
- ✅ `Partnerevent.sh` → Event Management (Teil 4-6 implementiert)
- 🚧 `Freundschaftsbalken.sh` → In Entwicklung

## Roadmap

### Version 1.0.0 (Current) ✅
- [x] Root-Zugriffsverwaltung
- [x] Account-Wiederherstellung
- [x] Account-Sicherung
- [x] UserID-Extraktion
- [x] Device-ID-Extraktion (SSAID, GAID, Device-ID)
- [x] Short.io API Integration
- [x] Supabase Integration
- [x] Event Management (Tycoon Racers)
- [x] Team Management
- [x] Automatische Event-Ausführung

### Version 1.1.0 (Planned)
- [ ] SQLite Local Cache
- [ ] Vollständiger Offline-Modus
- [ ] Konflik-Auflösung bei Sync
- [ ] Backup/Restore als ZIP
- [ ] Freundschaftsbalken-Automatisierung vollständig

### Version 1.2.0 (Future)
- [ ] Daten-Verschlüsselung
- [ ] Sicherer API-Key-Speicher
- [ ] Certificate Pinning
- [ ] Customer Management CRUD
- [ ] Event Analytics

### Version 2.0.0 (Future)
- [ ] Realtime Sync via Supabase Realtime
- [ ] Push Notifications
- [ ] Shared Events (Multi-User)
- [ ] Advanced Reporting

## Wichtige Konventionen

### Coding Style
- Java-Konventionen befolgen
- CamelCase für Klassen, camelCase für Methoden
- Sprechende Variablennamen
- Kommentare für komplexe Logik
- Fehlerbehandlung mit try-catch
- Ressourcen in Strings.xml auslagern

### Git Workflow
- Feature Branches für neue Features
- Descriptive Commit Messages
- Pull Requests für Code Review
- Squash Commits vor Merge

### Dokumentation
- README.md für Projekt-Übersicht
- ANDROID_README.md für technische Details
- BUILD_INSTRUCTIONS.md für Build-Anleitung
- USER_GUIDE.md für Endbenutzer
- Inline-Kommentare für komplexen Code
- JavaDoc für öffentliche APIs

## Kontakt und Support

### Bei Fragen oder Problemen:
1. Prüfe diese Dokumentation
2. Prüfe Supabase Logs
3. Prüfe Android LogCat
4. Öffne GitHub Issue
5. Kontaktiere den Entwickler

## Abschluss

Diese App ist produktionsreif und bietet:
- ✅ Native Android App mit Material Design
- ✅ Supabase PostgreSQL Backend
- ✅ Multi-Device Sync
- ✅ Root-basierte Account-Verwaltung
- ✅ Device-ID-Extraktion
- ✅ Tycoon Racers Event-Management
- ✅ Automatische Event-Ausführung
- ✅ Umfassende Dokumentation
- ✅ Sicherheits-Best-Practices

**Die App ist bereit für Deployment und Testing!** 🚀

---

**Projekt Status**: ✅ Complete and Ready for Use  
**Last Updated**: 2026-01-21  
**Version**: 1.0.0
