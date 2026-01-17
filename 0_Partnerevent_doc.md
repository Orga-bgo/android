# Modul Partnerevent

Hier sollen künftig Skripte entstehen, die das Partnerevent automatisieren. Momentan liegt noch keine Umsetzung vor.

## Weiteres
- Vor dem starten von jedem Script in diesem Modul frage ab für welches Event geplant wird.
  - Liste dafür die Ordner in "/storage/emulated/0/MonopolyGo/Partnerevents/" auf und biete zusätzlich die Option an ein neues Event hinzuzufügen.
    - Neue Events sollen vom Nutzer benannt werden und werden als Ordner in "/storage/emulated/0/MonopolyGo/Partnerevents/" gespeichert.
    - Wird ein bereits erstelltes Event gewählt werden die sich darin befindlichen Dateien für die weitere Bearbeitung im Script genutzt.
  
## Geplante Inhalte
1. Kunden Account eingeben
   -> Abfrage: Bereits hinterlegt?
      Wenn ja: Kunden auflisten & nach Auswahl erfragen wie viele Plätze gebucht wurden
      Wenn nein: Kundenname / Accountname / Wie viele Plätze / Freundschaftslink
2. Eigene Accounts wählen
   -> Abfrage welche eigenen Accounts zur Verfügung stehen. (Mehrfachauswahl moglich)
3. Zuweisung
   -> Die gebuchten Plätze auf die eigenen Accounts verteilen. Jeder Kunde darf nur einen Platz pro eigenen Account belegen. Eigene Accounts haben max 4 Platze insgesamt zur Verfügung.
4. Hinzufügen
   -> Stelle eigenen Account wieder her. Starte Monopoly Go. Warte 10 Sekunden. Öffne den Freundschaftslink bom Kunden der den 1. Platz belegt. Wiederhole für Platze 2 bis 4. Nachsteeigenen Account wiederherstellen und Plätze belegen.
5. Zuweisung anzeigen
   -> Zeige die Zuweisung an. "Eigene Acc":"Zugewiesene Kunden 1-4"
