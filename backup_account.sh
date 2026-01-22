#!/bin/bash
# Backup MonopolyGo Account mit ID-Extraktion und CSV-Speicherung
set -e

# Konfiguration
MONOPOLYGO_PKG="com.scopely.monopolygo"
MONOPOLYGO_DATA="/data/data/${MONOPOLYGO_PKG}"
BACKUP_BASE="/storage/emulated/0/MonopolyGo/Backups"
CSV_FILE="${BACKUP_BASE}/accounts.csv"
SSAID_FILE="/data/system/users/0/settings_ssaid.xml"

# Termux Toast Unterstützung
toast() {
    if command -v termux-toast >/dev/null; then
        termux-toast "$1"
    fi
}

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "FEHLER: $1" >&2
    toast "Fehler: $1"
    exit 1
}

# Root-Zugriff prüfen
if ! su -c 'true' 2>/dev/null; then
    error_exit "Root-Zugriff erforderlich!"
fi

# MonopolyGo installiert?
if ! pm list packages | grep -q "$MONOPOLYGO_PKG"; then
    error_exit "MonopolyGo ist nicht installiert!"
fi

log "=== MonopolyGo Account Backup ==="
toast "Account Backup starten"

# Account-Name eingeben
read -r -p "Account-Name: " account_name
[ -z "$account_name" ] && error_exit "Account-Name ist erforderlich!"

# Notiz (optional)
read -r -p "Notiz (optional): " notiz

# Backup-Verzeichnis erstellen
timestamp=$(date +%Y%m%d_%H%M%S)
backup_dir="${BACKUP_BASE}/${account_name}_${timestamp}"
mkdir -p "$backup_dir" || error_exit "Konnte Backup-Verzeichnis nicht erstellen"

log "Stoppe MonopolyGo..."
am force-stop "$MONOPOLYGO_PKG" || error_exit "Konnte App nicht stoppen"
sleep 1

# CSV-Datei initialisieren (falls nicht vorhanden)
if [ ! -f "$CSV_FILE" ]; then
    mkdir -p "$(dirname "$CSV_FILE")"
    echo "AccountName,UserID,GAID,DeviceToken,AppSetID,SSAID,BackupPfad,Datum,ZuletztGespielt,Notiz" > "$CSV_FILE"
    log "CSV-Datei erstellt: $CSV_FILE"
fi

# Dateien kopieren
log "Kopiere Spielstände..."
su -c "cp -r ${MONOPOLYGO_DATA}/files/DiskBasedCacheDirectory '${backup_dir}/'" || error_exit "DiskBasedCacheDirectory kopieren fehlgeschlagen"
su -c "cp -r ${MONOPOLYGO_DATA}/shared_prefs '${backup_dir}/'" || error_exit "shared_prefs kopieren fehlgeschlagen"
su -c "cp ${SSAID_FILE} '${backup_dir}/settings_ssaid.xml'" 2>/dev/null || log "Warnung: settings_ssaid.xml konnte nicht kopiert werden"

log "Dateien erfolgreich gesichert"

# IDs extrahieren
log "Extrahiere IDs..."
PREFS_FILE="${backup_dir}/shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml"

if [ ! -f "$PREFS_FILE" ]; then
    error_exit "Preferences-Datei nicht gefunden: $PREFS_FILE"
fi

# XML-Wert extrahieren (Hilfsfunktion)
extract_xml_value() {
    local file="$1"
    local key="$2"
    local type="${3:-string}"  # string oder int
    
    if [ "$type" = "string" ]; then
        grep -Po "<string name=\"$key\">\K[^<]+" "$file" 2>/dev/null | head -n1
    else
        grep -Po "<int name=\"$key\" value=\"\K[^\"]+" "$file" 2>/dev/null | head -n1
    fi
}

# Verschiedene mögliche Feldnamen probieren
USER_ID=""
for field in "Scopely.Attribution.UserId" "ScopelyProfile.UserId" "Scopely.UserId" "UserId" "user_id" "PlayerId"; do
    USER_ID=$(extract_xml_value "$PREFS_FILE" "$field" "string")
    [ -n "$USER_ID" ] && break
    USER_ID=$(extract_xml_value "$PREFS_FILE" "$field" "int")
    [ -n "$USER_ID" ] && break
done
[ -z "$USER_ID" ] && USER_ID="N/A"

GAID=$(extract_xml_value "$PREFS_FILE" "Scopely.Attribution.GoogleAdvertisingId" "string")
[ -z "$GAID" ] && GAID="N/A"

DEVICE_TOKEN=$(extract_xml_value "$PREFS_FILE" "Scopely.DeviceToken" "string")
[ -z "$DEVICE_TOKEN" ] && DEVICE_TOKEN="N/A"

APP_SET_ID=$(extract_xml_value "$PREFS_FILE" "Scopely.AppSetId" "string")
[ -z "$APP_SET_ID" ] && APP_SET_ID="N/A"

# SSAID extrahieren (16-stelliger Hex-Wert)
SSAID="N/A"
if [ -f "${backup_dir}/settings_ssaid.xml" ]; then
    SSAID=$(grep -Po "com\.scopely\.monopolygo[^/]*/[^/]*/[^/]*/\K[0-9a-f]{16}" "${backup_dir}/settings_ssaid.xml" 2>/dev/null | head -n1)
    [ -z "$SSAID" ] && SSAID="N/A"
fi

log "Extrahierte IDs:"
log "  User ID: $USER_ID"
log "  GAID: $GAID"
log "  Device Token: $DEVICE_TOKEN"
log "  App Set ID: $APP_SET_ID"
log "  SSAID: $SSAID"

# In CSV speichern
datum=$(date +%Y-%m-%d)
zuletzt_gespielt=$(date +'%Y-%m-%d %H:%M:%S')

# Duplikat-Prüfung (User ID)
if [ "$USER_ID" != "N/A" ]; then
    if grep -q ",${USER_ID}," "$CSV_FILE" 2>/dev/null; then
        log "Warnung: User ID $USER_ID existiert bereits in der Datenbank"
        read -r -p "Trotzdem speichern? (j/n): " confirm
        [[ ! "$confirm" =~ ^[jJ]$ ]] && error_exit "Backup abgebrochen (Duplikat)"
    fi
fi

# CSV-Zeile hinzufügen (mit Anführungszeichen für Felder mit möglichen Kommas)
printf '"%s","%s","%s","%s","%s","%s","%s","%s","%s","%s"\n' \
    "$account_name" "$USER_ID" "$GAID" "$DEVICE_TOKEN" "$APP_SET_ID" "$SSAID" \
    "$backup_dir" "$datum" "$zuletzt_gespielt" "$notiz" >> "$CSV_FILE"

log "Backup erfolgreich gespeichert!"
log "Backup-Verzeichnis: $backup_dir"
log "CSV-Datei: $CSV_FILE"

toast "Backup erfolgreich: $account_name"

# Zusammenfassung anzeigen
echo ""
echo "=========================================="
echo "Backup abgeschlossen!"
echo "Account: $account_name"
echo "User ID: $USER_ID"
echo "Pfad: $backup_dir"
echo "=========================================="
