package de.babixgo.monopolygo.models

import java.io.Serializable

/**
 * Datenmodell für einen gesicherten MonopolyGo Account
 * Basiert auf CSV-Struktur: AccountName,UserID,GAID,DeviceToken,AppSetID,SSAID,BackupPfad,Datum,ZuletztGespielt,Notiz
 */
data class BackupAccount(
    val accountName: String,           // Account-Name (eindeutig + Timestamp)
    val userId: String,                // Spieler-ID aus XML
    val gaid: String,                  // Google Advertising ID
    val deviceToken: String,           // Geräte-Token
    val appSetId: String,              // App-Set-ID
    val ssaid: String,                 // Android SSAID (16-stellig hex)
    val backupPath: String,            // Pfad zum Backup-Verzeichnis
    val datum: String,                 // Erstellungsdatum (yyyy-MM-dd)
    val zuletztGespielt: String,       // Zuletzt gespielt (yyyy-MM-dd HH:mm:ss)
    val notiz: String = ""             // Optionale Notiz
) : Serializable {

    /**
     * Prüft ob eine User ID vorhanden ist
     */
    fun hasValidUserId(): Boolean = userId != "N/A" && userId.isNotEmpty()

    /**
     * Gibt eine formatierte Zusammenfassung zurück
     */
    fun toSummary(): String = """
        Account: $accountName
        User ID: $userId
        GAID: $gaid
        Device Token: $deviceToken
        App Set ID: $appSetId
        SSAID: $ssaid
        Erstellt: $datum
        Zuletzt gespielt: $zuletztGespielt
        Backup: $backupPath
        Notiz: ${if (notiz.isEmpty()) "Keine" else notiz}
    """.trimIndent()

    /**
     * Gibt eine CSV-Zeile zurück
     */
    fun toCsvLine(): String {
        return "\"$accountName\",\"$userId\",\"$gaid\",\"$deviceToken\",\"$appSetId\",\"$ssaid\",\"$backupPath\",\"$datum\",\"$zuletztGespielt\",\"$notiz\""
    }

    /**
     * Status-Icon für UI
     */
    fun getStatusIcon(): String = if (backupPath.startsWith("/")) "✓" else "✗"
}
