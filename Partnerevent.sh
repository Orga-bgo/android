#!/bin/bash
set -e

# Master script for the Partnerevent module
PS3="Aktion wählen: "
options=(
    "Kunde hinzufügen"
    "Eigene Accounts wählen"
    "Zuweisung erstellen"
    "Team zusammenstellen"
    "Abbrechen"
)
select opt in "${options[@]}"; do
    case "$REPLY" in
        1)
            cmd="1_Kunde_hinzufuegen.sh"
            ;;
        2)
            cmd="2_Eigene_Accounts.sh"
            ;;
        3)
            cmd="3_Zuweisung.sh"
            ;;
        4)
            cmd="4_Teamzusammenstellung.sh"
            ;;
        5)
            echo "Abgebrochen."
            exit 0
            ;;
        *)
            echo "Ungültige Auswahl." >&2
            continue
            ;;
    esac

    if command -v termux-toast >/dev/null; then
        termux-toast "Starte $cmd"
    fi

    bash "$cmd"

    if command -v termux-toast >/dev/null; then
        termux-toast "Fertig: $cmd"
    fi
    break

done
