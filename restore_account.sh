#!/bin/bash
# Restore MonopolyGo Account aus Backup
set -e

# Konfiguration
MONOPOLYGO_PKG="com.scopely.monopolygo"
MONOPOLYGO_DATA="/data/data/${MONOPOLYGO_PKG}"
BACKUP_BASE="/storage/emulated/0/MonopolyGo/Backups"
CSV_FILE="${BACKUP_BASE}/accounts.csv"

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

# CSV-Datei vorhanden?
if [ ! -f "$CSV_FILE" ]; then
    error_exit "Keine Backups gefunden! CSV-Datei nicht vorhanden: $CSV_FILE"
fi

log "=== MonopolyGo Account Restore ==="
toast "Account Restore"

# Accounts aus CSV auslesen
log "Verfügbare Backups:"
echo ""

mapfile -t accounts < <(tail -n +2 "$CSV_FILE")

if [ ${#accounts[@]} -eq 0 ]; then
    error_exit "Keine Backups in der CSV-Datei gefunden!"
fi

# Accounts anzeigen
for i in "${!accounts[@]}"; do
    IFS=',' read -r name userid gaid token appset ssaid path datum zuletzt notiz <<< "${accounts[$i]}"
    # Anführungszeichen entfernen
    name=$(echo "$name" | tr -d '"')
    userid=$(echo "$userid" | tr -d '"')
    datum=$(echo "$datum" | tr -d '"')
    path=$(echo "$path" | tr -d '"')
    notiz=$(echo "$notiz" | tr -d '"')
    
    echo "[$((i+1))] $name"
    echo "    User ID: $userid"
    echo "    Datum: $datum"
    echo "    Pfad: $path"
    [ -n "$notiz" ] && echo "    Notiz: $notiz"
    echo ""
done

# Account auswählen
read -r -p "Account-Nummer zum Wiederherstellen (1-${#accounts[@]}) oder 'q' zum Abbrechen: " selection

[[ "$selection" =~ ^[qQ]$ ]] && { log "Abgebrochen."; exit 0; }

if ! [[ "$selection" =~ ^[0-9]+$ ]] || [ "$selection" -lt 1 ] || [ "$selection" -gt ${#accounts[@]} ]; then
    error_exit "Ungültige Auswahl!"
fi

selected_idx=$((selection-1))
selected_account="${accounts[$selected_idx]}"

IFS=',' read -r account_name user_id gaid device_token app_set_id ssaid backup_path datum zuletzt_gespielt notiz <<< "$selected_account"

# Anführungszeichen entfernen
account_name=$(echo "$account_name" | tr -d '"')
backup_path=$(echo "$backup_path" | tr -d '"')

log "Gewählter Account: $account_name"
log "Backup-Pfad: $backup_path"

# Backup-Verzeichnis prüfen
if [ ! -d "$backup_path" ]; then
    error_exit "Backup-Verzeichnis nicht gefunden: $backup_path"
fi

if [ ! -d "${backup_path}/shared_prefs" ] || [ ! -d "${backup_path}/DiskBasedCacheDirectory" ]; then
    error_exit "Backup ist unvollständig! Erforderliche Verzeichnisse fehlen."
fi

# Sicherheitsabfrage
echo ""
echo "WARNUNG: Dieser Vorgang überschreibt den aktuellen Spielstand!"
read -r -p "Möchten Sie fortfahren? (ja/nein): " confirm

if [[ ! "$confirm" =~ ^(ja|j|yes|y)$ ]]; then
    log "Restore abgebrochen."
    exit 0
fi

log "Stoppe MonopolyGo..."
am force-stop "$MONOPOLYGO_PKG" || error_exit "Konnte App nicht stoppen"
sleep 1

toast "Stelle Account wieder her"

# Dateien zurückkopieren
log "Stelle Dateien wieder her..."

# shared_prefs wiederherstellen
log "Kopiere shared_prefs..."
su -c "rm -rf ${MONOPOLYGO_DATA}/shared_prefs/*" || log "Warnung: Konnte alte shared_prefs nicht löschen"
su -c "cp -r ${backup_path}/shared_prefs/* ${MONOPOLYGO_DATA}/shared_prefs/" || error_exit "shared_prefs kopieren fehlgeschlagen"
su -c "chmod 660 ${MONOPOLYGO_DATA}/shared_prefs/*" || log "Warnung: Konnte Berechtigungen nicht setzen"

# DiskBasedCacheDirectory wiederherstellen
log "Kopiere DiskBasedCacheDirectory..."
su -c "rm -rf ${MONOPOLYGO_DATA}/files/DiskBasedCacheDirectory/*" || log "Warnung: Konnte alten Cache nicht löschen"
su -c "cp -r ${backup_path}/DiskBasedCacheDirectory/* ${MONOPOLYGO_DATA}/files/DiskBasedCacheDirectory/" || error_exit "DiskBasedCacheDirectory kopieren fehlgeschlagen"
su -c "chmod -R 771 ${MONOPOLYGO_DATA}/files/DiskBasedCacheDirectory" || log "Warnung: Konnte Berechtigungen nicht setzen"

# Owner setzen (u0_aXXX - MonopolyGo App)
app_uid=$(stat -c '%U' "${MONOPOLYGO_DATA}" 2>/dev/null)
if [ -n "$app_uid" ]; then
    log "Setze Dateieigentümer auf: $app_uid"
    su -c "chown -R ${app_uid}:${app_uid} ${MONOPOLYGO_DATA}/shared_prefs" 2>/dev/null || log "Warnung: chown fehlgeschlagen"
    su -c "chown -R ${app_uid}:${app_uid} ${MONOPOLYGO_DATA}/files/DiskBasedCacheDirectory" 2>/dev/null || log "Warnung: chown fehlgeschlagen"
fi

log "Dateien erfolgreich wiederhergestellt"

# CSV aktualisieren (Zuletzt gespielt)
log "Aktualisiere CSV-Datei..."
tmp_csv=$(mktemp)
zuletzt_gespielt_neu=$(date +'%Y-%m-%d %H:%M:%S')

head -n 1 "$CSV_FILE" > "$tmp_csv"
awk -F',' -v OFS=',' -v idx="$((selected_idx+2))" -v new_date="$zuletzt_gespielt_neu" \
    'NR==idx {$9="\"" new_date "\""; print; next} NR>1 {print}' "$CSV_FILE" >> "$tmp_csv"
mv "$tmp_csv" "$CSV_FILE"

log "Account erfolgreich wiederhergestellt!"
toast "Restore erfolgreich: $account_name"

# MonopolyGo starten?
echo ""
echo "=========================================="
echo "Restore abgeschlossen!"
echo "Account: $account_name"
echo "User ID: $(echo $user_id | tr -d '\"')"
echo "=========================================="
echo ""
read -r -p "MonopolyGo jetzt starten? (j/n): " start_app

if [[ "$start_app" =~ ^[jJ]$ ]]; then
    log "Starte MonopolyGo..."
    am start -n "${MONOPOLYGO_PKG}/com.scopely.monopolygo.MainActivity" 2>/dev/null || \
        monkey -p "$MONOPOLYGO_PKG" 1 2>/dev/null || \
        log "Konnte App nicht automatisch starten. Bitte manuell öffnen."
    toast "MonopolyGo gestartet"
fi

log "Fertig!"
