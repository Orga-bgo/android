# Supabase Setup Guide - babixGO MonopolyGo Manager

## 🚀 SETUP-ANLEITUNG

### 1. Supabase Projekt erstellen

1. Gehe zu https://supabase.com
2. Klicke auf "New Project"
3. Wähle Organisation oder erstelle neue
4. Projektname: "babixgo-monopolygo"
5. Database Password: Speichere sicher!
6. Region: Wähle nächstgelegene (z.B. Frankfurt)
7. Plan: Free Tier ausreichend für Start

### 2. SQL Schema ausführen

1. Öffne Supabase Dashboard
2. Gehe zu "SQL Editor" (linkes Menü)
3. Klicke "New Query"
4. Kopiere KOMPLETTES SQL-Schema aus `supabase_schema.sql`
5. Klicke "Run"
6. Warte auf Erfolgsmeldung

### 3. Credentials kopieren

1. Gehe zu "Settings" → "API"
2. Kopiere:
   - **Project URL** (z.B. https://xxxxx.supabase.co)
   - **anon/public key** (langer String)

### 4. In Android Studio einfügen

Öffne `gradle.properties` oder erstelle `secrets.xml` und füge ein:

**Option 1: gradle.properties**
```properties
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGc...langer-key-hier
```

**Option 2: app/src/main/res/values/secrets.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="supabase_url">https://xxxxx.supabase.co</string>
    <string name="supabase_anon_key">eyJhbGc...langer-key-hier</string>
</resources>
```

### 5. Sync & Build

1. Gradle Sync durchführen
2. Build → Build APK
3. Auf Gerät installieren

---

## 🐛 TROUBLESHOOTING

### Problem: "Failed to load accounts"
**Lösung:**
1. Prüfe Internet-Verbindung
2. Prüfe Supabase URL & Key in secrets.xml
3. Prüfe RLS Policies (sollten für authenticated erlauben)
4. Prüfe Supabase Dashboard → Logs für Fehler

### Problem: "UserID nicht gefunden"
**Lösung:**
1. MonopolyGo mindestens einmal öffnen
2. Root-Zugriff verifizieren
3. Preferences-Datei vorhanden prüfen unter:
   `/data/data/com.scopely.monopolygo/shared_prefs/`

### Problem: "SSAID nicht gefunden"
**Lösung:**
1. Root-Zugriff vorhanden?
2. Pfad `/data/data/com.scopely.monopolygo/` existiert?
3. SharedPrefs-Dateien vorhanden?
4. Prüfe mit Root Explorer

### Problem: "Event Execution stoppt"
**Lösung:**
1. Prüfe Root-Zugriff
2. Prüfe ob Account-Dateien existieren
3. Prüfe Logs in Progress Dialog
4. Prüfe ob MonopolyGo installiert ist

### Problem: "Supabase Connection Timeout"
**Lösung:**
1. Prüfe Internet-Verbindung
2. Prüfe Firewall/VPN-Einstellungen
3. Teste Supabase URL im Browser
4. Prüfe ob Supabase-Projekt aktiv ist

---

## 📊 DATABASE SCHEMA ÜBERSICHT

### Tabellen

**accounts**
- Speichert alle MonopolyGo Accounts
- Device-IDs (SSAID, GAID, Device-ID)
- Suspension-Tracking
- Soft-Delete Support

**events**
- Tycoon Racers Events
- Start/End Datum
- Status (planned, active, completed, cancelled)

**customers**
- Event-Kunden
- Friend Links & User IDs
- Slot-Anzahl

**teams**
- Teams pro Event
- 4 Account-Slots
- Zuordnung zu Customer

### Beziehungen

```
events (1) ─── (N) teams
customers (1) ─── (N) teams
accounts (1) ─── (N) teams.slot_X_account_id
```

### Views

- **active_accounts**: Nicht gelöschte, nicht suspendierte Accounts
- **teams_with_details**: Teams mit allen Namen (Event, Customer, Accounts)
- **events_with_stats**: Events mit Team-Anzahl

---

## 🔒 SECURITY

### Row Level Security (RLS)

Alle Tabellen haben RLS aktiviert mit Policies:
- Authenticated users: Voller Zugriff
- Anonymous users: Voller Zugriff (für Free Tier)

**Für Production:**
- Erstelle separate Policies für INSERT, UPDATE, DELETE
- Beschränke Zugriff auf user_id
- Aktiviere Email-Verifizierung

### API Keys

- **anon/public key**: Client-seitig sicher
- Niemals **service_role key** in App verwenden!
- Keys in `secrets.xml` speichern (nicht in Git)

---

## 📈 MONITORING

### Supabase Dashboard

**Database → Logs**
- SQL Queries
- Fehler-Logs
- Performance-Metriken

**API → Logs**
- API Requests
- Fehlerhafte Requests
- Rate Limits

### Android Logcat

```bash
adb logcat | grep -i "supabase\|monopolygo"
```

Filter für:
- Supabase-Fehler
- Network-Requests
- Repository-Operationen

---

## 🎯 BEST PRACTICES

### 1. Error Handling
- Alle Repository-Calls mit `.exceptionally()` abfangen
- User-freundliche Fehlermeldungen via Toast
- Detaillierte Logs für Debugging

### 2. Caching
- Accounts lokal cachen wenn möglich
- Nur bei Änderungen sync
- Timestamp-basierte Synchronisation

### 3. Performance
- Indizes auf häufig abgefragte Spalten
- Limit/Offset für große Listen
- Lazy Loading für Details

### 4. Data Integrity
- Foreign Keys für Beziehungen
- Constraints für Validierung
- Triggers für Auto-Updates

---

## 🔄 MIGRATION & BACKUP

### Schema-Updates

1. Neue Version in `schema_version` eintragen
2. Migration-SQL schreiben
3. In Supabase SQL Editor ausführen
4. App-Version erhöhen

### Database Backup

**Automatisch:**
- Supabase erstellt tägliche Backups (Free: 7 Tage)

**Manuell:**
- Dashboard → Database → Backups
- Export als SQL Dump
- Lokal speichern

### Data Export

```sql
-- Alle Accounts exportieren
COPY accounts TO '/tmp/accounts.csv' CSV HEADER;

-- Alle Events exportieren
COPY events TO '/tmp/events.csv' CSV HEADER;
```

---

## 📝 NÄCHSTE SCHRITTE

Nach Setup:
1. ✅ Schema erfolgreich ausgeführt
2. ✅ Credentials konfiguriert
3. ✅ App gebaut und installiert
4. → Weiter zu [TESTING_GUIDE.md](TESTING_GUIDE.md)

Bei Problemen:
- Prüfe diese Dokumentation
- Prüfe Supabase Logs
- Prüfe Android Logcat
- Öffne GitHub Issue
