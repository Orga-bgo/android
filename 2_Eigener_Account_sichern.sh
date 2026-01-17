#!/bin/bash
set -e

if command -v termux-toast >/dev/null; then
    termux-toast "Sichere eigenen Account"
fi

# Pfade laut Übersicht.md
acc_eigene="/storage/emulated/0/MonopolyGo/Accounts/Eigene/"
acc_datapath="/data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat"
acc_infos="/data/data/com.scopely.monopolygo/shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml"
acc_eigene_infos="/storage/emulated/0/MonopolyGo/Accounts/Eigene/Accountinfos.csv"

api_key="sk_MaQODQPO0HKJTZF1"
domain="go.babixgo.de"

read -r -p "Interne ID: " interneid
read -r -p "Notiz (optional): " notiz

# UserID aus Einstellungen auslesen
# Try multiple possible field names as fallbacks
# Array of possible field names (ordered from most specific to most general)
string_fields=(
    "Scopely.Attribution.UserId"
    "ScopelyProfile.UserId"
    "Scopely.UserId"
    "UserId"
    "user_id"
    "userId"
    "PlayerId"
    "player_id"
    "playerId"
)

int_fields=(
    "Scopely.Attribution.UserId"
    "UserId"
    "user_id"
    "PlayerId"
    "player_id"
)

userid=""
# Try string format fields
for field in "${string_fields[@]}"; do
    userid=$(grep -Po "<string name=\"$field\">\K[0-9]+" "$acc_infos" 2>/dev/null)
    if [ -n "$userid" ]; then
        break
    fi
done

# If not found, try integer format fields
if [ -z "$userid" ]; then
    for field in "${int_fields[@]}"; do
        userid=$(grep -Po "<int name=\"$field\" value=\"\K[0-9]+" "$acc_infos" 2>/dev/null)
        if [ -n "$userid" ]; then
            break
        fi
    done
fi

if [ -z "$userid" ]; then
    echo "Warnung: UserId konnte nicht gefunden werden." >&2
    echo "Fortfahren mit Backup ohne UserID..." >&2
    userid="N/A"
fi

# Duplikate vermeiden
if [ -f "$acc_eigene_infos" ]; then
    # Check for duplicate InterneID only (skip UserID check if it's N/A)
    if [ "$userid" = "N/A" ]; then
        if awk -F',' -v iid="$interneid" 'NR>1 && $1==iid {exit 0} END {exit 1}' "$acc_eigene_infos"; then
            echo "Eintrag mit gleicher Interne ID existiert bereits." >&2
            exit 1
        fi
    else
        if awk -F',' -v iid="$interneid" -v uid="$userid" 'NR>1 && ($1==iid || $2==uid) {exit 0} END {exit 1}' "$acc_eigene_infos"; then
            echo "Eintrag mit gleicher UserId oder Interne ID existiert bereits." >&2
            exit 1
        fi
    fi
else
    echo "InterneID,UserID,Datum,Shortlink,Notiz" > "$acc_eigene_infos"
fi

# Kurzlink erzeugen (nur wenn UserID verfügbar ist)
shortlink="N/A"
if [ "$userid" != "N/A" ]; then
    orig_url="monopolygo://add-friend/$userid"
    shortlink=$(curl -s -X POST \
        -H "authorization: $api_key" \
        -H "content-type: application/json" \
        -d "{\"domain\":\"$domain\",\"originalURL\":\"$orig_url\",\"path\":\"$interneid\",\"title\":\"$interneid\"}" \
        "https://api.short.io/links" | jq -r '.shortURL')
    if [ -z "$shortlink" ] || [ "$shortlink" = "null" ]; then
        echo "Warnung: Shortlink konnte nicht erstellt werden." >&2
        shortlink="N/A"
    fi
fi

# Account-Datei kopieren
# Stop MonopolyGo app to ensure file consistency
echo "Stoppe MonopolyGo App..."
am force-stop com.scopely.monopolygo
sleep 1

target_dir="${acc_eigene}${interneid}"
mkdir -p "$target_dir"
cp "$acc_datapath" "$target_dir/" || { echo "Kopieren fehlgeschlagen." >&2; exit 1; }
if command -v termux-toast >/dev/null; then
    termux-toast "Datei gesichert"
fi

datum=$(date +%Y-%m-%d)
printf '%s,%s,%s,%s,"%s"\n' "$interneid" "$userid" "$datum" "$shortlink" "$notiz" >> "$acc_eigene_infos"

echo "Account gesichert unter $target_dir"
if command -v termux-toast >/dev/null; then
    termux-toast "Account gesichert"
fi

