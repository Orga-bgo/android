#!/bin/bash
set -e

if command -v termux-toast >/dev/null; then
    termux-toast "Starte Zuweisung"
fi

base_dir="/storage/emulated/0/MonopolyGo/Partnerevents/"

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

# --- Hauptlogik ---

select_event

kunden_file="${event_dir}Kunden.csv"
eigene_file="${event_dir}Eigene Accounts.csv"
assign_file="${event_dir}Einteilung.csv"
assign2_file="${event_dir}Einteilung 2.csv"

# Kunden laden
customer_name=()
customer_acc=()
customer_slots=()
customer_rest=()
customer_link=()
customer_uid=()
customer_code=()
cust_next=()

while IFS=',' read -r name acc slots rest link uid code; do
    customer_name+=("$name")
    customer_acc+=("$acc")
    customer_slots+=("$slots")
    customer_rest+=("$rest")
    customer_link+=("$link")
    customer_uid+=("$uid")
    customer_code+=("$code")
    cust_next+=(1)
done < <(tail -n +2 "$kunden_file")

# Eigene Accounts laden
acc_name=()
acc_short=()
acc_left=()
acc_next=()
acc_slot1=()
acc_slot2=()
acc_slot3=()
acc_slot4=()

while IFS=',' read -r name short left; do
    acc_name+=("$name")
    acc_short+=("$short")
    acc_left+=("$left")
    acc_next+=(1)
    acc_slot1+=("")
    acc_slot2+=("")
    acc_slot3+=("")
    acc_slot4+=("")
done < <(tail -n +2 "$eigene_file")

# Kundenslots arrays
cust_slot1=()
cust_slot2=()
cust_slot3=()
cust_slot4=()
for ((i=0; i<${#customer_name[@]}; i++)); do
    cust_slot1+=("")
    cust_slot2+=("")
    cust_slot3+=("")
    cust_slot4+=("")
done

# Zuweisung
for ((i=0; i<${#customer_name[@]}; i++)); do
    remain=${customer_rest[i]}
    for ((j=0; j<${#acc_name[@]}; j++)); do
        [ "$remain" -gt 0 ] || break
        if [ ${acc_left[j]} -gt 0 ]; then
            # Account Slot bestimmen
            slot_index=${acc_next[j]}
            if [ "$slot_index" -le 4 ]; then
                case $slot_index in
                    1) acc_slot1[j]="${customer_name[i]}";;
                    2) acc_slot2[j]="${customer_name[i]}";;
                    3) acc_slot3[j]="${customer_name[i]}";;
                    4) acc_slot4[j]="${customer_name[i]}";;
                esac
                acc_next[j]=$((slot_index+1))
                acc_left[j]=$((acc_left[j]-1))

                cust_index=${cust_next[i]}
                case $cust_index in
                    1) cust_slot1[i]="${acc_name[j]}";;
                    2) cust_slot2[i]="${acc_name[j]}";;
                    3) cust_slot3[i]="${acc_name[j]}";;
                    4) cust_slot4[i]="${acc_name[j]}";;
                esac
                cust_next[i]=$((cust_index+1))
                remain=$((remain-1))
            fi
        fi
    done
    customer_rest[i]=$remain
done

# Einteilung.csv schreiben
{
    echo "Eigener Account Name,Eig. Acc Shortlink,Slot 1,Slot 2,Slot 3,Slot 4"
    for ((j=0; j<${#acc_name[@]}; j++)); do
        echo "${acc_name[j]},${acc_short[j]},${acc_slot1[j]},${acc_slot2[j]},${acc_slot3[j]},${acc_slot4[j]}"
    done
} > "$assign_file"

# Einteilung 2.csv schreiben
{
    echo "Kunden Acc Name,Freundschaftslink,Gebuchte Slots,Slot 1,Slot 2,Slot 3,Slot 4"
    for ((i=0; i<${#customer_name[@]}; i++)); do
        echo "${customer_acc[i]},${customer_link[i]},${customer_slots[i]},${cust_slot1[i]},${cust_slot2[i]},${cust_slot3[i]},${cust_slot4[i]}"
    done
} > "$assign2_file"

# Kunden.csv aktualisieren
{
    echo "Name,Accountname,Slots,Slots_übrig,Freundschaftslink,UserID,Code"
    for ((i=0; i<${#customer_name[@]}; i++)); do
        echo "${customer_name[i]},${customer_acc[i]},${customer_slots[i]},${customer_rest[i]},${customer_link[i]},${customer_uid[i]},${customer_code[i]}"
    done
} > "$kunden_file"

# Eigene Accounts.csv aktualisieren
{
    echo "Name,Shortlink,Slots übrig"
    for ((j=0; j<${#acc_name[@]}; j++)); do
        echo "${acc_name[j]},${acc_short[j]},${acc_left[j]}"
    done
} > "$eigene_file"

echo "Zuweisung gespeichert."
if command -v termux-toast >/dev/null; then
    termux-toast "Zuweisung gespeichert"
fi

