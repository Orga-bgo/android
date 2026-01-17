#!/bin/bash
set -e

# Master script for the Accountverwaltung module
PS3="Aktion wählen: "
options=(
    "Account wiederherstellen"
    "Eigenen Account sichern"
    "Kunden Account sichern"
    "Infos bearbeiten"
    "Kopiere Links"
    "Backup und Restore"
    "Abbrechen"
)
select opt in "${options[@]}"; do
    case "$REPLY" in
        1)
            cmd="1_Account_wiederherstellen.sh"
            ;;
        2)
            cmd="2_Eigener_Account_sichern.sh"
            ;;
        3)
            cmd="2_Kunden_Account_sichern.sh"
            ;;
        4)
            cmd="3_Infos_bearbeiten.sh"
            ;;
        5)
            cmd="4_Kopiere_Links.sh"
            ;;
        6)
            cmd="5_Backup_und_restore.sh"
            ;;
        7)
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
