#!/bin/bash
# Exportiere Account-Details aus CSV in lesbare Textdatei

# Konfiguration
BACKUP_BASE="/storage/emulated/0/MonopolyGo/Backups"
CSV_FILE="${BACKUP_BASE}/accounts.csv"
EXPORT_FILE="${BACKUP_BASE}/accounts_export_$(date +%Y%m%d_%H%M%S).txt"

if [ ! -f "$CSV_FILE" ]; then
    echo "Fehler: CSV-Datei nicht gefunden: $CSV_FILE" >&2
    exit 1
fi

echo "Exportiere Accounts nach: $EXPORT_FILE"

{
    echo "========================================="
    echo "MonopolyGo Accounts Export"
    echo "Datum: $(date +'%Y-%m-%d %H:%M:%S')"
    echo "========================================="
    echo ""
    
    counter=1
    while IFS=',' read -r name userid gaid token appset ssaid path datum zuletzt notiz; do
        # Header überspringen
        [ "$name" = "AccountName" ] && continue
        
        # Anführungszeichen entfernen
        name=$(echo "$name" | tr -d '"')
        userid=$(echo "$userid" | tr -d '"')
        gaid=$(echo "$gaid" | tr -d '"')
        token=$(echo "$token" | tr -d '"')
        appset=$(echo "$appset" | tr -d '"')
        ssaid=$(echo "$ssaid" | tr -d '"')
        path=$(echo "$path" | tr -d '"')
        datum=$(echo "$datum" | tr -d '"')
        zuletzt=$(echo "$zuletzt" | tr -d '"')
        notiz=$(echo "$notiz" | tr -d '"')
        
        echo "[$counter] $name"
        echo "  User ID:          $userid"
        echo "  GAID:             $gaid"
        echo "  Device Token:     $token"
        echo "  App Set ID:       $appset"
        echo "  SSAID:            $ssaid"
        echo "  Backup-Pfad:      $path"
        echo "  Erstellt am:      $datum"
        echo "  Zuletzt gespielt: $zuletzt"
        [ -n "$notiz" ] && [ "$notiz" != "N/A" ] && echo "  Notiz:            $notiz"
        echo ""
        
        ((counter++))
    done < "$CSV_FILE"
    
    echo "========================================="
    echo "Gesamt: $((counter-1)) Account(s)"
    echo "========================================="
} > "$EXPORT_FILE"

echo "Export abgeschlossen!"
echo "Datei: $EXPORT_FILE"

if command -v termux-toast >/dev/null; then
    termux-toast "Export abgeschlossen"
fi
