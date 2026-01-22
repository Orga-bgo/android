# MonopolyGo Manager - Android APK mit Root-Rechten

Eine native Android-App mit Root-Zugriff zur Verwaltung von MonopolyGo Accounts. Diese App ersetzt die Termux-basierten Bash-Skripte durch eine benutzerfreundliche GUI.

## ⚠️ Wichtige Anforderungen

### Root-Zugriff erforderlich
Diese App benötigt **Root-Zugriff** (SuperSU oder Magisk) für folgende Funktionen:
- ✅ Account-Wiederherstellung
- ✅ Account-Sicherung  
- ✅ Zugriff auf `/data/data/com.scopely.monopolygo/`
- ✅ Automatische App-Steuerung (force-stop, start)
- ✅ Freundschaftsanfragen automatisieren

### Systemanforderungen
- Android 5.0 (API 21) oder höher
- Gerootetes Android-Gerät mit SuperSU oder Magisk
- Berechtigungen: Storage, Internet
- ~10 MB freier Speicherplatz

## 🏗️ Projektstruktur

```
app/
├── src/main/
│   ├── java/de/babixgo/monopolygo/
│   │   ├── MainActivity.java                    # Hauptmenü
│   │   ├── AccountManagementActivity.java       # Accountverwaltung
│   │   ├── PartnerEventActivity.java           # Partnerevent-Management
│   │   ├── FriendshipActivity.java             # Freundschaftsbalken
│   │   ├── RootManager.java                    # Root-Zugriffsverwaltung
│   │   ├── AccountManager.java                 # Account-Operationen
│   │   ├── DataExtractor.java                  # Datenextraktion
│   │   └── ShortLinkManager.java               # Short.io API Integration
│   ├── res/
│   │   ├── layout/                             # UI-Layouts
│   │   ├── values/                             # Strings, Farben, Styles
│   │   └── drawable/                           # Icons und Grafiken
│   └── AndroidManifest.xml
└── build.gradle                                 # App-Dependencies
```

## 📱 Funktionen

### 1. Accountverwaltung
- **Account wiederherstellen**: Wähle gespeicherte Accounts und stelle sie wieder her
- **Eigenen Account sichern**: Sichere den aktuellen Account mit UserID-Extraktion
- **Kunden Account sichern**: Verwalte Kundenaccounts
- **Kopiere Links**: Zugriff auf gespeicherte Freundschaftslinks

### 2. Partnerevent (In Entwicklung)
- Kunde hinzufügen
- Eigene Accounts wählen
- Zuweisung erstellen
- Team zusammenstellen

### 3. Freundschaftsbalken (In Entwicklung)
- Download und Installation

## 🔧 Technische Implementierung

### Root-Zugriff
Die App verwendet die folgenden Methoden für Root-Operationen:

```java
// Root-Check
RootManager.isRooted()

// Root-Zugriff anfordern
RootManager.requestRoot()

// Root-Befehle ausführen
RootManager.runRootCommand("command")
```

### Dateioperationen
Alle kritischen Dateien befinden sich in geschützten Verzeichnissen:

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

## 🔥 Backend

### Firebase Realtime Database
- Firebase Realtime Database for cloud storage
  - Offline-first architecture
  - Automatic synchronization
  - Real-time updates across devices

## 📦 Dependencies

Die App verwendet folgende Libraries:

```gradle
// Root-Zugriff
implementation 'com.github.topjohnwu.libsu:core:5.0.1'

// Firebase
implementation platform('com.google.firebase:firebase-bom:32.7.0')
implementation 'com.google.firebase:firebase-database'
implementation 'com.google.firebase:firebase-auth'

// HTTP-Requests (Short.io API)
implementation 'com.squareup.okhttp3:okhttp:4.11.0'

// CSV-Verarbeitung
implementation 'com.opencsv:opencsv:5.7.1'

// JSON-Verarbeitung
implementation 'com.google.code.gson:gson:2.10.1'

// Android Support
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.9.0'
```

## 🚀 Build-Anleitung

## Setup Instructions

### 1. Development Environment
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK (API 21-34)
- Git

### 2. Firebase Configuration

**Required for cloud features** (optional):

1. Create Firebase project: [console.firebase.google.com](https://console.firebase.google.com)
2. Add Android app: `de.babixgo.monopolygo`
3. Download `google-services.json`
4. Place in: `app/google-services.json`

**Detailed guide**: See [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

### 3. Build

```bash
# Clone repository
git clone https://github.com/Orga-bgo/android.git
cd android

# Gradle sync
./gradlew --refresh-dependencies

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install & Run

```bash
# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Grant root access when prompted (SuperSU/Magisk)

# Run app
adb shell am start -n de.babixgo.monopolygo/.MainActivity
```

## 🔐 Sicherheit

### Root-Permission-Handling
Die App fordert Root-Zugriff beim ersten Start an und zeigt eine Warnung:

```
⚠️ WARNUNG: Diese App benötigt Root-Zugriff
Die folgenden Funktionen erfordern Root-Rechte:
✓ Account-Wiederherstellung
✓ Account-Sicherung  
✓ Zugriff auf App-Daten
✓ Automatische Freundschaftsanfragen

Ohne Root sind nur Basis-Funktionen verfügbar.
```

### Berechtigungen
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 📄 Lizenz

Dieses Projekt ist für den privaten Gebrauch bestimmt.

## 🤝 Mitwirken

Beiträge sind willkommen! Bitte beachte:
1. Fork das Projekt
2. Erstelle einen Feature-Branch
3. Committe deine Änderungen
4. Push zum Branch
5. Öffne einen Pull Request

## ⚙️ Fehlerbehandlung

Die App implementiert umfassende Fehlerbehandlung:
- Root-Verlust wird erkannt und neu angefordert
- Fehlgeschlagene Dateioperationen werden gemeldet
- Netzwerkfehler (Short.io API) werden abgefangen

## 📞 Support

Bei Fragen oder Problemen:
- Öffne ein Issue auf GitHub
- Kontaktiere den Entwickler

## 🔄 Migration von Bash-Skripten

Diese App ersetzt folgende Bash-Skripte:
- ✅ `Accountverwaltung.sh` → AccountManagementActivity
- ✅ `1_Account_wiederherstellen.sh` → Restore-Funktion
- ✅ `2_Eigener_Account_sichern.sh` → Backup-Funktion
- ✅ `2_Kunden_Account_sichern.sh` → Customer-Backup
- 🚧 `Partnerevent.sh` → In Entwicklung
- 🚧 `Freundschaftsbalken.sh` → In Entwicklung

## 🎯 Roadmap

- [x] Root-Zugriffsverwaltung
- [x] Account-Wiederherstellung
- [x] Account-Sicherung
- [x] UserID-Extraktion
- [x] Short.io API Integration
- [ ] CSV-Datenverwaltung
- [ ] Partnerevent vollständig implementieren
- [ ] Freundschaftsbalken-Automatisierung
- [ ] Backup/Restore als ZIP
- [ ] UI-Verbesserungen
