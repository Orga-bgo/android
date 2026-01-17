#!/bin/bash
set -e

if command -v termux-toast >/dev/null; then
    termux-toast "Eigene Accounts wählen"
fi

base_dir="/storage/emulated/0/MonopolyGo/Partnerevents/"
acc_infos="/storage/emulated/0/MonopolyGo/Accounts/Eigene/Accountinfos.csv"

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

select_event

if [ ! -f "$acc_infos" ]; then
    echo "Eigene Accountinfos nicht gefunden: $acc_infos" >&2
    exit 1
fi

eigene_csv="${event_dir}Eigene Accounts.csv"

mapfile -t interneids < <(tail -n +2 "$acc_infos" | cut -d',' -f1)

if [ ${#interneids[@]} -eq 0 ]; then
    echo "Keine eigenen Accounts vorhanden." >&2
    exit 1
fi

echo "Verfügbare eigene Accounts:"
for i in "${!interneids[@]}"; do
    printf "%d) %s\n" $((i+1)) "${interneids[$i]}"
done

read -r -p "Nummern auswählen (z.B. 1 2 5): " selection

for num in $selection; do
    idx=$((num-1))
    if [ $idx -ge 0 ] && [ $idx -lt ${#interneids[@]} ]; then
        name="${interneids[$idx]}"
        shortlink=$(awk -F',' -v iid="$name" '$1==iid {print $4}' "$acc_infos")
        if ! grep -q "^${name}," "$eigene_csv" 2>/dev/null; then
            echo "$name,$shortlink,4" >> "$eigene_csv"
        fi
    fi
done

echo "Auswahl gespeichert in $eigene_csv"
if command -v termux-toast >/dev/null; then
    termux-toast "Auswahl gespeichert"
fi

