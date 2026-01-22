# MonopolyGo Manager - Android App

Eine native Android-App mit Root-Zugriff zur Verwaltung von MonopolyGo Accounts.

## 🔥 Firebase Realtime Database

Diese App verwendet **Firebase Realtime Database** für Cloud-Synchronisation von:
- Account-Daten
- Tycoon Racers Events
- Kunden-Verwaltung
- Team-Zuordnungen
- Activity-Logging (Audit Trail)

### Setup

1. **Firebase-Projekt erstellen**: [Firebase Console](https://console.firebase.google.com)
2. **Android-App registrieren** mit Paketname: `de.babixgo.monopolygo`
3. **`google-services.json` herunterladen**
4. **Datei platzieren**: `app/google-services.json`
5. **Build & Run**: `./gradlew assembleDebug`

**📚 Vollständige Anleitung**: Siehe [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

### Funktioniert auch ohne Firebase

Die App ist **nicht** abhängig von Firebase. Folgende Features funktionieren auch ohne Cloud-Verbindung:

✅ **Lokale Account-Backups** (via Root-Zugriff)  
✅ **Account-Wiederherstellung**  
✅ **Device-ID Extraktion** (SSAID, GAID, Android ID)  
✅ **Root-basierte File-Operationen**

Ohne Firebase nicht verfügbar:

❌ Account-Liste und Cloud-Synchronisation  
❌ Multi-Device Support  
❌ Tycoon Racers Event-Management  
❌ Kunden-Verwaltung  
❌ Activity-Logging

### Migration von Supabase

> **Hinweis**: Frühere Versionen verwendeten Supabase PostgreSQL. Ab Version 1.1.0 wurde auf Firebase Realtime Database migriert für besseren Offline-Support und einfachere Einrichtung.

Falls du eine alte Version mit Supabase nutzt, siehe Legacy-Dokumentation in Git-History.

## 📖 Weitere Dokumentation

Für detaillierte Informationen siehe:
- [ANDROID_README.md](ANDROID_README.md) - Technische Details
- [USER_GUIDE.md](USER_GUIDE.md) - Benutzerhandbuch
- [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) - Build-Anleitung
- [FIREBASE_SETUP.md](FIREBASE_SETUP.md) - Firebase Einrichtung

