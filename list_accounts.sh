#!/bin/bash
# Liste alle gesicherten Accounts aus CSV

# Konfiguration
BACKUP_BASE="/storage/emulated/0/MonopolyGo/Backups"
CSV_FILE="${BACKUP_BASE}/accounts.csv"

# Farben (optional)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

if [ ! -f "$CSV_FILE" ]; then
    echo "${RED}Keine Backups gefunden!${NC}"
    echo "CSV-Datei nicht vorhanden: $CSV_FILE"
    exit 1
fi

echo "${BLUE}=== Gesicherte MonopolyGo Accounts ===${NC}"
echo ""

# Header
echo "${GREEN}Nr. | Account-Name | User ID | Datum | Zuletzt gespielt${NC}"
echo "------------------------------------------------------------"

# Accounts auslesen und formatiert anzeigen
counter=1
while IFS=',' read -r name userid gaid token appset ssaid path datum zuletzt notiz; do
    # Header-Zeile überspringen
    [ "$name" = "AccountName" ] && continue
    
    # Anführungszeichen entfernen
    name=$(echo "$name" | tr -d '"')
    userid=$(echo "$userid" | tr -d '"')
    datum=$(echo "$datum" | tr -d '"')
    zuletzt=$(echo "$zuletzt" | tr -d '"')
    notiz=$(echo "$notiz" | tr -d '"')
    path=$(echo "$path" | tr -d '"')
    
    # Backup-Pfad existiert?
    if [ -d "$path" ]; then
        status="${GREEN}✓${NC}"
    else
        status="${RED}✗${NC}"
    fi
    
    printf "${YELLOW}%3d${NC} | %-20s | %-15s | %s | %s %s\n" \
        "$counter" "$name" "$userid" "$datum" "$zuletzt" "$status"
    
    # Notiz anzeigen (falls vorhanden)
    if [ -n "$notiz" ] && [ "$notiz" != "N/A" ]; then
        echo "     Notiz: $notiz"
    fi
    
    ((counter++))
done < "$CSV_FILE"

echo ""
echo "${BLUE}Gesamt: $((counter-1)) Account(s)${NC}"
echo "${GREEN}✓${NC} = Backup vorhanden | ${RED}✗${NC} = Backup fehlt"
echo ""
echo "CSV-Datei: $CSV_FILE"
