#!/bin/bash
set -e

if command -v termux-toast >/dev/null; then
    termux-toast "Starte Teamsetup"
fi

base_dir="/storage/emulated/0/MonopolyGo/Partnerevents/"
acc_base="/storage/emulated/0/MonopolyGo/Accounts/Eigene/"
acc_datapath="/data/data/com.scopely.monopolygo/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat"

init_event_dir() {
    local dir="$1"
    [ -f "$dir/Kunden.csv" ] || echo "Name,Accountname,Slots,Slots_übrig,Freundschaftslink,UserID,Code" > "$dir/Kunden.csv"
    [ -f "$dir/Eigene Accounts.csv" ] || echo "Name,Shortlink,Slots übrig" > "$dir/Eigene Accounts.csv"
    [ -f "$dir/Einteilung.csv" ] || echo "Eigener Account Name,Eig. Acc Shortlink,Slot 1,Slot 2,Slot 3,Slot 4" > "$dir/Einteilung.csv"
    [ -f "$dir/Einteilung 2.csv" ] || echo "Kunden Acc Name,Freundschaftslink,Gebuchte Slots,Slot 1,Slot 2,Slot 3,Slot 4" > "$dir/Einteilung 2.csv"
}

select_event() {
    mkdir -p "$base_dir"
    events=()
    while IFS= read -r -d '' d; do
        events+=("$(basename "$d")")
    done < <(find "$base_dir" -mindepth 1 -maxdepth 1 -type d -print0 | sort -z)

    PS3="Event auswählen: "
    select evt in "${events[@]}" "Neues Event" "Abbrechen"; do
        if [ "$REPLY" -gt 0 ] && [ "$REPLY" -le ${#events[@]} ]; then
            selected_event="$evt"
            break
        elif [ "$evt" = "Neues Event" ]; then
            read -r -p "Eventname: " new_evt
            [ -z "$new_evt" ] && { echo "Kein Name." >&2; exit 1; }
            selected_event="$new_evt"
            event_dir="${base_dir}${new_evt}/"
            mkdir -p "$event_dir"
            init_event_dir "$event_dir"
            break
        elif [ "$evt" = "Abbrechen" ]; then
            echo "Abgebrochen."; exit 0
        else
            echo "Ungültige Auswahl." >&2
        fi
    done

    event_dir="${base_dir}${selected_event}/"
    mkdir -p "$event_dir"
    init_event_dir "$event_dir"
}

add_link() {
    local uid="$1"
    [ -z "$uid" ] && return
    am start -a android.intent.action.VIEW -d "monopolygo://add-friend/$uid" >/dev/null 2>&1
    sleep 2
}

record_info() {
    local acc="$1" uid1="$2" uid2="$3" uid3="$4" uid4="$5"
    local t=$(date +"%d.%m %H:%M")
    if [ ! -f "$info_file" ]; then
        echo "Account,Slot1 User,Slot1 Time,Slot2 User,Slot2 Time,Slot3 User,Slot3 Time,Slot4 User,Slot4 Time" > "$info_file"
    fi
    printf '%s,%s,%s,%s,%s,%s,%s,%s,%s\n' "$acc" "$uid1" "$t" "$uid2" "$t" "$uid3" "$t" "$uid4" "$t" >> "$info_file"
}

select_event

kunden_file="${event_dir}Kunden.csv"
assign_file="${event_dir}Einteilung.csv"
info_file="${event_dir}Info.csv"

# Mapping Kundennamen -> UserID
declare -A name_uid
while IFS=',' read -r n acc slots rest link uid code; do
    name_uid["$n"]="$uid"
done < <(tail -n +2 "$kunden_file")

# App sicherheitshalber beenden
am force-stop com.scopely.monopolygo

while IFS=',' read -r accname short slot1 slot2 slot3 slot4; do
    [ -z "$accname" ] && continue
    acc_dir="${acc_base}${accname}"
    data_file="${acc_dir}/WithBuddies.Services.User.0Production.dat"
    if [ ! -f "$data_file" ]; then
        echo "Account-Datei fehlt: $data_file" >&2
        continue
    fi
    [ -n "$TERMUX_VERSION" ] && termux-toast "Account $accname" 2>/dev/null
    am force-stop com.scopely.monopolygo
    cp "$data_file" "$acc_datapath" || { echo "Kopieren fehlgeschlagen: $accname" >&2; continue; }
    monkey -p com.scopely.monopolygo 1 >/dev/null 2>&1
    sleep 10
    add_link "${name_uid[$slot1]}"
    add_link "${name_uid[$slot2]}"
    add_link "${name_uid[$slot3]}"
    add_link "${name_uid[$slot4]}"
    am force-stop com.scopely.monopolygo
    record_info "$accname" "${name_uid[$slot1]}" "${name_uid[$slot2]}" "${name_uid[$slot3]}" "${name_uid[$slot4]}"
    sleep 1
done < <(tail -n +2 "$assign_file")

if command -v termux-toast >/dev/null; then
    termux-toast "Fertig"
fi

