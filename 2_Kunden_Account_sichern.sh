#!/bin/bash
set -e

if command -v termux-toast >/dev/null; then
    termux-toast "Sichere Kundenaccount"
fi

# Pfade laut Übersicht.md
acc_kunden="/storage/emulated/0/MonopolyGo/Accounts/Kunden/"
acc_datapath="/data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat"
acc_infos="/data/data/com.scopely.monopolygo/shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml"
acc_kunden_infos="/storage/emulated/0/MonopolyGo/Accounts/Kunden/Kundeninfos.csv"

extract_userid_from_link() {
    local url="$1"
    curl -sIL "$url" | grep -i "^location:" | tail -n1 | grep -Po '(?<=/add-friend/)[0-9]+'
}

read -r -p "Kunden ID (Name+Nummer): " kundenid
read -r -p "Nutzername: " nutzername
read -r -p "Passwort: " pass
read -r -p "AuTok: " autok
read -r -p "Freundschaftslink: " freundschaftslink
read -r -p "Code: " code
read -r -p "Notiz (optional): " notiz

userid=$(extract_userid_from_link "$freundschaftslink")

if [ -z "$userid" ]; then
    echo "Warnung: UserID konnte nicht ermittelt werden." >&2
    echo "Fortfahren mit Speichern ohne UserID..." >&2
    userid="N/A"
fi

# Create customer folder (without account backup files)
target_dir="${acc_kunden}${kundenid}"
mkdir -p "$target_dir"

if [ -f "$acc_kunden_infos" ]; then
    # Check for duplicate KundenID only if UserID is N/A, otherwise check both
    if [ "$userid" = "N/A" ]; then
        if awk -F',' -v kid="$kundenid" 'NR>1 && $1==kid {exit 0} END {exit 1}' "$acc_kunden_infos"; then
            echo "Eintrag mit gleicher Kunden ID existiert bereits." >&2
            exit 1
        fi
    else
        if awk -F',' -v kid="$kundenid" -v uid="$userid" 'NR>1 && ($1==kid || ($7==uid && uid!="")) {exit 0} END {exit 1}' "$acc_kunden_infos"; then
            echo "Eintrag mit gleicher Kunden ID oder UserId existiert bereits." >&2
            exit 1
        fi
    fi
else
    echo "KundenID,Nutzername,Passwort,AuTok,Freundschaftslink,Code,UserID,Notiz" > "$acc_kunden_infos"
fi

printf '%s,"%s","%s","%s","%s","%s","%s","%s"\n' \
    "$kundenid" "$nutzername" "$pass" "$autok" "$freundschaftslink" "$code" "$userid" "$notiz" >> "$acc_kunden_infos"

echo "Kundenaccount-Daten gespeichert in $acc_kunden_infos"
echo "Kundenordner erstellt: $target_dir"
if command -v termux-toast >/dev/null; then
    termux-toast "Daten gespeichert"
fi
