#!/bin/bash
set -e

if command -v termux-toast >/dev/null; then
    termux-toast "Backup/Restore starten"
fi

# Pfade laut Übersicht.md
monopolygo_dir="/storage/emulated/0/MonopolyGo"
accounts_dir="${monopolygo_dir}/Accounts"
backup_dir="${monopolygo_dir}/Backups"

# Remote-Ziel für scp/ssh konfigurieren
remote_user="babixgode"  # ← Ändere dies bei Bedarf
remote_host="babixgo.de"
remote_path="/files.babixgo.de/MoGo_backup"
remote="${remote_user}@${remote_host}:${remote_path}"

# Prüfen, ob ein SSH-Key vorhanden ist und diesen ggf. auf dem Server installieren
key_file="$HOME/.ssh/id_rsa"
if [ ! -f "$key_file" ]; then
    echo "Kein SSH-Key gefunden. Erstelle neues Schlüsselpaar..."
    mkdir -p "$(dirname "$key_file")"
    ssh-keygen -t rsa -N "" -f "$key_file" || {
        echo "ssh-keygen fehlgeschlagen." >&2
        exit 1
    }
    echo "Übertrage öffentlichen Schlüssel zum Server..."
    cat "${key_file}.pub" | ssh "${remote_user}@${remote_host}" \
        "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys" || {
        echo "Konnte Schlüssel nicht auf Server kopieren." >&2
        exit 1
    }
fi
ssh_opts="-i $key_file"

mkdir -p "$backup_dir"

PS3="Aktion wählen: "
select action in "Backup" "Restore" "Abbrechen"; do
    case "$action" in
        "Backup")
            datum=$(date +%Y-%m-%d)
            archive="Accounts_${datum}.zip"
            (cd "$monopolygo_dir" && zip -r "${backup_dir}/${archive}" "Accounts") || {
                echo "Archivieren fehlgeschlagen." >&2
                exit 1
            }
            if command -v termux-toast >/dev/null; then
                termux-toast "Lade Backup hoch"
            fi
            if command -v rsync >/dev/null; then
                rsync -ah --progress -e "ssh $ssh_opts" "${backup_dir}/${archive}" "$remote/"
            else
                scp $ssh_opts "${backup_dir}/${archive}" "$remote/"
            fi
            if [ $? -eq 0 ]; then
                echo "Backup hochgeladen: ${archive}"
                if command -v termux-toast >/dev/null; then
                    termux-toast "Backup fertig"
                fi
            else
                echo "Upload fehlgeschlagen." >&2
            fi
            break
            ;;
        "Restore")
            latest=$(ssh $ssh_opts "${remote_user}@${remote_host}" "ls -1t ${remote_path}/Acc*.zip 2>/dev/null | head -n1")
            if [ -z "$latest" ]; then
                echo "Kein Backup auf dem Server gefunden." >&2
                exit 1
            fi
            local_file="${backup_dir}/$(basename "$latest")"
            if command -v termux-toast >/dev/null; then
                termux-toast "Lade Backup herunter"
            fi
            if command -v rsync >/dev/null; then
                rsync -ah --progress -e "ssh $ssh_opts" "${remote_user}@${remote_host}:$latest" "$local_file"
            else
                scp $ssh_opts "${remote_user}@${remote_host}:$latest" "$local_file"
            fi || {
                echo "Download fehlgeschlagen." >&2
                exit 1
            }
            mkdir -p "$monopolygo_dir"
            unzip -o "$local_file" -d "$monopolygo_dir" || {
                echo "Entpacken fehlgeschlagen." >&2
                exit 1
            }
            echo "Wiederherstellung abgeschlossen."
            if command -v termux-toast >/dev/null; then
                termux-toast "Restore fertig"
            fi
            break
            ;;
        "Abbrechen")
            echo "Abgebrochen."
            exit 0
            ;;
    esac
done
