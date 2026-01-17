#!/bin/bash
set -e

# Master script for the Freundschaftsbalken module
PS3="Aktion wählen: "
options=(
    "Download und Installation"
    "Abbrechen"
)
select opt in "${options[@]}"; do
    case "$REPLY" in
        1)
            cmd="1_Download_und_Installation.sh"
            ;;
        2)
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

