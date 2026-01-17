# Modul Accountverwaltung

Dieses Modul dient dazu, gespeicherte Accounts von **Monopoly Go** zu sichern, wiederherzustellen und zu verwalten. Alle Skripte lassen sich einzeln per `bash Skriptname.sh` aufrufen.

## Wichtige Pfade
- `acc_eigene` – Speicherort der eigenen Accounts
- `acc_kunden` – Speicherort der Kundenaccounts
- `acc_datapath` – Datei des aktuell verwendeten Accounts
- `acc_infos` – Einstellungen der App mit der UserID
- `acc_eigene_infos` – Metadaten der eigenen Accounts
- `acc_kunden_infos` – Metadaten der Kundenaccounts

## Enthaltene Skripte

### 1_Account_wiederherstellen.sh
Zeigt alle vorhandenen Sicherungen an und kopiert die ausgewählten Dateien nach `acc_datapath`. So kann ein gespeicherter Account aktiviert werden.

### 2_Eigener_Account_sichern.sh
Liest die UserID aus den App-Einstellungen aus, fragt eine interne ID ab und erstellt einen Kurzlink. Anschließend werden die Accountdateien und Informationen unter `acc_eigene` gesichert.

### 2_Kunden_Account_sichern.sh
Speichert Metadaten für Kundenaccounts. Aus dem angegebenen Freundschaftslink wird automatisch die UserID extrahiert. Erstellt einen Ordner für den Kunden, aber speichert keine Accountdateien (nur Metadaten in `acc_kunden_infos`).

### 3_Infos_bearbeiten.sh
Listet vorhandene Einträge und erlaubt das Bearbeiten aller Felder. Änderungen werden direkt in den JSON-Dateien gespeichert.

### 4_Kopiere_Links.sh
Liest gespeicherte Kurzlinks aus `acc_eigene_infos` und kopiert die ausgewählten Links in die Zwischenablage.

### 5_Backup_und_restore.sh
Erstellt ein Zip-Archiv des Ordners `Accounts` und überträgt es auf einen Backup-Server oder lädt das neueste Archiv herunter und stellt es wieder her.
