#!/bin/bash
set -e

if command -v termux-toast >/dev/null; then
    termux-toast "Starte Wiederherstellung"
fi

# Pfade laut Übersicht.md
acc_eigene="/storage/emulated/0/MonopolyGo/Accounts/Eigene/"
acc_datapath="/data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat"

# Only restore from own accounts (customer folders don't contain backups)
base_path="$acc_eigene"

echo "Verfügbare Accounts in $base_path:"
folders=()
while IFS= read -r -d '' dir; do
    folders+=("$(basename "$dir")")
done < <(find "$base_path" -mindepth 1 -maxdepth 1 -type d -print0 | sort -z)

if [ ${#folders[@]} -eq 0 ]; then
    echo "Keine Ordner gefunden." >&2
    exit 1
fi

PS3="Account auswählen: "
select folder in "${folders[@]}" "Abbrechen"; do
    if [ "$REPLY" -gt 0 ] && [ "$REPLY" -le ${#folders[@]} ]; then
        account_dir="${base_path}${folder}"
        break
    else
        echo "Abgebrochen."; exit 0
    fi
done

# Monopoly-Go-App beenden
am force-stop com.scopely.monopolygo

# Account-Datei wiederherstellen
if cp "${account_dir}/WithBuddies.Services.User.0Production.dat" "$acc_datapath"; then
    echo "Datei kopiert."
    if command -v termux-toast >/dev/null; then
        termux-toast "Account kopiert"
    fi
else
    echo "Fehler beim Kopieren." >&2
    exit 1
fi

# Nachfragen, ob die App gestartet werden soll
read -r -p "App starten? [j/N] " answer
if [[ $answer =~ ^[Jj]$ ]]; then
    monkey -p com.scopely.monopolygo 1
fi

if command -v termux-toast >/dev/null; then
    termux-toast "Wiederherstellung abgeschlossen"
fi
