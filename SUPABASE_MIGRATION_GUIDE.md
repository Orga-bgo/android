# Supabase Schema Migration Guide

## Übersicht

Dieses Projekt enthält drei SQL-Skripte für die Verwaltung des Supabase-Datenbankschemas:

1. **`supabase_schema.sql`** - Vollständiges Schema (Original)
2. **`supabase_migration_safe.sql`** - Idempotentes Migrations-Skript ✅ **EMPFOHLEN**
3. **`supabase_verify.sql`** - Verifikations-Skript

---

## 🎯 Welches Skript soll ich verwenden?

### Für neue Datenbank (Erstinstallation)
Verwende **`supabase_schema.sql`** oder **`supabase_migration_safe.sql`**

### Für bestehende Datenbank (Update/Migration)
Verwende **`supabase_migration_safe.sql`** ✅

### Zur Überprüfung der Installation
Verwende **`supabase_verify.sql`**

---

## 📋 1. supabase_schema.sql

### Beschreibung
Vollständiges Schema-Skript, das die Datenbank von Grund auf neu erstellt.

### Verwendung
```sql
-- ⚠️ WARNUNG: Löscht alle bestehenden Daten!
-- Nur für neue Datenbanken verwenden
```

### Vorteile
- ✅ Vollständige Dokumentation
- ✅ Übersichtlich strukturiert
- ✅ Mit Kommentaren und Beispieldaten

### Nachteile
- ❌ Nicht idempotent (kann nicht mehrfach ausgeführt werden)
- ❌ Fehler bei bereits existierenden Objekten

### Wann verwenden?
- Neue Supabase-Datenbank
- Kompletter Neuanfang (nach Datenbank-Reset)
- Als Referenz-Dokumentation

---

## 🔄 2. supabase_migration_safe.sql (EMPFOHLEN)

### Beschreibung
Idempotentes Migrations-Skript, das intelligent prüft, welche Objekte fehlen und nur diese erstellt.

### Verwendung
```bash
# In Supabase SQL Editor:
# 1. Gehe zu SQL Editor in Supabase Dashboard
# 2. Öffne neues Query
# 3. Kopiere Inhalt von supabase_migration_safe.sql
# 4. Führe aus (kann beliebig oft wiederholt werden)
```

### Vorteile
- ✅ **Idempotent** - kann beliebig oft ausgeführt werden
- ✅ Prüft vor dem Erstellen, ob Objekte existieren
- ✅ Sicher für bestehende Datenbanken
- ✅ Fügt fehlende Spalten hinzu
- ✅ Aktualisiert Trigger und Views automatisch
- ✅ Keine Datenverluste

### Was wird geprüft?
1. ✓ Extensions (uuid-ossp)
2. ✓ ENUMs (account_status, event_status)
3. ✓ Tabellen (accounts, events, customers, customer_accounts, teams)
4. ✓ Spalten (fügt fehlende Spalten hinzu)
5. ✓ Foreign Key Constraints
6. ✓ Indizes
7. ✓ Trigger
8. ✓ Views
9. ✓ RLS Policies

### Wann verwenden?
- ✅ **Immer, wenn du unsicher bist**
- ✅ Migration von älteren Schema-Versionen
- ✅ Nach Schema-Änderungen
- ✅ Fehlende Spalten/Indizes hinzufügen
- ✅ Reparatur beschädigter Schemas

---

## 🔍 3. supabase_verify.sql

### Beschreibung
Verifikations-Skript, das einen detaillierten Report über den aktuellen Schema-Status erstellt.

### Verwendung
```bash
# In Supabase SQL Editor:
# 1. Führe supabase_verify.sql aus
# 2. Prüfe Output auf Vollständigkeit
```

### Was wird überprüft?
1. Extensions
2. ENUM Types
3. Tabellen und Spalten-Anzahl
4. Kritische Spalten
5. Foreign Key Constraints
6. Indizes
7. Trigger
8. Views
9. Row Level Security Status
10. RLS Policies
11. Schema Version History
12. Tabellen-Statistiken
13. Detaillierte Spalten-Information

### Wann verwenden?
- Nach jeder Migration
- Bei Problemen mit der Datenbank
- Zur Dokumentation des aktuellen Zustands
- Vor größeren Änderungen

---

## 🚀 Schritt-für-Schritt Anleitung

### Szenario 1: Neue Datenbank einrichten

```bash
# 1. Führe Schema-Skript aus
supabase_schema.sql

# 2. Verifiziere Installation
supabase_verify.sql

# 3. Prüfe Expected Counts:
#    accounts: 21 columns ✓
#    customer_accounts: 14 columns ✓
#    customers: 5 columns ✓
#    events: 7 columns ✓
#    teams: 11 columns ✓
```

### Szenario 2: Bestehende Datenbank aktualisieren

```bash
# 1. Backup erstellen (optional aber empfohlen)
#    In Supabase: Settings → Database → Create Backup

# 2. Migrations-Skript ausführen
supabase_migration_safe.sql

# 3. Verifikation
supabase_verify.sql

# 4. Check für Fehler im Output
#    - Alle Tabellen vorhanden? ✓
#    - Alle Foreign Keys gesetzt? ✓
#    - RLS aktiviert? ✓
```

### Szenario 3: Probleme beheben

```bash
# 1. Status prüfen
supabase_verify.sql

# 2. Fehlende Objekte identifizieren
#    - Tabellen fehlen?
#    - Spalten fehlen?
#    - Constraints fehlen?

# 3. Reparatur durchführen
supabase_migration_safe.sql

# 4. Erneut verifizieren
supabase_verify.sql
```

---

## ✅ Erwartete Spalten-Anzahl

Nach erfolgreicher Migration sollten folgende Spalten-Anzahlen vorhanden sein:

| Tabelle | Spalten | Wichtigste Spalten |
|---------|---------|-------------------|
| **accounts** | 21 | id, name, user_id, suspension_status, is_customer_account, customer_account_id |
| **events** | 7 | id, name, start_date, end_date, status |
| **customers** | 5 | id, name, notes |
| **customer_accounts** | 14 | id, customer_id, ingame_name, service_partner, service_race, service_boost |
| **teams** | 11 | id, event_id, name, customer_id, slot_1-4_account_id |

---

## 🔒 Row Level Security (RLS)

### Aktuelle Konfiguration (Development/Testing)
```sql
-- ⚠️ WARNUNG: Erlaubt anonymen Zugriff!
-- Aktuell für Development-Phase

CREATE POLICY "Allow all for authenticated users" ON <table>
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');
```

### Für Production (TODO)
```sql
-- 🔒 Entferne anonymen Zugriff
-- Implementiere benutzer-spezifische Policies

CREATE POLICY "Users can read own data" ON <table>
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own data" ON <table>
    FOR INSERT WITH CHECK (auth.uid() = user_id);
```

---

## 📊 Schema Version History

Das Schema verwendet eine Versionstabelle zur Nachverfolgung:

```sql
SELECT * FROM schema_version ORDER BY version;
```

### Versionen:
- **v1**: Initial schema (accounts, events, customers, teams)
- **v2**: Simplified suspension tracking
- **v3**: Customer management restructure
- **v4**: Fixed forward reference error
- **v5**: Safe migration script (idempotent) ✅

---

## 🛠️ Troubleshooting

### Problem: "relation already exists"
**Lösung**: Verwende `supabase_migration_safe.sql` statt `supabase_schema.sql`

### Problem: "column does not exist"
**Lösung**:
```bash
# 1. Führe Migration aus
supabase_migration_safe.sql

# 2. Prüfe, ob Spalte jetzt existiert
supabase_verify.sql
```

### Problem: "foreign key constraint fails"
**Lösung**: Reihenfolge beachten - Constraints werden automatisch am Ende hinzugefügt

### Problem: Schema-Inkonsistenzen
**Lösung**:
```bash
# 1. Backup erstellen!
# 2. Migration ausführen
supabase_migration_safe.sql
# 3. Verifikation
supabase_verify.sql
```

---

## 📝 Best Practices

### ✅ DO
- ✅ Immer `supabase_migration_safe.sql` für Updates verwenden
- ✅ Nach Migration immer `supabase_verify.sql` ausführen
- ✅ Backup vor größeren Änderungen erstellen
- ✅ Schema-Version in `schema_version` Tabelle tracken

### ❌ DON'T
- ❌ `supabase_schema.sql` auf bestehende Datenbank ausführen
- ❌ Sample Data in Production aktivieren
- ❌ RLS in Production mit `anon` Zugriff lassen
- ❌ Manuelle ALTER TABLE ohne Prüfung auf Existenz

---

## 🔗 Nächste Schritte

Nach erfolgreicher Migration:

1. ✅ Supabase URL & API Key in `gradle.properties` eintragen
2. ✅ App bauen und testen
3. ✅ RLS Policies für Production anpassen
4. ✅ Backup-Strategie implementieren

---

## 📞 Support

Bei Problemen:
1. Führe `supabase_verify.sql` aus und prüfe Output
2. Checke Schema Version: `SELECT * FROM schema_version;`
3. Prüfe Supabase Logs im Dashboard

---

**Version**: 1.0
**Letzte Aktualisierung**: 2024-01-21
**Schema Version**: v5 (idempotent migration)
