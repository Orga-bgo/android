# Testing Guide - babixGO MonopolyGo Manager

## ✅ TESTING CHECKLISTE

### Setup Tests

- [ ] **Supabase Projekt erstellt**
  - Projekt ist aktiv und erreichbar
  - Dashboard öffnet sich ohne Fehler
  
- [ ] **SQL Schema erfolgreich ausgeführt**
  - Alle Tabellen erstellt (accounts, events, customers, teams)
  - Alle Views erstellt (active_accounts, teams_with_details, events_with_stats)
  - Keine SQL-Fehler in Logs
  
- [ ] **Credentials in gradle.properties eingetragen**
  - SUPABASE_URL ist korrekt
  - SUPABASE_ANON_KEY ist korrekt
  - Build.gradle lädt Credentials korrekt
  
- [ ] **App baut ohne Fehler**
  - `./gradlew assembleDebug` erfolgreich
  - APK wird erstellt
  - Keine Build-Fehler

---

### Account Management Tests

#### Account Backup
- [ ] **Account Backup erstellt Datei lokal**
  - MonopolyGo Daten werden gelesen
  - Datei wird in `/storage/emulated/0/MonopolyGo/Backups/` erstellt
  - Dateiname: `{AccountName}/WithBuddies.Services.User.0Production.dat`

- [ ] **UserID wird extrahiert**
  - Preferences-Datei wird gelesen
  - UserID wird korrekt geparst
  - UserID ist numerisch

- [ ] **Device-IDs werden extrahiert**
  - SSAID extrahiert aus com.scopely.monopolygo_preferences.xml
  - GAID extrahiert (falls vorhanden)
  - Device-ID extrahiert

- [ ] **Account wird in Supabase gespeichert**
  - POST-Request erfolgreich
  - Account erscheint in Supabase Dashboard → Table Editor → accounts
  - Alle Felder korrekt befüllt

#### Account List
- [ ] **Account erscheint in AccountList**
  - RecyclerView lädt Accounts
  - Account-Name wird angezeigt
  - Account-Status wird angezeigt
  - Suspend-Badge sichtbar (falls suspended)

- [ ] **Pull-to-Refresh funktioniert**
  - Swipe-Down lädt Accounts neu
  - Loading-Indicator erscheint
  - Liste wird aktualisiert

#### Account Detail
- [ ] **Account Detail zeigt alle Informationen**
  - Name, UserID, Friend Link
  - Device-IDs (SSAID, GAID, Device-ID)
  - Suspension-Status
  - Timestamps (Created, Updated, Last Played)

- [ ] **"Account wiederherstellen" funktioniert**
  - Root-Zugriff wird angefordert
  - Datei wird kopiert nach `/data/data/com.scopely.monopolygo/`
  - Erfolgsmeldung erscheint

#### Account Edit
- [ ] **Account Edit funktioniert**
  - Alle Felder editierbar
  - Save Button aktualisiert Supabase
  - Änderungen erscheinen in Detail-View

- [ ] **Suspension-Status aktualisiert sich**
  - Suspension-Felder können inkrementiert werden
  - is_suspended wird automatisch berechnet
  - suspension_count wird automatisch berechnet

- [ ] **Last Played wird aktualisiert**
  - Beim Restore wird last_played gesetzt
  - Timestamp ist korrekt

---

### Event Management Tests

#### Event List
- [ ] **Event kann erstellt werden**
  - "Neues Event" Button öffnet Dialog
  - Name und Datumsbereich können eingegeben werden
  - Event wird in Supabase gespeichert

- [ ] **Event erscheint in Event List**
  - RecyclerView lädt Events
  - Event-Name wird angezeigt
  - Datumsbereich formatiert (01.02 bis 05.02)
  - Team-Anzahl wird angezeigt

- [ ] **Event Detail öffnet sich**
  - Click auf Event öffnet EventDetailActivity
  - Event-Titel wird angezeigt
  - Teams-Tabelle ist sichtbar

#### Team Management
- [ ] **Team kann hinzugefügt werden**
  - "Team hinzufügen" Button öffnet Dialog
  - Team-Name kann eingegeben werden
  - Team wird in Supabase gespeichert

- [ ] **Team erscheint in Teams-Liste**
  - RecyclerView lädt Teams für Event
  - Team-Name wird angezeigt
  - Kunde-Name wird angezeigt (falls zugewiesen)
  - Account-Slots zeigen Account-Namen

- [ ] **Team kann bearbeitet werden**
  - Click auf Team öffnet Edit-Dialog
  - Team-Name kann geändert werden
  - Änderungen werden gespeichert

- [ ] **Accounts können Slots zugewiesen werden**
  - Spinner zeigt alle verfügbaren Accounts
  - Account kann Slot 1-4 zugewiesen werden
  - Multiple Accounts können gleichzeitig zugewiesen werden
  - Leere Slots zeigen "-- Leer --"

#### Customer Management
- [ ] **Kunde kann hinzugefügt werden**
  - "Kunde hinzufügen" Button öffnet Dialog
  - Name, Friend Link, Friend Code eingeben
  - UserID wird aus Link extrahiert
  - Kunde wird in Supabase gespeichert

- [ ] **Customer wird Team zugewiesen**
  - Team-Edit-Dialog zeigt Customer-Spinner
  - Customer kann ausgewählt werden
  - Zuordnung wird gespeichert

---

### Event Execution Tests

#### Automated Execution
- [ ] **Event Executor startet**
  - "Event ausführen" Button startet Execution
  - Confirmation-Dialog erscheint
  - User kann bestätigen oder abbrechen

- [ ] **Accounts werden sequenziell wiederhergestellt**
  - Erster Account wird restored
  - Zweiter Account wird restored (nach 1. abgeschlossen)
  - Keine parallele Execution

- [ ] **MonopolyGo wird gestartet**
  - App wird via Intent gestartet
  - App läuft im Vordergrund
  - 10 Sekunden Wait-Time

- [ ] **Freundschaftslinks werden geöffnet**
  - Friend Link öffnet in MonopolyGo
  - UserID ist korrekt
  - 2 Sekunden Wait-Time

- [ ] **Progress wird live angezeigt**
  - Progress-Dialog zeigt aktuelle Steps
  - "Account wiederherstellen: {Name}"
  - "MonopolyGo gestartet - warte 10 Sekunden..."
  - "Öffne Freundschaftslink für Kunde: {Name}"
  - "Slot X abgeschlossen"

- [ ] **Fehler werden abgefangen**
  - Bei Fehler: Fehlermeldung erscheint
  - Execution continues mit nächstem Team
  - Fehler-Log wird angezeigt

- [ ] **Completion-Meldung erscheint**
  - "Event vollständig ausgeführt!" Toast
  - Progress-Dialog schließt sich
  - User kehrt zu Event Detail zurück

---

### Multi-Device Sync Tests

- [ ] **Account auf Gerät 1 erstellt**
  - Account wird via Backup-Funktion erstellt
  - Account erscheint lokal

- [ ] **Account erscheint auf Gerät 2**
  - Gerät 2 öffnet AccountList
  - Account von Gerät 1 ist sichtbar
  - Alle Daten sind synchronisiert

- [ ] **Änderungen auf Gerät 1**
  - Account-Edit auf Gerät 1
  - Save → Supabase Update

- [ ] **Änderungen sichtbar auf Gerät 2**
  - Pull-to-Refresh auf Gerät 2
  - Änderungen sind sichtbar
  - Daten sind konsistent

---

### Performance Tests

#### Load Tests
- [ ] **100 Accounts laden**
  - Liste lädt in < 3 Sekunden
  - Scrolling ist flüssig
  - Keine Memory-Leaks

- [ ] **50 Events laden**
  - Liste lädt in < 2 Sekunden
  - Alle Events werden angezeigt

- [ ] **20 Teams pro Event laden**
  - Teams-Liste lädt in < 2 Sekunden
  - Alle Slots werden korrekt angezeigt

#### Network Tests
- [ ] **App funktioniert ohne Internet (Offline)**
  - Root-Operationen funktionieren
  - Fehler-Meldung bei Sync-Versuchen
  - User kann weiter arbeiten

- [ ] **App reconnected nach Internet-Verlust**
  - Sync wird automatisch fortgesetzt
  - Keine Datenverluste

---

### Error Handling Tests

#### Network Errors
- [ ] **Supabase nicht erreichbar**
  - Fehler-Toast erscheint
  - User kann Retry versuchen
  - App stürzt nicht ab

- [ ] **Invalid Credentials**
  - Fehler-Meldung ist klar
  - User wird zu Settings geleitet

#### Root Errors
- [ ] **Root-Zugriff verweigert**
  - Fehler-Meldung erscheint
  - App zeigt Anleitung
  - App stürzt nicht ab

- [ ] **MonopolyGo Daten nicht gefunden**
  - Fehler-Meldung ist hilfreich
  - User kann MonopolyGo öffnen
  - Retry nach MonopolyGo-Öffnung

#### Data Errors
- [ ] **Ungültige UserID**
  - Fehler wird abgefangen
  - User kann manuell eingeben

- [ ] **Account existiert bereits**
  - Fehler-Meldung "Account name already exists"
  - User kann anderen Namen wählen

---

### UI/UX Tests

#### Material Design
- [ ] **Alle Screens verwenden Material Components**
  - Cards haben Elevation
  - Buttons haben Ripple-Effect
  - Colors konsistent

- [ ] **Dark Theme Support** (optional)
  - Colors passen zu Theme
  - Text ist lesbar

#### Navigation
- [ ] **Back-Button funktioniert**
  - Von Detail zurück zu List
  - Von Dialog zurück zu Activity

- [ ] **Navigation ist intuitiv**
  - User findet Funktionen schnell
  - Keine versteckten Features

---

## 🔧 MANUAL TESTING SCRIPT

### 1. Fresh Install Test

```bash
# Deinstalliere alte Version
adb uninstall de.babixgo.monopolygo

# Installiere neue Version
adb install app/build/outputs/apk/debug/app-debug.apk

# Starte App
adb shell am start -n de.babixgo.monopolygo/.MainActivity

# Grant Root Permission
# → SuperSU Dialog erscheint → Grant klicken
```

### 2. Account Backup Test

```bash
# Öffne MonopolyGo
# Spiele mindestens 1 Minute
# Schließe MonopolyGo

# In babixGO:
# 1. Main Menu → Account Backup
# 2. Gib Account-Namen ein
# 3. Klick "Create Backup"
# 4. Wait for Success Toast
# 5. Check AccountList → Account sollte erscheinen
```

### 3. Event Execution Test

```bash
# Prerequisites:
# - Mindestens 1 Event erstellt
# - Mindestens 1 Team mit Kunde und 2+ Accounts

# In babixGO:
# 1. Event List → Click Event
# 2. Event Detail → "Event ausführen"
# 3. Confirmation → "Ja, starten"
# 4. Warte und beobachte Progress
# 5. Verify: MonopolyGo öffnet sich pro Account
# 6. Verify: Friend Links werden geöffnet
# 7. Wait for Completion Toast
```

---

## 📊 ACCEPTANCE CRITERIA

### Must Have (MVP)
- ✅ Account Backup funktioniert
- ✅ Accounts erscheinen in List
- ✅ Events können erstellt werden
- ✅ Teams können erstellt und editiert werden
- ✅ Event Execution funktioniert
- ✅ Multi-Device Sync funktioniert

### Should Have
- ✅ Error Handling für alle Fehler
- ✅ Progress Feedback überall
- ✅ Pull-to-Refresh
- ✅ Material Design consistent

### Nice to Have
- ⏳ Offline-Cache (lokales SQLite)
- ⏳ Export/Import Funktionen
- ⏳ Analytics & Statistiken
- ⏳ Push Notifications

---

## 🐛 BUG REPORTING

Bei gefundenen Bugs:

**Template:**
```
**Bug:** [Kurze Beschreibung]
**Steps to Reproduce:**
1. ...
2. ...
3. ...

**Expected:** [Was sollte passieren]
**Actual:** [Was passiert tatsächlich]
**Logs:** [Logcat output]
**Screenshots:** [Falls vorhanden]
```

**Wo reporten:**
- GitHub Issues
- Discord Channel
- Direct Message

---

## ✅ SIGN-OFF

Nach vollständigem Testing:

- [ ] Alle Must-Have Tests passed
- [ ] Alle Should-Have Tests passed
- [ ] Keine Critical Bugs offen
- [ ] Performance akzeptabel
- [ ] UX ist intuitiv

**Status:** ⏳ In Testing | ✅ Ready for Production

**Tester:** _______________
**Datum:** _______________
**Build:** _______________
