#!/bin/bash
set -e

if command -v termux-toast >/dev/null; then
    termux-toast "Kunde hinzufügen"
fi

base_dir="/storage/emulated/0/MonopolyGo/Partnerevents/"

extract_userid_from_link() {
    local url="$1"
    curl -sIL "$url" | grep -i "^location:" | tail -n1 | grep -Po '(?<=/add-friend/)[0-9]+'
}

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

add_new_customer() {
    read -r -p "Name: " name
    read -r -p "Accountname: " account
    read -r -p "Gebuchte Slots: " slots
    read -r -p "Freundschaftslink: " link
    userid=$(extract_userid_from_link "$link")
    read -r -p "Code: " code
    echo "$name,$account,$slots,$slots,$link,$userid,$code" >> "$kunden_file"
    echo "Kunde hinzugefügt."
    if command -v termux-toast >/dev/null; then
        termux-toast "Kunde hinzugefügt"
    fi
}

update_customer() {
    local idx="$1"
    line=$(tail -n +2 "$kunden_file" | sed -n "${idx}p")
    IFS=',' read -r name account slots slots_rest link userid code <<<"$line"
    read -r -p "Gebuchte Slots [$slots]: " new_slots
    if [ -n "$new_slots" ]; then
        slots="$new_slots"
        slots_rest="$new_slots"
    fi
    if [ -z "$link" ]; then
        read -r -p "Freundschaftslink: " link
        userid=$(extract_userid_from_link "$link")
    fi
    tmp=$(mktemp)
    awk -F',' -v OFS=',' -v n=$((idx+1)) \
        -v name="$name" -v acc="$account" -v slots="$slots" -v rest="$slots_rest" \
        -v link="$link" -v uid="$userid" -v code="$code" \
        'NR==1 {print; next} NR==n {print name,acc,slots,rest,link,uid,code; next} {print}' \
        "$kunden_file" > "$tmp" && mv "$tmp" "$kunden_file"
    echo "Kunde aktualisiert."
    if command -v termux-toast >/dev/null; then
        termux-toast "Kunde aktualisiert"
    fi
}

select_event
kunden_file="${event_dir}Kunden.csv"

mapfile -t kunden_list < <(tail -n +2 "$kunden_file" | cut -d',' -f1)

PS3="Kunde wählen: "
select kunde in "${kunden_list[@]}" "Neuer Kunde" "Abbrechen"; do
    if [ "$REPLY" -gt 0 ] && [ "$REPLY" -le ${#kunden_list[@]} ]; then
        update_customer "$REPLY"
        break
    elif [ "$kunde" = "Neuer Kunde" ]; then
        add_new_customer
        break
    elif [ "$kunde" = "Abbrechen" ]; then
        echo "Abgebrochen."; exit 0
    else
        echo "Ungültige Auswahl." >&2
    fi
done
